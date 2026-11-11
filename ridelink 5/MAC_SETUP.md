# RideLink — macOS Setup Guide

Beginner-friendly instructions to run all four RideLink microservices on an Apple Silicon MacBook.

This is **one cross-platform codebase** (Windows + macOS). You do **not** need a Mac-only fork.

---

## Port map (source of truth: `application.properties`)

| Service | Folder | Port | Database name (in `MONGO_URI`) |
|---------|--------|------|--------------------------------|
| Account Service | `account-service` | **8081** | `ridelink_account_db` |
| Driver & Vehicle Service | `driver-vehicle-service` | **8082** | `ridelink_driver_vehicle_db` |
| Ride Management Service | `ride-management-service` | **8083** | `ridelink_ride_management_db` |
| Fare & Payment Service | `fare-payment-service` | **8084** | `ridelink_fare_payment_db` |

---

## 1. Check Java 21

```bash
java -version
```

You want something like: `openjdk version "21.x.x"`.

If Java is missing, install with Homebrew:

```bash
brew install openjdk@21
```

---

## 2. Configure `JAVA_HOME` (macOS)

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Do **not** hard-code `JAVA_HOME` inside application source files.

Add the two `export` lines to `~/.zshrc` if you want them permanent.

---

## 3. Identify the four service folders

From the project root (`ridelink 2`):

```text
account-service/
driver-vehicle-service/
ride-management-service/
fare-payment-service/
```

---

## 4. Configure environment variables

### Required variables (from actual project config)

| Variable | Used by | Purpose |
|----------|---------|---------|
| `MONGO_URI` | All four | MongoDB Atlas (or local) connection string |
| `JWT_SECRET` | All four | **Same shared secret** for signing/validating JWTs |
| `JWT_EXPIRATION` | Account only | Token lifetime in ms (default `86400000`) |
| `ACCOUNT_SERVICE_URL` | Driver, Ride, Fare | Default `http://localhost:8081` |
| `DRIVER_SERVICE_URL` | Ride | Default `http://localhost:8082` |
| `RIDE_SERVICE_URL` | Fare | Default `http://localhost:8083` |
| `FARE_PAYMENT_SERVICE_URL` | Ride | Default `http://localhost:8084` |
| `FARE_BASE_FARE` | Fare | Optional (default `150.00`) |
| `FARE_PER_KM_RATE` | Fare | Optional (default `80.00`) |
| `FARE_PER_MINUTE_RATE` | Fare | Optional (default `5.00`) |
| `FARE_MINIMUM_FARE` | Fare | Optional (default `250.00`) |

### How Spring Boot loads config on this project

Each service has:

```properties
spring.config.import=optional:file:.env[.properties]
```

So a local `.env` file **in that service folder** can be loaded at startup.

You can also export variables in the shell:

```bash
cd account-service
set -a
source .env
set +a
```

Then start the service **in the same terminal**.

### Templates

Copy placeholders only (never commit real secrets):

```bash
cp account-service/.env.example account-service/.env
cp driver-vehicle-service/.env.example driver-vehicle-service/.env
cp ride-management-service/.env.example ride-management-service/.env
cp fare-payment-service/.env.example fare-payment-service/.env
```

Edit each `.env` with your Atlas URI and shared JWT secret.

`.env` is listed in `.gitignore`.

---

## 5. Configure MongoDB Atlas URIs

Each service must use its **own** database name in the URI path:

```text
.../ridelink_account_db
.../ridelink_driver_vehicle_db
.../ridelink_ride_management_db
.../ridelink_fare_payment_db
```

Do **not** point all four services at the same database.

Do **not** hard-code Atlas usernames/passwords in Java source.

### Atlas network access

If credentials are correct but connections still fail on this Mac, open MongoDB Atlas → **Network Access** and allow your current IP (or temporarily `0.0.0.0/0` for local testing only).

A URI that worked on a Windows laptop can fail here if Atlas rejects the new Mac’s public IP.

---

## 6. Configure the shared JWT secret

Account Service **issues** JWTs. Driver, Ride, and Fare **validate** them.

All four must use the **same** `JWT_SECRET` value.

Do not generate a different secret per service.

Do not commit the real secret.

---

## 7. Build each service (clean)

Maven wrappers use Unix LF line endings. On macOS use `./mvnw` or `sh mvnw` (not `mvnw.cmd`).

If needed:

```bash
chmod +x mvnw
```

If execution is blocked (quarantine / permissions):

```bash
sh mvnw clean test
```

First build will download dependencies — that is normal on a new Mac.

### Recommended order

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"

cd account-service && sh mvnw clean test && cd ..
cd driver-vehicle-service && sh mvnw clean test && cd ..
cd ride-management-service && sh mvnw clean test && cd ..
cd fare-payment-service && sh mvnw clean test && cd ..
```

Ignore copied Windows `target/` folders — always `clean` first.

---

## 8. Start each service (four separate terminals)

Preferred startup order (dependencies):

1. Account Service  
2. Driver & Vehicle Service  
3. Ride Management Service  
4. Fare & Payment Service  

A downstream service should still **start** if another service is briefly offline; Feign calls happen during business operations.

### Terminal 1 — Account

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
cd "/path/to/ridelink 2/account-service"
set -a && source .env && set +a
sh mvnw spring-boot:run
```

### Terminal 2 — Driver

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
cd "/path/to/ridelink 2/driver-vehicle-service"
set -a && source .env && set +a
sh mvnw spring-boot:run
```

### Terminal 3 — Ride

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
cd "/path/to/ridelink 2/ride-management-service"
set -a && source .env && set +a
sh mvnw spring-boot:run
```

### Terminal 4 — Fare

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
cd "/path/to/ridelink 2/fare-payment-service"
set -a && source .env && set +a
sh mvnw spring-boot:run
```

### Port already in use?

```bash
lsof -i :8081
lsof -i :8082
lsof -i :8083
lsof -i :8084
```

Identify the PID, then stop that process (or free the port) before restarting.

Do **not** randomly change RideLink service ports.

---

## 9. Verify each Swagger page

| Service | Swagger URL |
|---------|-------------|
| Account | http://localhost:8081/swagger-ui/index.html |
| Driver | http://localhost:8082/swagger-ui/index.html |
| Ride | http://localhost:8083/swagger-ui/index.html |
| Fare | http://localhost:8084/swagger-ui/index.html |

(Also redirected from `/swagger-ui.html` where configured.)

---

## 10–12. Login, copy JWT, Authorize in Swagger

1. Open Account Swagger → `POST /api/auth/register` (create a DRIVER account) or login.
2. `POST /api/auth/login` → copy the JWT from the response.
3. In each service’s Swagger UI → **Authorize** → `Bearer <token>` (or paste token only if the UI adds Bearer).

---

## 13. Test Driver → Account Feign communication

| | |
|--|--|
| **Source** | Driver Service |
| **Target** | Account Service |
| **When** | `POST /api/drivers` (create driver) |
| **Feign call** | `GET /api/users/{id}` |
| **Required JWT** | Valid Bearer token (forwarded to Account) |
| **Expected** | `201 Created` if account exists, role=`DRIVER`, status=`ACTIVE`; `404`/`400` if account invalid |

Flow:

```text
Client → POST http://localhost:8082/api/drivers
         (JWT)
Driver Service → Feign GET http://localhost:8081/api/users/{accountId}
Account Service → returns user profile
Driver Service → creates driver profile
```

---

## 14. Test Ride → Driver Feign communication

| | |
|--|--|
| **Source** | Ride Service |
| **Target** | Driver Service |
| **Example ops** | Assign driver / update availability (business endpoints under `/api/rides/**`) |
| **Feign calls** | `GET /api/drivers/{id}`, `GET /api/drivers/account/{accountId}`, `PUT /api/drivers/{id}/availability` |
| **Required JWT** | Authenticated passenger/driver token (forwarded) |
| **Expected** | Successful ride state change when driver is `AVAILABLE`; Feign errors if Driver is down |

---

## 15. Test Fare → Ride Feign communication

| | |
|--|--|
| **Source** | Fare & Payment Service |
| **Target** | Ride Service |
| **Endpoint** | `POST /api/fares/rides/{rideId}/final` |
| **Feign call** | `GET /api/rides/{rideId}` |
| **Required JWT** | Passenger (owner) or admin |
| **Expected** | `200` with fare when ride status is `COMPLETED`; error if ride missing / not completed |

---

## 16. Common macOS errors and fixes

### `permission denied: ./mvnw`

```bash
chmod +x mvnw
# or
sh mvnw clean test
```

### `operation not permitted: ./mvnw` (quarantine)

```bash
xattr -d com.apple.quarantine mvnw
sh mvnw clean test
```

### Port already in use

```bash
lsof -i :8081
```

Stop the conflicting process; keep RideLink on 8081–8084.

### MongoDB connection timeout / auth failure

Distinguish:

1. Wrong URI / database name  
2. Wrong username/password  
3. Atlas **Network Access** blocking this Mac’s IP  
4. DNS / TLS / VPN issues (e.g. `NXDOMAIN` or `nodename nor servname provided`)  
5. Service pointing at the wrong DB name  
6. Windows CRLF in `.env` (fix with `sed -i '' $'s/\r$//' .env`)

Do not commit credentials to fix connectivity.

**DNS check (safe — no secrets):**

```bash
nslookup <your-cluster-host-from-uri>
```

If DNS fails (`NXDOMAIN`), fix Wi‑Fi/VPN/DNS first — Feign and JWT cannot be live-tested until Atlas is reachable.

### `401 Unauthorized`

Missing/expired JWT, or Swagger Authorize not set.

### `403 Forbidden`

Valid JWT but wrong role for that endpoint.

### Feign connection refused

Target service not running, or wrong `*_SERVICE_URL` / port.

### JWT signature invalid

`JWT_SECRET` differs between Account (issuer) and the validating service. Use the **same** secret everywhere.

### CRLF / bad interpreter on `mvnw`

Wrappers in this project use LF. If you still see `/bin/bash^M`, convert:

```bash
sed -i '' $'s/\r$//' mvnw
```

---

## Apple Silicon notes

This stack is normal JVM / Spring Boot / MongoDB driver code — no Windows-native or x86-only dependencies are required. Prefer ARM64 JDK (Homebrew `openjdk@21`).

---

## Portability checklist for teammates

Another machine only needs:

1. Java 21  
2. Environment variables / `.env` (from `.env.example`)  
3. MongoDB Atlas access (URI + Network Access)  

No Java source edits should be required for machine-specific values.
