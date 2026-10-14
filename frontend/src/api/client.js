const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export async function apiFetch(endpoint, options = {}) {
  const token = localStorage.getItem('token');

  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401 && !endpoint.startsWith('/api/auth/login')) {
    localStorage.removeItem('token');
    window.dispatchEvent(new CustomEvent('auth:logout'));
  }

  return response;
}
