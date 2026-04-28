#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

REMOTE_USER="${REMOTE_USER:-root}"
REMOTE_HOST="${REMOTE_HOST:-117.72.111.39}"
REMOTE_PORT="${REMOTE_PORT:-22}"
REMOTE_PATH="${REMOTE_PATH:-/opt/ai-platform}"
REMOTE_MANAGE_INFRA="${REMOTE_MANAGE_INFRA:-auto}"
REMOTE_NACOS_GRPC_CHECK="${REMOTE_NACOS_GRPC_CHECK:-warn}"
REMOTE_NACOS_SERVER_ADDR="${REMOTE_NACOS_SERVER_ADDR:-111.228.20.88:8848}"
REMOTE_USE_TUNNEL="${REMOTE_USE_TUNNEL:-0}"
REMOTE_STOP_INFRA="${REMOTE_STOP_INFRA:-0}"
REMOTE_BROWSER_HOST="${REMOTE_BROWSER_HOST:-$REMOTE_HOST}"
REMOTE_SYNC_DELETE="${REMOTE_SYNC_DELETE:-1}"
REMOTE_INFRA_CONTAINERS="${REMOTE_INFRA_CONTAINERS:-mysql redis rabbitmq minio minio_init livekit}"
REMOTE_ADMIN_WEB_BASE_PATH="${REMOTE_ADMIN_WEB_BASE_PATH:-/admin/}"
REMOTE_ENABLE_AI_AGENT="${REMOTE_ENABLE_AI_AGENT:-0}"
REMOTE_AI_AGENT_HOST="${REMOTE_AI_AGENT_HOST:-127.0.0.1}"

case "$REMOTE_USE_TUNNEL" in
  1|true|yes)
    REMOTE_FRONTEND_HOST="${REMOTE_FRONTEND_HOST:-127.0.0.1}"
    REMOTE_LIVEKIT_WS_URL="${REMOTE_LIVEKIT_WS_URL:-ws://127.0.0.1:7880}"
    ;;
  0|false|no)
    REMOTE_FRONTEND_HOST="${REMOTE_FRONTEND_HOST:-0.0.0.0}"
    REMOTE_LIVEKIT_WS_URL="${REMOTE_LIVEKIT_WS_URL:-ws://${REMOTE_BROWSER_HOST}/livekit}"
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
echo "[dev_remote_sync_up] remote LIVEKIT_WS_URL=$REMOTE_LIVEKIT_WS_URL"
echo "[dev_remote_sync_up] remote SYNC_DELETE=$REMOTE_SYNC_DELETE"
echo "[dev_remote_sync_up] remote INFRA_CONTAINERS=$REMOTE_INFRA_CONTAINERS"
echo "[dev_remote_sync_up] remote ADMIN_WEB_BASE_PATH=$REMOTE_ADMIN_WEB_BASE_PATH"
echo "[dev_remote_sync_up] remote ENABLE_AI_AGENT=$REMOTE_ENABLE_AI_AGENT"
echo "[dev_remote_sync_up] remote AI_AGENT_HOST=$REMOTE_AI_AGENT_HOST"

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
  "cd '$REMOTE_PATH' && MANAGE_INFRA='$REMOTE_MANAGE_INFRA' INFRA_CONTAINERS='$REMOTE_INFRA_CONTAINERS' NACOS_GRPC_CHECK='$REMOTE_NACOS_GRPC_CHECK' NACOS_SERVER_ADDR='$REMOTE_NACOS_SERVER_ADDR' FRONTEND_HOST='$REMOTE_FRONTEND_HOST' LIVEKIT_WS_URL='$REMOTE_LIVEKIT_WS_URL' ADMIN_WEB_BASE_PATH='$REMOTE_ADMIN_WEB_BASE_PATH' ENABLE_AI_AGENT='$REMOTE_ENABLE_AI_AGENT' AI_AGENT_HOST='$REMOTE_AI_AGENT_HOST' sh scripts/dev_all_up.sh"

case "$REMOTE_USE_TUNNEL" in
  1|true|yes)
    echo "[dev_remote_sync_up] 本地访问："
    echo "  http://localhost:5173"
    echo "  http://localhost:5174${REMOTE_ADMIN_WEB_BASE_PATH}"
    echo "  http://localhost:18080"
    echo "  ws://localhost:7880"
    echo "  livekit rtc tcp://localhost:7881"
    ;;
  0|false|no)
    echo "[dev_remote_sync_up] 远程直连访问："
    echo "  http://${REMOTE_BROWSER_HOST}/"
    echo "  http://${REMOTE_BROWSER_HOST}${REMOTE_ADMIN_WEB_BASE_PATH}"
    echo "[dev_remote_sync_up] 原始端口访问（需安全组放通对应端口）："
    echo "  http://${REMOTE_BROWSER_HOST}:5173"
    echo "  http://${REMOTE_BROWSER_HOST}:5174${REMOTE_ADMIN_WEB_BASE_PATH}"
    echo "  http://${REMOTE_BROWSER_HOST}:18080"
    echo "  ws://${REMOTE_BROWSER_HOST}:7880"
    echo "[dev_remote_sync_up] 提示：需确保安全组/防火墙已放通 5173/5174/7880/7881 以及 LiveKit UDP 50000-50100。"
    ;;
esac
