import { useState } from 'react';

function BudgetModalContent({
  budget,
  categories,
  year,
  month,
  onClose,
  onSubmit,
  submitting,
  serverError,
}) {
  // Only expense categories are allowed for budgets
  const expenseCategories = categories.filter((c) => c.type === 'EXPENSE');

  const [categoryId, setCategoryId] = useState(() =>
    budget ? String(budget.categoryId || '') : expenseCategories.length > 0 ? String(expenseCategories[0].id) : ''
  );
  const [limitAmount, setLimitAmount] = useState(() =>
    budget ? String(budget.limitAmount || '') : ''
  );
  const [errors, setErrors] = useState({});

  function validate() {
    const errs = {};
    const parsed = parseFloat(limitAmount);
    if (!limitAmount || isNaN(parsed) || parsed <= 0) {
      errs.limitAmount = 'Please enter a valid limit amount greater than 0';
    }
    if (!categoryId) {
      errs.categoryId = 'Please select an expense category';
    }
    return errs;
  }

  async function handleSubmit(e) {
    e.preventDefault();
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setErrors({});
    const payload = {
      categoryId: Number(categoryId),
      limitAmount: parseFloat(limitAmount),
      year,
      month,
    };

    await onSubmit(payload);
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="bg-white rounded-lg shadow-xl max-w-md w-full p-6"
    >
      <div className="flex items-center justify-between mb-4 border-b pb-3">
        <h2 className="text-lg font-semibold text-gray-900">
          {budget ? 'Edit Budget' : 'Add Budget'}
        </h2>
        <button
          type="button"
          onClick={onClose}
          className="text-gray-400 hover:text-gray-600 text-xl font-bold"
        >
          ×
        </button>
      </div>

      {serverError && (
        <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-md">
          {serverError}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <div>
          <label htmlFor="budget-category" className="block text-sm font-medium text-gray-700 mb-1">
            Expense Category
          </label>
          <select
            id="budget-category"
            value={categoryId}
            onChange={(e) => {
              setCategoryId(e.target.value);
              if (errors.categoryId) setErrors((prev) => ({ ...prev, categoryId: '' }));
            }}
            className={`w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-1 bg-white ${
              errors.categoryId
                ? 'border-red-300 focus:ring-red-500'
                : 'border-gray-300 focus:ring-blue-500'
            }`}
          >
            <option value="">Select an expense category</option>
            {expenseCategories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
          {errors.categoryId && (
            <p className="mt-1 text-xs text-red-600">{errors.categoryId}</p>
          )}
          {expenseCategories.length === 0 && (
            <p className="mt-1 text-xs text-amber-600">
              No expense categories available. Create an expense category first.
            </p>
          )}
        </div>

        <div>
          <label htmlFor="budget-limit" className="block text-sm font-medium text-gray-700 mb-1">
            Monthly Limit (₹)
          </label>
          <input
            id="budget-limit"
            type="number"
            step="0.01"
            min="0.01"
            value={limitAmount}
            onChange={(e) => {
              setLimitAmount(e.target.value);
              if (errors.limitAmount) setErrors((prev) => ({ ...prev, limitAmount: '' }));
            }}
            placeholder="e.g. 15000"
            className={`w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-1 ${
              errors.limitAmount
                ? 'border-red-300 focus:ring-red-500'
                : 'border-gray-300 focus:ring-blue-500'
            }`}
          />
          {errors.limitAmount && (
            <p className="mt-1 text-xs text-red-600">{errors.limitAmount}</p>
          )}
        </div>

        <div className="flex justify-end space-x-3 pt-3 border-t">
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md disabled:opacity-50 transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-md disabled:opacity-50 transition-colors"
          >
            {submitting ? 'Saving...' : budget ? 'Save Changes' : 'Add Budget'}
          </button>
        </div>
      </form>
    </div>
  );
}

export default function BudgetModal(props) {
  if (!props.isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <BudgetModalContent
        key={props.budget ? props.budget.id : 'new'}
        {...props}
      />
    </div>
  );
}
