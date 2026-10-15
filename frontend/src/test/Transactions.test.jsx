import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Transactions from '../pages/Transactions';
import { renderWithProviders, createMockResponse } from './test-utils';
import * as apiClient from '../api/client';

vi.mock('../api/client', () => ({
  apiFetch: vi.fn(),
}));

const mockCategories = [
  { id: 1, name: 'Salary', type: 'INCOME' },
  { id: 2, name: 'Groceries', type: 'EXPENSE' },
  { id: 3, name: 'Rent', type: 'EXPENSE' },
];

const mockTransactionsPage0 = {
  content: [
    {
      id: 101,
      amount: 50000.0,
      type: 'INCOME',
      date: '2026-10-01',
      description: 'Monthly Salary',
      categoryId: 1,
      categoryName: 'Salary',
    },
    {
      id: 102,
      amount: 1200.0,
      type: 'EXPENSE',
      date: '2026-10-05',
      description: 'Weekly Supermarket',
      categoryId: 2,
      categoryName: 'Groceries',
    },
  ],
  totalPages: 2,
  totalElements: 12,
  number: 0,
  size: 10,
};

const mockTransactionsPage1 = {
  content: [
    {
      id: 103,
      amount: 15000.0,
      type: 'EXPENSE',
      date: '2026-10-10',
      description: 'Apartment Rent',
      categoryId: 3,
      categoryName: 'Rent',
    },
  ],
  totalPages: 2,
  totalElements: 12,
  number: 1,
  size: 10,
};

describe('Transactions Page', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('renders rows, amounts with Indian rupee formatting and correct colors', async () => {
    apiClient.apiFetch.mockImplementation((endpoint) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      return createMockResponse(mockTransactionsPage0);
    });

    renderWithProviders(<Transactions />);

    expect(screen.getByText('Loading transactions...')).toBeInTheDocument();

    expect(await screen.findByText('Monthly Salary')).toBeInTheDocument();
    expect(screen.getByText('Weekly Supermarket')).toBeInTheDocument();

    // Check income amount (green)
    const incomeAmount = screen.getByText(/\+\s*₹\s*50,000\.00/);
    expect(incomeAmount).toBeInTheDocument();
    expect(incomeAmount).toHaveClass('text-green-600');

    // Check expense amount (red)
    const expenseAmount = screen.getByText(/-\s*₹\s*1,200\.00/);
    expect(expenseAmount).toBeInTheDocument();
    expect(expenseAmount).toHaveClass('text-red-600');
  });

  it('navigates pagination and calls API with the right page parameter', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockImplementation((endpoint) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      if (endpoint.includes('page=1')) {
        return createMockResponse(mockTransactionsPage1);
      }
      return createMockResponse(mockTransactionsPage0);
    });

    renderWithProviders(<Transactions />);
    expect(await screen.findByText('Monthly Salary')).toBeInTheDocument();

    expect(screen.getByText(/Page 1 of 2/)).toBeInTheDocument();

    const nextButton = screen.getByRole('button', { name: /next/i });
    await user.click(nextButton);

    await waitFor(() => {
      expect(apiClient.apiFetch).toHaveBeenCalledWith(expect.stringContaining('page=1'));
    });
    expect(await screen.findByText('Apartment Rent')).toBeInTheDocument();
  });

  it('resets filters to page 1 and sends query params on filter change and clear', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockImplementation((endpoint) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      return createMockResponse(mockTransactionsPage0);
    });

    renderWithProviders(<Transactions />);
    expect(await screen.findByText('Monthly Salary')).toBeInTheDocument();

    // Select type filter
    const typeFilter = screen.getByLabelText(/^type$/i);
    await user.selectOptions(typeFilter, 'EXPENSE');

    await waitFor(() => {
      expect(apiClient.apiFetch).toHaveBeenCalledWith(
        expect.stringContaining('type=EXPENSE')
      );
      expect(apiClient.apiFetch).toHaveBeenCalledWith(
        expect.stringContaining('page=0')
      );
    });

    // Clear filters
    const clearButton = screen.getByRole('button', { name: /clear filters/i });
    await user.click(clearButton);

    await waitFor(() => {
      expect(apiClient.apiFetch).toHaveBeenCalledWith(
        expect.stringContaining('/api/transactions?page=0&size=10')
      );
    });
  });

  it('category dropdown in modal follows the chosen transaction type', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockImplementation((endpoint) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      return createMockResponse(mockTransactionsPage0);
    });

    renderWithProviders(<Transactions />);
    expect(await screen.findByText('Monthly Salary')).toBeInTheDocument();

    // Open add modal
    await user.click(screen.getByRole('button', { name: /add transaction/i }));

    const modal = screen.getByRole('dialog');
    const typeSelect = within(modal).getByLabelText(/transaction type/i);
    const categorySelect = within(modal).getByLabelText(/category/i);

    // Default type is EXPENSE -> should show Groceries and Rent, but NOT Salary
    expect(typeSelect).toHaveValue('EXPENSE');
    expect(within(categorySelect).getByText('Groceries')).toBeInTheDocument();
    expect(within(categorySelect).getByText('Rent')).toBeInTheDocument();
    expect(within(categorySelect).queryByText('Salary')).not.toBeInTheDocument();

    // Switch type to INCOME -> should show Salary, but NOT Groceries or Rent
    await user.selectOptions(typeSelect, 'INCOME');
    expect(within(categorySelect).getByText('Salary')).toBeInTheDocument();
    expect(within(categorySelect).queryByText('Groceries')).not.toBeInTheDocument();
    expect(within(categorySelect).queryByText('Rent')).not.toBeInTheDocument();
  });

  it('creates a new transaction successfully', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockImplementation((endpoint, options) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      if (endpoint === '/api/transactions' && options?.method === 'POST') {
        return createMockResponse({ id: 104 }, { ok: true, status: 201 });
      }
      return createMockResponse(mockTransactionsPage0);
    });

    renderWithProviders(<Transactions />);
    expect(await screen.findByText('Monthly Salary')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /add transaction/i }));

    const modal = screen.getByRole('dialog');
    await user.type(within(modal).getByLabelText(/amount/i), '250.50');
    await user.selectOptions(within(modal).getByLabelText(/category/i), '2');
    await user.type(within(modal).getByLabelText(/description/i), 'Coffee beans');

    await user.click(within(modal).getByRole('button', { name: /add transaction/i }));

    await waitFor(() => {
      expect(apiClient.apiFetch).toHaveBeenCalledWith('/api/transactions', {
        method: 'POST',
        body: expect.stringContaining('"amount":250.5'),
      });
    });
  });

  it('edits an existing transaction and updates it', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockImplementation((endpoint, options) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      if (endpoint === '/api/transactions/101' && options?.method === 'PUT') {
        return createMockResponse({ id: 101 }, { ok: true, status: 200 });
      }
      return createMockResponse(mockTransactionsPage0);
    });

    renderWithProviders(<Transactions />);
    expect(await screen.findByText('Monthly Salary')).toBeInTheDocument();

    const editButtons = screen.getAllByRole('button', { name: /^edit$/i });
    await user.click(editButtons[0]);

    const modal = screen.getByRole('dialog');
    const amountInput = within(modal).getByLabelText(/amount/i);
    await user.clear(amountInput);
    await user.type(amountInput, '55000');

    await user.click(within(modal).getByRole('button', { name: /save changes/i }));

    await waitFor(() => {
      expect(apiClient.apiFetch).toHaveBeenCalledWith('/api/transactions/101', {
        method: 'PUT',
        body: expect.stringContaining('"amount":55000'),
      });
    });
  });

  it('deletes a transaction after confirmation in modal', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockImplementation((endpoint, options) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      if (endpoint === '/api/transactions/102' && options?.method === 'DELETE') {
        return createMockResponse(null, { ok: true, status: 204 });
      }
      return createMockResponse(mockTransactionsPage0);
    });

    renderWithProviders(<Transactions />);
    expect(await screen.findByText('Weekly Supermarket')).toBeInTheDocument();

    const deleteButtons = screen.getAllByRole('button', { name: /^delete$/i });
    await user.click(deleteButtons[1]);

    const modal = screen.getByRole('dialog');
    const confirmDeleteBtn = within(modal).getByRole('button', { name: /^delete$/i });
    await user.click(confirmDeleteBtn);

    await waitFor(() => {
      expect(apiClient.apiFetch).toHaveBeenCalledWith('/api/transactions/102', {
        method: 'DELETE',
      });
    });
  });

  it('displays error message on failed transactions fetch', async () => {
    apiClient.apiFetch.mockImplementation((endpoint) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      return createMockResponse({ message: 'Internal server error' }, { ok: false, status: 500 });
    });

    renderWithProviders(<Transactions />);

    expect(await screen.findByText('Internal server error')).toBeInTheDocument();
  });
});
