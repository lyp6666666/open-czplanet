#!/bin/sh

set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-111.228.20.88:8848}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-beacf12a-2e06-43ae-80f1-6579081850a1}"
NACOS_CONFIG_NAMESPACE="${NACOS_CONFIG_NAMESPACE:-$NACOS_NAMESPACE}"
NACOS_DISCOVERY_NAMESPACE="${NACOS_DISCOVERY_NAMESPACE:-$NACOS_NAMESPACE}"

JWT_ISSUER="${JWT_ISSUER:-ai-tutor}"
JWT_SECRET_PRIMARY="${JWT_SECRET_PRIMARY:-LypJwtSecretKey123LypJwtSecretKey123}"
GATEWAY_JWT_ISSUER="${GATEWAY_JWT_ISSUER:-$JWT_ISSUER}"
GATEWAY_JWT_SECRET="${GATEWAY_JWT_SECRET:-$JWT_SECRET_PRIMARY}"
GATEWAY_SIGN_SECRET="${GATEWAY_SIGN_SECRET:-DevGatewaySignSecretKey_ChangeMe_AtLeast32Bytes}"

JWT_SECRETS_0="${JWT_SECRETS_0:-$JWT_SECRET_PRIMARY}"
GATEWAY_JWT_SECRETS_0="${GATEWAY_JWT_SECRETS_0:-$JWT_SECRET_PRIMARY}"

export JWT_ISSUER JWT_SECRET_PRIMARY GATEWAY_JWT_ISSUER GATEWAY_JWT_SECRET GATEWAY_SIGN_SECRET JWT_SECRETS_0 GATEWAY_JWT_SECRETS_0 NACOS_SERVER_ADDR NACOS_CONFIG_NAMESPACE NACOS_DISCOVERY_NAMESPACE

GATEWAY_PORT="${GATEWAY_PORT:-18080}"
APPOINTMENT_PORT="${APPOINTMENT_PORT:-18081}"
IM_PORT="${IM_PORT:-18082}"
PAYMENT_PORT="${PAYMENT_PORT:-18083}"
ADMIN_PORT="${ADMIN_PORT:-18084}"
WEB_PORT="${WEB_PORT:-5173}"
ADMIN_WEB_PORT="${ADMIN_WEB_PORT:-5174}"

LOG_DIR="$ROOT_DIR/.logs"
PID_DIR="$ROOT_DIR/.pids"
mkdir -p "$LOG_DIR" "$PID_DIR"

echo "[dev_all_up] profile=$SPRING_PROFILES_ACTIVE nacos.server-addr=$NACOS_SERVER_ADDR"
echo "[dev_all_up] nacos.namespace=$NACOS_NAMESPACE"
echo "[dev_all_up] nacos.config.namespace=$NACOS_CONFIG_NAMESPACE"
echo "[dev_all_up] nacos.discovery.namespace=$NACOS_DISCOVERY_NAMESPACE"

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
  nc -z -w 2 "$nacos_host" "$nacos_grpc_port" >/dev/null 2>&1 || {
    echo "[dev_all_up] Nacos gRPC 端口不可达：$nacos_host:$nacos_grpc_port"
    echo "[dev_all_up] 说明：Nacos 2.x 的服务注册（Discovery）需要 gRPC 端口可达（常见为 9848=8848+1000）。"
    echo "[dev_all_up] 处理：请放通 $nacos_grpc_port（以及按部署可能还要 $((nacos_grpc_port + 1))），或绕过仅暴露 8848 的反向代理直连 Nacos 节点。"
    exit 1
  }
else
  echo "[dev_all_up] 未检测到 nc，跳过 Nacos 端口连通性检查"
fi

echo "[dev_all_up] 确认 Docker 可用..."
if ! command -v docker >/dev/null 2>&1; then
  echo "请先安装并启动 Docker Desktop 再运行本脚本"
  exit 1
fi

echo "[dev_all_up] 启动基础依赖（MySQL/Redis/MinIO/RabbitMQ）..."
docker compose -f Dockerfile/docker-compose.yml up -d

echo "[dev_all_up] 构建本地依赖模块（ai-tutor-common/ai-tutor-mq）..."
./mvnw -q -DskipTests install -pl ai-tutor-common,ai-tutor-mq -am

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
    SERVER_PORT="$port" SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" NACOS_NAMESPACE="$NACOS_NAMESPACE" \
      nohup ./mvnw -q -f "$svc_dir/pom.xml" spring-boot:run >"$log_file" 2>&1 &
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
    if [ "$i" -ge 80 ]; then
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

  if [ ! -d "$ROOT_DIR/$app_dir/node_modules" ]; then
    echo "[dev_all_up] 安装 $app_name 依赖（node_modules 不存在）"
    (cd "$ROOT_DIR/$app_dir" && npm ci >/dev/null)
  fi

  echo "[dev_all_up] 启动 $app_name port=$port"
  (
    cd "$ROOT_DIR/$app_dir"
    nohup npm run dev -- --port "$port" --strictPort >"$log_file" 2>&1 &
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
    if [ "$i" -ge 80 ]; then
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
start_frontend "ai-tutor-web" "ai-tutor-web" "$WEB_PORT"
start_frontend "ai-tutor-admin-web" "ai-tutor-admin-web" "$ADMIN_WEB_PORT"

echo "[dev_all_up] 已拉起网关 + 4 个服务 + 2 个前端"
echo "[dev_all_up] 日志目录：$LOG_DIR"
echo "[dev_all_up] PID 目录：$PID_DIR"
echo "[dev_all_up] 用户端前端：http://localhost:$WEB_PORT"
echo "[dev_all_up] 管理端前端：http://localhost:$ADMIN_WEB_PORT"
