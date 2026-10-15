import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Register from '../pages/Register';
import { renderWithProviders, createMockResponse } from './test-utils';
import * as apiClient from '../api/client';

vi.mock('../api/client', () => ({
  apiFetch: vi.fn(),
}));

describe('Register Page', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders register form with name, email, password and submit button', () => {
    renderWithProviders(<Register />, { auth: { token: null, isAuthenticated: false } });

    expect(screen.getByRole('heading', { name: /create your account/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/full name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /sign in here/i })).toBeInTheDocument();
  });

  it('validates required fields on submit', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Register />, { auth: { token: null, isAuthenticated: false } });

    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText('Name is required')).toBeInTheDocument();
    expect(screen.getByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
    expect(apiClient.apiFetch).not.toHaveBeenCalled();
  });

  it('validates password length must be at least 8 characters', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Register />, { auth: { token: null, isAuthenticated: false } });

    await user.type(screen.getByLabelText(/full name/i), 'Jane Doe');
    await user.type(screen.getByLabelText(/email address/i), 'jane@example.com');
    await user.type(screen.getByLabelText(/password/i), 'short');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(await screen.findByText('Password must be at least 8 characters')).toBeInTheDocument();
    expect(apiClient.apiFetch).not.toHaveBeenCalled();
  });

  it('submits registration and logs in user automatically on success', async () => {
    const user = userEvent.setup();
    const loginMock = vi.fn();

    // First call: register endpoint
    apiClient.apiFetch.mockResolvedValueOnce(
      createMockResponse({ id: 1, name: 'Jane Doe', email: 'jane@example.com' }, { ok: true, status: 201 })
    );
    // Second call: auto-login
    apiClient.apiFetch.mockResolvedValueOnce(
      createMockResponse({ token: 'new-token-123', name: 'Jane Doe' }, { ok: true, status: 200 })
    );

    renderWithProviders(<Register />, {
      auth: { token: null, isAuthenticated: false, login: loginMock },
    });

    await user.type(screen.getByLabelText(/full name/i), 'Jane Doe');
    await user.type(screen.getByLabelText(/email address/i), 'jane@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(apiClient.apiFetch).toHaveBeenCalledWith('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({
        name: 'Jane Doe',
        email: 'jane@example.com',
        password: 'password123',
      }),
    });

    await vi.waitFor(() => {
      expect(loginMock).toHaveBeenCalledWith('new-token-123', { name: 'Jane Doe' });
    });
  });

  it('displays plain-language error when email is already registered (409)', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockResolvedValueOnce(
      createMockResponse({ message: 'Email already exists' }, { ok: false, status: 409 })
    );

    renderWithProviders(<Register />, { auth: { token: null, isAuthenticated: false } });

    await user.type(screen.getByLabelText(/full name/i), 'Jane Doe');
    await user.type(screen.getByLabelText(/email address/i), 'jane@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(
      await screen.findByText(
        'This email address is already registered. Please log in or use a different email.'
      )
    ).toBeInTheDocument();
  });

  it('displays network error message on connection failure', async () => {
    const user = userEvent.setup();
    apiClient.apiFetch.mockRejectedValueOnce(new Error('Network error'));

    renderWithProviders(<Register />, { auth: { token: null, isAuthenticated: false } });

    await user.type(screen.getByLabelText(/full name/i), 'Jane Doe');
    await user.type(screen.getByLabelText(/email address/i), 'jane@example.com');
    await user.type(screen.getByLabelText(/password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /create account/i }));

    expect(
      await screen.findByText('Unable to connect to the server. Please try again later.')
    ).toBeInTheDocument();
  });
});
