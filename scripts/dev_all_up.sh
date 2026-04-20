#!/bin/sh

set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-}"
NACOS_NAMESPACE_DEV="${NACOS_NAMESPACE_DEV:-481e4376-4576-4b18-ac19-f61e170ca3ae}"
NACOS_NAMESPACE_PROD="${NACOS_NAMESPACE_PROD:-44cf681d-9f93-443e-aa9e-ba6ec8f721d5}"
AUTO_NACOS_TUNNEL="${AUTO_NACOS_TUNNEL:-auto}"
LOCAL_NACOS_SERVER_ADDR="${LOCAL_NACOS_SERVER_ADDR:-127.0.0.1:8848}"
TUNNELED_NACOS_SERVER_ADDR="${TUNNELED_NACOS_SERVER_ADDR:-127.0.0.1:18848}"
case "$SPRING_PROFILES_ACTIVE" in
  prod)
    DEFAULT_NACOS_NAMESPACE="$NACOS_NAMESPACE_PROD"
    ;;
  *)
    DEFAULT_NACOS_NAMESPACE="$NACOS_NAMESPACE_DEV"
    ;;
esac
NACOS_NAMESPACE="${NACOS_NAMESPACE:-$DEFAULT_NACOS_NAMESPACE}"
NACOS_CONFIG_NAMESPACE="${NACOS_CONFIG_NAMESPACE:-$NACOS_NAMESPACE}"
NACOS_DISCOVERY_NAMESPACE="${NACOS_DISCOVERY_NAMESPACE:-$NACOS_NAMESPACE}"
NACOS_GRPC_CHECK="${NACOS_GRPC_CHECK:-fail}"

JWT_ISSUER="${JWT_ISSUER:-ai-tutor}"
JWT_SECRET_PRIMARY="${JWT_SECRET_PRIMARY:-LypJwtSecretKey123LypJwtSecretKey123}"
GATEWAY_JWT_ISSUER="${GATEWAY_JWT_ISSUER:-$JWT_ISSUER}"
GATEWAY_JWT_SECRET="${GATEWAY_JWT_SECRET:-$JWT_SECRET_PRIMARY}"
GATEWAY_SIGN_SECRET="${GATEWAY_SIGN_SECRET:-DevGatewaySignSecretKey_ChangeMe_AtLeast32Bytes}"

JWT_SECRETS_0="${JWT_SECRETS_0:-$JWT_SECRET_PRIMARY}"
GATEWAY_JWT_SECRETS_0="${GATEWAY_JWT_SECRETS_0:-$JWT_SECRET_PRIMARY}"

GATEWAY_PORT="${GATEWAY_PORT:-18080}"
APPOINTMENT_PORT="${APPOINTMENT_PORT:-18081}"
IM_PORT="${IM_PORT:-18082}"
PAYMENT_PORT="${PAYMENT_PORT:-18083}"
ADMIN_PORT="${ADMIN_PORT:-18084}"
LIVE_CLASS_PORT="${LIVE_CLASS_PORT:-18085}"
LIVEKIT_PORT="${LIVEKIT_PORT:-7880}"
WEB_PORT="${WEB_PORT:-5173}"
ADMIN_WEB_PORT="${ADMIN_WEB_PORT:-5174}"
FRONTEND_HOST="${FRONTEND_HOST:-127.0.0.1}"
WEB_BASE_PATH="${WEB_BASE_PATH:-/}"
ADMIN_WEB_BASE_PATH="${ADMIN_WEB_BASE_PATH:-/}"
MANAGE_INFRA="${MANAGE_INFRA:-auto}"
SERVICE_STARTUP_WAIT_LOOPS="${SERVICE_STARTUP_WAIT_LOOPS:-300}"
FRONTEND_STARTUP_WAIT_LOOPS="${FRONTEND_STARTUP_WAIT_LOOPS:-150}"
DOCKER_COMPOSE_FILE="${DOCKER_COMPOSE_FILE:-Dockerfile/docker-compose.yml}"
INFRA_CONTAINERS="${INFRA_CONTAINERS:-mysql redis rabbitmq minio prometheus grafana livekit}"
AUTO_BOOTSTRAP_DEV_DB="${AUTO_BOOTSTRAP_DEV_DB:-1}"
LIVEKIT_API_KEY="${LIVEKIT_API_KEY:-dev-api-key}"
LIVEKIT_API_SECRET="${LIVEKIT_API_SECRET:-CHANGE_ME_LIVEKIT_API_SECRET}"
LIVEKIT_WS_URL="${LIVEKIT_WS_URL:-ws://127.0.0.1:${LIVEKIT_PORT}}"
OPS_VERIFY_TOKEN="${OPS_VERIFY_TOKEN:-DevOpsVerifyTokenForE2E}"
DEV_EXPOSE_SMS_CODE="${DEV_EXPOSE_SMS_CODE:-true}"

LOG_DIR="$ROOT_DIR/.logs"
PID_DIR="$ROOT_DIR/.pids"
mkdir -p "$LOG_DIR" "$PID_DIR"

nacos_addr_host() {
  echo "$1" | awk -F: '{print $1}'
}

nacos_addr_port() {
  port="$(echo "$1" | awk -F: '{print $2}')"
  if [ -z "$port" ]; then
    port="8848"
  fi
  echo "$port"
}

http_endpoint_reachable() {
  addr="$1"
  host="$(nacos_addr_host "$addr")"
  port="$(nacos_addr_port "$addr")"
  if command -v curl >/dev/null 2>&1; then
    code="$(curl -s -o /dev/null -m 2 -w '%{http_code}' "http://${host}:${port}/nacos/" || true)"
    case "$code" in
      200|301|302|401|403)
        return 0
        ;;
    esac
  fi
  if command -v nc >/dev/null 2>&1; then
    nc -z -w 2 "$host" "$port" >/dev/null 2>&1
    return $?
  fi
  return 1
}

ensure_nacos_server_addr() {
  if [ -n "$NACOS_SERVER_ADDR" ]; then
    return 0
  fi

  if http_endpoint_reachable "$LOCAL_NACOS_SERVER_ADDR"; then
    NACOS_SERVER_ADDR="$LOCAL_NACOS_SERVER_ADDR"
    echo "[dev_all_up] 使用本机 Nacos：$NACOS_SERVER_ADDR"
    return 0
  fi

  if http_endpoint_reachable "$TUNNELED_NACOS_SERVER_ADDR"; then
    NACOS_SERVER_ADDR="$TUNNELED_NACOS_SERVER_ADDR"
    echo "[dev_all_up] 使用现有本地 Nacos 隧道：$NACOS_SERVER_ADDR"
    return 0
  fi

  case "$AUTO_NACOS_TUNNEL" in
    auto|always)
      echo "[dev_all_up] 本机未检测到可用 Nacos，尝试建立本地 Nacos 隧道..."
      bash scripts/nacos_tunnel.sh start
      NACOS_SERVER_ADDR="$TUNNELED_NACOS_SERVER_ADDR"
      echo "[dev_all_up] 使用隧道 Nacos：$NACOS_SERVER_ADDR"
      ;;
    never)
      echo "[dev_all_up] 本机未检测到可用 Nacos，且 AUTO_NACOS_TUNNEL=never"
      echo "[dev_all_up] 请手动设置 NACOS_SERVER_ADDR，或先执行 bash scripts/nacos_tunnel.sh start"
      exit 1
      ;;
    *)
      echo "[dev_all_up] 不支持的 AUTO_NACOS_TUNNEL=$AUTO_NACOS_TUNNEL，可选值：auto/always/never"
      exit 1
      ;;
  esac
}

ensure_nacos_server_addr

export JWT_ISSUER JWT_SECRET_PRIMARY GATEWAY_JWT_ISSUER GATEWAY_JWT_SECRET GATEWAY_SIGN_SECRET JWT_SECRETS_0 GATEWAY_JWT_SECRETS_0 NACOS_SERVER_ADDR NACOS_CONFIG_NAMESPACE NACOS_DISCOVERY_NAMESPACE LIVEKIT_API_KEY LIVEKIT_API_SECRET LIVEKIT_WS_URL OPS_VERIFY_TOKEN DEV_EXPOSE_SMS_CODE
SPRING_APPLICATION_JSON=$(cat <<EOF
{"ops":{"verifyToken":"$OPS_VERIFY_TOKEN"},"dev":{"exposeSmsCode":$DEV_EXPOSE_SMS_CODE},"livekit":{"apiKey":"$LIVEKIT_API_KEY","apiSecret":"$LIVEKIT_API_SECRET","wsUrl":"$LIVEKIT_WS_URL"}}
EOF
)
export SPRING_APPLICATION_JSON

echo "[dev_all_up] profile=$SPRING_PROFILES_ACTIVE nacos.server-addr=$NACOS_SERVER_ADDR"
echo "[dev_all_up] nacos.namespace.dev=$NACOS_NAMESPACE_DEV"
echo "[dev_all_up] nacos.namespace.prod=$NACOS_NAMESPACE_PROD"
echo "[dev_all_up] nacos.namespace=$NACOS_NAMESPACE"
echo "[dev_all_up] nacos.config.namespace=$NACOS_CONFIG_NAMESPACE"
echo "[dev_all_up] nacos.discovery.namespace=$NACOS_DISCOVERY_NAMESPACE"
echo "[dev_all_up] nacos.grpc.check=$NACOS_GRPC_CHECK"
echo "[dev_all_up] manage.infra=$MANAGE_INFRA"
echo "[dev_all_up] auto.nacos.tunnel=$AUTO_NACOS_TUNNEL"
echo "[dev_all_up] livekit.ws-url=$LIVEKIT_WS_URL"
echo "[dev_all_up] dev.exposeSmsCode=$DEV_EXPOSE_SMS_CODE"

if command -v nc >/dev/null 2>&1; then
  nacos_host="$(echo "$NACOS_SERVER_ADDR" | awk -F: '{print $1}')"
  nacos_port="$(echo "$NACOS_SERVER_ADDR" | awk -F: '{print $2}')"
  if [ -z "$nacos_port" ]; then
    nacos_port="8848"
  fi
  nacos_grpc_port=$((nacos_port + 1000))
  echo "[dev_all_up] 检查 Nacos 端口连通性：$nacos_host:$nacos_port (HTTP), $nacos_host:$nacos_grpc_port (gRPC)"
  nc -z -w 2 "$nacos_host" "$nacos_port" >/dev/null 2>&1 || {
    echo "[dev_all_up] Nacos HTTP 端口不可达：$nacos_host:$nacos_port"
    exit 1
  }
  if ! nc -z -w 2 "$nacos_host" "$nacos_grpc_port" >/dev/null 2>&1; then
    case "$NACOS_GRPC_CHECK" in
      skip)
        echo "[dev_all_up] 跳过 Nacos gRPC 端口检查（NACOS_GRPC_CHECK=skip）"
        ;;
      warn)
        echo "[dev_all_up] 警告：Nacos gRPC 端口不可达：$nacos_host:$nacos_grpc_port，将继续启动。若使用服务发现，可能导致注册/订阅失败。"
        ;;
      fail)
        echo "[dev_all_up] Nacos gRPC 端口不可达：$nacos_host:$nacos_grpc_port"
        echo "[dev_all_up] 说明：Nacos 2.x 的服务注册（Discovery）需要 gRPC 端口可达（常见为 9848=8848+1000）。"
        echo "[dev_all_up] 处理：请放通 $nacos_grpc_port（以及按部署可能还要 $((nacos_grpc_port + 1))），或绕过仅暴露 8848 的反向代理直连 Nacos 节点。"
        exit 1
        ;;
      *)
        echo "[dev_all_up] 不支持的 NACOS_GRPC_CHECK=$NACOS_GRPC_CHECK，可选值：fail/warn/skip"
        exit 1
        ;;
    esac
  fi
else
  echo "[dev_all_up] 未检测到 nc，跳过 Nacos 端口连通性检查"
fi

echo "[dev_all_up] 确认 Docker 可用..."
if ! command -v docker >/dev/null 2>&1; then
  echo "请先安装并启动 Docker Desktop 再运行本脚本"
  exit 1
fi

docker_compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
    return
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
    return
  fi
  echo "[dev_all_up] 未检测到 docker compose 或 docker-compose"
  exit 1
}

is_container_running() {
  container_name="$1"
  docker ps --format '{{.Names}}' | grep -Fx "$container_name" >/dev/null 2>&1
}

container_exists() {
  container_name="$1"
  docker ps -a --format '{{.Names}}' | grep -Fx "$container_name" >/dev/null 2>&1
}

start_infra() {
  if [ ! -f "$ROOT_DIR/$DOCKER_COMPOSE_FILE" ]; then
    echo "[dev_all_up] 未找到 compose 文件：$DOCKER_COMPOSE_FILE"
    exit 1
  fi
  echo "[dev_all_up] 启动基础依赖..."
  docker_compose_cmd -f "$DOCKER_COMPOSE_FILE" up -d
}

ensure_infra_running() {
  missing_services=""
  for container_name in $INFRA_CONTAINERS; do
    if is_container_running "$container_name"; then
      echo "[dev_all_up] 基础依赖已运行：$container_name"
      continue
    fi
    if container_exists "$container_name"; then
      echo "[dev_all_up] 基础依赖已存在但未运行，执行 docker start：$container_name"
      docker start "$container_name" >/dev/null
      continue
    fi
    missing_services="$missing_services $container_name"
  done

  if [ -n "$missing_services" ]; then
    echo "[dev_all_up] 缺少基础依赖，使用 compose 补启动:${missing_services}"
    docker_compose_cmd -f "$DOCKER_COMPOSE_FILE" up -d $missing_services
  else
    echo "[dev_all_up] 基础依赖已就绪，跳过 compose 启动"
  fi
}

case "$MANAGE_INFRA" in
  never)
    echo "[dev_all_up] 跳过基础依赖管理（MANAGE_INFRA=never）"
    ;;
  always)
    start_infra
    ;;
  auto)
    ensure_infra_running
    ;;
  *)
    echo "[dev_all_up] 不支持的 MANAGE_INFRA=$MANAGE_INFRA，可选值：auto/always/never"
    exit 1
    ;;
esac

case "$AUTO_BOOTSTRAP_DEV_DB" in
  1|true|yes)
    echo "[dev_all_up] 检查开发库是否需要初始化..."
    sh scripts/db_bootstrap_if_missing.sh
    ;;
  0|false|no)
    echo "[dev_all_up] 跳过开发库初始化（AUTO_BOOTSTRAP_DEV_DB=$AUTO_BOOTSTRAP_DEV_DB）"
    ;;
  *)
    echo "[dev_all_up] 不支持的 AUTO_BOOTSTRAP_DEV_DB=$AUTO_BOOTSTRAP_DEV_DB，可选值：1/0 true/false yes/no"
    exit 1
    ;;
esac

echo "[dev_all_up] 构建本地依赖模块（ai-tutor-common/ai-tutor-mq，跳过测试与测试编译）..."
./mvnw -q -Dmaven.test.skip=true install -pl ai-tutor-common,ai-tutor-mq -am

start_service() {
  svc_dir="$1"
  svc_name="$2"
  port="$3"
  log_file="$LOG_DIR/$svc_name.log"
  pid_file="$PID_DIR/$svc_name.pid"

  if [ -f "$pid_file" ]; then
    old_pid="$(cat "$pid_file" 2>/dev/null || true)"
    if [ -n "$old_pid" ] && kill -0 "$old_pid" >/dev/null 2>&1; then
      echo "[dev_all_up] $svc_name 已在运行 pid=$old_pid"
      return 0
    fi
    rm -f "$pid_file"
  fi

  existing_listen_pid="$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null || true)"
  if [ -n "$existing_listen_pid" ]; then
    echo "[dev_all_up] $svc_name port=$port 已被占用，尝试停止 pid=$existing_listen_pid"
    kill "$existing_listen_pid" >/dev/null 2>&1 || true
    i=0
    while lsof -ti tcp:"$port" -sTCP:LISTEN >/dev/null 2>&1; do
      i=$((i + 1))
      if [ "$i" -ge 20 ]; then
        kill -9 "$existing_listen_pid" >/dev/null 2>&1 || true
        break
      fi
      sleep 0.2
    done
  fi

  echo "[dev_all_up] 启动 $svc_name port=$port"
  (
    cd "$ROOT_DIR"
    # 远程同步通常会保留源码时间戳；先 clean 再 compile，避免 resources 插件因 target 更新时间更晚而继续复用旧配置。
    ./mvnw -q -Dmaven.test.skip=true -f "$svc_dir/pom.xml" clean compile >/dev/null
    SERVER_PORT="$port" SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" NACOS_NAMESPACE="$NACOS_NAMESPACE" \
      nohup ./mvnw -q -Dmaven.test.skip=true -f "$svc_dir/pom.xml" spring-boot:run >"$log_file" 2>&1 &
    launcher_pid=$!
    echo "$launcher_pid" >"$pid_file"
  )

  i=0
  while true; do
    listen_pid="$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null | sed -n '1p' || true)"
    if [ -n "$listen_pid" ]; then
      echo "$listen_pid" >"$pid_file"
      return 0
    fi
    i=$((i + 1))
    if [ "$i" -ge "$SERVICE_STARTUP_WAIT_LOOPS" ]; then
      echo "[dev_all_up] $svc_name 启动失败（端口 $port 未监听）"
      if [ -f "$log_file" ]; then
        echo "[dev_all_up] $svc_name 最近日志（$log_file）"
        tail -n 120 "$log_file" || true
      fi
      return 1
    fi
    sleep 0.2
  done
}

start_frontend() {
  app_dir="$1"
  app_name="$2"
  port="$3"
  base_path="$4"
  log_file="$LOG_DIR/$app_name.log"
  pid_file="$PID_DIR/$app_name.pid"

  if ! command -v node >/dev/null 2>&1; then
    echo "[dev_all_up] 未检测到 node，跳过启动 $app_name"
    return 0
  fi
  if ! command -v npm >/dev/null 2>&1; then
    echo "[dev_all_up] 未检测到 npm，跳过启动 $app_name"
    return 0
  fi

  if [ -f "$pid_file" ]; then
    old_pid="$(cat "$pid_file" 2>/dev/null || true)"
    if [ -n "$old_pid" ] && kill -0 "$old_pid" >/dev/null 2>&1; then
      echo "[dev_all_up] $app_name 已在运行 pid=$old_pid"
      return 0
    fi
    rm -f "$pid_file"
  fi

  existing_listen_pid="$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null || true)"
  if [ -n "$existing_listen_pid" ]; then
    echo "[dev_all_up] $app_name port=$port 已被占用，尝试停止 pid=$existing_listen_pid"
    kill "$existing_listen_pid" >/dev/null 2>&1 || true
    i=0
    while lsof -ti tcp:"$port" -sTCP:LISTEN >/dev/null 2>&1; do
      i=$((i + 1))
      if [ "$i" -ge 20 ]; then
        kill -9 "$existing_listen_pid" >/dev/null 2>&1 || true
        break
      fi
      sleep 0.2
    done
  fi

  lock_file="$ROOT_DIR/$app_dir/package-lock.json"
  lock_stamp="$ROOT_DIR/$app_dir/node_modules/.package-lock.sync"
  if [ ! -d "$ROOT_DIR/$app_dir/node_modules" ]; then
    echo "[dev_all_up] 安装 $app_name 依赖（node_modules 不存在）"
    (cd "$ROOT_DIR/$app_dir" && npm ci >/dev/null)
    [ -f "$lock_file" ] && cp "$lock_file" "$lock_stamp"
  elif [ -f "$lock_file" ] && { [ ! -f "$lock_stamp" ] || ! cmp -s "$lock_file" "$lock_stamp"; }; then
    echo "[dev_all_up] 检测到 $app_name 锁文件变更，重新安装依赖"
    (cd "$ROOT_DIR/$app_dir" && npm ci >/dev/null)
    cp "$lock_file" "$lock_stamp"
  fi

  echo "[dev_all_up] 启动 $app_name host=$FRONTEND_HOST port=$port base=$base_path"
  (
    cd "$ROOT_DIR/$app_dir"
    VITE_BASE_PATH="$base_path" nohup npm run dev -- --host "$FRONTEND_HOST" --port "$port" --strictPort >"$log_file" 2>&1 &
    launcher_pid=$!
    echo "$launcher_pid" >"$pid_file"
  )

  i=0
  while true; do
    listen_pid="$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null | sed -n '1p' || true)"
    if [ -n "$listen_pid" ]; then
      echo "$listen_pid" >"$pid_file"
      return 0
    fi
    i=$((i + 1))
    if [ "$i" -ge "$FRONTEND_STARTUP_WAIT_LOOPS" ]; then
      echo "[dev_all_up] $app_name 启动失败（端口 $port 未监听）"
      if [ -f "$log_file" ]; then
        echo "[dev_all_up] $app_name 最近日志（$log_file）"
        tail -n 120 "$log_file" || true
      fi
      return 1
    fi
    sleep 0.2
  done
}

start_service "ai-tutor-gateway" "ai-tutor-gateway" "$GATEWAY_PORT"
start_service "tutor-appointment-service" "tutor-appointment-service" "$APPOINTMENT_PORT"
start_service "videoCall-IM-service" "videoCall-IM-service" "$IM_PORT"
start_service "payment-service" "payment-service" "$PAYMENT_PORT"
start_service "ai-tutor-admin" "ai-tutor-admin" "$ADMIN_PORT"
start_service "live-class-service" "live-class-service" "$LIVE_CLASS_PORT"
start_frontend "ai-tutor-web" "ai-tutor-web" "$WEB_PORT" "$WEB_BASE_PATH"
start_frontend "ai-tutor-admin-web" "ai-tutor-admin-web" "$ADMIN_WEB_PORT" "$ADMIN_WEB_BASE_PATH"

echo "[dev_all_up] 已拉起网关 + 5 个服务 + 2 个前端"
echo "[dev_all_up] 日志目录：$LOG_DIR"
echo "[dev_all_up] PID 目录：$PID_DIR"
echo "[dev_all_up] 用户端前端：http://$FRONTEND_HOST:$WEB_PORT"
echo "[dev_all_up] 管理端前端：http://$FRONTEND_HOST:$ADMIN_WEB_PORT"
