export default function TransactionFilters({
  filters,
  categories,
  onFilterChange,
  onClear,
}) {
  const filteredCategories = filters.type
    ? categories.filter((c) => c.type === filters.type)
    : categories;

  function handleChange(field, value) {
    const updated = { ...filters, [field]: value };
    // If user changed type, and selected category does not match new type, reset category
    if (field === 'type' && updated.categoryId) {
      const match = categories.find(
        (c) => String(c.id) === String(updated.categoryId) && c.type === value
      );
      if (!match) {
        updated.categoryId = '';
      }
    }
    onFilterChange(updated);
  }

  return (
    <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200 mb-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
        <div>
          <label htmlFor="filter-from" className="block text-xs font-medium text-gray-700 mb-1">
            From Date
          </label>
          <input
            id="filter-from"
            type="date"
            value={filters.from || ''}
            onChange={(e) => handleChange('from', e.target.value)}
            className="w-full px-3 py-1.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>

        <div>
          <label htmlFor="filter-to" className="block text-xs font-medium text-gray-700 mb-1">
            To Date
          </label>
          <input
            id="filter-to"
            type="date"
            value={filters.to || ''}
            onChange={(e) => handleChange('to', e.target.value)}
            className="w-full px-3 py-1.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>

        <div>
          <label htmlFor="filter-type" className="block text-xs font-medium text-gray-700 mb-1">
            Type
          </label>
          <select
            id="filter-type"
            value={filters.type || ''}
            onChange={(e) => handleChange('type', e.target.value)}
            className="w-full px-3 py-1.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-white"
          >
            <option value="">All Types</option>
            <option value="INCOME">Income</option>
            <option value="EXPENSE">Expense</option>
          </select>
        </div>

        <div>
          <label htmlFor="filter-category" className="block text-xs font-medium text-gray-700 mb-1">
            Category
          </label>
          <select
            id="filter-category"
            value={filters.categoryId || ''}
            onChange={(e) => handleChange('categoryId', e.target.value)}
            className="w-full px-3 py-1.5 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-blue-500 bg-white"
          >
            <option value="">All Categories</option>
            {filteredCategories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>

        <div className="flex items-end">
          <button
            type="button"
            onClick={onClear}
            className="w-full px-3 py-1.5 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 border border-gray-300 rounded-md transition-colors"
          >
            Clear Filters
          </button>
        </div>
      </div>
    </div>
  );
}
