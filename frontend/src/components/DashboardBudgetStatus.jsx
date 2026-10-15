import { Link } from 'react-router-dom';
import { formatCurrency } from '../utils/formatters';
import BudgetProgressBar from './BudgetProgressBar';

export default function DashboardBudgetStatus({ budgets, loading, error }) {
  if (loading) {
    return (
      <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm text-center text-gray-500 text-sm">
        Loading budget status...
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm">
        <h2 className="text-base font-semibold text-gray-900 mb-2">Budget Status</h2>
        <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-md">
          Failed to load budget status: {error}
        </div>
      </div>
    );
  }

  const items = Array.isArray(budgets) ? budgets : [];

  return (
    <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-base font-semibold text-gray-900">Monthly Budgets</h2>
        <Link
          to="/budgets"
          className="text-xs font-medium text-blue-600 hover:text-blue-800"
        >
          Manage Budgets →
        </Link>
      </div>

      {items.length === 0 ? (
        <p className="text-sm text-gray-500 text-center py-6">
          No budgets set for this month.
        </p>
      ) : (
        <div className="space-y-4">
          {items.map((b) => (
            <div key={b.id || b.categoryId} className="p-3 bg-gray-50 rounded-lg space-y-2">
              <div className="flex justify-between items-center text-sm">
                <span className="font-semibold text-gray-900">{b.categoryName}</span>
                <span className="text-xs text-gray-500">
                  {formatCurrency(b.amountSpent)} of {formatCurrency(b.limitAmount)}
                </span>
              </div>
              <BudgetProgressBar
                spent={b.amountSpent}
                limit={b.limitAmount}
                exceeded={b.exceeded}
              />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
