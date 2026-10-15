import { formatCurrency } from '../utils/formatters';

export default function DeleteBudgetModal({
  budget,
  onConfirm,
  onCancel,
  deleting,
  error,
}) {
  if (!budget) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div role="dialog" aria-modal="true" className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-2">Delete Budget</h3>
        <p className="text-sm text-gray-600 mb-3">
          Are you sure you want to delete the budget for{' '}
          <span className="font-semibold text-gray-800">&quot;{budget.categoryName}&quot;</span>?
        </p>

        <div className="bg-gray-50 p-3 rounded-md text-xs text-gray-700 space-y-1 mb-4">
          <div><span className="font-medium">Category:</span> {budget.categoryName}</div>
          <div><span className="font-medium">Monthly Limit:</span> {formatCurrency(budget.limitAmount)}</div>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-md">
            {error}
          </div>
        )}

        <div className="flex justify-end space-x-3">
          <button
            type="button"
            onClick={onCancel}
            disabled={deleting}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md disabled:opacity-50 transition-colors"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={deleting}
            className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-md disabled:opacity-50 transition-colors"
          >
            {deleting ? 'Deleting...' : 'Delete'}
          </button>
        </div>
      </div>
    </div>
  );
}
