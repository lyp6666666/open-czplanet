#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PID_DIR="$ROOT_DIR/.pids"
LOG_DIR="$ROOT_DIR/.logs"
LAUNCHD_DIR="$ROOT_DIR/.launchd"
mkdir -p "$PID_DIR" "$LOG_DIR" "$LAUNCHD_DIR"

ACTION="${1:-start}"

PUBLIC_ENTRY_HOST="${PUBLIC_ENTRY_HOST:-127.0.0.1}"
PUBLIC_ENTRY_PORT="${PUBLIC_ENTRY_PORT:-18090}"
WEB_PORT="${WEB_PORT:-5173}"
GATEWAY_PORT="${GATEWAY_PORT:-18080}"
LIVE_CLASS_PORT="${LIVE_CLASS_PORT:-18085}"
LIVEKIT_PORT="${LIVEKIT_PORT:-7880}"
LIVEKIT_RTC_TCP_PORT="${LIVEKIT_RTC_TCP_PORT:-7881}"
LIVEKIT_UDP_RANGE="${LIVEKIT_UDP_RANGE:-50000-50100}"
PUBLIC_TUNNEL_WAIT_LOOPS="${PUBLIC_TUNNEL_WAIT_LOOPS:-160}"
START_APP="${START_APP:-1}"
RESTART_LIVE_CLASS="${RESTART_LIVE_CLASS:-1}"
VERIFY_AFTER_START="${VERIFY_AFTER_START:-1}"
ENABLE_AI_AGENT_FOR_LIVE_TUNNEL="${ENABLE_AI_AGENT_FOR_LIVE_TUNNEL:-0}"

PROXY_PID_FILE="$PID_DIR/live-cloudflare-proxy.pid"
TUNNEL_PID_FILE="$PID_DIR/live-cloudflare-tunnel.pid"
PROXY_LOG_FILE="$LOG_DIR/live-cloudflare-proxy.log"
TUNNEL_LOG_FILE="$LOG_DIR/live-cloudflare-tunnel.log"
PROXY_JS_FILE="$PID_DIR/live-cloudflare-proxy.mjs"
URL_FILE="$PID_DIR/live-cloudflare-tunnel.urls"
OS_NAME="$(uname -s 2>/dev/null || echo unknown)"
PROXY_LABEL="com.ai.tutor.dev.live-cloudflare-proxy"
TUNNEL_LABEL="com.ai.tutor.dev.live-cloudflare-tunnel"

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

require_cmd() {
  cmd="$1"
  command -v "$cmd" >/dev/null 2>&1 || {
    echo "[live_cloudflare_tunnel] 未检测到 $cmd" >&2
    return 1
  }
}

start_detached_process() {
  service_label="$1"
  runtime_pid_file="$2"
  log_file="$3"
  work_dir="$4"
  shift 4

  rm -f "$runtime_pid_file"

  if [ "$OS_NAME" = "Darwin" ] && command -v launchctl >/dev/null 2>&1; then
    wrapper_script="$LAUNCHD_DIR/$service_label.sh"
    plist_file="$LAUNCHD_DIR/$service_label.plist"
    launchd_service="gui/$(id -u)/$service_label"
    quoted_cmd=""
    for arg in "$@"; do
      escaped_arg="$(printf "%s" "$arg" | sed "s/'/'\\\\''/g")"
      quoted_cmd="$quoted_cmd '$escaped_arg'"
    done
    cat >"$wrapper_script" <<EOF
#!/bin/sh
export PATH='$PATH'
cd '$work_dir'
echo \$\$ > '$runtime_pid_file'
exec$quoted_cmd
EOF
    chmod +x "$wrapper_script"
    cat >"$plist_file" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>$service_label</string>
  <key>ProgramArguments</key>
  <array>
    <string>$wrapper_script</string>
  </array>
  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <false/>
  <key>WorkingDirectory</key>
  <string>$work_dir</string>
  <key>StandardOutPath</key>
  <string>$log_file</string>
  <key>StandardErrorPath</key>
  <string>$log_file</string>
</dict>
</plist>
EOF
    launchctl bootout "$launchd_service" >/dev/null 2>&1 || true
    launchctl bootstrap "gui/$(id -u)" "$plist_file" >/dev/null
    launchctl kickstart -k "$launchd_service" >/dev/null

    i=0
    while [ ! -s "$runtime_pid_file" ]; do
      i=$((i + 1))
      [ "$i" -ge 50 ] && break
      sleep 0.1
    done
    if [ -s "$runtime_pid_file" ]; then
      cat "$runtime_pid_file"
      return 0
    fi
  fi

  if command -v setsid >/dev/null 2>&1; then
    setsid sh -c '
      pid_file="$1"
      log_file="$2"
      shift 2
      echo "$$" >"$pid_file"
      exec "$@" >>"$log_file" 2>&1
    ' sh "$runtime_pid_file" "$log_file" "$@" </dev/null >/dev/null 2>&1 &
  else
    nohup sh -c '
      pid_file="$1"
      log_file="$2"
      shift 2
      echo "$$" >"$pid_file"
      exec "$@" >>"$log_file" 2>&1
    ' sh "$runtime_pid_file" "$log_file" "$@" </dev/null >/dev/null 2>&1 &
  fi

  launcher_pid=$!
  i=0
  while [ ! -s "$runtime_pid_file" ]; do
    if ! kill -0 "$launcher_pid" >/dev/null 2>&1; then
      break
    fi
    i=$((i + 1))
    [ "$i" -ge 50 ] && break
    sleep 0.1
  done
  if [ -s "$runtime_pid_file" ]; then
    cat "$runtime_pid_file"
  else
    echo "$launcher_pid"
  fi
}

extract_url() {
  grep -Eo 'https://[^[:space:]]+' "$TUNNEL_LOG_FILE" 2>/dev/null \
    | grep -E 'trycloudflare\.com' \
    | tail -n 1 \
    | sed 's/[[:punct:]]*$//'
}

to_wss_livekit_url() {
  url="$1"
  printf "%s/livekit\n" "$url" | sed 's#^https://#wss://#; s#/$##'
}

http_code() {
  url="$1"
  curl -sS -o /dev/null -m 8 -w '%{http_code}' "$url" 2>/dev/null || printf "000"
}

wait_for_url() {
  i=0
  while true; do
    url="$(extract_url || true)"
    if [ -n "$url" ]; then
      echo "$url"
      return 0
    fi

    pid="$(read_pid "$TUNNEL_PID_FILE")"
    if ! is_pid_alive "$pid"; then
      echo "[live_cloudflare_tunnel] Cloudflare 隧道进程提前退出，请检查 $TUNNEL_LOG_FILE" >&2
      [ -f "$TUNNEL_LOG_FILE" ] && tail -n 100 "$TUNNEL_LOG_FILE" >&2 || true
      return 1
    fi

    i=$((i + 1))
    if [ "$i" -ge "$PUBLIC_TUNNEL_WAIT_LOOPS" ]; then
      echo "[live_cloudflare_tunnel] 等待 Cloudflare 公网 URL 超时，请检查 $TUNNEL_LOG_FILE" >&2
      [ -f "$TUNNEL_LOG_FILE" ] && tail -n 100 "$TUNNEL_LOG_FILE" >&2 || true
      return 1
    fi
    sleep 0.5
  done
}

write_proxy() {
  cat >"$PROXY_JS_FILE" <<'EOF'
import http from 'node:http'
import net from 'node:net'

const entryHost = process.env.PUBLIC_ENTRY_HOST || '127.0.0.1'
const entryPort = Number(process.env.PUBLIC_ENTRY_PORT || 18090)
const webPort = Number(process.env.WEB_PORT || 5173)
const livekitPort = Number(process.env.LIVEKIT_PORT || 7880)

function targetFor(pathname) {
  if (pathname === '/livekit' || pathname.startsWith('/livekit/')) {
    return { host: '127.0.0.1', port: livekitPort, name: 'livekit' }
  }
  return { host: '127.0.0.1', port: webPort, name: 'web' }
}

function forwardedProto(req) {
  const visitor = req.headers['cf-visitor']
  if (typeof visitor === 'string' && visitor.includes('https')) return 'https'
  const proto = req.headers['x-forwarded-proto']
  return Array.isArray(proto) ? proto[0] : proto || 'https'
}

const server = http.createServer((req, res) => {
  const pathname = new URL(req.url || '/', 'http://local').pathname
  const target = targetFor(pathname)
  const headers = {
    ...req.headers,
    host: `127.0.0.1:${target.port}`,
    'x-forwarded-host': req.headers.host || '',
    'x-forwarded-proto': forwardedProto(req),
    'x-forwarded-port': forwardedProto(req) === 'https' ? '443' : String(entryPort),
  }
  const upstream = http.request({
    host: target.host,
    port: target.port,
    method: req.method,
    path: req.url,
    headers,
  }, (upstreamRes) => {
    res.writeHead(upstreamRes.statusCode || 502, upstreamRes.headers)
    upstreamRes.pipe(res)
  })
  upstream.on('error', (error) => {
    res.writeHead(502, { 'content-type': 'text/plain; charset=utf-8' })
    res.end(`upstream ${target.name} unavailable: ${error.message}\n`)
  })
  req.pipe(upstream)
})

server.on('upgrade', (req, socket, head) => {
  const pathname = new URL(req.url || '/', 'http://local').pathname
  const target = targetFor(pathname)
  const upstream = net.connect(target.port, target.host)
  upstream.on('connect', () => {
    const lines = [`${req.method} ${req.url} HTTP/${req.httpVersion}`]
    const headers = {
      ...req.headers,
      host: `127.0.0.1:${target.port}`,
      'x-forwarded-host': req.headers.host || '',
      'x-forwarded-proto': forwardedProto(req),
      'x-forwarded-port': forwardedProto(req) === 'https' ? '443' : String(entryPort),
    }
    for (const [key, value] of Object.entries(headers)) {
      if (Array.isArray(value)) {
        for (const item of value) lines.push(`${key}: ${item}`)
      } else if (value != null) {
        lines.push(`${key}: ${value}`)
      }
    }
    upstream.write(`${lines.join('\r\n')}\r\n\r\n`)
    if (head?.length) upstream.write(head)
    upstream.pipe(socket)
    socket.pipe(upstream)
  })
  upstream.on('error', () => socket.destroy())
})

server.listen(entryPort, entryHost, () => {
  console.log(`[live-cloudflare-proxy] listening on http://${entryHost}:${entryPort}`)
  console.log(`[live-cloudflare-proxy] / -> http://127.0.0.1:${webPort}`)
  console.log(`[live-cloudflare-proxy] /livekit -> http://127.0.0.1:${livekitPort}`)
})
EOF
}

start_proxy() {
  require_cmd node
  old_pid="$(read_pid "$PROXY_PID_FILE")"
  if is_pid_alive "$old_pid"; then
    echo "[live_cloudflare_tunnel] 本地入口代理已在运行 pid=$old_pid"
    return 0
  fi

  existing_pid="$(lsof -ti tcp:"$PUBLIC_ENTRY_PORT" -sTCP:LISTEN 2>/dev/null | sed -n '1p' || true)"
  if [ -n "$existing_pid" ]; then
    echo "[live_cloudflare_tunnel] 本地入口端口 $PUBLIC_ENTRY_PORT 已被占用 pid=$existing_pid" >&2
    echo "[live_cloudflare_tunnel] 可设置 PUBLIC_ENTRY_PORT=其他端口后重试" >&2
    return 1
  fi

  write_proxy
  rm -f "$PROXY_LOG_FILE"
  echo "[live_cloudflare_tunnel] 启动本地入口代理：http://$PUBLIC_ENTRY_HOST:$PUBLIC_ENTRY_PORT"
  pid="$(start_detached_process "$PROXY_LABEL" "$PROXY_PID_FILE" "$PROXY_LOG_FILE" "$ROOT_DIR" node "$PROXY_JS_FILE")"

  i=0
  while ! lsof -ti tcp:"$PUBLIC_ENTRY_PORT" -sTCP:LISTEN >/dev/null 2>&1; do
    if ! is_pid_alive "$pid"; then
      echo "[live_cloudflare_tunnel] 本地入口代理启动失败，请检查 $PROXY_LOG_FILE" >&2
      [ -f "$PROXY_LOG_FILE" ] && tail -n 80 "$PROXY_LOG_FILE" >&2 || true
      return 1
    fi
    i=$((i + 1))
    [ "$i" -ge 40 ] && {
      echo "[live_cloudflare_tunnel] 等待本地入口代理监听超时，请检查 $PROXY_LOG_FILE" >&2
      return 1
    }
    sleep 0.25
  done
}

start_tunnel() {
  require_cmd cloudflared
  old_pid="$(read_pid "$TUNNEL_PID_FILE")"
  if is_pid_alive "$old_pid"; then
    url="$(extract_url || true)"
    if [ -n "$url" ]; then
      echo "$url"
      return 0
    fi
    wait_for_url
    return 0
  fi

  rm -f "$TUNNEL_LOG_FILE"
  echo "[live_cloudflare_tunnel] 启动 Cloudflare Tunnel -> http://$PUBLIC_ENTRY_HOST:$PUBLIC_ENTRY_PORT" >&2
  pid="$(start_detached_process "$TUNNEL_LABEL" "$TUNNEL_PID_FILE" "$TUNNEL_LOG_FILE" "$ROOT_DIR" cloudflared tunnel --protocol http2 --url "http://$PUBLIC_ENTRY_HOST:$PUBLIC_ENTRY_PORT" --no-autoupdate)"
  wait_for_url
}

stop_one() {
  label="$1"
  pid_file="$2"
  launchd_label="${3:-}"
  if [ -n "$launchd_label" ] && [ "$OS_NAME" = "Darwin" ] && command -v launchctl >/dev/null 2>&1; then
    launchctl bootout "gui/$(id -u)/$launchd_label" >/dev/null 2>&1 || true
  fi
  pid="$(read_pid "$pid_file")"
  if ! is_pid_alive "$pid"; then
    rm -f "$pid_file"
    return 0
  fi
  echo "[live_cloudflare_tunnel] 停止 $label pid=$pid"
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

restart_live_class_if_needed() {
  case "$RESTART_LIVE_CLASS" in
    1|true|yes)
      ;;
    0|false|no)
      return 0
      ;;
    *)
      echo "[live_cloudflare_tunnel] 不支持的 RESTART_LIVE_CLASS=$RESTART_LIVE_CLASS，可选值：1/0 true/false yes/no" >&2
      return 1
      ;;
  esac

  pid_file="$PID_DIR/live-class-service.pid"
  old_pid="$(read_pid "$pid_file")"
  if is_pid_alive "$old_pid"; then
    echo "[live_cloudflare_tunnel] 重启 live-class-service 以应用 LIVEKIT_PUBLIC_WS_URL"
    kill "$old_pid" >/dev/null 2>&1 || true
    i=0
    while is_pid_alive "$old_pid"; do
      i=$((i + 1))
      [ "$i" -ge 30 ] && { kill -9 "$old_pid" >/dev/null 2>&1 || true; break; }
      sleep 0.2
    done
    rm -f "$pid_file"
  fi
}

start_app_if_needed() {
  case "$START_APP" in
    1|true|yes)
      restart_live_class_if_needed
      echo "[live_cloudflare_tunnel] 启动/补齐本地视频联调服务，并注入 LIVEKIT_PUBLIC_WS_URL=$LIVEKIT_PUBLIC_WS_URL"
      (
        cd "$ROOT_DIR"
        export LIVEKIT_PUBLIC_WS_URL
        ENABLE_AI_AGENT="$ENABLE_AI_AGENT_FOR_LIVE_TUNNEL" sh scripts/dev_all_up.sh
      )
      ;;
    0|false|no)
      echo "[live_cloudflare_tunnel] 跳过本地服务启动（START_APP=$START_APP）"
      ;;
    *)
      echo "[live_cloudflare_tunnel] 不支持的 START_APP=$START_APP，可选值：1/0 true/false yes/no" >&2
      return 1
      ;;
  esac
}

verify_local() {
  ok=0
  fail=0
  check_tcp() {
    label="$1"
    port="$2"
    if nc -z -w 2 127.0.0.1 "$port" >/dev/null 2>&1; then
      echo "[live_cloudflare_tunnel] local OK: $label 127.0.0.1:$port"
      ok=$((ok + 1))
    else
      echo "[live_cloudflare_tunnel] local FAIL: $label 127.0.0.1:$port" >&2
      fail=$((fail + 1))
    fi
  }

  check_tcp "public-entry" "$PUBLIC_ENTRY_PORT"
  check_tcp "web" "$WEB_PORT"
  check_tcp "gateway" "$GATEWAY_PORT"
  check_tcp "live-class-service" "$LIVE_CLASS_PORT"
  check_tcp "livekit-ws" "$LIVEKIT_PORT"
  check_tcp "livekit-rtc-tcp" "$LIVEKIT_RTC_TCP_PORT"

  echo "[live_cloudflare_tunnel] local NOTE: LiveKit UDP $LIVEKIT_UDP_RANGE 需要公网 UDP/防火墙支持，Cloudflare Quick Tunnel 不提供浏览器可直连 UDP 媒体端口。"
  [ "$fail" -eq 0 ]
}

verify_public() {
  require_cmd curl
  url="${1:-}"
  if [ -z "$url" ] && [ -f "$URL_FILE" ]; then
    url="$(awk -F= '/^PUBLIC_URL=/{print $2}' "$URL_FILE" | tail -n 1)"
  fi
  if [ -z "$url" ]; then
    echo "[live_cloudflare_tunnel] 未找到 PUBLIC_URL，无法验证公网访问" >&2
    return 1
  fi

  web_code="$(http_code "$url/")"
  livekit_code="$(http_code "$url/livekit")"
  health_code="$(http_code "$url/api/v1/public/home/config")"

  echo "[live_cloudflare_tunnel] public web: $url/ code=$web_code"
  echo "[live_cloudflare_tunnel] public livekit websocket endpoint: $url/livekit code=$livekit_code"
  echo "[live_cloudflare_tunnel] public gateway-through-web-proxy: $url/api/v1/public/home/config code=$health_code"

  case "$web_code" in
    2*|3*) ;;
    *) echo "[live_cloudflare_tunnel] 公网页面验证失败 code=$web_code" >&2; return 1 ;;
  esac
  case "$livekit_code" in
    000|5*) echo "[live_cloudflare_tunnel] LiveKit Cloudflare 路由不可达 code=$livekit_code" >&2; return 1 ;;
  esac
  case "$health_code" in
    000|5*) echo "[live_cloudflare_tunnel] 网关代理验证失败 code=$health_code" >&2; return 1 ;;
  esac

  echo "[live_cloudflare_tunnel] Cloudflare HTTP/WebSocket 路由验证通过"
}

start_all() {
  start_proxy
  public_url="$(start_tunnel)"
  livekit_public_ws_url="$(to_wss_livekit_url "$public_url")"
  export LIVEKIT_PUBLIC_WS_URL="$livekit_public_ws_url"

  {
    echo "PUBLIC_URL=$public_url"
    echo "LIVEKIT_PUBLIC_WS_URL=$livekit_public_ws_url"
    echo "PUBLIC_ENTRY_URL=http://$PUBLIC_ENTRY_HOST:$PUBLIC_ENTRY_PORT"
    echo "WEB_LOCAL_URL=http://127.0.0.1:$WEB_PORT"
    echo "GATEWAY_LOCAL_URL=http://127.0.0.1:$GATEWAY_PORT"
    echo "LIVE_CLASS_LOCAL_URL=http://127.0.0.1:$LIVE_CLASS_PORT"
    echo "LIVEKIT_LOCAL_WS_URL=ws://127.0.0.1:$LIVEKIT_PORT"
    echo "LIVEKIT_RTC_TCP=127.0.0.1:$LIVEKIT_RTC_TCP_PORT"
    echo "LIVEKIT_UDP_RANGE=$LIVEKIT_UDP_RANGE"
  } >"$URL_FILE"

  start_app_if_needed

  if [ "$VERIFY_AFTER_START" = "1" ] || [ "$VERIFY_AFTER_START" = "true" ] || [ "$VERIFY_AFTER_START" = "yes" ]; then
    verify_local
    verify_public "$public_url"
  fi

  echo "[live_cloudflare_tunnel] 用户端公网入口: $public_url"
  echo "[live_cloudflare_tunnel] LiveKit 公网 WebSocket: $livekit_public_ws_url"
  echo "[live_cloudflare_tunnel] 地址文件: $URL_FILE"
}

stop_all() {
  stop_one "Cloudflare Tunnel" "$TUNNEL_PID_FILE" "$TUNNEL_LABEL"
  stop_one "本地入口代理" "$PROXY_PID_FILE" "$PROXY_LABEL"
  rm -f "$URL_FILE"
  echo "[live_cloudflare_tunnel] 实时视频公网联调隧道已停止"
}

status_all() {
  [ -f "$URL_FILE" ] && cat "$URL_FILE"
  proxy_pid="$(read_pid "$PROXY_PID_FILE")"
  tunnel_pid="$(read_pid "$TUNNEL_PID_FILE")"
  proxy_status="stopped"
  tunnel_status="stopped"
  is_pid_alive "$proxy_pid" && proxy_status="running pid=$proxy_pid"
  is_pid_alive "$tunnel_pid" && tunnel_status="running pid=$tunnel_pid"
  echo "[live_cloudflare_tunnel] 本地入口代理: $proxy_status"
  echo "[live_cloudflare_tunnel] Cloudflare Tunnel: $tunnel_status"
}

case "$ACTION" in
  start)
    start_all
    ;;
  stop)
    stop_all
    ;;
  restart)
    stop_all
    start_all
    ;;
  status)
    status_all
    ;;
  verify)
    verify_local
    verify_public
    ;;
  *)
    echo "usage: sh scripts/live_cloudflare_tunnel.sh [start|stop|restart|status|verify]"
    exit 1
    ;;
esac
