# Parking Reservation System

A parking reservation system migrated from **ASP.NET Web Forms** to **Java Spring Boot** (backend) + **React** (frontend). The Java project is a functional clone of the original ASP.NET application -- same business logic, same table designs, same UI layout and styling.

## Live Demo

The application is deployed on Railway: **[Live Demo](https://parking-reservation-system-java-production.up.railway.app)**

**Demo Credentials:**
- **Admin**: `admin@parking.com` / `Admin@123`
- **Customer**: Register a new account to test the customer flow

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
| PUT | `/api/users/{id}` | Update own profile |
| GET | `/api/parking/available` | List available spots |
| POST | `/api/bookings` | Create booking ($6.00) |
| POST | `/api/bookings/{id}/cancel` | Cancel own booking (refund $6.00) |
| POST | `/api/bookings/{id}/report-taken` | Report own spot taken, get reassigned |
| GET | `/api/bookings/my-active` | Get current user's active booking |
| GET | `/api/bookings/user/{userId}` | Get own booking history |

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

## Docker Deployment

### Local Demo (with Nginx)
```bash
docker-compose up --build
# Access at http://localhost:3080
```

### Railway Deployment
```bash
# Push to GitHub, then deploy from Railway dashboard
# See RAILWAY.md for detailed instructions
```

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) -- System design, file structure, tech decisions
- [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) -- Migration context, ASP.NET vs Java feature comparison
- [PROGRESS.md](PROGRESS.md) -- Detailed log of the comparison audit and all fixes applied
- [RAILWAY.md](RAILWAY.md) -- Railway deployment guide with troubleshooting

## Production Checklist

- [x] Replace JWT secret with environment variable
- [x] Security audit (16 vulnerabilities fixed)
- [x] IDOR protection on all endpoints
- [x] Docker containerization
- [x] Railway deployment
- [ ] Replace reCAPTCHA test keys with production keys
- [ ] Configure SQL Server for persistent data
- [ ] Set up custom domain with SSL
