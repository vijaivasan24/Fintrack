import { useState } from 'react';

function TransactionModalContent({
  transaction,
  categories,
  onClose,
  onSubmit,
  submitting,
  serverError,
}) {
  const [amount, setAmount] = useState(() => (transaction ? String(transaction.amount || '') : ''));
  const [type, setType] = useState(() => (transaction ? transaction.type || 'EXPENSE' : 'EXPENSE'));
  const [date, setDate] = useState(() =>
    transaction ? transaction.date || '' : new Date().toISOString().slice(0, 10)
  );
  const [categoryId, setCategoryId] = useState(() =>
    transaction ? String(transaction.categoryId || '') : ''
  );
  const [description, setDescription] = useState(() =>
    transaction ? transaction.description || '' : ''
  );
  const [errors, setErrors] = useState({});

  // Filter categories to only match chosen type
  const availableCategories = categories.filter((c) => c.type === type);

  function handleTypeChange(newType) {
    setType(newType);
    const matching = categories.filter((c) => c.type === newType);
    if (!matching.some((c) => String(c.id) === String(categoryId))) {
      setCategoryId(matching.length > 0 ? String(matching[0].id) : '');
    }
  }

  function validate() {
    const errs = {};
    const parsedAmount = parseFloat(amount);
    if (!amount || isNaN(parsedAmount) || parsedAmount <= 0) {
      errs.amount = 'Please enter a valid amount greater than 0';
    }
    if (!date) {
      errs.date = 'Date is required';
    }
    if (!categoryId) {
      errs.categoryId = 'Please select a category';
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
      amount: parseFloat(amount),
      type,
      date,
      categoryId: Number(categoryId),
      description: description.trim() || null,
    };

    await onSubmit(payload);
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="bg-white rounded-lg shadow-xl max-w-lg w-full p-6 max-h-[90vh] overflow-y-auto"
    >
      <div className="flex items-center justify-between mb-4 border-b pb-3">
        <h2 className="text-lg font-semibold text-gray-900">
          {transaction ? 'Edit Transaction' : 'Add Transaction'}
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
          <label htmlFor="modal-type" className="block text-sm font-medium text-gray-700 mb-1">
            Transaction Type
          </label>
          <select
            id="modal-type"
            value={type}
            onChange={(e) => handleTypeChange(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-white"
          >
            <option value="EXPENSE">Expense</option>
            <option value="INCOME">Income</option>
          </select>
        </div>

        <div>
          <label htmlFor="modal-amount" className="block text-sm font-medium text-gray-700 mb-1">
            Amount (₹)
          </label>
          <input
            id="modal-amount"
            type="number"
            step="0.01"
            min="0.01"
            value={amount}
            onChange={(e) => {
              setAmount(e.target.value);
              if (errors.amount) setErrors((prev) => ({ ...prev, amount: '' }));
            }}
            placeholder="e.g. 500"
            className={`w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-1 ${
              errors.amount
                ? 'border-red-300 focus:ring-red-500'
                : 'border-gray-300 focus:ring-blue-500'
            }`}
          />
          {errors.amount && (
            <p className="mt-1 text-xs text-red-600">{errors.amount}</p>
          )}
        </div>

        <div>
          <label htmlFor="modal-date" className="block text-sm font-medium text-gray-700 mb-1">
            Date
          </label>
          <input
            id="modal-date"
            type="date"
            value={date}
            onChange={(e) => {
              setDate(e.target.value);
              if (errors.date) setErrors((prev) => ({ ...prev, date: '' }));
            }}
            className={`w-full px-3 py-2 border rounded-md text-sm focus:outline-none focus:ring-1 ${
              errors.date
                ? 'border-red-300 focus:ring-red-500'
                : 'border-gray-300 focus:ring-blue-500'
            }`}
          />
          {errors.date && (
            <p className="mt-1 text-xs text-red-600">{errors.date}</p>
          )}
        </div>

        <div>
          <label htmlFor="modal-category" className="block text-sm font-medium text-gray-700 mb-1">
            Category
          </label>
          <select
            id="modal-category"
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
            <option value="">Select a category</option>
            {availableCategories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
          {errors.categoryId && (
            <p className="mt-1 text-xs text-red-600">{errors.categoryId}</p>
          )}
          {availableCategories.length === 0 && (
            <p className="mt-1 text-xs text-amber-600">
              No {type.toLowerCase()} categories found. Please create one on the Categories page first.
            </p>
          )}
        </div>

        <div>
          <label htmlFor="modal-description" className="block text-sm font-medium text-gray-700 mb-1">
            Description (optional)
          </label>
          <input
            id="modal-description"
            type="text"
            maxLength={255}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="e.g. Monthly grocery shopping"
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
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
            {submitting ? 'Saving...' : transaction ? 'Save Changes' : 'Add Transaction'}
          </button>
        </div>
      </form>
    </div>
  );
}

export default function TransactionModal(props) {
  if (!props.isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <TransactionModalContent
        key={props.transaction ? props.transaction.id : 'new'}
        {...props}
      />
    </div>
  );
}
