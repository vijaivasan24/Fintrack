import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Dashboard from '../pages/Dashboard';
import { renderWithProviders, createMockResponse } from './test-utils';
import * as apiClient from '../api/client';

vi.mock('../api/client', () => ({
  apiFetch: vi.fn(),
}));

const mockSummary = {
  totalIncome: 75000.0,
  totalExpense: 25000.0,
  netBalance: 50000.0,
  transactionCount: 8,
};

const mockCategorySpending = [
  { categoryId: 1, categoryName: 'Groceries', totalAmount: 15000.0 },
  { categoryId: 2, categoryName: 'Entertainment', totalAmount: 10000.0 },
];

const mockMonthlyData = Array.from({ length: 12 }, (_, i) => ({
  month: i + 1,
  totalIncome: i === 9 ? 75000.0 : 0,
  totalExpense: i === 9 ? 25000.0 : 0,
  netBalance: i === 9 ? 50000.0 : 0,
}));

const mockBudgets = [
  {
    id: 1,
    categoryId: 1,
    categoryName: 'Groceries',
    limitAmount: 20000.0,
    amountSpent: 15000.0,
    remainingAmount: 5000.0,
    exceeded: false,
  },
];

const mockTransactions = {
  content: [
    {
      id: 101,
      date: '2026-10-12',
      description: 'Organic Market',
      categoryName: 'Groceries',
      type: 'EXPENSE',
      amount: 1500.0,
    },
  ],
};

function setupDefaultMocks() {
  apiClient.apiFetch.mockImplementation((endpoint) => {
    if (endpoint.includes('/api/reports/summary')) {
      return createMockResponse(mockSummary);
    }
    if (endpoint.includes('/api/reports/by-category')) {
      return createMockResponse(mockCategorySpending);
    }
    if (endpoint.includes('/api/reports/monthly')) {
      return createMockResponse(mockMonthlyData);
    }
    if (endpoint.includes('/api/budgets/status')) {
      return createMockResponse(mockBudgets);
    }
    if (endpoint.includes('/api/transactions')) {
      return createMockResponse(mockTransactions);
    }
    return createMockResponse({});
  });
}

describe('Dashboard Page', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('renders all sections from mocked data', async () => {
    setupDefaultMocks();

    renderWithProviders(<Dashboard />);

    // 1. Summary cards
    const incomeMatches = await screen.findAllByText(/₹\s*75,000\.00/);
    expect(incomeMatches[0]).toBeInTheDocument();

    const expenseMatches = screen.getAllByText(/₹\s*25,000\.00/);
    expect(expenseMatches[0]).toBeInTheDocument();

    const netMatches = screen.getAllByText(/₹\s*50,000\.00/);
    expect(netMatches[0]).toBeInTheDocument();
    expect(screen.getByText('8')).toBeInTheDocument();

    // 2. Category spending
    const groceryElements = screen.getAllByText('Groceries');
    expect(groceryElements.length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Entertainment')).toBeInTheDocument();
    expect(screen.getByText(/60%/)).toBeInTheDocument(); // 15k / 25k = 60%
    expect(screen.getByText(/40%/)).toBeInTheDocument(); // 10k / 25k = 40%

    // 3. Yearly Bar Chart
    expect(screen.getByText(/Income vs Expense/)).toBeInTheDocument();
    expect(screen.getByText('Oct')).toBeInTheDocument();

    // 4. Budget status
    expect(screen.getByText('Monthly Budgets')).toBeInTheDocument();
    expect(screen.getByText(/₹\s*15,000\.00 of ₹\s*20,000\.00/)).toBeInTheDocument();

    // 5. Recent transactions
    expect(screen.getByText('Organic Market')).toBeInTheDocument();
    expect(screen.getByText(/-\s*₹\s*1,500\.00/)).toBeInTheDocument();
  });

  it('renders empty states when no data exists across sections', async () => {
    apiClient.apiFetch.mockImplementation((endpoint) => {
      if (endpoint.includes('/api/reports/summary')) {
        return createMockResponse({ totalIncome: 0, totalExpense: 0, netBalance: 0, transactionCount: 0 });
      }
      if (endpoint.includes('/api/reports/by-category')) {
        return createMockResponse([]);
      }
      if (endpoint.includes('/api/reports/monthly')) {
        return createMockResponse([]);
      }
      if (endpoint.includes('/api/budgets/status')) {
        return createMockResponse([]);
      }
      if (endpoint.includes('/api/transactions')) {
        return createMockResponse({ content: [] });
      }
      return createMockResponse({});
    });

    renderWithProviders(<Dashboard />);

    expect(await screen.findByText('No expenses recorded for this month.')).toBeInTheDocument();
    expect(screen.getByText(/No financial activity recorded for/)).toBeInTheDocument();
    expect(screen.getByText('No budgets set for this month.')).toBeInTheDocument();
    expect(screen.getByText('No recent transactions found.')).toBeInTheDocument();
  });

  it('one failing section does not break the other sections', async () => {
    apiClient.apiFetch.mockImplementation((endpoint) => {
      if (endpoint.includes('/api/reports/summary')) {
        return createMockResponse({ message: 'Summary service unavailable' }, { ok: false, status: 503 });
      }
      if (endpoint.includes('/api/reports/by-category')) {
        return createMockResponse(mockCategorySpending);
      }
      if (endpoint.includes('/api/reports/monthly')) {
        return createMockResponse(mockMonthlyData);
      }
      if (endpoint.includes('/api/budgets/status')) {
        return createMockResponse(mockBudgets);
      }
      if (endpoint.includes('/api/transactions')) {
        return createMockResponse(mockTransactions);
      }
      return createMockResponse({});
    });

    renderWithProviders(<Dashboard />);

    // Summary section should show error banner
    expect(
      await screen.findByText(/Failed to load monthly summary: Summary service unavailable/)
    ).toBeInTheDocument();

    // Other sections should still render successfully
    const groceries = screen.getAllByText('Groceries');
    expect(groceries.length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Monthly Budgets')).toBeInTheDocument();
    expect(screen.getByText('Organic Market')).toBeInTheDocument();
  });

  it('reloads the yearly chart when year selector changes', async () => {
    const user = userEvent.setup();
    setupDefaultMocks();

    renderWithProviders(<Dashboard />);
    expect(await screen.findByText('Organic Market')).toBeInTheDocument();

    const yearSelect = screen.getByLabelText(/year:/i);
    await user.selectOptions(yearSelect, '2025');

    await waitFor(() => {
      expect(apiClient.apiFetch).toHaveBeenCalledWith('/api/reports/monthly?year=2025');
    });
  });
});
