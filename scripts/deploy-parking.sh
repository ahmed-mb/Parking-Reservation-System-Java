#!/usr/bin/env bash
# =============================================================================
# Pull-based deploy for the Parking Reservation System (hal-server).
# =============================================================================
# This is the SSH forced command for the GitHub Actions deploy key
# (authorized_keys: command="/home/ahmed/deploy-parking.sh",no-pty,...).
# The operative copy lives at ~/deploy-parking.sh on the server; this repo
# copy is the source of truth for review — keep them in sync.
#
# GitHub Actions runs:  ssh ... "deploy <tag>"
# The forced command ignores the requested command line but receives it in
# SSH_ORIGINAL_COMMAND, which is where the image tag is read from.
#
# Flow: preflight the env file -> docker pull the CI-scanned GHCR image ->
# remember the currently running image -> swap the container -> wait for
# Docker health -> HTTP smoke test. If the new container never goes healthy,
# the previous image is restarted (rollback) and the script exits non-zero
# so the Actions job goes red.
# =============================================================================
set -euo pipefail

ts() { date -u +'%Y-%m-%dT%H:%M:%SZ'; }

IMAGE_REPO="ghcr.io/ahmed-mb/parking-reservation-system-java"
ENV_FILE="$HOME/parking.env"
CONTAINER="parking"

# --- resolve requested tag: "deploy <tag>" over SSH, or $1 when run by hand --
TAG="latest"
if [ -n "${SSH_ORIGINAL_COMMAND:-}" ]; then
  # shellcheck disable=SC2086
  set -- $SSH_ORIGINAL_COMMAND
  TAG="${2:-latest}"
elif [ $# -ge 1 ]; then
  TAG="$1"
fi

# Only docker-tag characters; anything else arriving over SSH is rejected so
# a leaked deploy key cannot make this host pull an arbitrary image ref.
case "$TAG" in
  (*[!A-Za-z0-9._-]*|"") echo "[$(ts)] ERROR: invalid tag '$TAG'" >&2; exit 1 ;;
esac

echo "[$(ts)] deploy start: $IMAGE_REPO:$TAG"

# --- preflight: required runtime config, before touching the live container --
for key in JWT_SECRET RECAPTCHA_SITE_KEY RECAPTCHA_SECRET_KEY SPRING_PROFILES_ACTIVE; do
  grep -q "^${key}=." "$ENV_FILE" || {
    echo "[$(ts)] ERROR: $key missing or empty in $ENV_FILE" >&2
    exit 1
  }
done

# --- pull first: a failed pull leaves the running container untouched -------
docker pull "$IMAGE_REPO:$TAG"

PREV_IMAGE="$(docker inspect -f '{{.Config.Image}}' "$CONTAINER" 2>/dev/null || true)"

start_container() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  docker run -d --name "$CONTAINER" --restart unless-stopped \
    -p 127.0.0.1:80:8080 --env-file "$ENV_FILE" "$1" >/dev/null
}

wait_healthy() {
  for _ in $(seq 1 40); do
    h="$(docker inspect -f '{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || echo starting)"
    [ "$h" = healthy ] && return 0
    sleep 3
  done
  return 1
}

echo "[$(ts)] starting new container..."
start_container "$IMAGE_REPO:$TAG"

if wait_healthy; then
  # Smoke test through the host port mapping that Tailscale Funnel fronts.
  curl -fsS -o /dev/null "http://127.0.0.1:80/actuator/health"
  echo "[$(ts)] deploy OK (healthy): $IMAGE_REPO:$TAG"
  # Dangling (untagged) images only; never touches the rollback target.
  docker image prune -f >/dev/null || true
  exit 0
fi

echo "[$(ts)] ERROR: new container failed to become healthy" >&2
docker logs --tail 30 "$CONTAINER" >&2 || true

if [ -n "$PREV_IMAGE" ]; then
  echo "[$(ts)] rolling back to $PREV_IMAGE ..." >&2
  start_container "$PREV_IMAGE"
  if wait_healthy; then
    echo "[$(ts)] rollback OK: $PREV_IMAGE is serving again" >&2
  else
    echo "[$(ts)] FATAL: rollback to $PREV_IMAGE also failed" >&2
  fi
else
  echo "[$(ts)] no previous image known; nothing to roll back to" >&2
fi
exit 1
