# FinTrack

A personal finance tracker application.

## Tech Stack

### Backend
- Java 17
- Spring Boot 3
- Spring MVC
- Spring Data JPA (Hibernate)
- Spring Security
- Bean Validation
- MySQL
- Maven wrapper

### Frontend
- React.js (Vite)
- JavaScript
- Tailwind CSS

## Prerequisites
- Java 17 or higher
- MySQL running on port 3306
- A database named `fintrack`:
  ```sql
  CREATE DATABASE fintrack;
  ```

## Backend Configuration
1. Open `backend/src/main/resources/application-local.properties`.
2. Set your MySQL credentials:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=<your-mysql-password>
   ```

## Running the Backend
From the `backend` directory:

Linux / macOS:
```bash
./mvnw spring-boot:run
```

Windows:
```cmd
mvnw.cmd spring-boot:run
```

The backend server runs on port 8080.

## Health Check
Verify the server status:
```bash
curl http://localhost:8080/api/health
```
Expected response:
```text
OK
```

## Running Tests
1. Create the test database in MySQL:
   ```sql
   CREATE DATABASE fintrack_test;
   ```
2. From the `backend` directory, run:

   Linux / macOS:
   ```bash
   ./mvnw test
   ```

   Windows:
   ```cmd
   mvnw.cmd test
   ```

## API Endpoints

### Authentication

#### Register a New User
- **Endpoint**: `POST /api/auth/register`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "name": "Jane Doe",
    "email": "jane@example.com",
    "password": "password123"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane@example.com"
  }
  ```

#### User Login
- **Endpoint**: `POST /api/auth/login`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "email": "jane@example.com",
    "password": "password123"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsIn...",
    "name": "Jane Doe"
  }
  ```

### Users

#### Get Current User Profile
- **Endpoint**: `GET /api/users/me`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "name": "Jane Doe",
    "email": "jane@example.com"
  }
  ```

### Categories

#### Create a Category
- **Endpoint**: `POST /api/categories`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "name": "Groceries",
    "type": "EXPENSE"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "id": 1,
    "name": "Groceries",
    "type": "EXPENSE"
  }
  ```

#### Get All Categories for Current User
- **Endpoint**: `GET /api/categories`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Response**: `200 OK`
  ```json
  [
    {
      "id": 1,
      "name": "Groceries",
      "type": "EXPENSE"
    }
  ]
  ```

#### Get Category by ID
- **Endpoint**: `GET /api/categories/{id}`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "name": "Groceries",
    "type": "EXPENSE"
  }
  ```

#### Update Category
- **Endpoint**: `PUT /api/categories/{id}`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "name": "Food & Groceries",
    "type": "EXPENSE"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "name": "Food & Groceries",
    "type": "EXPENSE"
  }
  ```

#### Delete Category
- **Endpoint**: `DELETE /api/categories/{id}`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Response**: `204 No Content`

### Transactions

#### Create a Transaction
- **Endpoint**: `POST /api/transactions`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "amount": 45.50,
    "type": "EXPENSE",
    "date": "2026-03-15",
    "description": "Weekly groceries",
    "categoryId": 1
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "id": 1,
    "amount": 45.50,
    "type": "EXPENSE",
    "date": "2026-03-15",
    "description": "Weekly groceries",
    "categoryId": 1,
    "categoryName": "Groceries",
    "createdAt": "2026-03-15T10:00:00"
  }
  ```

#### Get All Transactions (Paginated and Filtered)
- **Endpoint**: `GET /api/transactions`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Query Parameters**:
  - `page` (optional, default `0`): 0-based page index
  - `size` (optional, default `10`): Page size
  - `from` (optional): Start date filter (`YYYY-MM-DD`)
  - `to` (optional): End date filter (`YYYY-MM-DD`)
  - `type` (optional): `INCOME` or `EXPENSE`
  - `categoryId` (optional): Category ID filter
- **Response**: `200 OK`
  ```json
  {
    "content": [
      {
        "id": 1,
        "amount": 45.50,
        "type": "EXPENSE",
        "date": "2026-03-15",
        "description": "Weekly groceries",
        "categoryId": 1,
        "categoryName": "Groceries",
        "createdAt": "2026-03-15T10:00:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true,
    "size": 10,
    "number": 0,
    "first": true,
    "numberOfElements": 1,
    "empty": false
  }
  ```

#### Get Transaction by ID
- **Endpoint**: `GET /api/transactions/{id}`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "amount": 45.50,
    "type": "EXPENSE",
    "date": "2026-03-15",
    "description": "Weekly groceries",
    "categoryId": 1,
    "categoryName": "Groceries",
    "createdAt": "2026-03-15T10:00:00"
  }
  ```

#### Update Transaction
- **Endpoint**: `PUT /api/transactions/{id}`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "amount": 55.00,
    "type": "EXPENSE",
    "date": "2026-03-16",
    "description": "Weekly groceries and snacks",
    "categoryId": 1
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "amount": 55.00,
    "type": "EXPENSE",
    "date": "2026-03-16",
    "description": "Weekly groceries and snacks",
    "categoryId": 1,
    "categoryName": "Groceries",
    "createdAt": "2026-03-15T10:00:00"
  }
  ```

#### Delete Transaction
- **Endpoint**: `DELETE /api/transactions/{id}`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Response**: `204 No Content`

### Budgets

#### Create a Budget
- **Endpoint**: `POST /api/budgets`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "categoryId": 1,
    "limitAmount": 500.00,
    "year": 2026,
    "month": 3
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "id": 1,
    "categoryId": 1,
    "categoryName": "Groceries",
    "limitAmount": 500.00,
    "year": 2026,
    "month": 3
  }
  ```

#### Get All Budgets for Current User
- **Endpoint**: `GET /api/budgets`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Query Parameters**:
  - `year` (optional): Filter by year (e.g., `2026`)
  - `month` (optional): Filter by month (`1`-`12`)
- **Response**: `200 OK`
  ```json
  [
    {
      "id": 1,
      "categoryId": 1,
      "categoryName": "Groceries",
      "limitAmount": 500.00,
      "year": 2026,
      "month": 3
    }
  ]
  ```

#### Get Budget by ID
- **Endpoint**: `GET /api/budgets/{id}`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "categoryId": 1,
    "categoryName": "Groceries",
    "limitAmount": 500.00,
    "year": 2026,
    "month": 3
  }
  ```

#### Update Budget
- **Endpoint**: `PUT /api/budgets/{id}`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Request Body**:
  ```json
  {
    "categoryId": 1,
    "limitAmount": 600.00,
    "year": 2026,
    "month": 3
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "id": 1,
    "categoryId": 1,
    "categoryName": "Groceries",
    "limitAmount": 600.00,
    "year": 2026,
    "month": 3
  }
  ```

#### Delete Budget
- **Endpoint**: `DELETE /api/budgets/{id}`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Response**: `204 No Content`

#### Get Monthly Budget Status
- **Endpoint**: `GET /api/budgets/status`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Query Parameters**:
  - `year` (required): Year (e.g., `2026`)
  - `month` (required): Month (`1`-`12`)
- **Response**: `200 OK`
  ```json
  [
    {
      "id": 1,
      "categoryId": 1,
      "categoryName": "Groceries",
      "limitAmount": 500.00,
      "amountSpent": 150.00,
      "remainingAmount": 350.00,
      "exceeded": false
    }
  ]
  ```

### Reports

#### Get Financial Summary
- **Endpoint**: `GET /api/reports/summary`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Query Parameters**:
  - `from` (required): Start date (`YYYY-MM-DD`)
  - `to` (required): End date (`YYYY-MM-DD`)
- **Response**: `200 OK`
  ```json
  {
    "totalIncome": 3500.00,
    "totalExpense": 400.00,
    "netBalance": 3100.00,
    "transactionCount": 4
  }
  ```

#### Get Spending or Income by Category
- **Endpoint**: `GET /api/reports/by-category`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Query Parameters**:
  - `from` (required): Start date (`YYYY-MM-DD`)
  - `to` (required): End date (`YYYY-MM-DD`)
  - `type` (required): `INCOME` or `EXPENSE`
- **Response**: `200 OK`
  ```json
  [
    {
      "categoryId": 1,
      "categoryName": "Groceries",
      "totalAmount": 200.00
    },
    {
      "categoryId": 2,
      "categoryName": "Dining",
      "totalAmount": 65.00
    }
  ]
  ```

#### Get Monthly Income and Expense Breakdown
- **Endpoint**: `GET /api/reports/monthly`
- **Access**: Authenticated (Requires Bearer token)
- **Headers**:
  - `Authorization: Bearer <token>`
- **Query Parameters**:
  - `year` (required): Year (e.g., `2026`)
- **Response**: `200 OK`
  ```json
  [
    {
      "month": 1,
      "totalIncome": 2500.00,
      "totalExpense": 300.00,
      "netBalance": 2200.00
    },
    {
      "month": 2,
      "totalIncome": 0,
      "totalExpense": 0,
      "netBalance": 0
    }
  ]
  ```





