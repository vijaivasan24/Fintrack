import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Login from '../pages/Login';
import { renderWithProviders, createMockResponse } from './test-utils';
import * as apiClient from '../api/client';

vi.mock('../api/client', () => ({
  apiFetch: vi.fn(),
}));

describe('Login Page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders login form with all inputs and submit button', () => {
    renderWithProviders(<Login />, { auth: { token: null, isAuthenticated: false } });

    expect(screen.getByRole('heading', { name: /sign in to your account/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /register here/i })).toBeInTheDocument();
  });

  it('displays client-side validation errors when submitting empty form', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Login />, { auth: { token: null, isAuthenticated: false } });

    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(apiClient.apiFetch).not.toHaveBeenCalled();
  });

  it('displays validation error for invalid email format', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Login />, { auth: { token: null, isAuthenticated: false } });

    await user.type(screen.getByLabelText(/email address/i), 'invalid-email');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText('Please enter a valid email address')).toBeInTheDocument();
    expect(apiClient.apiFetch).not.toHaveBeenCalled();
  });

  it('handles successful login and calls auth login', async () => {
    const user = userEvent.setup();
    const loginMock = vi.fn();
    apiClient.apiFetch.mockResolvedValueOnce(
      createMockResponse({ token: 'test-jwt-token', name: 'John Doe' }, { ok: true, status: 200 })
    );

    renderWithProviders(<Login />, {
      auth: { token: null, isAuthenticated: false, login: loginMock },
    });

    await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(apiClient.apiFetch).toHaveBeenCalledWith('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        email: 'john@example.com',
        password: 'password123',
      }),
    });

    await vi.waitFor(() => {
      expect(loginMock).toHaveBeenCalledWith('test-jwt-token', { name: 'John Doe' });
    });
  });

  it('displays plain-language error on 401 unauthorized', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockResolvedValueOnce(
      createMockResponse({ message: 'Bad credentials' }, { ok: false, status: 401 })
    );

    renderWithProviders(<Login />, { auth: { token: null, isAuthenticated: false } });

    await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'wrongpassword');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(
      await screen.findByText('Invalid email or password. Please try again.')
    ).toBeInTheDocument();
  });

  it('displays plain-language error on network failure', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockRejectedValueOnce(new Error('Network error'));

    renderWithProviders(<Login />, { auth: { token: null, isAuthenticated: false } });

    await user.type(screen.getByLabelText(/email address/i), 'john@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(
      await screen.findByText('Unable to connect to the server. Please try again later.')
    ).toBeInTheDocument();
  });
});
