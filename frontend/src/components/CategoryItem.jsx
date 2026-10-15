import { useState } from 'react';

export default function CategoryItem({ category, onUpdate, onDeleteClick }) {
  const [isEditing, setIsEditing] = useState(false);
  const [editName, setEditName] = useState(category.name);
  const [saving, setSaving] = useState(false);
  const [editError, setEditError] = useState('');

  function startEditing() {
    setEditName(category.name);
    setEditError('');
    setIsEditing(true);
  }

  function cancelEditing() {
    setIsEditing(false);
    setEditName(category.name);
    setEditError('');
  }

  async function handleSave() {
    const trimmed = editName.trim();
    if (!trimmed) {
      setEditError('Category name cannot be empty');
      return;
    }

    if (trimmed === category.name) {
      setIsEditing(false);
      return;
    }

    setSaving(true);
    setEditError('');
    const errorMsg = await onUpdate(category.id, trimmed);
    setSaving(false);

    if (errorMsg) {
      setEditError(errorMsg);
    } else {
      setIsEditing(false);
    }
  }

  function handleKeyDown(event) {
    if (event.key === 'Enter') {
      event.preventDefault();
      handleSave();
    } else if (event.key === 'Escape') {
      cancelEditing();
    }
  }

  return (
    <li className="py-3 px-4 flex flex-col sm:flex-row sm:items-center sm:justify-between border-b border-gray-100 last:border-b-0 hover:bg-gray-50 transition-colors">
      {isEditing ? (
        <div className="w-full">
          <div className="flex items-center space-x-2">
            <input
              type="text"
              aria-label="Category name"
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={saving}
              autoFocus
              className="flex-1 px-3 py-1.5 border border-blue-400 rounded-md text-sm focus:outline-none focus:ring-1 focus:ring-blue-500"
            />
            <button
              onClick={handleSave}
              disabled={saving}
              className="px-3 py-1.5 bg-blue-600 text-white text-xs font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {saving ? 'Saving...' : 'Save'}
            </button>
            <button
              onClick={cancelEditing}
              disabled={saving}
              className="px-3 py-1.5 bg-gray-200 text-gray-700 text-xs font-medium rounded-md hover:bg-gray-300 disabled:opacity-50 transition-colors"
            >
              Cancel
            </button>
          </div>
          {editError && (
            <p className="mt-1 text-xs text-red-600">{editError}</p>
          )}
        </div>
      ) : (
        <>
          <span className="text-sm font-medium text-gray-800 break-words">{category.name}</span>
          <div className="mt-2 sm:mt-0 flex items-center space-x-2">
            <button
              onClick={startEditing}
              className="px-2.5 py-1 text-xs font-medium text-blue-600 hover:text-blue-800 hover:bg-blue-50 rounded transition-colors"
            >
              Edit
            </button>
            <button
              onClick={() => onDeleteClick(category)}
              className="px-2.5 py-1 text-xs font-medium text-red-600 hover:text-red-800 hover:bg-red-50 rounded transition-colors"
            >
              Delete
            </button>
          </div>
        </>
      )}
    </li>
  );
}
