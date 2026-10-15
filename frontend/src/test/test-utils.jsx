import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { AuthContext } from '../context/authContextInstance';
import * as apiClient from '../api/client';

export function createMockResponse(data, { ok = true, status = 200 } = {}) {
  return Promise.resolve({
    ok,
    status,
    json: async () => data,
    text: async () => (typeof data === 'string' ? data : JSON.stringify(data)),
  });
}

export function renderWithProviders(ui, {
  initialEntries = ['/'],
  auth = {},
  ...renderOptions
} = {}) {
  const defaultAuth = {
    token: 'test-token',
    user: { id: 1, name: 'Test User', email: 'test@example.com' },
    setUser: vi.fn(),
    loading: false,
    login: vi.fn(),
    logout: vi.fn(),
    isAuthenticated: true,
    ...auth,
  };

  function Wrapper({ children }) {
    return (
      <AuthContext.Provider value={defaultAuth}>
        <MemoryRouter initialEntries={initialEntries}>
          {children}
        </MemoryRouter>
      </AuthContext.Provider>
    );
  }

  return {
    ...render(ui, { wrapper: Wrapper, ...renderOptions }),
    authValue: defaultAuth,
  };
}

export { apiClient };
