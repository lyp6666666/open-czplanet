#!/bin/sh

set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

GATEWAY_PORT="${GATEWAY_PORT:-18080}"
APPOINTMENT_PORT="${APPOINTMENT_PORT:-18081}"
IM_PORT="${IM_PORT:-18082}"
PAYMENT_PORT="${PAYMENT_PORT:-18083}"
ADMIN_PORT="${ADMIN_PORT:-18084}"
WEB_PORT="${WEB_PORT:-5173}"
ADMIN_WEB_PORT="${ADMIN_WEB_PORT:-5174}"

PID_DIR="$ROOT_DIR/.pids"

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

stop_by_pid_file "ai-tutor-admin-web"
stop_by_pid_file "ai-tutor-web"
stop_by_pid_file "ai-tutor-admin"
stop_by_pid_file "payment-service"
stop_by_pid_file "videoCall-IM-service"
stop_by_pid_file "tutor-appointment-service"
stop_by_pid_file "ai-tutor-gateway"

stop_by_port "ai-tutor-gateway" "$GATEWAY_PORT"
stop_by_port "tutor-appointment-service" "$APPOINTMENT_PORT"
stop_by_port "videoCall-IM-service" "$IM_PORT"
stop_by_port "payment-service" "$PAYMENT_PORT"
stop_by_port "ai-tutor-admin" "$ADMIN_PORT"
stop_by_port "ai-tutor-web" "$WEB_PORT"
stop_by_port "ai-tutor-admin-web" "$ADMIN_WEB_PORT"

echo "[dev_all_down] 服务进程已停止（若仍残留 Java 进程，请检查 .logs/*.log 末尾）"
