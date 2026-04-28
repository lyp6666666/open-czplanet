#!/bin/sh

set -u

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

COMPOSE_FILE="$ROOT_DIR/Dockerfile/docker-compose.yml"
CONTAINER="${DB_CONTAINER:-mysql}"
DB_NAME="${DB_NAME:-ai_tutor}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-Aa123456}"

docker_compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
    return
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
    return
  fi
  echo "[db_apply_migrations] 未检测到 docker compose 或 docker-compose"
  exit 1
}

case "${1:-}" in
  ""|"--migrations")
    ;;
  "--seed")
    ;;
  *)
    echo "Usage: sh scripts/db_apply_migrations.sh [--seed]"
    exit 2
    ;;
esac

docker_compose_cmd -f "$COMPOSE_FILE" up -d mysql

MYSQL_CMD="mysql -u${MYSQL_USER} -p${MYSQL_PASSWORD} --default-character-set=utf8mb4 ${DB_NAME}"

applied=0
failed=0
failed_files=""
for f in "$ROOT_DIR"/sqlDoc/migrations/*.sql; do
  if [ ! -f "$f" ]; then
    continue
  fi
  echo "[db_apply_migrations] Applying $(basename "$f")"
  if docker exec -i "$CONTAINER" $MYSQL_CMD < "$f"; then
    applied=$((applied + 1))
  else
    failed=$((failed + 1))
    failed_files="${failed_files} $(basename "$f")"
    echo "[db_apply_migrations] FAILED $(basename "$f")"
  fi
done

echo "[db_apply_migrations] Applied migrations: $applied"
if [ "$failed" -gt 0 ]; then
  echo "[db_apply_migrations] Failed migrations: $failed"
  echo "[db_apply_migrations] Failed files:$failed_files"
fi

if [ "${1:-}" = "--seed" ]; then
  echo "[db_apply_migrations] Applying seed_dev_data.sql"
  docker exec -i "$CONTAINER" $MYSQL_CMD < "$ROOT_DIR/sqlDoc/seed_dev_data.sql"
  echo "[db_apply_migrations] Applied seed_dev_data.sql"
fi

if [ "$failed" -gt 0 ]; then
  exit 1
fi
