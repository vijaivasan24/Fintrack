import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Budgets from '../pages/Budgets';
import { renderWithProviders, createMockResponse } from './test-utils';
import * as apiClient from '../api/client';

vi.mock('../api/client', () => ({
  apiFetch: vi.fn(),
}));

const mockCategories = [
  { id: 1, name: 'Salary', type: 'INCOME' },
  { id: 2, name: 'Freelance', type: 'INCOME' },
  { id: 3, name: 'Groceries', type: 'EXPENSE' },
  { id: 4, name: 'Entertainment', type: 'EXPENSE' },
];

const mockBudgetStatuses = [
  {
    id: 1,
    categoryId: 3,
    categoryName: 'Groceries',
    limitAmount: 10000.0,
    amountSpent: 7000.0,
    remainingAmount: 3000.0,
    exceeded: false,
  },
  {
    id: 2,
    categoryId: 4,
    categoryName: 'Entertainment',
    limitAmount: 4000.0,
    amountSpent: 4500.0,
    remainingAmount: -500.0,
    exceeded: true,
  },
];

describe('Budgets Page', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('renders status rows with progress bar for exceeded and not exceeded', async () => {
    apiClient.apiFetch.mockImplementation((endpoint) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      return createMockResponse(mockBudgetStatuses);
    });

    renderWithProviders(<Budgets />);

    expect(screen.getByText('Loading budgets...')).toBeInTheDocument();

    expect(await screen.findByText('Groceries')).toBeInTheDocument();
    expect(screen.getByText('Entertainment')).toBeInTheDocument();

    // Check Groceries (not exceeded)
    expect(screen.getByText(/70% used/)).toBeInTheDocument();
    const progressFills = screen.getAllByTestId('budget-progress-fill');
    expect(progressFills[0]).toHaveClass('bg-blue-600');

    // Check Entertainment (exceeded)
    expect(screen.getByText('Exceeded')).toBeInTheDocument();
    expect(progressFills[1]).toHaveClass('bg-red-600');

    // Check Indian rupee currency amounts
    expect(screen.getByText(/₹\s*10,000\.00/)).toBeInTheDocument();
    expect(screen.getByText(/₹\s*7,000\.00/)).toBeInTheDocument();
    expect(screen.getByText(/₹\s*3,000\.00/)).toBeInTheDocument();
  });

  it('offers only expense categories in budget modal dropdown', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockImplementation((endpoint) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      return createMockResponse(mockBudgetStatuses);
    });

    renderWithProviders(<Budgets />);
    expect(await screen.findByText('Groceries')).toBeInTheDocument();

    // Click Add Budget
    await user.click(screen.getByRole('button', { name: /add budget/i }));

    const modal = screen.getByRole('dialog');
    const categorySelect = within(modal).getByLabelText(/expense category/i);

    // Expense categories must be present
    expect(within(categorySelect).getByText('Groceries')).toBeInTheDocument();
    expect(within(categorySelect).getByText('Entertainment')).toBeInTheDocument();

    // Income categories must NOT be present
    expect(within(categorySelect).queryByText('Salary')).not.toBeInTheDocument();
    expect(within(categorySelect).queryByText('Freelance')).not.toBeInTheDocument();
  });

  it('creates, edits, and deletes a budget successfully', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockImplementation((endpoint, options) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      if (endpoint === '/api/budgets' && options?.method === 'POST') {
        return createMockResponse({ id: 3 }, { ok: true, status: 201 });
      }
      if (endpoint === '/api/budgets/1' && options?.method === 'PUT') {
        return createMockResponse({ id: 1 }, { ok: true, status: 200 });
      }
      if (endpoint === '/api/budgets/2' && options?.method === 'DELETE') {
        return createMockResponse(null, { ok: true, status: 204 });
      }
      return createMockResponse(mockBudgetStatuses);
    });

    renderWithProviders(<Budgets />);
    expect(await screen.findByText('Groceries')).toBeInTheDocument();

    // 1. Create budget
    await user.click(screen.getByRole('button', { name: /add budget/i }));
    const addModal = screen.getByRole('dialog');
    await user.selectOptions(within(addModal).getByLabelText(/expense category/i), '4');
    await user.type(within(addModal).getByLabelText(/monthly limit/i), '6000');
    await user.click(within(addModal).getByRole('button', { name: /add budget/i }));

    await waitFor(() => {
      expect(apiClient.apiFetch).toHaveBeenCalledWith('/api/budgets', {
        method: 'POST',
        body: expect.stringContaining('"limitAmount":6000'),
      });
    });

    // 2. Edit budget (Groceries id=1)
    const editButtons = screen.getAllByRole('button', { name: /^edit$/i });
    await user.click(editButtons[0]);

    const editModal = screen.getByRole('dialog');
    const limitInput = within(editModal).getByLabelText(/monthly limit/i);
    await user.clear(limitInput);
    await user.type(limitInput, '12000');
    await user.click(within(editModal).getByRole('button', { name: /save changes/i }));

    await waitFor(() => {
      expect(apiClient.apiFetch).toHaveBeenCalledWith('/api/budgets/1', {
        method: 'PUT',
        body: expect.stringContaining('"limitAmount":12000'),
      });
    });

    // 3. Delete budget (Entertainment id=2)
    const deleteButtons = screen.getAllByRole('button', { name: /^delete$/i });
    await user.click(deleteButtons[1]);

    const deleteModal = screen.getByRole('dialog');
    expect(within(deleteModal).getByText(/"Entertainment"/)).toBeInTheDocument();
    await user.click(within(deleteModal).getByRole('button', { name: /^delete$/i }));

    await waitFor(() => {
      expect(apiClient.apiFetch).toHaveBeenCalledWith('/api/budgets/2', {
        method: 'DELETE',
      });
    });
  });

  it('reloads budget status when month changes', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockImplementation((endpoint) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      return createMockResponse(mockBudgetStatuses);
    });

    renderWithProviders(<Budgets />);
    expect(await screen.findByText('Groceries')).toBeInTheDocument();

    const monthSelect = screen.getByLabelText(/^month$/i);
    await user.selectOptions(monthSelect, '5'); // May

    await waitFor(() => {
      expect(apiClient.apiFetch).toHaveBeenCalledWith(
        expect.stringContaining('month=5')
      );
    });
  });

  it('displays plain-language error when creating duplicate budget (409)', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockImplementation((endpoint, options) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      if (endpoint === '/api/budgets' && options?.method === 'POST') {
        return createMockResponse(
          { message: 'Duplicate budget' },
          { ok: false, status: 409 }
        );
      }
      return createMockResponse(mockBudgetStatuses);
    });

    renderWithProviders(<Budgets />);
    expect(await screen.findByText('Groceries')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /add budget/i }));
    const modal = screen.getByRole('dialog');
    await user.type(within(modal).getByLabelText(/monthly limit/i), '5000');
    await user.click(within(modal).getByRole('button', { name: /add budget/i }));

    expect(
      await screen.findByText(
        'A budget already exists for this category in the selected month.'
      )
    ).toBeInTheDocument();
  });

  it('displays plain-language error when creating budget for non-expense category (400)', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockImplementation((endpoint, options) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      if (endpoint === '/api/budgets' && options?.method === 'POST') {
        return createMockResponse(
          { message: 'Budget can only be created for expense categories' },
          { ok: false, status: 400 }
        );
      }
      return createMockResponse(mockBudgetStatuses);
    });

    renderWithProviders(<Budgets />);
    expect(await screen.findByText('Groceries')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /add budget/i }));
    const modal = screen.getByRole('dialog');
    await user.type(within(modal).getByLabelText(/monthly limit/i), '5000');
    await user.click(within(modal).getByRole('button', { name: /add budget/i }));

    expect(
      await screen.findByText('Budget can only be created for expense categories.')
    ).toBeInTheDocument();
  });

  it('displays error banner when loading budgets status fails', async () => {
    apiClient.apiFetch.mockImplementation((endpoint) => {
      if (endpoint === '/api/categories') {
        return createMockResponse(mockCategories);
      }
      return createMockResponse({ message: 'Internal Server Error' }, { ok: false, status: 500 });
    });

    renderWithProviders(<Budgets />);

    expect(await screen.findByText('Internal Server Error')).toBeInTheDocument();
  });
});
