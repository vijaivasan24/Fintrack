import { Link } from 'react-router-dom';
import { formatCurrency, formatDate } from '../utils/formatters';

export default function RecentTransactions({ transactions, loading, error }) {
  if (loading) {
    return (
      <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm text-center text-gray-500 text-sm">
        Loading recent transactions...
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm">
        <h2 className="text-base font-semibold text-gray-900 mb-2">Recent Transactions</h2>
        <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-md">
          Failed to load recent transactions: {error}
        </div>
      </div>
    );
  }

  const items = Array.isArray(transactions) ? transactions : [];

  return (
    <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-base font-semibold text-gray-900">Recent Transactions</h2>
        <Link
          to="/transactions"
          className="text-xs font-medium text-blue-600 hover:text-blue-800"
        >
          View All ({items.length}) →
        </Link>
      </div>

      {items.length === 0 ? (
        <p className="text-sm text-gray-500 text-center py-6">
          No recent transactions found.
        </p>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-100 text-sm">
            <thead>
              <tr className="text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                <th className="pb-3">Date</th>
                <th className="pb-3">Description</th>
                <th className="pb-3">Category</th>
                <th className="pb-3 text-right">Amount</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {items.map((t) => {
                const isIncome = t.type === 'INCOME';
                return (
                  <tr key={t.id} className="hover:bg-gray-50 transition-colors">
                    <td className="py-2.5 whitespace-nowrap text-gray-600">
                      {formatDate(t.date)}
                    </td>
                    <td className="py-2.5 text-gray-900 font-medium max-w-xs truncate">
                      {t.description || '-'}
                    </td>
                    <td className="py-2.5 whitespace-nowrap text-gray-600">
                      <span className="inline-block px-2 py-0.5 text-xs bg-gray-100 text-gray-700 rounded">
                        {t.categoryName || '-'}
                      </span>
                    </td>
                    <td
                      className={`py-2.5 whitespace-nowrap text-right font-semibold ${
                        isIncome ? 'text-green-600' : 'text-red-600'
                      }`}
                    >
                      {isIncome ? '+' : '-'} {formatCurrency(t.amount)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
