import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';
import { getCurrentMonthRange } from '../utils/formatters';
import DashboardSummaryCards from '../components/DashboardSummaryCards';
import CategorySpendingBars from '../components/CategorySpendingBars';
import YearlyBarChart from '../components/YearlyBarChart';
import DashboardBudgetStatus from '../components/DashboardBudgetStatus';
import RecentTransactions from '../components/RecentTransactions';

export default function Dashboard() {
  const [monthRange] = useState(() => getCurrentMonthRange());
  const [chartYear, setChartYear] = useState(() => new Date().getFullYear());

  // Section 1: Summary Cards
  const [summary, setSummary] = useState(null);
  const [summaryLoading, setSummaryLoading] = useState(true);
  const [summaryError, setSummaryError] = useState(null);

  // Section 2: Category Spending
  const [categoriesData, setCategoriesData] = useState([]);
  const [categoriesLoading, setCategoriesLoading] = useState(true);
  const [categoriesError, setCategoriesError] = useState(null);

  // Section 3: Yearly Bar Chart
  const [monthlyData, setMonthlyData] = useState([]);
  const [monthlyLoading, setMonthlyLoading] = useState(true);
  const [monthlyError, setMonthlyError] = useState(null);

  // Section 4: Budget Status
  const [budgetStatus, setBudgetStatus] = useState([]);
  const [budgetLoading, setBudgetLoading] = useState(true);
  const [budgetError, setBudgetError] = useState(null);

  // Section 5: Recent Transactions
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [transactionsLoading, setTransactionsLoading] = useState(true);
  const [transactionsError, setTransactionsError] = useState(null);

  // Fetch summary
  useEffect(() => {
    let ignore = false;
    async function loadSummary() {
      try {
        const res = await apiFetch(
          `/api/reports/summary?from=${monthRange.from}&to=${monthRange.to}`
        );
        if (!ignore) {
          if (res.ok) {
            setSummary(await res.json());
          } else {
            const err = await res.json().catch(() => null);
            setSummaryError(err?.message || 'Failed to load summary');
          }
        }
      } catch {
        if (!ignore) setSummaryError('Network error');
      } finally {
        if (!ignore) setSummaryLoading(false);
      }
    }
    loadSummary();
    return () => {
      ignore = true;
    };
  }, [monthRange]);

  // Fetch category spending
  useEffect(() => {
    let ignore = false;
    async function loadCategorySpending() {
      try {
        const res = await apiFetch(
          `/api/reports/by-category?from=${monthRange.from}&to=${monthRange.to}&type=EXPENSE`
        );
        if (!ignore) {
          if (res.ok) {
            setCategoriesData(await res.json());
          } else {
            const err = await res.json().catch(() => null);
            setCategoriesError(err?.message || 'Failed to load category spending');
          }
        }
      } catch {
        if (!ignore) setCategoriesError('Network error');
      } finally {
        if (!ignore) setCategoriesLoading(false);
      }
    }
    loadCategorySpending();
    return () => {
      ignore = true;
    };
  }, [monthRange]);

  // Fetch yearly monthly data (reloads when chartYear changes)
  useEffect(() => {
    let ignore = false;
    async function loadMonthly() {
      try {
        const res = await apiFetch(`/api/reports/monthly?year=${chartYear}`);
        if (!ignore) {
          if (res.ok) {
            setMonthlyData(await res.json());
          } else {
            const err = await res.json().catch(() => null);
            setMonthlyError(err?.message || 'Failed to load annual data');
          }
        }
      } catch {
        if (!ignore) setMonthlyError('Network error');
      } finally {
        if (!ignore) setMonthlyLoading(false);
      }
    }
    loadMonthly();
    return () => {
      ignore = true;
    };
  }, [chartYear]);

  // Fetch budget status for current month
  useEffect(() => {
    let ignore = false;
    async function loadBudgets() {
      try {
        const res = await apiFetch(
          `/api/budgets/status?year=${monthRange.year}&month=${monthRange.month}`
        );
        if (!ignore) {
          if (res.ok) {
            setBudgetStatus(await res.json());
          } else {
            const err = await res.json().catch(() => null);
            setBudgetError(err?.message || 'Failed to load budget status');
          }
        }
      } catch {
        if (!ignore) setBudgetError('Network error');
      } finally {
        if (!ignore) setBudgetLoading(false);
      }
    }
    loadBudgets();
    return () => {
      ignore = true;
    };
  }, [monthRange]);

  // Fetch 5 most recent transactions
  useEffect(() => {
    let ignore = false;
    async function loadRecentTransactions() {
      try {
        const res = await apiFetch('/api/transactions?page=0&size=5');
        if (!ignore) {
          if (res.ok) {
            const data = await res.json();
            setRecentTransactions(data.content || []);
          } else {
            const err = await res.json().catch(() => null);
            setTransactionsError(err?.message || 'Failed to load transactions');
          }
        }
      } catch {
        if (!ignore) setTransactionsError('Network error');
      } finally {
        if (!ignore) setTransactionsLoading(false);
      }
    }
    loadRecentTransactions();
    return () => {
      ignore = true;
    };
  }, []);

  function handleYearChange(newYear) {
    setMonthlyLoading(true);
    setChartYear(newYear);
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
        <p className="text-sm text-gray-500 mt-1">
          Financial overview and monthly spending summary
        </p>
      </div>

      {/* Section 1: Summary Cards */}
      <DashboardSummaryCards
        summary={summary}
        loading={summaryLoading}
        error={summaryError}
      />

      {/* Section 2: Yearly Bar Chart */}
      <YearlyBarChart
        year={chartYear}
        onYearChange={handleYearChange}
        monthlyData={monthlyData}
        loading={monthlyLoading}
        error={monthlyError}
      />

      {/* Middle row: Category spending + Budget status */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <CategorySpendingBars
          data={categoriesData}
          loading={categoriesLoading}
          error={categoriesError}
        />

        <DashboardBudgetStatus
          budgets={budgetStatus}
          loading={budgetLoading}
          error={budgetError}
        />
      </div>

      {/* Bottom row: Recent Transactions */}
      <RecentTransactions
        transactions={recentTransactions}
        loading={transactionsLoading}
        error={transactionsError}
      />
    </div>
  );
}
