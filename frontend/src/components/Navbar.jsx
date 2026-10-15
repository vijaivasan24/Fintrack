import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/useAuth';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  function handleLogout() {
    logout();
    navigate('/login');
  }

  const linkClass = ({ isActive }) =>
    `px-3 py-2 rounded-md text-sm font-medium transition-colors ${
      isActive
        ? 'bg-blue-600 text-white'
        : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
    }`;

  const mobileLinkClass = ({ isActive }) =>
    `block px-3 py-2 rounded-md text-base font-medium transition-colors ${
      isActive
        ? 'bg-blue-600 text-white'
        : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
    }`;

  return (
    <nav className="bg-white border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex items-center space-x-8">
            <Link to="/dashboard" className="text-xl font-bold text-blue-600">
              FinTrack
            </Link>
            <div className="hidden md:flex space-x-2">
              <NavLink to="/dashboard" className={linkClass}>
                Dashboard
              </NavLink>
              <NavLink to="/transactions" className={linkClass}>
                Transactions
              </NavLink>
              <NavLink to="/categories" className={linkClass}>
                Categories
              </NavLink>
              <NavLink to="/budgets" className={linkClass}>
                Budgets
              </NavLink>
            </div>
          </div>

          <div className="hidden md:flex items-center space-x-4">
            {user?.name && (
              <span className="text-sm text-gray-600 font-medium">
                {user.name}
              </span>
            )}
            <button
              onClick={handleLogout}
              className="px-3 py-2 text-sm font-medium text-gray-700 hover:text-red-600 hover:bg-red-50 rounded-md transition-colors"
            >
              Logout
            </button>
          </div>

          {/* Mobile hamburger menu button */}
          <div className="flex items-center md:hidden">
            <button
              type="button"
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              aria-label="Toggle navigation menu"
              className="p-2 rounded-md text-gray-600 hover:text-gray-900 hover:bg-gray-100 focus:outline-none"
            >
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                {mobileMenuOpen ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                )}
              </svg>
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu dropdown */}
      {mobileMenuOpen && (
        <div className="md:hidden border-t border-gray-200 px-4 pt-2 pb-4 space-y-1">
          <NavLink
            to="/dashboard"
            onClick={() => setMobileMenuOpen(false)}
            className={mobileLinkClass}
          >
            Dashboard
          </NavLink>
          <NavLink
            to="/transactions"
            onClick={() => setMobileMenuOpen(false)}
            className={mobileLinkClass}
          >
            Transactions
          </NavLink>
          <NavLink
            to="/categories"
            onClick={() => setMobileMenuOpen(false)}
            className={mobileLinkClass}
          >
            Categories
          </NavLink>
          <NavLink
            to="/budgets"
            onClick={() => setMobileMenuOpen(false)}
            className={mobileLinkClass}
          >
            Budgets
          </NavLink>
          <div className="pt-3 border-t border-gray-100 flex items-center justify-between">
            {user?.name && (
              <span className="text-sm text-gray-600 font-medium">{user.name}</span>
            )}
            <button
              onClick={handleLogout}
              className="px-3 py-1.5 text-sm font-medium text-red-600 hover:bg-red-50 rounded-md transition-colors"
            >
              Logout
            </button>
          </div>
        </div>
      )}
    </nav>
  );
}
