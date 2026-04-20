#!/bin/sh

set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

GATEWAY_PORT="${GATEWAY_PORT:-18080}"
APPOINTMENT_PORT="${APPOINTMENT_PORT:-18081}"
IM_PORT="${IM_PORT:-18082}"
PAYMENT_PORT="${PAYMENT_PORT:-18083}"
ADMIN_PORT="${ADMIN_PORT:-18084}"
LIVE_CLASS_PORT="${LIVE_CLASS_PORT:-18085}"
WEB_PORT="${WEB_PORT:-5173}"
ADMIN_WEB_PORT="${ADMIN_WEB_PORT:-5174}"
STOP_INFRA="${STOP_INFRA:-0}"
DOCKER_COMPOSE_FILE="${DOCKER_COMPOSE_FILE:-Dockerfile/docker-compose.yml}"
INFRA_CONTAINERS="${INFRA_CONTAINERS:-mysql redis rabbitmq minio prometheus grafana livekit}"

PID_DIR="$ROOT_DIR/.pids"

docker_compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
    return
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
    return
  fi
  echo "[dev_all_down] 未检测到 docker compose 或 docker-compose"
  exit 1
}

stop_by_pid_file() {
  svc_name="$1"
  pid_file="$PID_DIR/$svc_name.pid"
  if [ ! -f "$pid_file" ]; then
    return 0
  fi
  pid="$(cat "$pid_file" 2>/dev/null || true)"
  if [ -z "$pid" ]; then
    rm -f "$pid_file"
    return 0
  fi
  if kill -0 "$pid" >/dev/null 2>&1; then
    echo "[dev_all_down] 停止 $svc_name pid=$pid"
    kill "$pid" >/dev/null 2>&1 || true
    i=0
    while kill -0 "$pid" >/dev/null 2>&1; do
      i=$((i + 1))
      if [ "$i" -ge 20 ]; then
        kill -9 "$pid" >/dev/null 2>&1 || true
        break
      fi
      sleep 0.2
    done
  fi
  rm -f "$pid_file"
}

stop_by_port() {
  svc_name="$1"
  port="$2"
  pid="$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null | sed -n '1p' || true)"
  if [ -z "$pid" ]; then
    return 0
  fi
  echo "[dev_all_down] 停止 $svc_name port=$port pid=$pid"
  kill "$pid" >/dev/null 2>&1 || true
  i=0
  while lsof -ti tcp:"$port" -sTCP:LISTEN >/dev/null 2>&1; do
    i=$((i + 1))
    if [ "$i" -ge 20 ]; then
      kill -9 "$pid" >/dev/null 2>&1 || true
      break
    fi
    sleep 0.2
  done
}

is_container_running() {
  container_name="$1"
  docker ps --format '{{.Names}}' | grep -Fx "$container_name" >/dev/null 2>&1
}

any_infra_running() {
  for container_name in $INFRA_CONTAINERS; do
    if is_container_running "$container_name"; then
      return 0
    fi
  done
  return 1
}

stop_by_pid_file "ai-tutor-admin-web"
stop_by_pid_file "ai-tutor-web"
stop_by_pid_file "ai-tutor-admin"
stop_by_pid_file "payment-service"
stop_by_pid_file "videoCall-IM-service"
stop_by_pid_file "tutor-appointment-service"
stop_by_pid_file "ai-tutor-gateway"
stop_by_pid_file "live-class-service"

stop_by_port "ai-tutor-gateway" "$GATEWAY_PORT"
stop_by_port "tutor-appointment-service" "$APPOINTMENT_PORT"
stop_by_port "videoCall-IM-service" "$IM_PORT"
stop_by_port "payment-service" "$PAYMENT_PORT"
stop_by_port "ai-tutor-admin" "$ADMIN_PORT"
stop_by_port "live-class-service" "$LIVE_CLASS_PORT"
stop_by_port "ai-tutor-web" "$WEB_PORT"
stop_by_port "ai-tutor-admin-web" "$ADMIN_WEB_PORT"

if [ "$STOP_INFRA" = "1" ]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "[dev_all_down] 未检测到 docker，跳过基础依赖停止"
  elif [ ! -f "$ROOT_DIR/$DOCKER_COMPOSE_FILE" ]; then
    echo "[dev_all_down] 未找到 compose 文件：$DOCKER_COMPOSE_FILE，跳过基础依赖停止"
  elif any_infra_running; then
    echo "[dev_all_down] 停止基础依赖（STOP_INFRA=1）"
    docker_compose_cmd -f "$DOCKER_COMPOSE_FILE" stop
  else
    echo "[dev_all_down] 基础依赖未在运行，跳过停止"
  fi
else
  echo "[dev_all_down] 保留基础依赖运行中；如需停止请使用 STOP_INFRA=1"
fi

bash scripts/nacos_tunnel.sh stop >/dev/null 2>&1 || true

echo "[dev_all_down] 服务进程已停止（若仍残留 Java 进程，请检查 .logs/*.log 末尾）"
