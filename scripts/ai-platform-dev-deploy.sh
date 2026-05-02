#!/bin/sh

set -eu

ARCHIVE_PATH="${1:-}"
REVISION="${2:-unknown}"

DEPLOY_PATH="${DEPLOY_PATH:-/opt/ai-platform}"
RELEASES_DIR="${RELEASES_DIR:-/opt/ai-platform-releases}"
RELEASE_ID="$(date +%Y%m%d%H%M%S)-$(printf '%s' "$REVISION" | cut -c1-12)"
RELEASE_DIR="$RELEASES_DIR/$RELEASE_ID"

MANAGE_INFRA="${MANAGE_INFRA:-auto}"
INFRA_CONTAINERS="${INFRA_CONTAINERS:-mysql redis rabbitmq minio minio_init livekit}"
NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-127.0.0.1:18848}"
NACOS_GRPC_CHECK="${NACOS_GRPC_CHECK:-warn}"
TEST_NACOS_TUNNEL_HOST="${TEST_NACOS_TUNNEL_HOST:-111.228.20.88}"
AUTO_VERIFY_DB_SCHEMA="${AUTO_VERIFY_DB_SCHEMA:-0}"
FRONTEND_HOST="${FRONTEND_HOST:-0.0.0.0}"
LIVEKIT_WS_URL="${LIVEKIT_WS_URL:-ws://117.72.111.39/livekit}"
ADMIN_WEB_BASE_PATH="${ADMIN_WEB_BASE_PATH:-/admin/}"
ENABLE_AI_AGENT="${ENABLE_AI_AGENT:-1}"
AI_AGENT_HOST="${AI_AGENT_HOST:-127.0.0.1}"
GATEWAY_PORT="${GATEWAY_PORT:-18080}"
AI_AGENT_PORT="${AI_AGENT_PORT:-18086}"

log() {
  printf '[ai-platform-dev-deploy] %s\n' "$1"
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    log "missing command: $1"
    exit 1
  fi
}

if [ -z "$ARCHIVE_PATH" ] || [ ! -f "$ARCHIVE_PATH" ]; then
  log "usage: $0 /tmp/ai-platform-dev-<sha>.tar.gz <sha>"
  exit 1
fi

require_cmd tar
require_cmd rsync
require_cmd curl

log "revision=$REVISION"
log "archive=$ARCHIVE_PATH"
log "deployPath=$DEPLOY_PATH"

mkdir -p "$RELEASE_DIR" "$DEPLOY_PATH"
tar -xzf "$ARCHIVE_PATH" -C "$RELEASE_DIR"

if [ -f "$DEPLOY_PATH/scripts/dev_all_down.sh" ]; then
  log "stopping current test services"
  (cd "$DEPLOY_PATH" && STOP_INFRA=0 sh scripts/dev_all_down.sh) || true
fi

log "syncing release into deploy path"
rsync -a --delete \
  --exclude='.git/' \
  --exclude='.logs/' \
  --exclude='.pids/' \
  --exclude='.launchd/' \
  --exclude='**/node_modules/' \
  --exclude='**/target/' \
  --exclude='**/.venv/' \
  --exclude='Dockerfile/mysql-data/' \
  --exclude='Dockerfile/redis-data/' \
  --exclude='Dockerfile/minio-data/' \
  --exclude='Dockerfile/prometheus-data/' \
  --exclude='Dockerfile/grafana-data/' \
  "$RELEASE_DIR/" "$DEPLOY_PATH/"

if [ -f "$DEPLOY_PATH/scripts/ai-platform-dev-deploy.sh" ]; then
  if ! cmp -s "$DEPLOY_PATH/scripts/ai-platform-dev-deploy.sh" /usr/local/bin/ai-platform-dev-deploy.sh 2>/dev/null; then
    log "refreshing installed deploy script"
    cp "$DEPLOY_PATH/scripts/ai-platform-dev-deploy.sh" /usr/local/bin/ai-platform-dev-deploy.sh
    chmod 755 /usr/local/bin/ai-platform-dev-deploy.sh
  fi
fi

cd "$DEPLOY_PATH"
printf '%s\n' "$REVISION" > .deploy-revision

log "starting Nacos tunnel"
REMOTE_HOST="$TEST_NACOS_TUNNEL_HOST" bash scripts/nacos_tunnel.sh start

log "starting test environment"
AUTO_VERIFY_DB_SCHEMA="$AUTO_VERIFY_DB_SCHEMA" \
MANAGE_INFRA="$MANAGE_INFRA" \
INFRA_CONTAINERS="$INFRA_CONTAINERS" \
NACOS_GRPC_CHECK="$NACOS_GRPC_CHECK" \
NACOS_SERVER_ADDR="$NACOS_SERVER_ADDR" \
FRONTEND_HOST="$FRONTEND_HOST" \
LIVEKIT_WS_URL="$LIVEKIT_WS_URL" \
ADMIN_WEB_BASE_PATH="$ADMIN_WEB_BASE_PATH" \
ENABLE_AI_AGENT="$ENABLE_AI_AGENT" \
AI_AGENT_HOST="$AI_AGENT_HOST" \
sh scripts/dev_all_up.sh

log "verifying gateway health"
curl -fsS --max-time 15 "http://127.0.0.1:$GATEWAY_PORT/actuator/health" >/dev/null

case "$ENABLE_AI_AGENT" in
  1|true|yes)
    log "verifying ai-agent health"
    curl -fsS --max-time 15 "http://127.0.0.1:$AI_AGENT_PORT/health" >/dev/null

    log "verifying uv environment and Tencent realtime ASR SDK"
    (
      cd "$DEPLOY_PATH/ai-agent-service"
      .venv/bin/python - <<'PY'
from asr import speech_recognizer
from common import credential
print("ai_agent_asr_sdk_ok")
PY
    )
    ;;
esac

log "cleanup old release archives"
rm -f "$ARCHIVE_PATH"
find "$RELEASES_DIR" -mindepth 1 -maxdepth 1 -type d | sort | head -n -5 | xargs -r rm -rf

log "deployment complete"
