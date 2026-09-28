# RideLink Account Service

The **RideLink Account Service** is one of the core backend microservices of the RideLink system. It is responsible for user account registration, authentication, role management, profile management, and account status management.

The service supports **PASSENGER**, **DRIVER**, and **ADMIN** roles and uses JWT-based authentication and role-based authorization.

---

## Technologies Used

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data MongoDB
- MongoDB
- Spring Security
- JWT (JSON Web Token)
- Bean Validation
- Maven
- OpenAPI / Swagger
- JUnit
- Mockito

---

## Account Service Features

The Account Service provides the following functionality:

- Passenger account registration
- Driver account registration
- Secure user login
- JWT token issuance
- JWT-based authentication
- Role-based authorization
- View authenticated user profile
- Update authenticated user profile
- Admin role management
- Admin account status management
- Password encryption using BCrypt
- Request validation
- Centralized exception handling
- Swagger/OpenAPI documentation

---

## User Roles

The system supports the following roles:

- `PASSENGER`
- `DRIVER`
- `ADMIN`

Public registration supports Passenger and Driver accounts.

ADMIN privileges are protected and cannot be obtained through normal public registration.

---

## Account Statuses

User accounts can have the following statuses:

- `ACTIVE`
- `INACTIVE`
- `SUSPENDED`

Account status can be managed by an ADMIN user.

Suspended accounts are prevented from successfully authenticating.

---

## Prerequisites

Before running the project, ensure the following are installed:

- JDK 21
- MongoDB
- Git
- Maven or the included Maven Wrapper

Verify Java:

```bash
java -version
```

The project should run using Java 21.

On macOS, Java 21 can be selected using:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

---

## MongoDB Configuration

For local development, MongoDB should be running on:

```text
mongodb://localhost:27017
```

The Account Service uses the following database:

```text
ridelink_account_db
```

The application configuration uses:

```properties
spring.mongodb.uri=${MONGO_URI:mongodb://localhost:27017/ridelink_account_db}
```

A remote MongoDB deployment such as MongoDB Atlas can also be configured through the `MONGO_URI` environment variable.

---

## Environment Variables

The application supports the following environment variables:

| Environment Variable | Description |
|---|---|
| `MONGO_URI` | MongoDB connection URI |
| `JWT_SECRET` | Secret key used to sign JWT tokens |
| `JWT_EXPIRATION` | JWT expiration duration in milliseconds |

Example local MongoDB configuration:

```bash
export MONGO_URI="mongodb://localhost:27017/ridelink_account_db"
```

JWT configuration can be supplied using:

```bash
export JWT_SECRET="your-secure-jwt-secret"
export JWT_EXPIRATION="86400000"
```

`86400000` milliseconds represents 24 hours.

> Production credentials and secrets should never be committed to the Git repository.

---

## How to Run

### 1. Clone or extract the project

Navigate to the Account Service project directory:

```bash
cd account-service
```

### 2. Ensure MongoDB is running

For a local MongoDB installation, verify that the MongoDB service is running.

### 3. Build the project

```bash
./mvnw clean install
```

### 4. Run the application

```bash
./mvnw spring-boot:run
```

The service runs by default on:

```text
http://localhost:8080
```

---

## API Documentation

Swagger/OpenAPI documentation is available while the application is running.

### Swagger UI

```text
http://localhost:8080/swagger-ui.html
```

### OpenAPI JSON

```text
http://localhost:8080/v3/api-docs
```

Swagger can be used to inspect the Account Service REST API endpoints and request/response structures.

---

## API Endpoints

### Public Endpoints

#### Register User

```http
POST /api/auth/register
```

Registers a new Passenger or Driver account.

Example:

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "Password123",
  "phone": "0771234567",
  "role": "PASSENGER"
}
```

---

#### Login

```http
POST /api/auth/login
```

Authenticates a user and returns a JWT.

Example:

```json
{
  "email": "john@example.com",
  "password": "Password123"
}
```

Successful authentication returns a Bearer token.

---

## Authenticated User Endpoints

These endpoints require a valid JWT Bearer token.

### View Current Profile

```http
GET /api/users/me
```

Example authorization header:

```text
Authorization: Bearer <JWT_TOKEN>
```

---

### Update Current Profile

```http
PUT /api/users/me
```

Example:

```json
{
  "firstName": "John",
  "lastName": "Smith",
  "phone": "0712345678"
}
```

---

## Admin Endpoints

The following endpoints require an authenticated user with the `ADMIN` role.

### Update User Role

```http
PUT /api/users/{id}/role
```

Example:

```json
{
  "role": "DRIVER"
}
```

---

### Update Account Status

```http
PUT /api/users/{id}/status
```

Example:

```json
{
  "status": "SUSPENDED"
}
```

Supported statuses:

```text
ACTIVE
INACTIVE
SUSPENDED
```

---

## Authentication and Authorization

The Account Service uses JWT-based stateless authentication.

Public endpoints:

```text
/api/auth/register
/api/auth/login
```

Protected endpoints require:

```text
Authorization: Bearer <JWT_TOKEN>
```

Role-based authorization protects administrative operations.

Typical security responses include:

| Situation | HTTP Status |
|---|---:|
| Missing authentication | `401 Unauthorized` |
| Invalid login credentials | `401 Unauthorized` |
| Suspended account authentication | `401 Unauthorized` |
| Insufficient role/permission | `403 Forbidden` |
| Duplicate email registration | `409 Conflict` |
| Invalid request data | `400 Bad Request` |
| Unsupported HTTP method | `405 Method Not Allowed` |

---

## Validation

Registration requests are validated before user creation.

Examples include:

- Required first name
- Required last name
- Valid email format
- Minimum password requirements
- Valid 10-digit phone number
- Valid user role

Invalid requests return:

```text
400 Bad Request
```

---

## Password Security

Passwords are never stored as plain text.

The Account Service uses:

```text
BCryptPasswordEncoder
```

Passwords are hashed before being stored in MongoDB.

Password values are not included in normal user API responses.

---

## Error Handling

The application provides centralized exception handling for common API errors, including:

- User not found
- Duplicate email
- Invalid credentials
- Validation failures
- Forbidden access
- Unsupported HTTP methods
- Unexpected server errors

Errors are returned using consistent JSON responses.

---

## Running Automated Tests

Run all tests using:

```bash
./mvnw clean test
```

A successful test run should finish with:

```text
BUILD SUCCESS
```

The project includes automated tests for Account Service functionality.

---

## Manual API Testing

The API has also been tested using Postman.

Verified scenarios include:

- User registration
- User login
- JWT generation
- Authenticated profile retrieval
- Profile update
- Admin role update
- Admin account status update
- Suspended account authentication rejection
- Duplicate email rejection
- Registration validation
- Unauthorized access
- Forbidden role access
- Invalid credentials
- Unsupported HTTP methods

---

## Postman Testing Workflow

A typical Postman workflow is:

1. Register a Passenger or Driver.
2. Login using the registered credentials.
3. Copy the returned JWT.
4. Use the JWT as a Bearer Token.
5. Retrieve the authenticated profile.
6. Update the authenticated profile.
7. Login using an ADMIN account.
8. Use the ADMIN JWT to update a user's role.
9. Use the ADMIN JWT to update a user's account status.
10. Verify authorization and error scenarios.

---

## Project Structure

```text
src/main/java/com/ridelink/accountservice
├── config
├── controller
├── dto
├── exception
├── model
├── repository
├── security
├── service
└── AccountServiceApplication.java
```

The project follows a layered structure to separate API, business logic, persistence, security, configuration, and exception-handling responsibilities.

---

## Database

Database:

```text
ridelink_account_db
```

Primary collection:

```text
users
```

The Account Service owns its MongoDB persistence boundary and does not directly access databases belonging to other RideLink microservices.

---

## Security Notes

- Passwords are hashed using BCrypt.
- JWT is used for stateless authentication.
- Protected endpoints require authentication.
- Administrative operations require the ADMIN role.
- Suspended users cannot successfully authenticate.
- Sensitive production configuration should be supplied through environment variables.
- Secrets and credentials must not be committed to source control.

---

## Service Responsibility

This repository contains only the **RideLink Account Service**.

Its responsibilities are limited to:

- Account registration
- Authentication and token issuance
- Role management
- Profile management
- Account status management

Other RideLink business functions are implemented by separate microservices.