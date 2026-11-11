# RideLink - Fare & Payment Service (Member 4)

## Overview
This is the **Fare & Payment Service** for the RideLink microservices university project. It is responsible for calculating fares based on distance and duration, recording final fares, and processing simulated payments for completed rides.

## Technology Stack
- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data MongoDB
- Spring Security with JWT
- RestClient (for inter-service communication)
- Jakarta Bean Validation
- Springdoc OpenAPI (Swagger)
- JUnit 5 & Mockito

## Architecture & Database Ownership
This service owns its own isolated MongoDB database: `ridelink_fare_payment_db`.
It **does not** access databases of other services directly. All cross-service communication (e.g., verifying a ride's status) is performed via REST API calls using `RestClient`.

## Fare Calculation Rule
The final fare is calculated using a simple time-and-distance model. It enforces a minimum fare constraint.

**Configuration Variables:**
- Base Fare (`FARE_BASE_FARE`) = 150.00 LKR
- Per Km Rate (`FARE_PER_KM_RATE`) = 80.00 LKR
- Per Minute Rate (`FARE_PER_MINUTE_RATE`) = 5.00 LKR
- Minimum Fare (`FARE_MINIMUM_FARE`) = 250.00 LKR

**Formula:**
```
calculatedFare = baseFare + (distanceKm × perKmRate) + (durationMinutes × perMinuteRate)
finalFare = max(calculatedFare, minimumFare)
```

**Example:**
For a ride of 10 km taking 20 minutes:
- Base: 150
- Distance: 10 × 80 = 800
- Duration: 20 × 5 = 100
- Total: 150 + 800 + 100 = 1050 LKR

## Project Setup

### 1. Environment Variables
Copy `.env.example` to `.env` and provide appropriate values.

```env
MONGO_URI=mongodb+srv://<username>:<password>@cluster0.example.mongodb.net/ridelink_fare_payment_db?appName=Cluster0
JWT_SECRET=your_base64_encoded_jwt_secret_here
RIDE_SERVICE_URL=http://localhost:8083
FARE_BASE_FARE=150.00
FARE_PER_KM_RATE=80.00
FARE_PER_MINUTE_RATE=5.00
FARE_MINIMUM_FARE=250.00
```

### 2. Running the Application
```bash
./mvnw spring-boot:run
```
Service runs on port **8084**.

### 3. Running Tests
```bash
./mvnw clean test
```

## Security (JWT)
This service expects a Bearer token issued by the Account Service.
Endpoints under `/api/**` require valid authentication.
The service uses `RestClient` to propagate the JWT token when communicating with the Ride Management Service.

## API Documentation (Swagger)
When the application is running, Swagger UI is available at:
`http://localhost:8084/swagger-ui.html`

## Postman Workflow

### Step 1: Login
Obtain a token via Account Service (Port 8081).
```
POST http://localhost:8081/api/auth/login
```

### Step 2: Complete a Ride
Create and complete a ride via Ride Management Service (Port 8082). Ensure the ride reaches `COMPLETED` status.

### Step 3: Estimate Fare
```
POST http://localhost:8084/api/fares/estimate
Authorization: Bearer <JWT>
Body: { "distanceKm": 10, "estimatedDurationMinutes": 20 }
```

### Step 4: Calculate Final Fare
```
POST http://localhost:8084/api/fares/rides/{rideId}/final
Authorization: Bearer <JWT>
Body: { "actualDistanceKm": 10, "actualDurationMinutes": 20 }
```

### Step 5: Process Simulated Payment
```
POST http://localhost:8084/api/payments
Authorization: Bearer <JWT>
Body: { "rideId": "YOUR_RIDE_ID", "paymentMethod": "CARD" }
```
*(The client does not send the amount; it is securely retrieved from the finalized fare record).*

### Step 6: Get Receipt
```
GET http://localhost:8084/api/payments/{paymentId}/receipt
Authorization: Bearer <JWT>
```
