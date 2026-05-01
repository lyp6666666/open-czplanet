#!/bin/sh

set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-}"
NACOS_NAMESPACE_DEV="${NACOS_NAMESPACE_DEV:-481e4376-4576-4b18-ac19-f61e170ca3ae}"
NACOS_NAMESPACE_PROD="${NACOS_NAMESPACE_PROD:-c3476048-10f6-4cc3-b3f1-90135d736a73}"
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
NACOS_USERNAME="${NACOS_USERNAME:-}"
NACOS_PASSWORD="${NACOS_PASSWORD:-}"
NACOS_GRPC_CHECK="${NACOS_GRPC_CHECK:-fail}"

JWT_ISSUER="${JWT_ISSUER:-ai-tutor}"
JWT_SECRET_PRIMARY="${JWT_SECRET_PRIMARY:-LypJwtSecretKey123LypJwtSecretKey123}"
GATEWAY_JWT_ISSUER="${GATEWAY_JWT_ISSUER:-$JWT_ISSUER}"
GATEWAY_JWT_SECRET="${GATEWAY_JWT_SECRET:-$JWT_SECRET_PRIMARY}"
GATEWAY_SIGN_SECRET="${GATEWAY_SIGN_SECRET:-DevGatewaySignSecret_1234567890_abcd}"

JWT_SECRETS_0="${JWT_SECRETS_0:-$JWT_SECRET_PRIMARY}"
GATEWAY_JWT_SECRETS_0="${GATEWAY_JWT_SECRETS_0:-$JWT_SECRET_PRIMARY}"

GATEWAY_PORT="${GATEWAY_PORT:-18080}"
APPOINTMENT_PORT="${APPOINTMENT_PORT:-18081}"
IM_PORT="${IM_PORT:-18082}"
PAYMENT_PORT="${PAYMENT_PORT:-18083}"
ADMIN_PORT="${ADMIN_PORT:-18084}"
LIVE_CLASS_PORT="${LIVE_CLASS_PORT:-18085}"
AI_AGENT_PORT="${AI_AGENT_PORT:-18086}"
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
INFRA_CONTAINERS="${INFRA_CONTAINERS:-mysql redis rabbitmq minio prometheus grafana loki alertmanager promtail node-exporter cadvisor mysqld-exporter redis-exporter rabbitmq-exporter livekit}"
AUTO_BOOTSTRAP_DEV_DB="${AUTO_BOOTSTRAP_DEV_DB:-1}"
AUTO_VERIFY_DB_SCHEMA="${AUTO_VERIFY_DB_SCHEMA:-1}"
LIVEKIT_API_KEY="${LIVEKIT_API_KEY:-dev-api-key}"
LIVEKIT_API_SECRET="${LIVEKIT_API_SECRET:-CHANGE_ME_LIVEKIT_API_SECRET}"
LIVEKIT_WS_URL="${LIVEKIT_WS_URL:-ws://127.0.0.1:${LIVEKIT_PORT}}"
OPS_VERIFY_TOKEN="${OPS_VERIFY_TOKEN:-DevOpsVerifyTokenForE2E}"
DEV_EXPOSE_SMS_CODE="${DEV_EXPOSE_SMS_CODE:-true}"
ENABLE_AI_AGENT="${ENABLE_AI_AGENT:-1}"
AI_AGENT_HOST="${AI_AGENT_HOST:-127.0.0.1}"

LOG_DIR="$ROOT_DIR/.logs"
PID_DIR="$ROOT_DIR/.pids"
LAUNCHD_DIR="$ROOT_DIR/.launchd"
mkdir -p "$LOG_DIR" "$PID_DIR" "$LAUNCHD_DIR"

OS_NAME="$(uname -s 2>/dev/null || echo unknown)"
case "$OS_NAME" in
  Darwin)
    # Docker Desktop on macOS cannot run some Linux host-observability containers reliably.
    INFRA_CONTAINERS="$(printf '%s\n' "$INFRA_CONTAINERS" | tr ' ' '\n' | grep -Ev '^(cadvisor|node-exporter|promtail)$' | tr '\n' ' ' | xargs)"
    echo "[dev_all_up] 检测到 macOS，默认跳过 cadvisor/node-exporter/promtail"
    ;;
esac

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
    echo "[dev_all_up] 本地应用将连接本机 Nacos：$NACOS_SERVER_ADDR"
    return 0
  fi

  if http_endpoint_reachable "$TUNNELED_NACOS_SERVER_ADDR"; then
    NACOS_SERVER_ADDR="$TUNNELED_NACOS_SERVER_ADDR"
    echo "[dev_all_up] 本地应用仍运行在本机；仅 Nacos 通过现有本地隧道访问远端 dev：$NACOS_SERVER_ADDR"
    return 0
  fi

  case "$AUTO_NACOS_TUNNEL" in
    auto|always)
      echo "[dev_all_up] 本地应用将运行在本机，但本机未检测到可用 Nacos。"
      echo "[dev_all_up] 尝试建立本地 Nacos 隧道，用于访问远端 dev 配置中心..."
      bash scripts/nacos_tunnel.sh start
      NACOS_SERVER_ADDR="$TUNNELED_NACOS_SERVER_ADDR"
      echo "[dev_all_up] 本地应用仍运行在本机；仅 Nacos 通过新建隧道访问远端 dev：$NACOS_SERVER_ADDR"
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

echo "[dev_all_up] 应用进程启动位置：本机"
echo "[dev_all_up] 配置中心访问地址：$NACOS_SERVER_ADDR"

export JWT_ISSUER JWT_SECRET_PRIMARY GATEWAY_JWT_ISSUER GATEWAY_JWT_SECRET GATEWAY_SIGN_SECRET JWT_SECRETS_0 GATEWAY_JWT_SECRETS_0 NACOS_SERVER_ADDR NACOS_CONFIG_NAMESPACE NACOS_DISCOVERY_NAMESPACE NACOS_USERNAME NACOS_PASSWORD LIVEKIT_API_KEY LIVEKIT_API_SECRET LIVEKIT_WS_URL OPS_VERIFY_TOKEN DEV_EXPOSE_SMS_CODE
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

case "$AUTO_VERIFY_DB_SCHEMA" in
  1|true|yes)
    echo "[dev_all_up] 校验本地数据库结构是否与 sqlDoc/huoyue.sql 对齐..."
    sh scripts/verify_db_schema_against_huoyue.sh
    ;;
  0|false|no)
    echo "[dev_all_up] 跳过数据库结构校验（AUTO_VERIFY_DB_SCHEMA=$AUTO_VERIFY_DB_SCHEMA）"
    ;;
  *)
    echo "[dev_all_up] 不支持的 AUTO_VERIFY_DB_SCHEMA=$AUTO_VERIFY_DB_SCHEMA，可选值：1/0 true/false yes/no"
    exit 1
    ;;
esac

echo "[dev_all_up] 构建本地依赖模块（ai-tutor-common/ai-tutor-mq，跳过测试与测试编译）..."
./mvnw -q -Dmaven.test.skip=true install -pl ai-tutor-common,ai-tutor-mq -am

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

    wait_loops=0
    while [ ! -s "$runtime_pid_file" ]; do
      wait_loops=$((wait_loops + 1))
      if [ "$wait_loops" -ge 50 ]; then
        break
      fi
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
  wait_loops=0
  while [ ! -s "$runtime_pid_file" ]; do
    if ! kill -0 "$launcher_pid" >/dev/null 2>&1; then
      break
    fi
    wait_loops=$((wait_loops + 1))
    if [ "$wait_loops" -ge 50 ]; then
      break
    fi
    sleep 0.1
  done

  if [ -s "$runtime_pid_file" ]; then
    cat "$runtime_pid_file"
    return 0
  fi

  echo "$launcher_pid"
  return 0
}

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
    runtime_pid="$(start_detached_process "com.ai.tutor.dev.$svc_name" "$pid_file" "$log_file" "$ROOT_DIR" env \
      SERVER_PORT="$port" \
      SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" \
      NACOS_NAMESPACE="$NACOS_NAMESPACE" \
      NACOS_SERVER_ADDR="$NACOS_SERVER_ADDR" \
      NACOS_CONFIG_NAMESPACE="$NACOS_CONFIG_NAMESPACE" \
      NACOS_DISCOVERY_NAMESPACE="$NACOS_DISCOVERY_NAMESPACE" \
      NACOS_USERNAME="$NACOS_USERNAME" \
      NACOS_PASSWORD="$NACOS_PASSWORD" \
      SPRING_APPLICATION_JSON="$SPRING_APPLICATION_JSON" \
      ./mvnw -q -Dmaven.test.skip=true -f "$svc_dir/pom.xml" spring-boot:run)"
    echo "$runtime_pid" >"$pid_file"
  )

  i=0
  while true; do
    launcher_pid="$(cat "$pid_file" 2>/dev/null || true)"
    if [ -n "$launcher_pid" ] && ! kill -0 "$launcher_pid" >/dev/null 2>&1; then
      echo "[dev_all_up] $svc_name 启动进程已退出"
      [ -f "$log_file" ] && tail -n 120 "$log_file" || true
      return 1
    fi
    listen_pid="$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null | sed -n '1p' || true)"
    if [ -n "$listen_pid" ]; then
      if command -v curl >/dev/null 2>&1; then
        health_url="http://127.0.0.1:$port/actuator/health"
        health_code="$(curl -s -o /dev/null -m 3 -w '%{http_code}' "$health_url" || true)"
        if [ "$health_code" != "200" ]; then
          i=$((i + 1))
          if [ "$i" -ge "$SERVICE_STARTUP_WAIT_LOOPS" ]; then
            echo "[dev_all_up] $svc_name 健康检查失败：$health_url code=$health_code"
            [ -f "$log_file" ] && tail -n 120 "$log_file" || true
            return 1
          fi
          sleep 0.2
          continue
        fi
      fi
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

start_ai_agent_service() {
  svc_name="ai-agent-service"
  worker_name="ai-agent-worker"
  port="$AI_AGENT_PORT"
  log_file="$LOG_DIR/$svc_name.log"
  pid_file="$PID_DIR/$svc_name.pid"
  worker_log_file="$LOG_DIR/$worker_name.log"
  worker_pid_file="$PID_DIR/$worker_name.pid"

  case "$ENABLE_AI_AGENT" in
    1|true|yes)
      ;;
    0|false|no)
      echo "[dev_all_up] 跳过 $svc_name（ENABLE_AI_AGENT=$ENABLE_AI_AGENT）"
      return 0
      ;;
    *)
      echo "[dev_all_up] 不支持的 ENABLE_AI_AGENT=$ENABLE_AI_AGENT，可选值：1/0 true/false yes/no"
      return 1
      ;;
  esac

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

  echo "[dev_all_up] 启动 $svc_name host=$AI_AGENT_HOST port=$port"
  (
    cd "$ROOT_DIR/ai-agent-service"
    AI_AGENT_HOST="$AI_AGENT_HOST" AI_AGENT_PORT="$port" sh scripts/bootstrap_env.sh
    UV_BIN_DIR="${UV_INSTALL_DIR:-$HOME/.local/bin}"
    PATH="$UV_BIN_DIR:$PATH"
    export PATH
    ai_agent_env_file="$LAUNCHD_DIR/$svc_name.env"
    {
      printf "export NACOS_SERVER_ADDR='%s'\n" "$(printf "%s" "$NACOS_SERVER_ADDR" | sed "s/'/'\\\\''/g")"
      printf "export NACOS_NAMESPACE='%s'\n" "$(printf "%s" "$NACOS_NAMESPACE" | sed "s/'/'\\\\''/g")"
      printf "export NACOS_CONFIG_NAMESPACE='%s'\n" "$(printf "%s" "$NACOS_CONFIG_NAMESPACE" | sed "s/'/'\\\\''/g")"
      printf "export NACOS_DISCOVERY_NAMESPACE='%s'\n" "$(printf "%s" "$NACOS_DISCOVERY_NAMESPACE" | sed "s/'/'\\\\''/g")"
      printf "export NACOS_USERNAME='%s'\n" "$(printf "%s" "$NACOS_USERNAME" | sed "s/'/'\\\\''/g")"
      printf "export NACOS_PASSWORD='%s'\n" "$(printf "%s" "$NACOS_PASSWORD" | sed "s/'/'\\\\''/g")"
      printf "export SPRING_PROFILES_ACTIVE='%s'\n" "$(printf "%s" "$SPRING_PROFILES_ACTIVE" | sed "s/'/'\\\\''/g")"
      uv run --active --python .venv/bin/python python scripts/export_nacos_env.py
    } >"$ai_agent_env_file"
    chmod 600 "$ai_agent_env_file"
    . "$ai_agent_env_file"
    export AI_AGENT_HOST="$AI_AGENT_HOST"
    export AI_AGENT_PORT="$port"
    runtime_pid="$(start_detached_process "com.ai.tutor.dev.$svc_name" "$pid_file" "$log_file" "$ROOT_DIR/ai-agent-service" env AI_AGENT_ENV_FILE="$ai_agent_env_file" sh scripts/run_prod.sh)"
    echo "$runtime_pid" >"$pid_file"
    case "${AI_AGENT_USE_ASYNC_WORKER:-true}" in
      1|true|yes)
        worker_pid="$(start_detached_process "com.ai.tutor.dev.$worker_name" "$worker_pid_file" "$worker_log_file" "$ROOT_DIR/ai-agent-service" env AI_AGENT_ENV_FILE="$ai_agent_env_file" sh scripts/run_worker.sh)"
        echo "$worker_pid" >"$worker_pid_file"
        ;;
    esac
  )

  i=0
  while true; do
    launcher_pid="$(cat "$pid_file" 2>/dev/null || true)"
    if [ -n "$launcher_pid" ] && ! kill -0 "$launcher_pid" >/dev/null 2>&1; then
      echo "[dev_all_up] $svc_name 启动进程已退出"
      [ -f "$log_file" ] && tail -n 120 "$log_file" || true
      return 1
    fi
    listen_pid="$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null | sed -n '1p' || true)"
    if [ -n "$listen_pid" ]; then
      if command -v curl >/dev/null 2>&1; then
        health_url="http://$AI_AGENT_HOST:$port/health"
        health_code="$(curl -s -o /dev/null -m 3 -w '%{http_code}' "$health_url" || true)"
        if [ "$health_code" != "200" ]; then
          i=$((i + 1))
          if [ "$i" -ge "$SERVICE_STARTUP_WAIT_LOOPS" ]; then
            echo "[dev_all_up] $svc_name 健康检查失败：$health_url code=$health_code"
            [ -f "$log_file" ] && tail -n 120 "$log_file" || true
            return 1
          fi
          sleep 0.2
          continue
        fi
      fi
      if [ -f "$worker_pid_file" ]; then
        worker_pid="$(cat "$worker_pid_file" 2>/dev/null || true)"
        if [ -n "$worker_pid" ] && ! kill -0 "$worker_pid" >/dev/null 2>&1; then
          echo "[dev_all_up] $worker_name 未存活"
          [ -f "$worker_log_file" ] && tail -n 120 "$worker_log_file" || true
          return 1
        fi
      fi
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
    runtime_pid="$(start_detached_process "com.ai.tutor.dev.$app_name" "$pid_file" "$log_file" "$ROOT_DIR/$app_dir" env \
      VITE_BASE_PATH="$base_path" \
      npm run dev -- --host "$FRONTEND_HOST" --port "$port" --strictPort)"
    echo "$runtime_pid" >"$pid_file"
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
start_ai_agent_service
start_service "live-class-service" "live-class-service" "$LIVE_CLASS_PORT"
start_frontend "ai-tutor-web" "ai-tutor-web" "$WEB_PORT" "$WEB_BASE_PATH"
start_frontend "ai-tutor-admin-web" "ai-tutor-admin-web" "$ADMIN_WEB_PORT" "$ADMIN_WEB_BASE_PATH"

echo "[dev_all_up] 已拉起网关 + 5 个 Java 服务 + 可选 ai-agent-service + 2 个前端"
echo "[dev_all_up] 日志目录：$LOG_DIR"
echo "[dev_all_up] PID 目录：$PID_DIR"
echo "[dev_all_up] 用户端前端：http://$FRONTEND_HOST:$WEB_PORT"
echo "[dev_all_up] 管理端前端：http://$FRONTEND_HOST:$ADMIN_WEB_PORT"
