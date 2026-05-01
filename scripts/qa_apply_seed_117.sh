#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

MYSQL_HOST="${MYSQL_HOST:-117.72.111.39}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_DATABASE="${MYSQL_DATABASE:-ai_tutor}"

if [[ -z "${MYSQL_PWD:-}" ]]; then
  echo "MYSQL_PWD is required. Example: MYSQL_PWD='***' bash scripts/qa_apply_seed_117.sh" >&2
  exit 1
fi

mysql \
  --host="${MYSQL_HOST}" \
  --port="${MYSQL_PORT}" \
  --user="${MYSQL_USER}" \
  --database="${MYSQL_DATABASE}" \
  --default-character-set=utf8mb4 \
  < "${ROOT_DIR}/sqlDoc/qa_seed_data.sql"

echo "QA seed data applied to ${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}"
