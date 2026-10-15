import { useState, useEffect } from 'react';
import { apiFetch } from '../api/client';
import TransactionTable from '../components/TransactionTable';
import TransactionFilters from '../components/TransactionFilters';
import TransactionModal from '../components/TransactionModal';
import DeleteTransactionModal from '../components/DeleteTransactionModal';
import Pagination from '../components/Pagination';

export default function Transactions() {
  const [transactions, setTransactions] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState(null);

  // Pagination state (0-indexed for Spring Data)
  const [page, setPage] = useState(0);
  const pageSize = 10;
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filters state
  const [filters, setFilters] = useState({
    from: '',
    to: '',
    type: '',
    categoryId: '',
  });

  // Reload trigger
  const [refreshIndex, setRefreshIndex] = useState(0);

  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState(null);
  const [modalSubmitting, setModalSubmitting] = useState(false);
  const [modalError, setModalError] = useState(null);

  // Delete modal state
  const [deletingTransaction, setDeletingTransaction] = useState(null);
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

  // Fetch transactions when page, filters or refreshIndex changes
  useEffect(() => {
    let ignore = false;

    async function fetchTransactions() {
      const params = new URLSearchParams();
      params.set('page', String(page));
      params.set('size', String(pageSize));
      if (filters.from) params.set('from', filters.from);
      if (filters.to) params.set('to', filters.to);
      if (filters.type) params.set('type', filters.type);
      if (filters.categoryId) params.set('categoryId', filters.categoryId);

      try {
        const response = await apiFetch(`/api/transactions?${params.toString()}`);
        if (!ignore) {
          if (response.ok) {
            const data = await response.json();
            setTransactions(data.content || []);
            setTotalPages(data.totalPages || 0);
            setTotalElements(data.totalElements || 0);
          } else {
            const errorData = await response.json().catch(() => null);
            setPageError(errorData?.message || 'Failed to load transactions');
          }
        }
      } catch {
        if (!ignore) {
          setPageError('Network error. Unable to load transactions.');
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    fetchTransactions();

    return () => {
      ignore = true;
    };
  }, [page, filters, refreshIndex]);

  function handleFilterChange(newFilters) {
    setLoading(true);
    setFilters(newFilters);
    setPage(0);
  }

  function handleClearFilters() {
    setLoading(true);
    setFilters({ from: '', to: '', type: '', categoryId: '' });
    setPage(0);
  }

  function handlePageChange(newPage) {
    setLoading(true);
    setPage(newPage);
  }

  function openAddModal() {
    setEditingTransaction(null);
    setModalError(null);
    setIsModalOpen(true);
  }

  function openEditModal(transaction) {
    setEditingTransaction(transaction);
    setModalError(null);
    setIsModalOpen(true);
  }

  function closeModal() {
    setIsModalOpen(false);
    setEditingTransaction(null);
    setModalError(null);
  }

  async function handleModalSubmit(payload) {
    setModalSubmitting(true);
    setModalError(null);

    const isEdit = Boolean(editingTransaction);
    const url = isEdit
      ? `/api/transactions/${editingTransaction.id}`
      : '/api/transactions';
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
        if (errorData?.errors) {
          const firstError = Object.values(errorData.errors)[0];
          setModalError(firstError || 'Validation failed');
        } else {
          setModalError(
            errorData?.message || `Failed to ${isEdit ? 'update' : 'create'} transaction`
          );
        }
      }
    } catch {
      setModalError('Network error. Please try again later.');
    } finally {
      setModalSubmitting(false);
    }
  }

  function openDeleteModal(transaction) {
    setDeletingTransaction(transaction);
    setDeleteError(null);
  }

  function closeDeleteModal() {
    setDeletingTransaction(null);
    setDeleteError(null);
  }

  async function handleConfirmDelete() {
    if (!deletingTransaction) return;

    setDeleteSubmitting(true);
    setDeleteError(null);

    try {
      const response = await apiFetch(`/api/transactions/${deletingTransaction.id}`, {
        method: 'DELETE',
      });

      if (response.ok) {
        closeDeleteModal();
        setLoading(true);
        setRefreshIndex((prev) => prev + 1);
      } else {
        const errorData = await response.json().catch(() => null);
        setDeleteError(errorData?.message || 'Failed to delete transaction');
      }
    } catch {
      setDeleteError('Network error. Unable to delete transaction.');
    } finally {
      setDeleteSubmitting(false);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Transactions</h1>
          <p className="text-sm text-gray-500 mt-1">
            Track and filter all your income and expenses
          </p>
        </div>
        <button
          type="button"
          onClick={openAddModal}
          className="inline-flex items-center justify-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 transition-colors"
        >
          Add Transaction
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

      <TransactionFilters
        filters={filters}
        categories={categories}
        onFilterChange={handleFilterChange}
        onClear={handleClearFilters}
      />

      <div className="space-y-0">
        <TransactionTable
          transactions={transactions}
          loading={loading}
          onEdit={openEditModal}
          onDelete={openDeleteModal}
        />

        <Pagination
          currentPage={page}
          totalPages={totalPages}
          totalElements={totalElements}
          onPageChange={handlePageChange}
        />
      </div>

      <TransactionModal
        isOpen={isModalOpen}
        transaction={editingTransaction}
        categories={categories}
        onClose={closeModal}
        onSubmit={handleModalSubmit}
        submitting={modalSubmitting}
        serverError={modalError}
      />

      <DeleteTransactionModal
        transaction={deletingTransaction}
        onConfirm={handleConfirmDelete}
        onCancel={closeDeleteModal}
        deleting={deleteSubmitting}
        error={deleteError}
      />
    </div>
  );
}
