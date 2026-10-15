import { formatCurrency } from '../utils/formatters';

export default function DashboardSummaryCards({ summary, loading, error }) {
  if (loading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm animate-pulse h-24"
          >
            <div className="h-4 bg-gray-200 rounded w-1/2 mb-3"></div>
            <div className="h-6 bg-gray-200 rounded w-3/4"></div>
          </div>
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
        Failed to load monthly summary: {error}
      </div>
    );
  }

  const totalIncome = summary?.totalIncome || 0;
  const totalExpense = summary?.totalExpense || 0;
  const netBalance = summary?.netBalance || 0;
  const transactionCount = summary?.transactionCount || 0;

  const isNetPositive = netBalance >= 0;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm">
        <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">
          Total Income
        </p>
        <p className="mt-2 text-2xl font-bold text-green-600">
          {formatCurrency(totalIncome)}
        </p>
      </div>

      <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm">
        <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">
          Total Expense
        </p>
        <p className="mt-2 text-2xl font-bold text-red-600">
          {formatCurrency(totalExpense)}
        </p>
      </div>

      <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm">
        <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">
          Net Balance
        </p>
        <p
          className={`mt-2 text-2xl font-bold ${
            isNetPositive ? 'text-gray-900' : 'text-red-600'
          }`}
        >
          {formatCurrency(netBalance)}
        </p>
      </div>

      <div className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm">
        <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">
          Transactions
        </p>
        <p className="mt-2 text-2xl font-bold text-gray-900">
          {transactionCount}
        </p>
      </div>
    </div>
  );
}
