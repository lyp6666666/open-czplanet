#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

REMOTE_USER="${REMOTE_USER:-root}"
REMOTE_HOST="${REMOTE_HOST:-111.228.20.88}"
REMOTE_PORT="${REMOTE_PORT:-22}"
REMOTE_PATH="${REMOTE_PATH:-/opt/ai-platform}"
REMOTE_SYNC_DELETE="${REMOTE_SYNC_DELETE:-1}"

echo "[sync_remote_code] remote=${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}"
echo "[sync_remote_code] remotePath=$REMOTE_PATH"
echo "[sync_remote_code] delete=$REMOTE_SYNC_DELETE"

if ! command -v rsync >/dev/null 2>&1; then
  echo "[sync_remote_code] 未检测到 rsync，请先安装 rsync"
  exit 1
fi

ssh -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}" \
  "mkdir -p '$REMOTE_PATH'"

set -- \
  -az \
  "--exclude=.git/" \
  "--exclude=.vscode/" \
  "--exclude=.DS_Store" \
  "--exclude=**/node_modules/**" \
  "--exclude=**/target/**" \
  "--exclude=**/.idea/**" \
  "--exclude=**/.logs/**" \
  "--exclude=**/.pids/**" \
  "--exclude=Dockerfile/mysql-data/" \
  "--exclude=Dockerfile/mysql-data/**" \
  "--exclude=Dockerfile/redis-data/" \
  "--exclude=Dockerfile/redis-data/**" \
  "--exclude=Dockerfile/minio-data/" \
  "--exclude=Dockerfile/minio-data/**" \
  "--exclude=Dockerfile/prometheus-data/" \
  "--exclude=Dockerfile/prometheus-data/**" \
  "--exclude=Dockerfile/grafana-data/" \
  "--exclude=Dockerfile/grafana-data/**" \
  "--filter=P Dockerfile/mysql-data/" \
  "--filter=P Dockerfile/redis-data/" \
  "--filter=P Dockerfile/minio-data/" \
  "--filter=P Dockerfile/prometheus-data/" \
  "--filter=P Dockerfile/grafana-data/"

case "$REMOTE_SYNC_DELETE" in
  1|true|yes)
    set -- "$@" --delete
    ;;
  0|false|no)
    ;;
  *)
    echo "[sync_remote_code] 不支持的 REMOTE_SYNC_DELETE=$REMOTE_SYNC_DELETE，可选值：1/0 true/false yes/no"
    exit 1
    ;;
esac

rsync -e "ssh -p $REMOTE_PORT" "$@" \
  "$ROOT_DIR/" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}/"

echo "[sync_remote_code] 代码已同步"
