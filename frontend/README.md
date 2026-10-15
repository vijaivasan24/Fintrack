# FinTrack Frontend

Frontend client for the FinTrack personal finance tracking application. Built with React, Vite, Tailwind CSS, react-router-dom, and Vitest.

## Tech Stack

- React 19
- Vite
- Tailwind CSS
- React Router v7
- Vitest and React Testing Library

## Pages and Features

### Authentication
- Login: Email and password sign-in with client validation and error handling.
- Register: New user account registration with automated sign-in on success.
- Protected Routes: Guards authenticated routes and redirects unauthenticated users to the login screen.

### Dashboard
- Monthly Summary: Metric cards displaying total income, total expenses, net balance, and transaction count for the current month.
- Annual Overview: 12-month bar chart comparing income versus expenses across the year with a year selector.
- Spending by Category: Horizontal progress bars showing expense breakdown by category and percentage of total monthly spending.
- Budget Status: Quick overview of active monthly budgets with visual progress indicators.
- Recent Transactions: Overview of the five most recent transactions with rupee formatting.

### Transactions
- Transaction Table: Paginated list displaying date, description, category, type, and rupee-formatted amounts (green for income, red for expense).
- Server-side Pagination: Page controls with previous and next navigation and a page counter.
- Filters: Filter by date range (from and to), transaction type (Income/Expense), and category, with a clear filters button.
- Add and Edit: Modal dialog to create and update transactions, with category options dynamically filtered by transaction type.
- Delete Confirmation: Modal prompt confirming deletion before removing transactions.

### Categories
- Category Lists: Split into Income and Expense categories.
- Category Management: Create new categories, inline edit category names, and delete categories with confirmation and conflict handling.

### Budgets
- Month and Year Selector: View budget progress for any selected month and year.
- Budget Status Cards: Monthly limit, amount spent, remaining balance, and progress bars that turn red when spending exceeds the limit.
- Add and Edit: Modal dialog to configure category limits for expense categories.
- Delete Confirmation: Modal prompt before deleting budget limits.

## Prerequisites

- Node.js (version 18 or higher)
- npm

## Installation

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

## Configuration

The application requires the backend API base URL.

1. Copy the example environment file to `.env`:
   ```bash
   cp .env.example .env
   ```

2. Open `.env` and set `VITE_API_URL` to point to the Spring Boot backend server:
   ```env
   VITE_API_URL=http://localhost:8080
   ```

## Running the Application

- Start the development server:
  ```bash
  npm run dev
  ```
  The app will be available at `http://localhost:5173`.

- Build for production:
  ```bash
  npm run build
  ```

- Preview the production build:
  ```bash
  npm run preview
  ```

## Running Tests and Linting

- Run frontend tests:
  ```bash
  npm test
  ```

- Run linter:
  ```bash
  npm run lint
  ```
