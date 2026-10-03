# RideLink - Ride Management Service

> **Member 3 | Microservice 3 of 4 | Port 8082**

---

## Overview

**RideLink** is a university group assignment implementing a backend-only ride-hailing platform using a microservices architecture. This repository contains **Member 3: Ride Management Service**.

The platform consists of four independent microservices:

| Service | Owner | Port | Responsibility |
|---|---|---|---|
| Account Service | Member 1 | 8081 | User registration, login, JWT issuance |
| Driver & Vehicle Service | Member 2 | 8081 | Driver profiles, vehicles, availability |
| **Ride Management Service** | **Member 3** | **8082** | **Ride lifecycle management** |
| Fare & Payment Service | Member 4 | 8083 | Fare calculation, payment processing |

---

## Ride Management Service Responsibilities

This service **only** manages the ride lifecycle:

- ✅ Create a ride request
- ✅ Assign a driver to a ride
- ✅ Track ride status through lifecycle stages
- ✅ Driver accepts / starts / completes a ride
- ✅ Passenger or authorized user cancels a ride
- ✅ Retrieve ride by ID
- ✅ Passenger ride history
- ✅ Driver ride history
- ✅ Rides by status

This service does **NOT** implement:
- ❌ User registration or login
- ❌ Password management
- ❌ Driver profile management
- ❌ Vehicle management
- ❌ Fare calculation
- ❌ Payment processing

---

## Technology Stack

| Technology | Version |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Web MVC | via Spring Boot |
| Spring Data MongoDB | via Spring Boot |
| Spring Security | via Spring Boot |
| JWT (JJWT) | 0.12.6 |
| Springdoc OpenAPI | 3.1.1 |
| JUnit 5 + Mockito | via Spring Boot Test |
| Maven | 3.9+ |

---

## Architecture

```
Account Service (8081)         Driver & Vehicle Service (8082)
      │                                    │
      │  JWT Token                         │  REST API
      │  (shared JWT_SECRET)               │  GET /api/drivers/{id}
      ▼                                    │  PUT /api/drivers/{id}/availability
┌─────────────────────────────────────────┐
│          Ride Management Service        │
│                Port 8082                │
│                                         │
│  RideController                         │
│       │                                 │
│  RideService (interface)                │
│       │                                 │
│  RideServiceImpl ─────── DriverServiceClient
│       │                                 │
│  RideRepository                         │
│       │                                 │
│  MongoDB: ridelink_ride_management_db   │
└─────────────────────────────────────────┘
```

### Database Boundary

Each service owns its own MongoDB database. **No cross-service database access is permitted.**

| Service | Database |
|---|---|
| Account Service | `ridelink_account_db` |
| Driver & Vehicle Service | `ridelink_driver_vehicle_db` |
| **Ride Management Service** | **`ridelink_ride_management_db`** |

Only IDs are stored as cross-service references (`passengerAccountId`, `driverId`, `vehicleId`).

---

## Project Structure

```
ride-management-service/
├── src/
│   ├── main/
│   │   ├── java/com/ridelink/ridemanagementservice/
│   │   │   ├── RideManagementServiceApplication.java
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── RestClientConfig.java
│   │   │   ├── controller/
│   │   │   │   └── RideController.java
│   │   │   ├── dto/
│   │   │   │   ├── AssignDriverRequest.java
│   │   │   │   ├── AvailabilityUpdateRequest.java
│   │   │   │   ├── CancelRideRequest.java
│   │   │   │   ├── CreateRideRequest.java
│   │   │   │   ├── DriverResponse.java
│   │   │   │   ├── LocationRequest.java
│   │   │   │   ├── LocationResponse.java
│   │   │   │   └── RideResponse.java
│   │   │   ├── exception/
│   │   │   │   ├── DriverNotAvailableException.java
│   │   │   │   ├── ExternalServiceException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── InvalidRideStateException.java
│   │   │   │   ├── ResourceConflictException.java
│   │   │   │   ├── RideNotFoundException.java
│   │   │   │   └── UnauthorizedRideAccessException.java
│   │   │   ├── model/
│   │   │   │   ├── Location.java
│   │   │   │   ├── Ride.java
│   │   │   │   └── RideStatus.java
│   │   │   ├── repository/
│   │   │   │   └── RideRepository.java
│   │   │   ├── security/
│   │   │   │   ├── AuthenticatedUserService.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtService.java
│   │   │   │   └── SecurityConfig.java
│   │   │   └── service/
│   │   │       ├── DriverServiceClient.java
│   │   │       ├── RideService.java
│   │   │       └── RideServiceImpl.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/ridelink/ridemanagementservice/
│           └── service/
│               └── RideServiceImplTest.java
├── .env                  ← actual credentials (NOT committed to git)
├── .env.example          ← placeholder template
├── .gitignore
├── pom.xml
└── README.md
```

---

## Setup Instructions

### Prerequisites

- Java 21
- Maven 3.9+
- MongoDB Atlas account (or local MongoDB)

### 1. Clone the repository

```bash
git clone <repository-url>
cd ride-management-service
```

### 2. Configure environment variables

Copy `.env.example` to `.env` and fill in your values:

```bash
cp .env.example .env
```

Edit `.env`:

```env
MONGO_URI=mongodb+srv://USERNAME:PASSWORD@HOST/ridelink_ride_management_db?appName=Cluster0
JWT_SECRET=your_shared_secret_same_as_account_service
DRIVER_SERVICE_URL=http://localhost:8082
FARE_PAYMENT_SERVICE_URL=http://localhost:8084
```

> **IMPORTANT:** The `JWT_SECRET` must be identical to the one configured in Account Service and Driver Service. All three services share the same secret to validate tokens.

### 3. Run the service

**Windows (PowerShell):**
```powershell
$env:MONGO_URI="mongodb+srv://..."; $env:JWT_SECRET="your_secret"; .\mvnw spring-boot:run
```

**Or with a `.env` loader:**
```bash
export $(cat .env | xargs) && ./mvnw spring-boot:run
```

**Or set environment variables in your IDE run configuration.**

---

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `MONGO_URI` | MongoDB Atlas connection string | `mongodb://localhost:27017/ridelink_ride_management_db` |
| `JWT_SECRET` | Shared JWT secret (same as Account Service) | *(required)* |
| `DRIVER_SERVICE_URL` | Driver & Vehicle Service base URL | `http://localhost:8082` |
| `FARE_PAYMENT_SERVICE_URL` | Fare & Payment Service base URL | `http://localhost:8084` |

---

## How to Run Tests

```bash
./mvnw clean test
```

This runs 15 unit tests covering all ride lifecycle scenarios. No MongoDB or Driver Service is required for tests — all dependencies are mocked.

---

## Swagger / API Documentation

Once running, access Swagger UI at:

```
http://localhost:8083/swagger-ui/index.html
```

or

```
http://localhost:8083/swagger-ui.html
```

Click **Authorize** and enter your JWT token:

```
Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## API Endpoints

| Method | Endpoint | Role Required | Description |
|---|---|---|---|
| `POST` | `/api/rides` | PASSENGER | Create a ride request |
| `GET` | `/api/rides/{rideId}` | Any authenticated | Get ride by ID |
| `PUT` | `/api/rides/{rideId}/assign-driver` | ADMIN | Assign driver and vehicle |
| `PUT` | `/api/rides/{rideId}/accept` | DRIVER | Driver accepts ride |
| `PUT` | `/api/rides/{rideId}/start` | DRIVER | Driver starts ride |
| `PUT` | `/api/rides/{rideId}/complete` | DRIVER | Driver completes ride |
| `PUT` | `/api/rides/{rideId}/cancel` | PASSENGER, DRIVER, ADMIN | Cancel ride |
| `GET` | `/api/rides/passenger/{passengerAccountId}` | PASSENGER, ADMIN | Passenger ride history |
| `GET` | `/api/rides/driver/{driverId}` | DRIVER, ADMIN | Driver ride history |
| `GET` | `/api/rides/status/{status}` | ADMIN | Rides by status |

---

## Ride Lifecycle

```
REQUESTED
    │
    ▼ (ADMIN assigns driver)
DRIVER_ASSIGNED
    │
    ▼ (DRIVER accepts)
ACCEPTED
    │
    ▼ (DRIVER starts)
IN_PROGRESS
    │
    ▼ (DRIVER completes)
COMPLETED
```

**Cancellation** is allowed from: `REQUESTED`, `DRIVER_ASSIGNED`, `ACCEPTED`

**Cancellation** is NOT allowed from: `IN_PROGRESS`, `COMPLETED`, `CANCELLED`

---

## JWT Authentication

This service **validates** JWT tokens issued by the Account Service. It does **not** issue tokens.

All services use the same `JWT_SECRET` environment variable.

### How it works:

1. Passenger logs in at Account Service: `POST http://localhost:8081/api/auth/login`
2. Account Service returns a JWT token
3. Client includes the JWT in all requests to Ride Management Service:
   ```
   Authorization: Bearer <jwt-token>
   ```
4. `JwtAuthenticationFilter` validates the token using the shared secret
5. User identity and role are extracted and stored in `SecurityContext`

---

## Role-Based Authorization

| Role | Permissions |
|---|---|
| `PASSENGER` | Create rides, view own rides, cancel own rides |
| `DRIVER` | Accept/start/complete assigned rides, view own ride history |
| `ADMIN` | Assign drivers, view all rides by status, manage all rides |

---

## Driver Service Integration

The Ride Management Service communicates with Driver & Vehicle Service via REST:

- `GET /api/drivers/{id}` — verify driver exists and check availability
- `PUT /api/drivers/{id}/availability` — update driver availability status

### Integration Contract

> **Note for Member 2 integration:** The DTOs `DriverResponse` and `AvailabilityUpdateRequest` in the `dto/` package define the expected API contract. Verify these field names against Member 2's Swagger at `http://localhost:8082/swagger-ui/index.html` and update if needed.

---

## Example Requests

### Create Ride

```http
POST http://localhost:8082/api/rides
Authorization: Bearer <PASSENGER_JWT>
Content-Type: application/json

{
  "pickupLocation": {
    "address": "123 Main Street, Colombo",
    "latitude": 6.9271,
    "longitude": 79.8612
  },
  "destinationLocation": {
    "address": "456 Park Avenue, Colombo",
    "latitude": 6.9147,
    "longitude": 79.8525
  }
}
```

### Assign Driver

```http
PUT http://localhost:8082/api/rides/{rideId}/assign-driver
Authorization: Bearer <ADMIN_JWT>
Content-Type: application/json

{
  "driverId": "driver-service-id-here",
  "vehicleId": "vehicle-id-here"
}
```

### Cancel Ride

```http
PUT http://localhost:8082/api/rides/{rideId}/cancel
Authorization: Bearer <PASSENGER_JWT>
Content-Type: application/json

{
  "reason": "Changed my mind"
}
```

---

## Example Response

```json
{
  "id": "64abc123def456",
  "passengerAccountId": "acc-111",
  "driverId": "drv-222",
  "vehicleId": "veh-333",
  "pickupLocation": {
    "address": "123 Main Street, Colombo",
    "latitude": 6.9271,
    "longitude": 79.8612
  },
  "destinationLocation": {
    "address": "456 Park Avenue, Colombo",
    "latitude": 6.9147,
    "longitude": 79.8525
  },
  "status": "ACCEPTED",
  "requestedAt": "2026-09-30T10:00:00",
  "acceptedAt": "2026-09-30T10:05:00",
  "startedAt": null,
  "completedAt": null,
  "cancelledAt": null,
  "cancellationReason": null
}
```

---

## Error Response Format

```json
{
  "timestamp": "2026-09-30T10:00:00.000",
  "status": 404,
  "error": "Not Found",
  "message": "Ride not found with ID: abc123",
  "path": "/api/rides/abc123"
}
```

---

## Postman Testing Workflow

### STEP 1 — Register Passenger (Account Service)

```http
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123",
  "role": "PASSENGER"
}
```

### STEP 2 — Login Passenger

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123"
}
```

Save the returned JWT token.

### STEP 3 — Create Ride

```http
POST http://localhost:8082/api/rides
Authorization: Bearer <PASSENGER_JWT>
Content-Type: application/json

{
  "pickupLocation": {
    "address": "123 Main St",
    "latitude": 6.9271,
    "longitude": 79.8612
  },
  "destinationLocation": {
    "address": "456 Park Ave",
    "latitude": 6.9147,
    "longitude": 79.8525
  }
}
```

Save the returned `rideId`.

### STEP 4 — Register & Login Admin, Create Driver (Account Service + Driver Service)

Register an ADMIN user and a DRIVER user at Account Service. Create a driver profile at Driver Service.

### STEP 5 — Assign Driver (Admin JWT)

```http
PUT http://localhost:8082/api/rides/{rideId}/assign-driver
Authorization: Bearer <ADMIN_JWT>
Content-Type: application/json

{
  "driverId": "driver-service-id",
  "vehicleId": "vehicle-id"
}
```

### STEP 6 — Login Driver at Account Service

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "email": "driver@example.com",
  "password": "DriverPass123"
}
```

### STEP 7 — Driver Accepts Ride

```http
PUT http://localhost:8082/api/rides/{rideId}/accept
Authorization: Bearer <DRIVER_JWT>
```

### STEP 8 — Driver Starts Ride

```http
PUT http://localhost:8082/api/rides/{rideId}/start
Authorization: Bearer <DRIVER_JWT>
```

### STEP 9 — Driver Completes Ride

```http
PUT http://localhost:8082/api/rides/{rideId}/complete
Authorization: Bearer <DRIVER_JWT>
```

### STEP 10 — Verify Ride Status

```http
GET http://localhost:8082/api/rides/{rideId}
Authorization: Bearer <any_valid_JWT>
```

---

## HTTP Status Codes

| Code | Meaning |
|---|---|
| 201 | Ride created |
| 200 | Success |
| 400 | Bad request / validation error |
| 401 | Missing or invalid JWT token |
| 403 | Authenticated but insufficient role/ownership |
| 404 | Ride not found |
| 405 | HTTP method not supported |
| 409 | Invalid ride state transition or conflict |
| 502 | Driver Service unreachable |
| 500 | Unexpected server error |

---

## Security Notes

- **Never commit** the `.env` file to git (it is listed in `.gitignore`)
- The `JWT_SECRET` must be **identical** across Account Service, Driver Service, and Ride Management Service
- All secrets must be passed as environment variables, never hard-coded
