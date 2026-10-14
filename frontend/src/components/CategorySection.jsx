import CategoryItem from './CategoryItem';

export default function CategorySection({ title, type, categories, onUpdate, onDeleteClick }) {
  const isIncome = type === 'INCOME';
  const badgeColor = isIncome
    ? 'bg-green-100 text-green-800'
    : 'bg-red-100 text-red-800';

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden flex flex-col h-full">
      <div className="px-5 py-4 border-b border-gray-200 flex items-center justify-between bg-gray-50">
        <h3 className="text-base font-semibold text-gray-900">{title}</h3>
        <span className={`text-xs px-2.5 py-0.5 rounded-full font-medium ${badgeColor}`}>
          {categories.length} {categories.length === 1 ? 'category' : 'categories'}
        </span>
      </div>

      <div className="p-2 flex-1">
        {categories.length === 0 ? (
          <div className="text-center py-8 px-4 text-gray-500 text-sm">
            <p className="font-medium text-gray-600">No {isIncome ? 'income' : 'expense'} categories found</p>
            <p className="mt-1 text-xs text-gray-400">Add a category using the form above</p>
          </div>
        ) : (
          <ul className="divide-y divide-gray-100">
            {categories.map((category) => (
              <CategoryItem
                key={category.id}
                category={category}
                onUpdate={onUpdate}
                onDeleteClick={onDeleteClick}
              />
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
