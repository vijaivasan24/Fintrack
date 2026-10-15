import { formatCurrency, formatDate } from '../utils/formatters';

export default function TransactionTable({
  transactions,
  loading,
  onEdit,
  onDelete,
}) {
  if (loading) {
    return (
      <div className="bg-white p-12 text-center text-gray-500 text-sm border border-gray-200 rounded-lg">
        Loading transactions...
      </div>
    );
  }

  if (!transactions || transactions.length === 0) {
    return (
      <div className="bg-white p-12 text-center text-gray-500 text-sm border border-gray-200 rounded-lg">
        No transactions found.
      </div>
    );
  }

  return (
    <div className="bg-white shadow-sm border border-gray-200 rounded-lg overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200 text-left text-sm">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 font-semibold text-gray-700">Date</th>
            <th className="px-6 py-3 font-semibold text-gray-700">Description</th>
            <th className="px-6 py-3 font-semibold text-gray-700">Category</th>
            <th className="px-6 py-3 font-semibold text-gray-700">Type</th>
            <th className="px-6 py-3 font-semibold text-gray-700 text-right">Amount</th>
            <th className="px-6 py-3 font-semibold text-gray-700 text-right">Actions</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100 bg-white">
          {transactions.map((t) => {
            const isIncome = t.type === 'INCOME';
            return (
              <tr key={t.id} className="hover:bg-gray-50 transition-colors">
                <td className="px-6 py-4 whitespace-nowrap text-gray-700">
                  {formatDate(t.date)}
                </td>
                <td className="px-6 py-4 text-gray-900 max-w-xs truncate">
                  {t.description || '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-gray-700">
                  {t.categoryName || '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span
                    className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                      isIncome
                        ? 'bg-green-100 text-green-800'
                        : 'bg-red-100 text-red-800'
                    }`}
                  >
                    {isIncome ? 'Income' : 'Expense'}
                  </span>
                </td>
                <td
                  className={`px-6 py-4 whitespace-nowrap text-right font-semibold ${
                    isIncome ? 'text-green-600' : 'text-red-600'
                  }`}
                >
                  {isIncome ? '+' : '-'} {formatCurrency(t.amount)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right space-x-2">
                  <button
                    type="button"
                    onClick={() => onEdit(t)}
                    className="px-2.5 py-1 text-xs font-medium text-blue-600 hover:text-blue-800 hover:bg-blue-50 rounded transition-colors"
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    onClick={() => onDelete(t)}
                    className="px-2.5 py-1 text-xs font-medium text-red-600 hover:text-red-800 hover:bg-red-50 rounded transition-colors"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
