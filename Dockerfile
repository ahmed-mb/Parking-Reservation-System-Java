# =============================================================================
# Production Dockerfile for Parking Reservation System
# =============================================================================
# The ONE production image: CI builds and Trivy-scans it, pushes it to GHCR,
# and hal-server pulls and runs that exact artifact (build once, deploy that).
# docker-compose builds the same file for local demos.
#
# Single-process: Spring Boot serves both the API and the built React static
# files (copied into src/main/resources/static/ before packaging). TLS and
# public routing are the host's job (Tailscale Funnel in the demo deployment).
# =============================================================================

# --- Stage 1: Build React Frontend ---
FROM node:20-alpine AS frontend-build

WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --production=false
COPY frontend/ ./
# No VITE_* build args: runtime config (e.g. the reCAPTCHA site key) is
# fetched from /api/config, so keys rotate without a rebuild.
RUN npm run build

# --- Stage 2: Build Java Backend ---
FROM maven:3.9-eclipse-temurin-17 AS backend-build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src/ ./src/

# Spring Boot automatically serves files from src/main/resources/static/
COPY --from=frontend-build /app/frontend/dist/ ./src/main/resources/static/

RUN mvn package -DskipTests -B

# --- Stage 3: Lightweight Runtime ---
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Run as a non-root user; the app writes nothing to disk (H2 is in-memory).
RUN addgroup -S app && adduser -S app -G app

# Copy the fat JAR (includes static frontend files)
COPY --from=backend-build /app/target/*.jar app.jar

# Default profile; override via env-file / compose for other setups.
ENV SPRING_PROFILES_ACTIVE=demo

EXPOSE 8080

# The app reads PORT itself (server.port=${PORT:8080} in the properties),
# so the health check must probe the same value.
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=3 \
  CMD wget -q --spider "http://127.0.0.1:${PORT:-8080}/actuator/health" || exit 1

USER app

# Exec form so the JVM is PID 1 and receives SIGTERM directly on stop.
# -Xmx400m keeps the heap inside the small demo host's memory budget.
ENTRYPOINT ["java", "-Xmx400m", "-jar", "app.jar"]
