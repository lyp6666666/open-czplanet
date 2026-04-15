#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PID_DIR="$ROOT_DIR/.pids"
LOG_DIR="$ROOT_DIR/.logs"
mkdir -p "$PID_DIR" "$LOG_DIR"

REMOTE_USER="${REMOTE_USER:-root}"
REMOTE_HOST="${REMOTE_HOST:-111.228.20.88}"
REMOTE_PORT="${REMOTE_PORT:-22}"
GATEWAY_PORT="${GATEWAY_PORT:-18080}"
WEB_PORT="${WEB_PORT:-5173}"
ADMIN_WEB_PORT="${ADMIN_WEB_PORT:-5174}"

PID_FILE="$PID_DIR/ssh-tunnel.pid"
LOG_FILE="$LOG_DIR/ssh-tunnel.log"

ACTION="${1:-start}"

port_is_listening() {
  port="$1"
  lsof -ti tcp:"$port" -sTCP:LISTEN >/dev/null 2>&1
}

is_pid_alive() {
  pid="$1"
  [ -n "$pid" ] && kill -0 "$pid" >/dev/null 2>&1
}

read_pid() {
  if [ -f "$PID_FILE" ]; then
    cat "$PID_FILE" 2>/dev/null || true
  fi
}

check_ports_available() {
  for port in "$WEB_PORT" "$ADMIN_WEB_PORT" "$GATEWAY_PORT"; do
    if port_is_listening "$port"; then
      pid="$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null | sed -n '1p' || true)"
      echo "[ssh_tunnel] 本地端口已被占用：$port pid=${pid:-unknown}"
      echo "[ssh_tunnel] 如确认是旧隧道，请先执行：bash scripts/ssh_tunnel.sh stop"
      exit 1
    fi
  done
}

wait_until_ready() {
  pid="$1"
  i=0
  while true; do
    if ! is_pid_alive "$pid"; then
      echo "[ssh_tunnel] 隧道进程提前退出，请检查 $LOG_FILE"
      exit 1
    fi
    ready=1
    for port in "$WEB_PORT" "$ADMIN_WEB_PORT" "$GATEWAY_PORT"; do
      if ! port_is_listening "$port"; then
        ready=0
        break
      fi
    done
    if [ "$ready" -eq 1 ]; then
      echo "[ssh_tunnel] 隧道已建立 pid=$pid"
      return 0
    fi
    i=$((i + 1))
    if [ "$i" -ge 50 ]; then
      echo "[ssh_tunnel] 等待隧道监听超时，请检查 $LOG_FILE"
      exit 1
    fi
    sleep 0.2
  done
}

start_tunnel() {
  old_pid="$(read_pid)"
  if is_pid_alive "$old_pid"; then
    echo "[ssh_tunnel] 隧道已在运行 pid=$old_pid"
    exit 0
  fi
  rm -f "$PID_FILE"
  check_ports_available

  echo "[ssh_tunnel] remote=${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}"
  echo "[ssh_tunnel] forwards:"
  echo "  localhost:${WEB_PORT} -> remote 127.0.0.1:${WEB_PORT}"
  echo "  localhost:${ADMIN_WEB_PORT} -> remote 127.0.0.1:${ADMIN_WEB_PORT}"
  echo "  localhost:${GATEWAY_PORT} -> remote 127.0.0.1:${GATEWAY_PORT}"

  nohup ssh \
    -N \
    -p "$REMOTE_PORT" \
    -o ExitOnForwardFailure=yes \
    -o ServerAliveInterval=30 \
    -o ServerAliveCountMax=3 \
    -L "${WEB_PORT}:127.0.0.1:${WEB_PORT}" \
    -L "${ADMIN_WEB_PORT}:127.0.0.1:${ADMIN_WEB_PORT}" \
    -L "${GATEWAY_PORT}:127.0.0.1:${GATEWAY_PORT}" \
    "${REMOTE_USER}@${REMOTE_HOST}" >"$LOG_FILE" 2>&1 &

  pid=$!
  echo "$pid" >"$PID_FILE"
  wait_until_ready "$pid"
}

stop_tunnel() {
  pid="$(read_pid)"
  if ! is_pid_alive "$pid"; then
    rm -f "$PID_FILE"
    echo "[ssh_tunnel] 隧道未运行"
    exit 0
  fi

  echo "[ssh_tunnel] 停止隧道 pid=$pid"
  kill "$pid" >/dev/null 2>&1 || true
  i=0
  while is_pid_alive "$pid"; do
    i=$((i + 1))
    if [ "$i" -ge 20 ]; then
      kill -9 "$pid" >/dev/null 2>&1 || true
      break
    fi
    sleep 0.2
  done
  rm -f "$PID_FILE"
}

status_tunnel() {
  pid="$(read_pid)"
  if is_pid_alive "$pid"; then
    echo "[ssh_tunnel] running pid=$pid"
    exit 0
  fi
  echo "[ssh_tunnel] stopped"
  exit 1
}

case "$ACTION" in
  start)
    start_tunnel
    ;;
  stop)
    stop_tunnel
    ;;
  restart)
    stop_tunnel || true
    start_tunnel
    ;;
  status)
    status_tunnel
    ;;
  *)
    echo "usage: bash scripts/ssh_tunnel.sh [start|stop|restart|status]"
    exit 1
    ;;
esac
