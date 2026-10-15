import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Categories from '../pages/Categories';
import { renderWithProviders, createMockResponse } from './test-utils';
import * as apiClient from '../api/client';

vi.mock('../api/client', () => ({
  apiFetch: vi.fn(),
}));

const mockCategories = [
  { id: 1, name: 'Salary', type: 'INCOME' },
  { id: 2, name: 'Freelance', type: 'INCOME' },
  { id: 3, name: 'Groceries', type: 'EXPENSE' },
  { id: 4, name: 'Rent', type: 'EXPENSE' },
];

describe('Categories Page', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('renders loading state initially and then lists income and expense categories', async () => {
    apiClient.apiFetch.mockResolvedValueOnce(
      createMockResponse(mockCategories, { ok: true, status: 200 })
    );

    renderWithProviders(<Categories />);

    expect(screen.getByText('Loading categories...')).toBeInTheDocument();

    expect(await screen.findByText('Salary')).toBeInTheDocument();
    expect(screen.getByText('Freelance')).toBeInTheDocument();
    expect(screen.getByText('Groceries')).toBeInTheDocument();
    expect(screen.getByText('Rent')).toBeInTheDocument();
  });

  it('validates empty category name when adding category', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockResolvedValueOnce(
      createMockResponse([], { ok: true, status: 200 })
    );

    renderWithProviders(<Categories />);
    await waitFor(() => {
      expect(screen.queryByText('Loading categories...')).not.toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /add category/i }));

    expect(await screen.findByText('Category name is required')).toBeInTheDocument();
  });

  it('successfully creates a new category and displays it', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch
      .mockResolvedValueOnce(createMockResponse(mockCategories, { ok: true, status: 200 }))
      .mockResolvedValueOnce(
        createMockResponse({ id: 5, name: 'Utilities', type: 'EXPENSE' }, { ok: true, status: 201 })
      );

    renderWithProviders(<Categories />);
    expect(await screen.findByText('Groceries')).toBeInTheDocument();

    await user.type(screen.getByLabelText(/category name/i), 'Utilities');
    await user.selectOptions(screen.getByLabelText(/category type/i), 'EXPENSE');
    await user.click(screen.getByRole('button', { name: /add category/i }));

    expect(apiClient.apiFetch).toHaveBeenCalledWith('/api/categories', {
      method: 'POST',
      body: JSON.stringify({ name: 'Utilities', type: 'EXPENSE' }),
    });

    expect(await screen.findByText('Utilities')).toBeInTheDocument();
  });

  it('shows plain-language error when creating a duplicate category (409)', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch
      .mockResolvedValueOnce(createMockResponse(mockCategories, { ok: true, status: 200 }))
      .mockResolvedValueOnce(
        createMockResponse({ message: 'Category already exists' }, { ok: false, status: 409 })
      );

    renderWithProviders(<Categories />);
    expect(await screen.findByText('Salary')).toBeInTheDocument();

    await user.type(screen.getByLabelText(/category name/i), 'Salary');
    await user.selectOptions(screen.getByLabelText(/category type/i), 'INCOME');
    await user.click(screen.getByRole('button', { name: /add category/i }));

    expect(
      await screen.findByText('A category named "Salary" already exists for income.')
    ).toBeInTheDocument();
  });

  it('edits an existing category name successfully', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch
      .mockResolvedValueOnce(createMockResponse(mockCategories, { ok: true, status: 200 }))
      .mockResolvedValueOnce(
        createMockResponse({ id: 3, name: 'Supermarket', type: 'EXPENSE' }, { ok: true, status: 200 })
      );

    renderWithProviders(<Categories />);
    expect(await screen.findByText('Groceries')).toBeInTheDocument();

    const editButtons = screen.getAllByRole('button', { name: /^edit$/i });
    await user.click(editButtons[2]);

    const editInput = screen.getByDisplayValue('Groceries');
    await user.clear(editInput);
    await user.type(editInput, 'Supermarket');
    await user.click(screen.getByRole('button', { name: /^save$/i }));

    expect(apiClient.apiFetch).toHaveBeenCalledWith('/api/categories/3', {
      method: 'PUT',
      body: JSON.stringify({ name: 'Supermarket', type: 'EXPENSE' }),
    });

    expect(await screen.findByText('Supermarket')).toBeInTheDocument();
    expect(screen.queryByText('Groceries')).not.toBeInTheDocument();
  });

  it('confirms and deletes a category with delete confirmation modal', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch
      .mockResolvedValueOnce(createMockResponse(mockCategories, { ok: true, status: 200 }))
      .mockResolvedValueOnce(createMockResponse(null, { ok: true, status: 204 }));

    renderWithProviders(<Categories />);
    expect(await screen.findByText('Rent')).toBeInTheDocument();

    const deleteButtons = screen.getAllByRole('button', { name: /^delete$/i });
    await user.click(deleteButtons[3]);

    const modalTitle = screen.getByRole('heading', { name: /delete category/i });
    expect(modalTitle).toBeInTheDocument();
    expect(screen.getByText(/"Rent"/)).toBeInTheDocument();

    const modalContainer = modalTitle.parentElement;
    const confirmButton = within(modalContainer).getByRole('button', { name: /^delete$/i });
    await user.click(confirmButton);

    expect(apiClient.apiFetch).toHaveBeenCalledWith('/api/categories/4', {
      method: 'DELETE',
    });

    await waitFor(() => {
      expect(screen.queryByText('Rent')).not.toBeInTheDocument();
    });
  });

  it('shows plain-language error when deleting a category in use (409)', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch
      .mockResolvedValueOnce(createMockResponse(mockCategories, { ok: true, status: 200 }))
      .mockResolvedValueOnce(
        createMockResponse({ message: 'Category in use' }, { ok: false, status: 409 })
      );

    renderWithProviders(<Categories />);
    expect(await screen.findByText('Groceries')).toBeInTheDocument();

    const deleteButtons = screen.getAllByRole('button', { name: /^delete$/i });
    await user.click(deleteButtons[2]);

    const modalTitle = screen.getByRole('heading', { name: /delete category/i });
    const modalContainer = modalTitle.parentElement;
    const confirmButton = within(modalContainer).getByRole('button', { name: /^delete$/i });
    await user.click(confirmButton);

    expect(
      await screen.findByText(
        'Cannot delete this category because it is being used by existing transactions.'
      )
    ).toBeInTheDocument();
  });

  it('displays error message when loading categories fails', async () => {
    apiClient.apiFetch.mockResolvedValueOnce(
      createMockResponse({ message: 'Server error' }, { ok: false, status: 500 })
    );

    renderWithProviders(<Categories />);

    expect(await screen.findByText('Server error')).toBeInTheDocument();
  });
});
