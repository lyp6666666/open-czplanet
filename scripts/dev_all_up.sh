#!/bin/sh

set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
NACOS_NAMESPACE="${NACOS_NAMESPACE:-3066af4f-57ee-4f4d-80fe-0d2a4e791f7d}"

GATEWAY_PORT="${GATEWAY_PORT:-18080}"
APPOINTMENT_PORT="${APPOINTMENT_PORT:-18081}"
IM_PORT="${IM_PORT:-18082}"
PAYMENT_PORT="${PAYMENT_PORT:-18083}"
ADMIN_PORT="${ADMIN_PORT:-18084}"

LOG_DIR="$ROOT_DIR/.logs"
PID_DIR="$ROOT_DIR/.pids"
mkdir -p "$LOG_DIR" "$PID_DIR"

echo "[dev_all_up] profile=$SPRING_PROFILES_ACTIVE namespace=$NACOS_NAMESPACE"

echo "[dev_all_up] 确认 Docker 可用..."
if ! command -v docker >/dev/null 2>&1; then
  echo "请先安装并启动 Docker Desktop 再运行本脚本"
  exit 1
fi

echo "[dev_all_up] 启动基础依赖（MySQL/Redis/MinIO/RabbitMQ）..."
docker compose -f Dockerfile/docker-compose.yml up -d

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

  echo "[dev_all_up] 启动 $svc_name port=$port"
  (
    cd "$svc_dir"
    SERVER_PORT="$port" SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" NACOS_NAMESPACE="$NACOS_NAMESPACE" \
      nohup ../mvnw -q spring-boot:run >"$log_file" 2>&1 &
    echo $! >"$pid_file"
  )
}

start_service "ai-tutor-gateway" "ai-tutor-gateway" "$GATEWAY_PORT"
start_service "tutor-appointment-service" "tutor-appointment-service" "$APPOINTMENT_PORT"
start_service "videoCall-IM-service" "videoCall-IM-service" "$IM_PORT"
start_service "payment-service" "payment-service" "$PAYMENT_PORT"
start_service "ai-tutor-admin" "ai-tutor-admin" "$ADMIN_PORT"

echo "[dev_all_up] 已拉起网关 + 4 个服务"
echo "[dev_all_up] 日志目录：$LOG_DIR"
echo "[dev_all_up] PID 目录：$PID_DIR"
