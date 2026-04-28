#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PID_DIR="$ROOT_DIR/.pids"
LOG_DIR="$ROOT_DIR/.logs"
mkdir -p "$PID_DIR" "$LOG_DIR"

ACTION="${1:-start}"
TUNNEL_PROVIDER="${TUNNEL_PROVIDER:-auto}"
PUBLIC_TUNNEL_HOST="${PUBLIC_TUNNEL_HOST:-127.0.0.1}"
WEB_PORT="${WEB_PORT:-5173}"
ADMIN_WEB_PORT="${ADMIN_WEB_PORT:-5174}"
PUBLIC_TUNNEL_WAIT_LOOPS="${PUBLIC_TUNNEL_WAIT_LOOPS:-120}"

WEB_PID_FILE="$PID_DIR/local-public-tunnel-web.pid"
ADMIN_PID_FILE="$PID_DIR/local-public-tunnel-admin-web.pid"
WEB_LOG_FILE="$LOG_DIR/local-public-tunnel-web.log"
ADMIN_LOG_FILE="$LOG_DIR/local-public-tunnel-admin-web.log"
URL_FILE="$PID_DIR/local-public-tunnel.urls"

is_pid_alive() {
  pid="$1"
  [ -n "$pid" ] && kill -0 "$pid" >/dev/null 2>&1
}

read_pid() {
  pid_file="$1"
  if [ -f "$pid_file" ]; then
    cat "$pid_file" 2>/dev/null || true
  fi
}

detect_provider() {
  case "$TUNNEL_PROVIDER" in
    auto)
      if command -v cloudflared >/dev/null 2>&1; then
        echo "cloudflared"
        return 0
      fi
      if command -v npx >/dev/null 2>&1; then
        echo "localtunnel"
        return 0
      fi
      echo "[local_public_tunnel] 未检测到 cloudflared 或 npx，无法自动创建公网隧道" >&2
      echo "[local_public_tunnel] 可安装 cloudflared，或确保 Node.js/npm 可用后重试" >&2
      exit 1
      ;;
    cloudflared)
      command -v cloudflared >/dev/null 2>&1 || {
        echo "[local_public_tunnel] TUNNEL_PROVIDER=cloudflared，但未检测到 cloudflared" >&2
        exit 1
      }
      echo "cloudflared"
      ;;
    localtunnel)
      command -v npx >/dev/null 2>&1 || {
        echo "[local_public_tunnel] TUNNEL_PROVIDER=localtunnel，但未检测到 npx" >&2
        exit 1
      }
      echo "localtunnel"
      ;;
    none|off|0|false|no)
      echo "none"
      ;;
    *)
      echo "[local_public_tunnel] 不支持的 TUNNEL_PROVIDER=${TUNNEL_PROVIDER}，可选值：auto/cloudflared/localtunnel/none" >&2
      exit 1
      ;;
  esac
}

extract_url() {
  log_file="$1"
  grep -Eo 'https://[^[:space:]]+' "$log_file" 2>/dev/null | grep -E '(trycloudflare\.com|loca\.lt)' | tail -n 1 | sed 's/[[:punct:]]*$//'
}

public_ip() {
  if ! command -v curl >/dev/null 2>&1; then
    return 1
  fi
  curl -fsS --max-time 5 https://ipv4.icanhazip.com 2>/dev/null | tr -d '[:space:]'
}

wait_for_url() {
  label="$1"
  log_file="$2"
  pid_file="$3"
  i=0
  while true; do
    url="$(extract_url "$log_file" || true)"
    if [ -n "$url" ]; then
      echo "$url"
      return 0
    fi

    pid="$(read_pid "$pid_file")"
    if ! is_pid_alive "$pid"; then
      echo "[local_public_tunnel] $label 隧道进程提前退出，请检查 $log_file" >&2
      [ -f "$log_file" ] && tail -n 80 "$log_file" >&2 || true
      return 1
    fi

    i=$((i + 1))
    if [ "$i" -ge "$PUBLIC_TUNNEL_WAIT_LOOPS" ]; then
      echo "[local_public_tunnel] 等待 $label 公网 URL 超时，请检查 $log_file" >&2
      [ -f "$log_file" ] && tail -n 80 "$log_file" >&2 || true
      return 1
    fi
    sleep 0.5
  done
}

start_one() {
  label="$1"
  port="$2"
  pid_file="$3"
  log_file="$4"
  provider="$5"

  old_pid="$(read_pid "$pid_file")"
  if is_pid_alive "$old_pid"; then
    old_url="$(extract_url "$log_file" || true)"
    if [ -n "$old_url" ]; then
      echo "$old_url"
      return 0
    fi
    echo "[local_public_tunnel] $label 隧道已在运行 pid=$old_pid，继续等待 URL" >&2
    wait_for_url "$label" "$log_file" "$pid_file"
    return 0
  fi

  rm -f "$pid_file" "$log_file"
  echo "[local_public_tunnel] 启动 $label 公网隧道：127.0.0.1:$port" >&2

  case "$provider" in
    cloudflared)
      nohup cloudflared tunnel --url "http://${PUBLIC_TUNNEL_HOST}:${port}" --no-autoupdate >"$log_file" 2>&1 &
      ;;
    localtunnel)
      nohup npx --yes localtunnel --port "$port" --local-host "$PUBLIC_TUNNEL_HOST" >"$log_file" 2>&1 &
      ;;
  esac
  pid="$!"
  echo "$pid" >"$pid_file"
  wait_for_url "$label" "$log_file" "$pid_file"
}

stop_one() {
  label="$1"
  pid_file="$2"
  pid="$(read_pid "$pid_file")"
  if ! is_pid_alive "$pid"; then
    rm -f "$pid_file"
    return 0
  fi

  echo "[local_public_tunnel] 停止 $label 公网隧道 pid=$pid"
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
  rm -f "$pid_file"
}

start_tunnels() {
  provider="$(detect_provider)"
  if [ "$provider" = "none" ]; then
    echo "[local_public_tunnel] 已跳过公网隧道（TUNNEL_PROVIDER=${TUNNEL_PROVIDER}）"
    return 0
  fi

  echo "[local_public_tunnel] provider=$provider"
  web_url="$(start_one "用户端" "$WEB_PORT" "$WEB_PID_FILE" "$WEB_LOG_FILE" "$provider")"
  admin_url="$(start_one "管理端" "$ADMIN_WEB_PORT" "$ADMIN_PID_FILE" "$ADMIN_LOG_FILE" "$provider")"
  host_public_ip=""
  if [ "$provider" = "localtunnel" ]; then
    host_public_ip="$(public_ip || true)"
  fi

  {
    echo "WEB_PUBLIC_URL=$web_url"
    echo "ADMIN_WEB_PUBLIC_URL=$admin_url"
    if [ -n "$host_public_ip" ]; then
      echo "LOCAL_TUNNEL_HOST_IP=$host_public_ip"
    fi
  } >"$URL_FILE"

  echo "[local_public_tunnel] 用户端公网入口: $web_url"
  echo "[local_public_tunnel] 管理端公网入口: $admin_url"
  if [ -n "$host_public_ip" ]; then
    echo "[local_public_tunnel] localtunnel 首次访问验证码 IP: $host_public_ip"
  fi
}

stop_tunnels() {
  stop_one "管理端" "$ADMIN_PID_FILE"
  stop_one "用户端" "$WEB_PID_FILE"
  rm -f "$URL_FILE"
  echo "[local_public_tunnel] 公网隧道已停止"
}

status_tunnels() {
  if [ -f "$URL_FILE" ]; then
    cat "$URL_FILE"
  fi
  web_pid="$(read_pid "$WEB_PID_FILE")"
  admin_pid="$(read_pid "$ADMIN_PID_FILE")"
  web_status="stopped"
  admin_status="stopped"
  is_pid_alive "$web_pid" && web_status="running pid=$web_pid"
  is_pid_alive "$admin_pid" && admin_status="running pid=$admin_pid"
  echo "[local_public_tunnel] 用户端隧道: $web_status"
  echo "[local_public_tunnel] 管理端隧道: $admin_status"
}

case "$ACTION" in
  start)
    start_tunnels
    ;;
  stop)
    stop_tunnels
    ;;
  restart)
    stop_tunnels
    start_tunnels
    ;;
  status)
    status_tunnels
    ;;
  *)
    echo "usage: sh scripts/local_public_tunnel.sh [start|stop|restart|status]"
    exit 1
    ;;
esac
