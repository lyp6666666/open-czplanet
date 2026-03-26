#!/bin/sh

set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

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

stop_by_pid_file "ai-tutor-admin"
stop_by_pid_file "payment-service"
stop_by_pid_file "videoCall-IM-service"
stop_by_pid_file "tutor-appointment-service"
stop_by_pid_file "ai-tutor-gateway"

echo "[dev_all_down] 服务进程已停止（若仍残留 Java 进程，请检查 .logs/*.log 末尾）"
