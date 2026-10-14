import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';
import CategoryForm from '../components/CategoryForm';
import CategorySection from '../components/CategorySection';
import DeleteConfirmModal from '../components/DeleteConfirmModal';

export default function Categories() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [deletingCategory, setDeletingCategory] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  useEffect(() => {
    fetchCategories();
  }, []);

  async function fetchCategories() {
    setLoading(true);
    setPageError(null);
    try {
      const response = await apiFetch('/api/categories');
      if (response.ok) {
        const data = await response.json();
        setCategories(data);
      } else {
        const errorData = await response.json().catch(() => null);
        setPageError(errorData?.message || 'Failed to load categories');
      }
    } catch {
      setPageError('Network error. Unable to load categories.');
    } finally {
      setLoading(false);
    }
  }

  async function handleAddCategory({ name, type }) {
    setSubmitting(true);
    setPageError(null);
    try {
      const response = await apiFetch('/api/categories', {
        method: 'POST',
        body: JSON.stringify({ name, type }),
      });

      if (response.ok) {
        const newCategory = await response.json();
        setCategories((prev) => [...prev, newCategory]);
        return true;
      }

      const errorData = await response.json().catch(() => null);
      if (response.status === 409) {
        setPageError(`A category named "${name}" already exists for ${type.toLowerCase()}.`);
      } else if (errorData?.errors?.name) {
        setPageError(errorData.errors.name);
      } else {
        setPageError(errorData?.message || 'Failed to add category');
      }
      return false;
    } catch {
      setPageError('Network error. Unable to add category.');
      return false;
    } finally {
      setSubmitting(false);
    }
  }

  async function handleUpdateCategory(id, newName) {
    const category = categories.find((c) => c.id === id);
    if (!category) return 'Category not found';

    try {
      const response = await apiFetch(`/api/categories/${id}`, {
        method: 'PUT',
        body: JSON.stringify({ name: newName, type: category.type }),
      });

      if (response.ok) {
        const updated = await response.json();
        setCategories((prev) => prev.map((c) => (c.id === id ? updated : c)));
        return null;
      }

      const errorData = await response.json().catch(() => null);
      if (response.status === 409) {
        return `A category named "${newName}" already exists for ${category.type.toLowerCase()}.`;
      }
      if (errorData?.errors?.name) {
        return errorData.errors.name;
      }
      return errorData?.message || 'Failed to update category';
    } catch {
      return 'Network error. Unable to update category.';
    }
  }

  function openDeleteModal(category) {
    setDeletingCategory(category);
    setDeleteError(null);
  }

  function closeDeleteModal() {
    setDeletingCategory(null);
    setDeleteError(null);
  }

  async function handleConfirmDelete() {
    if (!deletingCategory) return;

    setDeleteLoading(true);
    setDeleteError(null);
    try {
      const response = await apiFetch(`/api/categories/${deletingCategory.id}`, {
        method: 'DELETE',
      });

      if (response.ok) {
        setCategories((prev) => prev.filter((c) => c.id !== deletingCategory.id));
        closeDeleteModal();
      } else {
        const errorData = await response.json().catch(() => null);
        if (response.status === 409) {
          setDeleteError('Cannot delete this category because it is being used by existing transactions.');
        } else {
          setDeleteError(errorData?.message || 'Failed to delete category');
        }
      }
    } catch {
      setDeleteError('Network error. Unable to delete category.');
    } finally {
      setDeleteLoading(false);
    }
  }

  const incomeCategories = categories.filter((c) => c.type === 'INCOME');
  const expenseCategories = categories.filter((c) => c.type === 'EXPENSE');

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Categories</h1>
          <p className="text-sm text-gray-500 mt-1">
            Organize your transactions into income and expense categories
          </p>
        </div>
      </div>

      {pageError && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-md flex justify-between items-center text-sm text-red-700">
          <span>{pageError}</span>
          <button
            onClick={() => setPageError(null)}
            className="text-red-500 hover:text-red-700 font-bold ml-4"
          >
            Dismiss
          </button>
        </div>
      )}

      <CategoryForm onSubmit={handleAddCategory} submitting={submitting} />

      {loading ? (
        <div className="bg-white p-12 rounded-lg border border-gray-200 text-center text-gray-500 text-sm">
          Loading categories...
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <CategorySection
            title="Income Categories"
            type="INCOME"
            categories={incomeCategories}
            onUpdate={handleUpdateCategory}
            onDeleteClick={openDeleteModal}
          />
          <CategorySection
            title="Expense Categories"
            type="EXPENSE"
            categories={expenseCategories}
            onUpdate={handleUpdateCategory}
            onDeleteClick={openDeleteModal}
          />
        </div>
      )}

      <DeleteConfirmModal
        category={deletingCategory}
        onConfirm={handleConfirmDelete}
        onCancel={closeDeleteModal}
        deleting={deleteLoading}
        error={deleteError}
      />
    </div>
  );
}
