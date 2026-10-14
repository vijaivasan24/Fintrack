# FinTrack Frontend

Frontend client for the FinTrack personal finance tracking application. Built with React, Vite, Tailwind CSS, and react-router-dom.

## Features and Pages

- Categories: Manage income and expense categories with add, inline edit, and delete options.

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

2. Open `.env` and set `VITE_API_URL` to point to your running Spring Boot backend:
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
