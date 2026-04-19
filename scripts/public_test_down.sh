#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

APP_REMOTE_USER="${APP_REMOTE_USER:-root}"
APP_REMOTE_HOST="${APP_REMOTE_HOST:-111.228.20.88}"
APP_REMOTE_PORT="${APP_REMOTE_PORT:-22}"
APP_REMOTE_PATH="${APP_REMOTE_PATH:-/opt/ai-platform}"
REMOTE_STOP_INFRA="${REMOTE_STOP_INFRA:-0}"

echo "[public_test_down] 关闭共享公网测试应用栈"
echo "[public_test_down] app=${APP_REMOTE_USER}@${APP_REMOTE_HOST}:${APP_REMOTE_PORT} path=$APP_REMOTE_PATH"

ssh -p "$APP_REMOTE_PORT" "${APP_REMOTE_USER}@${APP_REMOTE_HOST}" \
  "cd '$APP_REMOTE_PATH' && STOP_INFRA='$REMOTE_STOP_INFRA' sh scripts/dev_all_down.sh"
