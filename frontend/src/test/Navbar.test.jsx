import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Navbar from '../components/Navbar';
import { renderWithProviders } from './test-utils';

describe('Navbar Component', () => {
  it('renders brand title and desktop navigation links', () => {
    renderWithProviders(<Navbar />, {
      auth: { token: 'mock-token', user: { name: 'Alice' } },
    });

    expect(screen.getByRole('link', { name: /^FinTrack$/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /^Dashboard$/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /^Transactions$/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /^Categories$/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /^Budgets$/i })).toBeInTheDocument();
    expect(screen.getByText('Alice')).toBeInTheDocument();
  });

  it('toggles mobile menu when clicking hamburger button', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Navbar />, {
      auth: { token: 'mock-token', user: { name: 'Alice' } },
    });

    const toggleButton = screen.getByRole('button', { name: /toggle navigation menu/i });
    expect(toggleButton).toBeInTheDocument();

    // Click toggle button to open mobile menu
    await user.click(toggleButton);

    // There should now be mobile navigation links
    const dashboardLinks = screen.getAllByRole('link', { name: /^Dashboard$/i });
    expect(dashboardLinks.length).toBe(2); // 1 desktop + 1 mobile
  });

  it('triggers logout when clicking logout button', async () => {
    const user = userEvent.setup();
    const logoutMock = vi.fn();
    renderWithProviders(<Navbar />, {
      auth: { token: 'mock-token', user: { name: 'Alice' }, logout: logoutMock },
    });

    const logoutButtons = screen.getAllByRole('button', { name: /logout/i });
    await user.click(logoutButtons[0]);

    expect(logoutMock).toHaveBeenCalled();
  });
});
