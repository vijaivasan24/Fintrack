import { useEffect, useState } from 'react';
import { apiFetch } from '../api/client';
import { useAuth } from '../context/useAuth';

export default function Dashboard() {
  const { user, setUser } = useAuth();
  const [loading, setLoading] = useState(!user);
  const [error, setError] = useState('');

  useEffect(() => {
    async function fetchCurrentUser() {
      try {
        const response = await apiFetch('/api/users/me');
        if (response.ok) {
          const data = await response.json();
          setUser(data);
        } else {
          setError('Failed to load user profile');
        }
      } catch {
        setError('Unable to connect to the server');
      } finally {
        setLoading(false);
      }
    }

    fetchCurrentUser();
  }, [setUser]);

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <p className="text-gray-600">Loading dashboard...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {error && (
        <div className="p-4 bg-red-50 border border-red-200 text-red-700 rounded-md text-sm">
          {error}
        </div>
      )}

      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <h1 className="text-2xl font-bold text-gray-900">
          Welcome back, {user?.name || 'User'}!
        </h1>
        <p className="text-gray-600 mt-1">
          Logged in as <span className="font-medium text-gray-800">{user?.email}</span>
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">Transactions</h2>
          <p className="text-gray-500 text-sm mt-2">
            Track and review your income and expenses.
          </p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">Categories</h2>
          <p className="text-gray-500 text-sm mt-2">
            Organize transactions into custom categories.
          </p>
        </div>
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">Budgets</h2>
          <p className="text-gray-500 text-sm mt-2">
            Set and monitor monthly spending limits.
          </p>
        </div>
      </div>
    </div>
  );
}
