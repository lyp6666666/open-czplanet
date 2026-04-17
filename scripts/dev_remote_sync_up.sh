#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

REMOTE_USER="${REMOTE_USER:-root}"
REMOTE_HOST="${REMOTE_HOST:-111.228.20.88}"
REMOTE_PORT="${REMOTE_PORT:-22}"
REMOTE_PATH="${REMOTE_PATH:-/opt/ai-platform}"
REMOTE_MANAGE_INFRA="${REMOTE_MANAGE_INFRA:-never}"
REMOTE_NACOS_GRPC_CHECK="${REMOTE_NACOS_GRPC_CHECK:-warn}"
REMOTE_NACOS_SERVER_ADDR="${REMOTE_NACOS_SERVER_ADDR:-127.0.0.1:8848}"
REMOTE_USE_TUNNEL="${REMOTE_USE_TUNNEL:-1}"
REMOTE_STOP_INFRA="${REMOTE_STOP_INFRA:-0}"
REMOTE_BROWSER_HOST="${REMOTE_BROWSER_HOST:-$REMOTE_HOST}"
REMOTE_SYNC_DELETE="${REMOTE_SYNC_DELETE:-1}"

case "$REMOTE_USE_TUNNEL" in
  1|true|yes)
    REMOTE_FRONTEND_HOST="${REMOTE_FRONTEND_HOST:-127.0.0.1}"
    ;;
  0|false|no)
    REMOTE_FRONTEND_HOST="${REMOTE_FRONTEND_HOST:-0.0.0.0}"
    ;;
  *)
    echo "[dev_remote_sync_up] 不支持的 REMOTE_USE_TUNNEL=$REMOTE_USE_TUNNEL，可选值：1/0 true/false yes/no"
    exit 1
    ;;
esac

echo "[dev_remote_sync_up] 远程同步并重启模式"
echo "[dev_remote_sync_up] remote=${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}"
echo "[dev_remote_sync_up] remotePath=$REMOTE_PATH"
echo "[dev_remote_sync_up] remote MANAGE_INFRA=$REMOTE_MANAGE_INFRA"
echo "[dev_remote_sync_up] remote STOP_INFRA=$REMOTE_STOP_INFRA"
echo "[dev_remote_sync_up] remote NACOS_GRPC_CHECK=$REMOTE_NACOS_GRPC_CHECK"
echo "[dev_remote_sync_up] remote NACOS_SERVER_ADDR=$REMOTE_NACOS_SERVER_ADDR"
echo "[dev_remote_sync_up] remote USE_TUNNEL=$REMOTE_USE_TUNNEL"
echo "[dev_remote_sync_up] remote FRONTEND_HOST=$REMOTE_FRONTEND_HOST"
echo "[dev_remote_sync_up] remote SYNC_DELETE=$REMOTE_SYNC_DELETE"

case "$REMOTE_USE_TUNNEL" in
  1|true|yes)
    bash scripts/ssh_tunnel.sh start
    ;;
  0|false|no)
    echo "[dev_remote_sync_up] 跳过本地 SSH 隧道，按远程地址直连"
    ;;
esac

REMOTE_USER="$REMOTE_USER" REMOTE_HOST="$REMOTE_HOST" REMOTE_PORT="$REMOTE_PORT" REMOTE_PATH="$REMOTE_PATH" \
REMOTE_SYNC_DELETE="$REMOTE_SYNC_DELETE" bash scripts/sync_remote_code.sh

ssh -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}" \
  "cd '$REMOTE_PATH' && STOP_INFRA='$REMOTE_STOP_INFRA' sh scripts/dev_all_down.sh || true"

ssh -p "$REMOTE_PORT" "${REMOTE_USER}@${REMOTE_HOST}" \
  "cd '$REMOTE_PATH' && MANAGE_INFRA='$REMOTE_MANAGE_INFRA' NACOS_GRPC_CHECK='$REMOTE_NACOS_GRPC_CHECK' NACOS_SERVER_ADDR='$REMOTE_NACOS_SERVER_ADDR' FRONTEND_HOST='$REMOTE_FRONTEND_HOST' sh scripts/dev_all_up.sh"

case "$REMOTE_USE_TUNNEL" in
  1|true|yes)
    echo "[dev_remote_sync_up] 本地访问："
    echo "  http://localhost:5173"
    echo "  http://localhost:5174"
    echo "  http://localhost:18080"
    ;;
  0|false|no)
    echo "[dev_remote_sync_up] 远程直连访问："
    echo "  http://${REMOTE_BROWSER_HOST}:5173"
    echo "  http://${REMOTE_BROWSER_HOST}:5174"
    echo "  http://${REMOTE_BROWSER_HOST}:18080"
    echo "[dev_remote_sync_up] 提示：需确保安全组/防火墙已放通 5173/5174/18080。"
    ;;
esac
