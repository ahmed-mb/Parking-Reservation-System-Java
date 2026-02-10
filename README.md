# Parking Reservation System

A parking reservation system migrated from **ASP.NET Web Forms** to **Java Spring Boot** (backend) + **React** (frontend). The Java project is a functional clone of the original ASP.NET application -- same business logic, same table designs, same UI layout and styling.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17, Spring Boot 3.2.0, Spring Security, Spring Data JPA |
| Frontend | React 18, React Router 6, Axios, Vite 5 |
| Auth | JWT (jjwt 0.12.3) + BCrypt + reCAPTCHA v3 |
| Database | H2 (dev) / SQL Server (prod) |
| Build | Maven (backend), npm (frontend) |

## Prerequisites

- Java 17+
- Maven 3.6+
- Node.js 16+

## Quick Start

### Backend

```bash
cd Parking-Reservation-System-Java
mvn clean install -DskipTests
mvn spring-boot:run
```

Backend runs at `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:parkingdb`, user: `sa`, pass: `password`)

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`

## Default Accounts

On startup, `DatabaseInitializer` seeds:
- **10 parking spots** (A-001 through C-004)
- **Admin account**: `admin@parking.com` / `Admin@123`

## Test Flow

1. Open `http://localhost:5173` -- Home page with "Check Availability"
2. Register a new customer account (password must be 8+ chars with uppercase, lowercase, and digit)
3. Login -- redirected to Dashboard
4. Click "Book Now" -- $6.00 deducted, spot assigned
5. Go to Current Booking -- cancel or report spot taken
6. Login as admin -- manage bookings, customers, parking spots

## Configuration

### JWT (application.properties)
```properties
jwt.secret=MySecretKeyForJWTTokenGenerationMustBeLongEnoughForHS256Algorithm
jwt.expiration=86400000  # 24 hours
```

### reCAPTCHA v3
- Frontend site key is set in `App.jsx` (`GoogleReCaptchaProvider`)
- Backend secret key is in `application.properties` (`recaptcha.secret-key`)
- Current keys are Google test keys -- replace with production keys before deploying

### Production Database
Use the `prod` Spring profile with `application-prod.properties` for SQL Server + HikariCP connection pooling.

## API Endpoints

### Public
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users/login` | Login (returns JWT) |
| POST | `/api/users/register` | Register new customer |
| GET | `/api/parking/available/count` | Count available spots |

### Customer (JWT required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/me` | Get current user profile |
| PUT | `/api/users/{id}` | Update profile |
| GET | `/api/parking/available` | List available spots |
| POST | `/api/bookings` | Create booking ($6.00) |
| POST | `/api/bookings/{id}/cancel` | Cancel booking (refund $6.00) |
| POST | `/api/bookings/{id}/report-taken` | Report spot taken, get reassigned |
| GET | `/api/bookings/active` | Get active bookings |
| GET | `/api/bookings/user/{userId}` | Get booking history |

### Admin (JWT + Admin role required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/users` | List all customers |
| PUT | `/api/admin/users/{id}` | Edit customer (including credit) |
| DELETE | `/api/admin/users/{id}` | Delete customer |
| GET | `/api/admin/bookings` | List all bookings |
| DELETE | `/api/admin/bookings/{id}` | Delete booking (with conditional refund) |
| GET | `/api/admin/parking` | List all parking spots |
| GET | `/api/admin/parking/stats` | Parking statistics |

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) -- System design, file structure, tech decisions
- [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) -- Migration context, ASP.NET vs Java feature comparison
- [PROGRESS.md](PROGRESS.md) -- Detailed log of the comparison audit and all fixes applied

## Production Checklist

- [ ] Replace JWT secret with a strong random key
- [ ] Replace reCAPTCHA test keys with production keys
- [ ] Configure SQL Server connection in `application-prod.properties`
- [ ] Set up SSL/TLS
- [ ] Configure CORS for production domain
- [ ] Set up logging and monitoring
- [ ] Run security audit and load testing
