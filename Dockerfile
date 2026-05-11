# =============================================================================
# Multi-stage Dockerfile for Parking Reservation System
# =============================================================================
# Stage 1: Build React frontend
# Stage 2: Build Java backend (includes frontend static files)
# Stage 3: Lightweight runtime image with Nginx + Java
# =============================================================================

# --- Stage 1: Build React Frontend ---
FROM node:20-alpine AS frontend-build

WORKDIR /app/frontend

# Copy package files first for better caching
COPY frontend/package.json frontend/package-lock.json ./

# Install dependencies
RUN npm ci --production=false

# Copy frontend source
COPY frontend/ ./

# ---------------------------------------------------------------------------
# Build-time arguments for Vite.
#
# Vite reads VITE_* env vars at BUILD time and inlines them into the
# JavaScript bundle (they are NOT looked up at runtime). Railway exposes the
# service's environment variables as Docker build args automatically, but
# only when the Dockerfile declares each ARG it wants to receive. Without
# these two lines, npm run build below cannot see VITE_RECAPTCHA_SITE_KEY,
# so the frontend falls back to the localhost-only test key hard-coded as
# the App.jsx default and Google rejects the page on any real domain with
# "ERROR for site owner: Invalid domain for site key".
# ---------------------------------------------------------------------------
ARG VITE_RECAPTCHA_SITE_KEY
ENV VITE_RECAPTCHA_SITE_KEY=${VITE_RECAPTCHA_SITE_KEY}

# Build for production. Demo mode is fine here too — the bundle still ships
# a site key so the reCAPTCHA badge can render; the *backend* decides
# whether to actually verify tokens based on the `demo.mode` property.
RUN npm run build

# --- Stage 2: Build Java Backend ---
FROM maven:3.9-eclipse-temurin-17 AS backend-build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source code
COPY src/ ./src/

# Build the application (skip tests for faster build)
RUN mvn clean package -DskipTests -B

# --- Stage 3: Runtime Image ---
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Install nginx for serving frontend + reverse proxy
RUN apk add --no-cache nginx

# Copy the Spring Boot JAR
COPY --from=backend-build /app/target/*.jar app.jar

# Copy the built frontend files
COPY --from=frontend-build /app/frontend/dist /usr/share/nginx/html

# Copy the loading page (served by Nginx while Spring Boot starts)
COPY docker/loading.html /usr/share/nginx/html/loading.html

# Copy Nginx configuration
COPY docker/nginx.conf /etc/nginx/nginx.conf

# Copy the entrypoint script
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Expose port 80 (Nginx)
EXPOSE 80

# Health check
HEALTHCHECK --interval=5s --timeout=3s --start-period=15s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Start both Nginx and Spring Boot
ENTRYPOINT ["/entrypoint.sh"]
