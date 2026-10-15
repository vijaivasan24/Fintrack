import { formatCurrency } from '../utils/formatters';

export default function CategorySpendingBars({ data, loading, error }) {
  if (loading) {
    return (
      <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm text-center text-gray-500 text-sm">
        Loading category spending...
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm">
        <h2 className="text-base font-semibold text-gray-900 mb-2">Spending by Category</h2>
        <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-md">
          Failed to load category spending: {error}
        </div>
      </div>
    );
  }

  const items = Array.isArray(data) ? data : [];
  const totalExpense = items.reduce(
    (sum, item) => sum + (Number(item.totalAmount) || 0),
    0
  );

  return (
    <div className="bg-white p-6 rounded-lg border border-gray-200 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-base font-semibold text-gray-900">Spending by Category</h2>
        {totalExpense > 0 && (
          <span className="text-xs font-medium text-gray-500">
            Total: {formatCurrency(totalExpense)}
          </span>
        )}
      </div>

      {items.length === 0 || totalExpense === 0 ? (
        <p className="text-sm text-gray-500 text-center py-6">
          No expenses recorded for this month.
        </p>
      ) : (
        <div className="space-y-4">
          {items.map((item) => {
            const amount = Number(item.totalAmount) || 0;
            const percentage = totalExpense > 0
              ? Math.round((amount / totalExpense) * 100)
              : 0;

            return (
              <div key={item.categoryId || item.categoryName} className="space-y-1">
                <div className="flex justify-between items-center text-sm">
                  <span className="font-medium text-gray-800">{item.categoryName}</span>
                  <div className="text-right">
                    <span className="font-semibold text-gray-900">
                      {formatCurrency(amount)}
                    </span>
                    <span className="text-xs text-gray-500 ml-2">({percentage}%)</span>
                  </div>
                </div>
                <div className="w-full bg-gray-100 rounded-full h-2.5 overflow-hidden">
                  <div
                    data-testid="category-bar-fill"
                    className="bg-blue-600 h-2.5 rounded-full transition-all duration-300"
                    style={{ width: `${percentage}%` }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
