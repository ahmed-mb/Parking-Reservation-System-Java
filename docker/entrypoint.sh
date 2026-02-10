#!/bin/sh
set -e

echo "==================================="
echo "Parking Reservation System - Demo"
echo "==================================="

# Start Nginx in background (serves loading page immediately)
echo "[1/2] Starting Nginx (loading page)..."
nginx

# Start Spring Boot in foreground
echo "[2/2] Starting Spring Boot backend..."
exec java \
  -Xmx256m \
  -Dspring.profiles.active=demo \
  -jar /app/app.jar
