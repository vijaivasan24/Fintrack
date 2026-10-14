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


