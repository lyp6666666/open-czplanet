#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

REMOTE_USER="${REMOTE_USER:-root}"
REMOTE_HOST="${REMOTE_HOST:-111.228.20.88}"
REMOTE_PORT="${REMOTE_PORT:-22}"
REMOTE_PATH="${REMOTE_PATH:-/opt/ai-platform}"

echo "[sync_remote_dev_support] remote=${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}"
echo "[sync_remote_dev_support] remotePath=$REMOTE_PATH"

ssh -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}" \
  "mkdir -p '$REMOTE_PATH/scripts'"

scp -P "$REMOTE_PORT" \
  scripts/dev_all_up.sh \
  scripts/dev_all_down.sh \
  scripts/dev_local_up.sh \
  scripts/dev_local_down.sh \
  scripts/dev_remote_up.sh \
  scripts/dev_remote_down.sh \
  scripts/nacos_tunnel.sh \
  scripts/ssh_tunnel.sh \
  "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/scripts/"

scp -P "$REMOTE_PORT" \
  common.md \
  "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/"

echo "[sync_remote_dev_support] 关键开发脚本与 common.md 已同步"
