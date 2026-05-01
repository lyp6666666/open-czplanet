#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

DEPLOY_PATH="${DEPLOY_PATH:-/opt/ai-platform-prod-runtime}"
RELEASE_ID="${RELEASE_ID:-$(date +%Y%m%d%H%M%S)-$(git rev-parse --short HEAD 2>/dev/null || echo unknown)}"
BUILD_DIR="${BUILD_DIR:-$ROOT_DIR/.deploy/$RELEASE_ID}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxMetaspaceSize=256m}"
RUN_USER="${RUN_USER:-ai-platform}"
START_TIMEOUT_SECONDS="${START_TIMEOUT_SECONDS:-120}"
ADMIN_BASE_PATH="${ADMIN_BASE_PATH:-/admin/}"
NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-127.0.0.1:8848}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-c3476048-10f6-4cc3-b3f1-90135d736a73}"
NACOS_CONFIG_NAMESPACE="${NACOS_CONFIG_NAMESPACE:-$NACOS_NAMESPACE}"
NACOS_DISCOVERY_NAMESPACE="${NACOS_DISCOVERY_NAMESPACE:-$NACOS_NAMESPACE}"
NACOS_USERNAME="${NACOS_USERNAME:-}"
NACOS_PASSWORD="${NACOS_PASSWORD:-}"
LIVEKIT_WS_URL="${LIVEKIT_WS_URL:-ws://127.0.0.1:7880}"
LIVEKIT_API_KEY="${LIVEKIT_API_KEY:-dev-api-key}"
LIVEKIT_API_SECRET="${LIVEKIT_API_SECRET:-CHANGE_ME_LIVEKIT_API_SECRET}"
DEV_EXPOSE_SMS_CODE="${DEV_EXPOSE_SMS_CODE:-false}"
OPS_VERIFY_TOKEN="${OPS_VERIFY_TOKEN:-}"
JWT_SECRET_PRIMARY="${JWT_SECRET_PRIMARY:-}"
GATEWAY_SIGN_SECRET="${GATEWAY_SIGN_SECRET:-}"

SERVICES="ai-tutor-gateway tutor-appointment-service videoCall-IM-service payment-service ai-tutor-admin live-class-service"
MAVEN_MODULES="ai-tutor-gateway,tutor-appointment-service,videoCall-IM-service,payment-service,ai-tutor-admin,live-class-service"

log() {
  printf "[prod_deploy_jar] %s\n" "$1"
}

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    log "缺少命令：$1"
    exit 1
  fi
}

need_cmd java
need_cmd npm
need_cmd rsync
need_cmd unzip

log "构建 Java 可执行 jar"
./mvnw -q -DskipTests -pl "$MAVEN_MODULES" -am clean package

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/frontend"

for service_name in $SERVICES; do
  jar_file="$service_name/target/$service_name-1.0-SNAPSHOT.jar"
  if [ ! -f "$jar_file" ]; then
    log "缺少构建产物：$jar_file"
    exit 1
  fi
  if ! unzip -p "$jar_file" META-INF/MANIFEST.MF 2>/dev/null | grep -q '^Main-Class: org.springframework.boot.loader'; then
    log "不是 Spring Boot 可执行 jar：$jar_file"
    exit 1
  fi
  cp "$jar_file" "$BUILD_DIR/$service_name.jar"
done

log "构建用户端前端"
(
  cd "$ROOT_DIR/ai-tutor-web"
  if [ -f package-lock.json ]; then
    npm ci
  elif [ ! -d node_modules ]; then
    npm install
  fi
  VITE_BASE_PATH=/ npm run build
)
mkdir -p "$BUILD_DIR/frontend/ai-tutor-web"
rsync -a --delete "$ROOT_DIR/ai-tutor-web/dist/" "$BUILD_DIR/frontend/ai-tutor-web/"

log "构建管理端前端"
(
  cd "$ROOT_DIR/ai-tutor-admin-web"
  if [ -f package-lock.json ]; then
    npm ci
  elif [ ! -d node_modules ]; then
    npm install
  fi
  VITE_BASE_PATH="$ADMIN_BASE_PATH" npm run build
)
mkdir -p "$BUILD_DIR/frontend/ai-tutor-admin-web"
rsync -a --delete "$ROOT_DIR/ai-tutor-admin-web/dist/" "$BUILD_DIR/frontend/ai-tutor-admin-web/"

log "部署 release：$RELEASE_ID"
log "停止旧的 dev_all_up 业务进程（保留中间件）"
STOP_INFRA=0 sh "$ROOT_DIR/scripts/dev_all_down.sh" || true

set -- \
  --deploy-path "$DEPLOY_PATH" \
  --release-id "$RELEASE_ID" \
  --profile "$SPRING_PROFILES_ACTIVE" \
  --java-opts "$JAVA_OPTS" \
  --run-user "$RUN_USER" \
  --timeout "$START_TIMEOUT_SECONDS" \
  --admin-base-path "$ADMIN_BASE_PATH" \
  --nacos-server-addr "$NACOS_SERVER_ADDR" \
  --nacos-namespace "$NACOS_NAMESPACE" \
  --nacos-config-namespace "$NACOS_CONFIG_NAMESPACE" \
  --nacos-discovery-namespace "$NACOS_DISCOVERY_NAMESPACE" \
  --nacos-username "$NACOS_USERNAME" \
  --nacos-password "$NACOS_PASSWORD" \
  --livekit-ws-url "$LIVEKIT_WS_URL" \
  --livekit-api-key "$LIVEKIT_API_KEY" \
  --livekit-api-secret "$LIVEKIT_API_SECRET"

if [ -n "$OPS_VERIFY_TOKEN" ]; then
  set -- "$@" --ops-verify-token "$OPS_VERIFY_TOKEN"
fi
if [ -n "$JWT_SECRET_PRIMARY" ]; then
  set -- "$@" --jwt-secret "$JWT_SECRET_PRIMARY"
fi
if [ -n "$GATEWAY_SIGN_SECRET" ]; then
  set -- "$@" --gateway-sign-secret "$GATEWAY_SIGN_SECRET"
fi

(
  cd "$BUILD_DIR"
  exec bash "$ROOT_DIR/deploy_java_jars.sh" "$@"
)

log "jar 部署完成：$RELEASE_ID"
