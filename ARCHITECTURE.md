# Architecture

## System Design

```
React SPA (Vite)          Java Spring Boot
 port 5173                  port 8080
+-----------------+       +---------------------------+
| Home            |       | REST Controllers          |
| Login/Register  | ----> |   UserController          |
| Dashboard       | HTTP  |   BookingController       |
| CurrentBooking  | JSON  |   ParkingController       |
| BookingHistory  |       |   AdminController         |
| UserProfile     |       +---------------------------+
| AdminPanel      |       | Service Layer             |
| ModernAlert     |       |   UserService             |
+-----------------+       |   BookingService          |
                          |   ParkingService          |
                          |   RecaptchaService        |
                          +---------------------------+
                          | Spring Data JPA Repos     |
                          +---------------------------+
                          | H2 (dev) / SQL Server     |
                          +---------------------------+
```

### Request Flow

1. React makes HTTP requests via Axios (JWT in `Authorization` header)
2. `JwtAuthenticationFilter` intercepts and validates the token
3. Spring Security sets the authenticated principal
4. Controller receives the request, delegates to a Service
5. Service executes business logic, uses JPA repositories for data access
6. Response returns as JSON

### Authentication Flow

1. **Login**: `POST /api/users/login` with email, password, reCAPTCHA token
2. Backend validates reCAPTCHA v3 with Google, checks credentials with BCrypt
3. Returns JWT token (24h expiry) containing email and role
4. Frontend stores token in localStorage via `AuthContext`
5. All subsequent requests include `Authorization: Bearer <token>`
6. Failed login attempts are tracked; 5 failures = 15-minute lockout

## Backend File Structure

```
src/main/java/com/ahmedbahaj/parking/
|
+-- ParkingSystemApplication.java        Main entry point
|
+-- config/
|   +-- DatabaseInitializer.java         Seeds 10 parking spots + admin user on startup
|   +-- OpenApiConfig.java               Swagger/OpenAPI configuration
|   +-- SecurityConfig.java              Spring Security, CORS, JWT filter chain
|
+-- controller/
|   +-- UserController.java              Login, register, profile endpoints
|   +-- BookingController.java           Create, cancel, report-taken, list bookings
|   +-- ParkingController.java           Available spots, count, CRUD
|   +-- AdminController.java             User/booking/parking management, stats
|
+-- service/
|   +-- UserService.java                 Auth logic, credit management, password validation
|   +-- BookingService.java              Booking creation, cancellation, report-spot-taken
|   +-- ParkingService.java              Spot availability, initialization, reset
|   +-- RecaptchaService.java            Google reCAPTCHA v3 server-side verification
|
+-- model/
|   +-- User.java                        @Entity for `logintable` (Lombok @Data)
|   +-- Booking.java                     @Entity for `booking` table (Lombok @Data)
|   +-- Parking.java                     @Entity for `parking` table (Lombok @Data)
|
+-- repository/
|   +-- UserRepository.java              findByEmail()
|   +-- BookingRepository.java           findByUserId(), findByStatus()
|   +-- ParkingRepository.java           findByAvailability()
|
+-- dto/
|   +-- LoginRequest.java                email, password, recaptchaToken
|   +-- LoginResponse.java               token, email, username, role
|   +-- RegisterRequest.java             All registration fields + recaptchaToken
|   +-- ErrorResponse.java               Standardized error format
|   +-- RecaptchaResponse.java           Google reCAPTCHA API response mapping
|
+-- security/
|   +-- JwtUtil.java                     Token generation and validation (HS256)
|   +-- JwtAuthenticationFilter.java     OncePerRequestFilter for JWT extraction
|
+-- exception/
|   +-- GlobalExceptionHandler.java      @RestControllerAdvice central error handling
|   +-- ResourceNotFoundException.java
|   +-- InsufficientCreditException.java
|   +-- ParkingNotAvailableException.java
|   +-- InvalidCredentialsException.java
|   +-- DuplicateEmailException.java
|
+-- scheduler/
    +-- ParkingResetScheduler.java       Daily 1 AM EST: complete bookings, reset spots

src/main/resources/
+-- application.properties               Dev config (H2, JWT secret, reCAPTCHA key)
+-- application-prod.properties          Prod config (SQL Server, HikariCP)
```

## Frontend File Structure

```
frontend/src/
|
+-- main.jsx                             React entry point
+-- App.jsx                              Router + providers (Auth, reCAPTCHA, ModernAlert)
+-- index.css                            Global styles (login/register/home pages)
+-- customer-dashboard.css               Customer page table and card styles
+-- admin-panel.css                      Admin panel table styles
+-- modern-alerts.css                    Themed modal alert styles (success/error/warning/confirm)
+-- not-found.css                        404 page styles
|
+-- context/
|   +-- AuthContext.jsx                  JWT storage, login/logout, role tracking
|
+-- components/
    +-- Navbar.jsx                       Public navigation bar (Home, Login, Register)
    +-- ProtectedRoute.jsx              Auth guard with optional admin role check
    +-- ModernAlert.jsx                 Alert provider + hook + modal component
    +-- Home.jsx                        Public landing page with availability check
    +-- Login.jsx                       Email/password login with reCAPTCHA v3
    +-- Register.jsx                    Registration form with password validation
    +-- Dashboard.jsx                   Customer "Book Now" page
    +-- CurrentBooking.jsx              Active booking with cancel/report-spot-taken
    +-- BookingHistory.jsx              Past bookings table
    +-- UserProfile.jsx                 Inline-editable profile table
    +-- AdminPanel.jsx                  Tabbed admin (Bookings, Customers, Parking)
    +-- NotFound.jsx                    404 page
```

## Database Schema

### logintable (Users)

| Column | Type | Notes |
|--------|------|-------|
| id | INT (PK, auto) | |
| Username | VARCHAR | |
| Email | VARCHAR (unique) | |
| pass | VARCHAR | BCrypt hash |
| Mobile | VARCHAR | |
| Address | VARCHAR | |
| Car_Plate_No | VARCHAR | |
| Credit | DECIMAL | Default 0.00 |
| Role | VARCHAR | "Customer" or "Admin" |

### Booking

| Column | Type | Notes |
|--------|------|-------|
| id | INT (PK, auto) | |
| user_id | INT | |
| user_name | VARCHAR | |
| user_contact | VARCHAR | |
| credit | INT | Cost (6) |
| car_plate | VARCHAR | |
| parking_spot | VARCHAR | FK to Parking |
| date | DATETIME | |
| status | VARCHAR | "Active", "Cancelled", "Completed" |
| cancelled_date | DATETIME | nullable |
| cancelled_reason | VARCHAR | nullable |
| created_date | DATETIME | |
| modified_date | DATETIME | |

### Parking

| Column | Type | Notes |
|--------|------|-------|
| parking_id | VARCHAR (PK) | e.g. "A-001" |
| availability | VARCHAR | "available", "booked", "unknown" |

## Key Design Decisions

1. **Stateless auth (JWT)** instead of ASP.NET's cookie sessions -- better for SPA architecture
2. **BCrypt** instead of PBKDF2 -- Spring Security standard, adaptive cost factor
3. **reCAPTCHA v3 (invisible)** instead of v2 checkbox -- better UX, no user interaction needed
4. **Lombok @Data** on models -- eliminates boilerplate getters/setters (LSP shows false errors, compiles fine)
5. **H2 for dev** -- zero-config in-memory database, auto-DDL from JPA entities
6. **ModernAlert context/hook pattern** -- replaces all browser `alert()`/`confirm()` with themed modals matching ASP.NET's `modern-alerts.css`
7. **Parking status "booked"** (not "occupied") -- matches original ASP.NET terminology
8. **Report Spot Taken** -- marks old spot as "unknown", assigns new spot, no extra charge (exact ASP.NET logic)

## Scheduled Tasks

`ParkingResetScheduler` runs daily at 1:00 AM EST (configurable via cron in `application.properties`):
- Marks all "Active" bookings as "Completed"
- Resets all parking spots to "available"
- Transactional -- all-or-nothing
