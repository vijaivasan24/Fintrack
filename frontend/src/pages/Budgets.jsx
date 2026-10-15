import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';
import MonthYearSelector from '../components/MonthYearSelector';
import BudgetTable from '../components/BudgetTable';
import BudgetModal from '../components/BudgetModal';
import DeleteBudgetModal from '../components/DeleteBudgetModal';

export default function Budgets() {
  const [year, setYear] = useState(() => new Date().getFullYear());
  const [month, setMonth] = useState(() => new Date().getMonth() + 1);

  const [budgets, setBudgets] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState(null);

  // Reload trigger
  const [refreshIndex, setRefreshIndex] = useState(0);

  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingBudget, setEditingBudget] = useState(null);
  const [modalSubmitting, setModalSubmitting] = useState(false);
  const [modalError, setModalError] = useState(null);

  // Delete modal state
  const [deletingBudget, setDeletingBudget] = useState(null);
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  // Load categories once
  useEffect(() => {
    let ignore = false;
    async function loadCategories() {
      try {
        const response = await apiFetch('/api/categories');
        if (response.ok) {
          const data = await response.json();
          if (!ignore && Array.isArray(data)) {
            setCategories(data);
          }
        }
      } catch {
        // Silently handled; categories dropdown will just be empty
      }
    }
    loadCategories();
    return () => {
      ignore = true;
    };
  }, []);

  // Fetch budget status for the selected year and month
  useEffect(() => {
    let ignore = false;

    async function fetchBudgetStatus() {
      try {
        const response = await apiFetch(`/api/budgets/status?year=${year}&month=${month}`);
        if (!ignore) {
          if (response.ok) {
            const data = await response.json();
            setBudgets(Array.isArray(data) ? data : []);
          } else {
            const errorData = await response.json().catch(() => null);
            setPageError(errorData?.message || 'Failed to load budget status');
          }
        }
      } catch {
        if (!ignore) {
          setPageError('Network error. Unable to load budget status.');
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    fetchBudgetStatus();

    return () => {
      ignore = true;
    };
  }, [year, month, refreshIndex]);

  function handlePeriodChange({ year: newYear, month: newMonth }) {
    setLoading(true);
    setYear(newYear);
    setMonth(newMonth);
  }

  function openAddModal() {
    setEditingBudget(null);
    setModalError(null);
    setIsModalOpen(true);
  }

  function openEditModal(budget) {
    setEditingBudget(budget);
    setModalError(null);
    setIsModalOpen(true);
  }

  function closeModal() {
    setIsModalOpen(false);
    setEditingBudget(null);
    setModalError(null);
  }

  async function handleModalSubmit(payload) {
    setModalSubmitting(true);
    setModalError(null);

    const isEdit = Boolean(editingBudget);
    const url = isEdit
      ? `/api/budgets/${editingBudget.id}`
      : '/api/budgets';
    const method = isEdit ? 'PUT' : 'POST';

    try {
      const response = await apiFetch(url, {
        method,
        body: JSON.stringify(payload),
      });

      if (response.ok) {
        closeModal();
        setLoading(true);
        setRefreshIndex((prev) => prev + 1);
      } else {
        const errorData = await response.json().catch(() => null);
        if (response.status === 409) {
          setModalError('A budget already exists for this category in the selected month.');
        } else if (response.status === 400 && errorData?.message?.includes('expense categories')) {
          setModalError('Budget can only be created for expense categories.');
        } else if (errorData?.errors) {
          const firstError = Object.values(errorData.errors)[0];
          setModalError(firstError || 'Validation failed');
        } else {
          setModalError(
            errorData?.message || `Failed to ${isEdit ? 'update' : 'create'} budget`
          );
        }
      }
    } catch {
      setModalError('Network error. Please try again later.');
    } finally {
      setModalSubmitting(false);
    }
  }

  function openDeleteModal(budget) {
    setDeletingBudget(budget);
    setDeleteError(null);
  }

  function closeDeleteModal() {
    setDeletingBudget(null);
    setDeleteError(null);
  }

  async function handleConfirmDelete() {
    if (!deletingBudget) return;

    setDeleteSubmitting(true);
    setDeleteError(null);

    try {
      const response = await apiFetch(`/api/budgets/${deletingBudget.id}`, {
        method: 'DELETE',
      });

      if (response.ok) {
        closeDeleteModal();
        setLoading(true);
        setRefreshIndex((prev) => prev + 1);
      } else {
        const errorData = await response.json().catch(() => null);
        setDeleteError(errorData?.message || 'Failed to delete budget');
      }
    } catch {
      setDeleteError('Network error. Unable to delete budget.');
    } finally {
      setDeleteSubmitting(false);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Budgets</h1>
          <p className="text-sm text-gray-500 mt-1">
            Set and track spending limits by category for each month
          </p>
        </div>
        <button
          type="button"
          onClick={openAddModal}
          className="inline-flex items-center justify-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 transition-colors"
        >
          Add Budget
        </button>
      </div>

      {pageError && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-md flex justify-between items-center text-sm text-red-700">
          <span>{pageError}</span>
          <button
            type="button"
            onClick={() => setPageError(null)}
            className="text-red-500 hover:text-red-700 font-bold ml-4"
          >
            Dismiss
          </button>
        </div>
      )}

      <MonthYearSelector
        year={year}
        month={month}
        onChange={handlePeriodChange}
      />

      <BudgetTable
        budgets={budgets}
        loading={loading}
        onEdit={openEditModal}
        onDelete={openDeleteModal}
      />

      <BudgetModal
        isOpen={isModalOpen}
        budget={editingBudget}
        categories={categories}
        year={year}
        month={month}
        onClose={closeModal}
        onSubmit={handleModalSubmit}
        submitting={modalSubmitting}
        serverError={modalError}
      />

      <DeleteBudgetModal
        budget={deletingBudget}
        onConfirm={handleConfirmDelete}
        onCancel={closeDeleteModal}
        deleting={deleteSubmitting}
        error={deleteError}
      />
    </div>
  );
}
