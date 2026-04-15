#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

REMOTE_USER="${REMOTE_USER:-root}"
REMOTE_HOST="${REMOTE_HOST:-111.228.20.88}"
REMOTE_PORT="${REMOTE_PORT:-22}"
REMOTE_PATH="${REMOTE_PATH:-/opt/ai-platform}"
REMOTE_STOP_INFRA="${REMOTE_STOP_INFRA:-0}"

echo "[dev_remote_down] 远程关闭模式"
echo "[dev_remote_down] remote=${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}"
echo "[dev_remote_down] remotePath=$REMOTE_PATH"
echo "[dev_remote_down] remote STOP_INFRA=$REMOTE_STOP_INFRA"

REMOTE_USER="$REMOTE_USER" REMOTE_HOST="$REMOTE_HOST" REMOTE_PORT="$REMOTE_PORT" REMOTE_PATH="$REMOTE_PATH" \
  bash scripts/sync_remote_dev_support.sh

remote_status=0
ssh -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}" \
  "cd '$REMOTE_PATH' && STOP_INFRA='$REMOTE_STOP_INFRA' sh scripts/dev_all_down.sh" || remote_status=$?

bash scripts/ssh_tunnel.sh stop || true
bash scripts/nacos_tunnel.sh stop >/dev/null 2>&1 || true

exit "$remote_status"
