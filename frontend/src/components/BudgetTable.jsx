import { formatCurrency } from '../utils/formatters';
import BudgetProgressBar from './BudgetProgressBar';

export default function BudgetTable({
  budgets,
  loading,
  onEdit,
  onDelete,
}) {
  if (loading) {
    return (
      <div className="bg-white p-12 text-center text-gray-500 text-sm border border-gray-200 rounded-lg">
        Loading budgets...
      </div>
    );
  }

  if (!budgets || budgets.length === 0) {
    return (
      <div className="bg-white p-12 text-center text-gray-500 text-sm border border-gray-200 rounded-lg">
        No budgets set for this month. Click &quot;Add Budget&quot; to get started.
      </div>
    );
  }

  return (
    <div className="bg-white shadow-sm border border-gray-200 rounded-lg overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200 text-left text-sm">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 font-semibold text-gray-700">Category</th>
            <th className="px-6 py-3 font-semibold text-gray-700">Limit</th>
            <th className="px-6 py-3 font-semibold text-gray-700">Spent</th>
            <th className="px-6 py-3 font-semibold text-gray-700">Remaining</th>
            <th className="px-6 py-3 font-semibold text-gray-700 min-w-[160px]">Progress</th>
            <th className="px-6 py-3 font-semibold text-gray-700 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 bg-white">
          {budgets.map((b) => (
            <tr key={b.id} className="hover:bg-gray-50 transition-colors">
              <td className="px-6 py-4 whitespace-nowrap font-medium text-gray-900">
                {b.categoryName}
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-gray-700">
                {formatCurrency(b.limitAmount)}
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-gray-700">
                {formatCurrency(b.amountSpent)}
              </td>
              <td
                className={`px-6 py-4 whitespace-nowrap font-medium ${
                  b.exceeded ? 'text-red-600' : 'text-gray-700'
                }`}
              >
                {formatCurrency(b.remainingAmount)}
              </td>
              <td className="px-6 py-4">
                <BudgetProgressBar
                  spent={b.amountSpent}
                  limit={b.limitAmount}
                  exceeded={b.exceeded}
                />
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-right space-x-2">
                <button
                  type="button"
                  onClick={() => onEdit(b)}
                  className="px-2.5 py-1 text-xs font-medium text-blue-600 hover:text-blue-800 hover:bg-blue-50 rounded transition-colors"
                >
                  Edit
                </button>
                <button
                  type="button"
                  onClick={() => onDelete(b)}
                  className="px-2.5 py-1 text-xs font-medium text-red-600 hover:text-red-800 hover:bg-red-50 rounded transition-colors"
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
