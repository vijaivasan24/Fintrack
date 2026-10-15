export default function Pagination({ currentPage, totalPages, totalElements, onPageChange }) {
  if (totalPages <= 1 && totalElements === 0) {
    return null;
  }

  const isFirst = currentPage <= 0;
  const isLast = currentPage >= totalPages - 1;

  return (
    <div className="flex flex-col sm:flex-row items-center justify-between gap-3 px-4 py-3 bg-white border-t border-gray-200 sm:px-6">
      <div className="text-sm text-gray-700">
        <span>Page {totalPages === 0 ? 0 : currentPage + 1} of {totalPages || 1}</span>
        {totalElements > 0 && (
          <span className="ml-2 text-gray-500">({totalElements} total)</span>
        )}
      </div>

      <div className="inline-flex space-x-2">
        <button
          type="button"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={isFirst}
          className="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        >
          Previous
        </button>
        <button
          type="button"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={isLast}
          className="px-3 py-1.5 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        >
          Next
        </button>
      </div>
    </div>
  );
}
