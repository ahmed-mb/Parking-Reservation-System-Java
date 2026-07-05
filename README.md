# Parking Reservation System

A parking reservation system migrated from **ASP.NET Web Forms** to **Java Spring Boot** (backend) + **React** (frontend). The Java project is a functional clone of the original ASP.NET application -- same business logic, same table designs, same UI layout and styling.

## Live Demo

The application is self-hosted via Docker: **[Live Demo](https://hal-server-832612.tail27051c.ts.net/)**

**Demo Credentials:**
- **Admin**: `admin@parking.com` / `Admin@123`
- **Customer**: Register a new account to test the customer flow

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17, Spring Boot 3.4.5, Spring Security, Spring Data JPA |
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
- H2 Console: disabled by default (`spring.h2.console.enabled=false`); set that property to `true` locally if you need it (JDBC URL: `jdbc:h2:mem:parkingdb`, user: `sa`, pass: `password`)

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
2. Register a new customer account (password must be 8+ characters with an uppercase letter, a lowercase letter, a digit, and a special character; it cannot contain your username or be a common password)
3. Login -- redirected to Dashboard
4. Click "Book Now" -- $6.00 deducted, spot assigned
5. Go to Current Booking -- cancel or report spot taken
6. Login as admin -- manage bookings, customers, parking spots

## Configuration

### JWT
No secret ships with the app. Set the `JWT_SECRET` environment variable
(32+ random characters / 256+ bits) before running any profile; without
it, the app still boots using a per-process random secret, but every
restart invalidates all outstanding tokens.
```properties
jwt.expiration=86400000  # 24 hours (prod); shorter in the demo profile
```

### reCAPTCHA v3
- The frontend fetches its site key at runtime from `GET /api/config` (see `ConfigController`) -- no key is baked into the JS bundle.
- Both `RECAPTCHA_SITE_KEY` and `RECAPTCHA_SECRET_KEY` are required environment variables in every profile; there's no bypass and no built-in fallback. Register a site at https://www.google.com/recaptcha/admin for whatever domain you deploy to.

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
Copy `.env.example` to `.env` and fill in `JWT_SECRET`, `ADMIN_DEFAULT_PASSWORD`,
`RECAPTCHA_SITE_KEY`, and `RECAPTCHA_SECRET_KEY` first -- `docker-compose.yml`
requires all four and will refuse to start without them.
```bash
docker compose --env-file .env up --build
# Access at http://localhost:3080
```

### Signed release images
Pushing a `v*` tag triggers `.github/workflows/release.yml`, which builds,
tests, and publishes a cosign-signed image with an attached CycloneDX SBOM
to `ghcr.io/<owner>/parking-reservation-system`. See [SECURITY.md](SECURITY.md#cryptographic-supply-chain)
for the verification command.

## Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) -- System design, file structure, tech decisions
- [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) -- Migration context, ASP.NET vs Java feature comparison
- [PROGRESS.md](PROGRESS.md) -- Detailed log of the comparison audit and all fixes applied
- [SECURITY.md](SECURITY.md) -- Supply-chain controls, pending manual security steps, vulnerability reporting

## Production Checklist

- [x] Replace JWT secret with environment variable (required, no fallback)
- [x] Security audit (16 vulnerabilities fixed; see SECURITY.md for follow-up items)
- [x] IDOR protection on all endpoints
- [x] Docker containerization
- [x] Content-Security-Policy header
- [x] JWT revocation via per-user token version
- [ ] Register production reCAPTCHA keys for the actual deployment domain
- [ ] Configure SQL Server for persistent data
- [ ] Set up custom domain with SSL
