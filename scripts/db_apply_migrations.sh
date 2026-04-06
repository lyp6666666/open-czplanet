#!/bin/sh

set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

COMPOSE_FILE="$ROOT_DIR/Dockerfile/docker-compose.yml"
CONTAINER="${DB_CONTAINER:-mysql}"
DB_NAME="${DB_NAME:-ai_tutor}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-Aa123456}"

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

docker compose -f "$COMPOSE_FILE" up -d mysql

MYSQL_CMD="mysql -u${MYSQL_USER} -p${MYSQL_PASSWORD} --default-character-set=utf8mb4 ${DB_NAME}"

applied=0
for f in "$ROOT_DIR"/sqlDoc/migrations/*.sql; do
  if [ ! -f "$f" ]; then
    continue
  fi
  echo "[db_apply_migrations] Applying $(basename "$f")"
  docker exec -i "$CONTAINER" $MYSQL_CMD < "$f"
  applied=$((applied + 1))
done

echo "[db_apply_migrations] Applied migrations: $applied"

if [ "${1:-}" = "--seed" ]; then
  echo "[db_apply_migrations] Applying seed_dev_data.sql"
  docker exec -i "$CONTAINER" $MYSQL_CMD < "$ROOT_DIR/sqlDoc/seed_dev_data.sql"
  echo "[db_apply_migrations] Applied seed_dev_data.sql"
fi
