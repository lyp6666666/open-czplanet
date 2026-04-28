#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PID_DIR="$ROOT_DIR/.pids"
LOG_DIR="$ROOT_DIR/.logs"
LAUNCHD_DIR="$ROOT_DIR/.launchd"
mkdir -p "$PID_DIR" "$LOG_DIR" "$LAUNCHD_DIR"
OS_NAME="$(uname -s 2>/dev/null || echo unknown)"

REMOTE_USER="${REMOTE_USER:-root}"
REMOTE_HOST="${REMOTE_HOST:-111.228.20.88}"
REMOTE_PORT="${REMOTE_PORT:-22}"

NACOS_LOCAL_PORT="${NACOS_LOCAL_PORT:-18848}"
NACOS_GRPC_LOCAL_PORT="${NACOS_GRPC_LOCAL_PORT:-19848}"
NACOS_RAFT_LOCAL_PORT="${NACOS_RAFT_LOCAL_PORT:-19849}"

NACOS_REMOTE_PORT="${NACOS_REMOTE_PORT:-8848}"
NACOS_GRPC_REMOTE_PORT="${NACOS_GRPC_REMOTE_PORT:-9848}"
NACOS_RAFT_REMOTE_PORT="${NACOS_RAFT_REMOTE_PORT:-9849}"

PID_FILE="$PID_DIR/nacos-tunnel.pid"
LOG_FILE="$LOG_DIR/nacos-tunnel.log"
LAUNCHD_LABEL="com.ai.tutor.dev.nacos-tunnel"

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
  for port in "$NACOS_LOCAL_PORT" "$NACOS_GRPC_LOCAL_PORT" "$NACOS_RAFT_LOCAL_PORT"; do
    if port_is_listening "$port"; then
      pid="$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null | sed -n '1p' || true)"
      echo "[nacos_tunnel] 本地端口已被占用：$port pid=${pid:-unknown}"
      echo "[nacos_tunnel] 如确认是旧隧道，请先执行：bash scripts/nacos_tunnel.sh stop"
      exit 1
    fi
  done
}

wait_until_ready() {
  pid="$1"
  i=0
  while true; do
    if ! is_pid_alive "$pid"; then
      echo "[nacos_tunnel] 隧道进程提前退出，请检查 $LOG_FILE"
      exit 1
    fi
    ready=1
    for port in "$NACOS_LOCAL_PORT" "$NACOS_GRPC_LOCAL_PORT" "$NACOS_RAFT_LOCAL_PORT"; do
      if ! port_is_listening "$port"; then
        ready=0
        break
      fi
    done
    if [ "$ready" -eq 1 ]; then
      echo "[nacos_tunnel] 隧道已建立 pid=$pid"
      return 0
    fi
    i=$((i + 1))
    if [ "$i" -ge 50 ]; then
      echo "[nacos_tunnel] 等待隧道监听超时，请检查 $LOG_FILE"
      exit 1
    fi
    sleep 0.2
  done
}

start_tunnel() {
  old_pid="$(read_pid)"
  if is_pid_alive "$old_pid"; then
    echo "[nacos_tunnel] 隧道已在运行 pid=$old_pid"
    exit 0
  fi
  rm -f "$PID_FILE"
  check_ports_available

  echo "[nacos_tunnel] remote=${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PORT}"
  echo "[nacos_tunnel] forwards:"
  echo "  localhost:${NACOS_LOCAL_PORT} -> remote 127.0.0.1:${NACOS_REMOTE_PORT}"
  echo "  localhost:${NACOS_GRPC_LOCAL_PORT} -> remote 127.0.0.1:${NACOS_GRPC_REMOTE_PORT}"
  echo "  localhost:${NACOS_RAFT_LOCAL_PORT} -> remote 127.0.0.1:${NACOS_RAFT_REMOTE_PORT}"

  if [ "$OS_NAME" = "Darwin" ] && command -v launchctl >/dev/null 2>&1; then
    wrapper_script="$LAUNCHD_DIR/$LAUNCHD_LABEL.sh"
    plist_file="$LAUNCHD_DIR/$LAUNCHD_LABEL.plist"
    cat >"$wrapper_script" <<EOF
#!/bin/sh
cd '$ROOT_DIR'
echo \$\$ > '$PID_FILE'
exec ssh -N -p '$REMOTE_PORT' \\
  -o ExitOnForwardFailure=yes \\
  -o ServerAliveInterval=30 \\
  -o ServerAliveCountMax=3 \\
  -L '${NACOS_LOCAL_PORT}:127.0.0.1:${NACOS_REMOTE_PORT}' \\
  -L '${NACOS_GRPC_LOCAL_PORT}:127.0.0.1:${NACOS_GRPC_REMOTE_PORT}' \\
  -L '${NACOS_RAFT_LOCAL_PORT}:127.0.0.1:${NACOS_RAFT_REMOTE_PORT}' \\
  '${REMOTE_USER}@${REMOTE_HOST}'
EOF
    chmod +x "$wrapper_script"
    cat >"$plist_file" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>$LAUNCHD_LABEL</string>
  <key>ProgramArguments</key>
  <array>
    <string>$wrapper_script</string>
  </array>
  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <false/>
  <key>WorkingDirectory</key>
  <string>$ROOT_DIR</string>
  <key>StandardOutPath</key>
  <string>$LOG_FILE</string>
  <key>StandardErrorPath</key>
  <string>$LOG_FILE</string>
</dict>
</plist>
EOF
    launchctl bootout "gui/$(id -u)/$LAUNCHD_LABEL" >/dev/null 2>&1 || true
    launchctl bootstrap "gui/$(id -u)" "$plist_file" >/dev/null
    launchctl kickstart -k "gui/$(id -u)/$LAUNCHD_LABEL" >/dev/null
    wait_loops=0
    while [ ! -s "$PID_FILE" ]; do
      wait_loops=$((wait_loops + 1))
      if [ "$wait_loops" -ge 50 ]; then
        break
      fi
      sleep 0.1
    done
    pid="$(read_pid)"
  else
    nohup ssh \
      -N \
      -p "$REMOTE_PORT" \
      -o ExitOnForwardFailure=yes \
      -o ServerAliveInterval=30 \
      -o ServerAliveCountMax=3 \
      -L "${NACOS_LOCAL_PORT}:127.0.0.1:${NACOS_REMOTE_PORT}" \
      -L "${NACOS_GRPC_LOCAL_PORT}:127.0.0.1:${NACOS_GRPC_REMOTE_PORT}" \
      -L "${NACOS_RAFT_LOCAL_PORT}:127.0.0.1:${NACOS_RAFT_REMOTE_PORT}" \
      "${REMOTE_USER}@${REMOTE_HOST}" >"$LOG_FILE" 2>&1 &
    pid=$!
    echo "$pid" >"$PID_FILE"
  fi
  wait_until_ready "$pid"
}

stop_tunnel() {
  if [ "$OS_NAME" = "Darwin" ] && command -v launchctl >/dev/null 2>&1; then
    launchctl bootout "gui/$(id -u)/$LAUNCHD_LABEL" >/dev/null 2>&1 || true
  fi
  pid="$(read_pid)"
  if ! is_pid_alive "$pid"; then
    rm -f "$PID_FILE"
    echo "[nacos_tunnel] 隧道未运行"
    exit 0
  fi

  echo "[nacos_tunnel] 停止隧道 pid=$pid"
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
    echo "[nacos_tunnel] running pid=$pid"
    exit 0
  fi
  echo "[nacos_tunnel] stopped"
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
    echo "usage: bash scripts/nacos_tunnel.sh [start|stop|restart|status]"
    exit 1
    ;;
esac
