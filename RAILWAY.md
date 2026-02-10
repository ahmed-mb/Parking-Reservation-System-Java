# Railway Deployment Guide

This guide walks you through deploying the Parking Reservation System to [Railway](https://railway.app).

## Architecture

The Railway deployment uses a simplified single-process architecture:

- **Spring Boot** serves both the API (`/api/*`) and React static files
- **No Nginx needed** - Railway handles HTTPS termination and routing
- **Demo mode** enabled - reCAPTCHA validation bypassed for easy testing

## Prerequisites

1. A [Railway account](https://railway.app) (free tier available)
2. [Git](https://git-scm.com/) installed
3. Your code pushed to a GitHub repository

## Quick Deploy

### Option 1: Deploy from GitHub (Recommended)

1. Push your code to GitHub
2. Log in to [Railway Dashboard](https://railway.app/dashboard)
3. Click **"New Project"** > **"Deploy from GitHub repo"**
4. Select your repository
5. Railway will auto-detect the `Dockerfile.railway` and begin building

### Option 2: Deploy via Railway CLI

```bash
# Install Railway CLI
npm install -g @railway/cli

# Login to Railway
railway login

# Initialize project (from repo root)
railway init

# Deploy
railway up
```

## Environment Variables

Configure these in Railway Dashboard > Your Project > Variables:

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `JWT_SECRET` | Yes | Secret for JWT signing (32+ chars) | `MySecureJWTSecretKey2024!@#$%^&*()` |
| `ADMIN_DEFAULT_PASSWORD` | No | Initial admin password | `Admin@123` (default) |
| `PORT` | No | Server port (Railway sets automatically) | `8080` |

### Setting Variables

1. Go to Railway Dashboard > Your Project
2. Click on your service
3. Go to **Variables** tab
4. Add each variable:
   ```
   JWT_SECRET=YourSecureRandomString32CharsOrMore!@#
   ADMIN_DEFAULT_PASSWORD=Admin@123
   ```

## Files Used for Railway

```
Parking-Reservation-System-Java/
├── Dockerfile.railway     # Single-process Dockerfile (Spring Boot only)
├── railway.toml           # Railway config (points to Dockerfile.railway)
├── src/main/java/.../config/
│   ├── WebConfig.java     # Serves React SPA from Spring Boot
│   └── SecurityConfig.java # Permits static resource access
└── src/main/resources/
    └── application-demo.properties  # Demo profile with ${PORT:8080}
```

## Build Process

Railway builds using `Dockerfile.railway`:

1. **Stage 1 (node:18-alpine)**: Builds React frontend with `npm run build`
2. **Stage 2 (maven:3.9-eclipse-temurin-17)**: Builds Spring Boot JAR
3. **Stage 3 (eclipse-temurin:17-jre-alpine)**: Runtime with static files + JAR

The React build output is copied to `src/main/resources/static/` before Maven builds the JAR, embedding the frontend inside the Spring Boot application.

## Deployment Verification

After deployment, Railway provides a URL like `https://your-app.up.railway.app`.

Test these endpoints:

```bash
# Homepage (React app)
curl https://your-app.up.railway.app/

# Config endpoint (should return demoMode: true)
curl https://your-app.up.railway.app/api/config

# Health check
curl https://your-app.up.railway.app/actuator/health

# Admin login
curl -X POST https://your-app.up.railway.app/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@parking.com","password":"Admin@123","recaptchaToken":"demo"}'
```

## Demo Mode Features

When deployed, the app runs in demo mode (`demo` Spring profile):

- **reCAPTCHA bypassed** - Any token value works (e.g., "demo")
- **Welcome modal** - First-time visitors see a 5-step guide with admin credentials
- **H2 in-memory database** - Data resets on redeploy
- **15-minute session timeout** - Matches original ASP.NET behavior

### Demo Credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@parking.com | Admin@123 |
| User | (create via Register) | (your password) |

## Troubleshooting

### Build Fails

1. Check Railway build logs
2. Ensure `Dockerfile.railway` and `railway.toml` are in the repo root
3. Verify `pom.xml` has correct dependencies

### App Returns 403 on Static Routes

Check that `SecurityConfig.java` permits static routes:
```java
.requestMatchers("/", "/index.html", "/login", "/register", 
                 "/dashboard", "/assets/**").permitAll()
```

### API Returns 401 Unauthorized

- Login first and use the JWT token in the `Authorization` header
- Format: `Authorization: Bearer <token>`

### Port Binding Issues

Ensure `application-demo.properties` uses:
```properties
server.port=${PORT:8080}
```

### Database Issues

The demo profile uses H2 in-memory database. Data is lost on:
- Redeploy
- Container restart
- Railway idle timeout (free tier)

For persistent data, configure a PostgreSQL or MySQL database service in Railway.

## Local Testing (Before Deploying)

Test the Railway Docker image locally:

```bash
# Build the image
docker build -f Dockerfile.railway -t parking-railway .

# Run the container
docker run -d --name parking-test -p 3081:8080 \
  -e PORT=8080 \
  -e JWT_SECRET=TestSecretKeyForLocalDevelopment2024! \
  -e ADMIN_DEFAULT_PASSWORD=Admin@123 \
  parking-railway

# Test endpoints
curl http://localhost:3081/
curl http://localhost:3081/api/config

# View logs
docker logs -f parking-test

# Cleanup
docker stop parking-test && docker rm parking-test
```

## Production Considerations

For a production deployment, consider:

1. **Use a real database** - Add PostgreSQL service in Railway
2. **Enable reCAPTCHA** - Set `RECAPTCHA_SITE_KEY` and `RECAPTCHA_SECRET_KEY`
3. **Use production profile** - Set `SPRING_PROFILES_ACTIVE=prod`
4. **Configure proper JWT secret** - Use a cryptographically secure random string
5. **Set up monitoring** - Railway provides built-in metrics

## Cost

- **Free tier**: 500 hours/month, 512MB RAM, sleeps after inactivity
- **Hobby tier ($5/month)**: Always-on, 8GB RAM, better performance

The demo is configured for always-on deployment as requested.

---

## Quick Reference

| What | Where |
|------|-------|
| Railway Dashboard | https://railway.app/dashboard |
| App URL | https://[your-project].up.railway.app |
| Admin Login | admin@parking.com / Admin@123 |
| API Docs (local) | http://localhost:8080/swagger-ui.html |
