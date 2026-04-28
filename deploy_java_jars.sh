#!/usr/bin/env bash
set -euo pipefail

DEPLOY_PATH="/opt/ai-platform"
RELEASE_ID=""
SPRING_PROFILES_ACTIVE="prod"
JAVA_OPTS="-Xms256m -Xmx512m"
RUN_USER="ai-platform"
START_TIMEOUT_SECONDS="90"
ADMIN_BASE_PATH="/admin/"
SETUP_NGINX="true"
NACOS_SERVER_ADDR="111.228.20.88:8848"
NACOS_NAMESPACE="44cf681d-9f93-443e-aa9e-ba6ec8f721d5"
NACOS_CONFIG_NAMESPACE=""
NACOS_DISCOVERY_NAMESPACE=""
NACOS_USERNAME=""
NACOS_PASSWORD=""
JWT_ISSUER="ai-tutor"
JWT_SECRET_PRIMARY=""
GATEWAY_JWT_ISSUER=""
GATEWAY_JWT_SECRET=""
GATEWAY_SIGN_SECRET=""
JWT_SECRETS_0=""
GATEWAY_JWT_SECRETS_0=""
LIVEKIT_API_KEY=""
LIVEKIT_API_SECRET=""
LIVEKIT_WS_URL=""
LIVEKIT_ROOM_PREFIX="class"
LIVEKIT_TOKEN_TTL_SECONDS="7200"
OPS_VERIFY_TOKEN=""
DEV_EXPOSE_SMS_CODE="false"

usage() {
  cat <<'USAGE'
Usage: sudo bash action/deploy_java_jars.sh [options]

Options:
  --deploy-path PATH       Deployment root, default: /opt/ai-platform
  --release-id ID          Release directory name under PATH/releases
  --profile PROFILE        Spring profile, default: prod
  --java-opts OPTS         JVM options, default: -Xms256m -Xmx512m
  --run-user USER          Linux user for systemd services, default: ai-platform
  --timeout SECONDS        Startup wait timeout, default: 90
  --admin-base-path PATH   Admin frontend base path, default: /admin/
  --skip-nginx             Do not write or reload nginx config
  --nacos-server-addr ADDR Nacos address, default: 111.228.20.88:8848
  --nacos-namespace ID     Nacos namespace, default: prod namespace from dev_all_up.sh
  --jwt-secret VALUE       JWT primary secret, recommended in production
  --gateway-sign-secret V  Gateway internal signing secret, recommended in production

The current directory must contain these jars:
  ai-tutor-gateway.jar
  tutor-appointment-service.jar
  videoCall-IM-service.jar
  payment-service.jar
  ai-tutor-admin.jar
  live-class-service.jar

Optional frontend build directories:
  frontend/ai-tutor-web
  frontend/ai-tutor-admin-web
USAGE
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --deploy-path)
      DEPLOY_PATH="$2"
      shift 2
      ;;
    --release-id)
      RELEASE_ID="$2"
      shift 2
      ;;
    --profile)
      SPRING_PROFILES_ACTIVE="$2"
      shift 2
      ;;
    --java-opts)
      JAVA_OPTS="$2"
      shift 2
      ;;
    --run-user)
      RUN_USER="$2"
      shift 2
      ;;
    --timeout)
      START_TIMEOUT_SECONDS="$2"
      shift 2
      ;;
    --admin-base-path)
      ADMIN_BASE_PATH="$2"
      shift 2
      ;;
    --skip-nginx)
      SETUP_NGINX="false"
      shift
      ;;
    --nacos-server-addr)
      NACOS_SERVER_ADDR="$2"
      shift 2
      ;;
    --nacos-namespace)
      NACOS_NAMESPACE="$2"
      shift 2
      ;;
    --nacos-config-namespace)
      NACOS_CONFIG_NAMESPACE="$2"
      shift 2
      ;;
    --nacos-discovery-namespace)
      NACOS_DISCOVERY_NAMESPACE="$2"
      shift 2
      ;;
    --nacos-username)
      NACOS_USERNAME="$2"
      shift 2
      ;;
    --nacos-password)
      NACOS_PASSWORD="$2"
      shift 2
      ;;
    --jwt-issuer)
      JWT_ISSUER="$2"
      shift 2
      ;;
    --jwt-secret)
      JWT_SECRET_PRIMARY="$2"
      shift 2
      ;;
    --gateway-jwt-issuer)
      GATEWAY_JWT_ISSUER="$2"
      shift 2
      ;;
    --gateway-jwt-secret)
      GATEWAY_JWT_SECRET="$2"
      shift 2
      ;;
    --gateway-sign-secret)
      GATEWAY_SIGN_SECRET="$2"
      shift 2
      ;;
    --livekit-api-key)
      LIVEKIT_API_KEY="$2"
      shift 2
      ;;
    --livekit-api-secret)
      LIVEKIT_API_SECRET="$2"
      shift 2
      ;;
    --livekit-ws-url)
      LIVEKIT_WS_URL="$2"
      shift 2
      ;;
    --ops-verify-token)
      OPS_VERIFY_TOKEN="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "[deploy_java_jars] Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [ "$(id -u)" -ne 0 ]; then
  echo "[deploy_java_jars] Please run with sudo/root because systemd services are managed." >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "[deploy_java_jars] java is not installed. Install OpenJDK 21 first." >&2
  exit 1
fi

if ! command -v systemctl >/dev/null 2>&1; then
  echo "[deploy_java_jars] systemctl is required on the target Ubuntu server." >&2
  exit 1
fi

if ! command -v ss >/dev/null 2>&1; then
  echo "[deploy_java_jars] ss is required on the target Ubuntu server. Install iproute2 first." >&2
  exit 1
fi

if [ -z "$RELEASE_ID" ]; then
  RELEASE_ID="$(basename "$(pwd)")"
fi

NACOS_CONFIG_NAMESPACE="${NACOS_CONFIG_NAMESPACE:-$NACOS_NAMESPACE}"
NACOS_DISCOVERY_NAMESPACE="${NACOS_DISCOVERY_NAMESPACE:-$NACOS_NAMESPACE}"
GATEWAY_JWT_ISSUER="${GATEWAY_JWT_ISSUER:-$JWT_ISSUER}"
GATEWAY_JWT_SECRET="${GATEWAY_JWT_SECRET:-$JWT_SECRET_PRIMARY}"
JWT_SECRETS_0="${JWT_SECRETS_0:-$JWT_SECRET_PRIMARY}"
GATEWAY_JWT_SECRETS_0="${GATEWAY_JWT_SECRETS_0:-$JWT_SECRET_PRIMARY}"

CURRENT_DIR="$(pwd)"
RELEASE_DIR="$DEPLOY_PATH/releases/$RELEASE_ID"
SHARED_DIR="$DEPLOY_PATH/shared"
LOG_DIR="$SHARED_DIR/logs"
SYSTEMD_UNIT="/etc/systemd/system/ai-platform@.service"
NGINX_CONF="/etc/nginx/sites-available/ai-platform.conf"

SERVICES=(
  "tutor-appointment-service:18081"
  "videoCall-IM-service:18082"
  "payment-service:18083"
  "ai-tutor-admin:18084"
  "live-class-service:18085"
  "ai-tutor-gateway:18080"
)

for item in "${SERVICES[@]}"; do
  service_name="${item%%:*}"
  if [ ! -f "$CURRENT_DIR/$service_name.jar" ]; then
    echo "[deploy_java_jars] Missing jar: $CURRENT_DIR/$service_name.jar" >&2
    exit 1
  fi
done

if ! id "$RUN_USER" >/dev/null 2>&1; then
  useradd --system --home "$DEPLOY_PATH" --shell /usr/sbin/nologin "$RUN_USER"
fi

mkdir -p "$RELEASE_DIR" "$SHARED_DIR" "$LOG_DIR"
cp "$CURRENT_DIR"/*.jar "$RELEASE_DIR"/
if [ -d "$CURRENT_DIR/frontend" ]; then
  cp -R "$CURRENT_DIR/frontend" "$RELEASE_DIR/"
fi
chown -R "$RUN_USER:$RUN_USER" "$DEPLOY_PATH"

escape_systemd_env_value() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

cat > "$SHARED_DIR/runtime.env" <<EOF
SPRING_PROFILES_ACTIVE="$(escape_systemd_env_value "$SPRING_PROFILES_ACTIVE")"
JAVA_OPTS="$(escape_systemd_env_value "$JAVA_OPTS")"
NACOS_SERVER_ADDR="$(escape_systemd_env_value "$NACOS_SERVER_ADDR")"
NACOS_NAMESPACE="$(escape_systemd_env_value "$NACOS_NAMESPACE")"
NACOS_CONFIG_NAMESPACE="$(escape_systemd_env_value "$NACOS_CONFIG_NAMESPACE")"
NACOS_DISCOVERY_NAMESPACE="$(escape_systemd_env_value "$NACOS_DISCOVERY_NAMESPACE")"
NACOS_USERNAME="$(escape_systemd_env_value "$NACOS_USERNAME")"
NACOS_PASSWORD="$(escape_systemd_env_value "$NACOS_PASSWORD")"
JWT_ISSUER="$(escape_systemd_env_value "$JWT_ISSUER")"
JWT_SECRET_PRIMARY="$(escape_systemd_env_value "$JWT_SECRET_PRIMARY")"
GATEWAY_JWT_ISSUER="$(escape_systemd_env_value "$GATEWAY_JWT_ISSUER")"
GATEWAY_JWT_SECRET="$(escape_systemd_env_value "$GATEWAY_JWT_SECRET")"
GATEWAY_SIGN_SECRET="$(escape_systemd_env_value "$GATEWAY_SIGN_SECRET")"
JWT_SECRETS_0="$(escape_systemd_env_value "$JWT_SECRETS_0")"
GATEWAY_JWT_SECRETS_0="$(escape_systemd_env_value "$GATEWAY_JWT_SECRETS_0")"
LIVEKIT_API_KEY="$(escape_systemd_env_value "$LIVEKIT_API_KEY")"
LIVEKIT_API_SECRET="$(escape_systemd_env_value "$LIVEKIT_API_SECRET")"
LIVEKIT_WS_URL="$(escape_systemd_env_value "$LIVEKIT_WS_URL")"
LIVEKIT_ROOM_PREFIX="$(escape_systemd_env_value "$LIVEKIT_ROOM_PREFIX")"
LIVEKIT_TOKEN_TTL_SECONDS="$(escape_systemd_env_value "$LIVEKIT_TOKEN_TTL_SECONDS")"
OPS_VERIFY_TOKEN="$(escape_systemd_env_value "$OPS_VERIFY_TOKEN")"
DEV_EXPOSE_SMS_CODE="$(escape_systemd_env_value "$DEV_EXPOSE_SMS_CODE")"
EOF

for item in "${SERVICES[@]}"; do
  service_name="${item%%:*}"
  port="${item##*:}"
  cat > "$SHARED_DIR/$service_name.env" <<EOF
SERVER_PORT=$port
EOF
done

cat > "$SYSTEMD_UNIT" <<EOF
[Unit]
Description=AI Platform service %i
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=$RUN_USER
WorkingDirectory=$DEPLOY_PATH/current
Environment=DEPLOY_PATH=$DEPLOY_PATH
EnvironmentFile=$SHARED_DIR/runtime.env
EnvironmentFile=$SHARED_DIR/%i.env
ExecStart=/bin/bash -lc 'exec /usr/bin/java \${JAVA_OPTS:-} -jar "\${DEPLOY_PATH}/current/%i.jar" --server.port="\${SERVER_PORT}" --spring.profiles.active="\${SPRING_PROFILES_ACTIVE:-prod}"'
Restart=always
RestartSec=5
SuccessExitStatus=143
StandardOutput=append:$LOG_DIR/%i.log
StandardError=append:$LOG_DIR/%i.err.log

[Install]
WantedBy=multi-user.target
EOF

ln -sfn "$RELEASE_DIR" "$DEPLOY_PATH/current"
systemctl daemon-reload

setup_nginx() {
  if [ "$SETUP_NGINX" != "true" ]; then
    echo "[deploy_java_jars] Skipping nginx setup"
    return 0
  fi

  if ! command -v nginx >/dev/null 2>&1; then
    echo "[deploy_java_jars] nginx is not installed. Install nginx or pass --skip-nginx." >&2
    exit 1
  fi

  if [ ! -d "$DEPLOY_PATH/current/frontend/ai-tutor-web" ]; then
    echo "[deploy_java_jars] Missing frontend directory: $DEPLOY_PATH/current/frontend/ai-tutor-web" >&2
    exit 1
  fi

  if [ ! -d "$DEPLOY_PATH/current/frontend/ai-tutor-admin-web" ]; then
    echo "[deploy_java_jars] Missing frontend directory: $DEPLOY_PATH/current/frontend/ai-tutor-admin-web" >&2
    exit 1
  fi

  cat > "$NGINX_CONF" <<EOF
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;

    access_log /var/log/nginx/ai-platform.access.log;
    error_log /var/log/nginx/ai-platform.error.log warn;

    proxy_http_version 1.1;
    proxy_set_header Host \$host;
    proxy_set_header X-Real-IP \$remote_addr;
    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto \$scheme;
    proxy_set_header X-Forwarded-Host \$host;
    proxy_set_header X-Forwarded-Port \$server_port;

    location = ${ADMIN_BASE_PATH%/} {
        return 302 $ADMIN_BASE_PATH;
    }

    location ^~ $ADMIN_BASE_PATH {
        alias $DEPLOY_PATH/current/frontend/ai-tutor-admin-web/;
        try_files \$uri \$uri/ ${ADMIN_BASE_PATH}index.html;
    }

    location ^~ /payment/notify/ {
        proxy_pass http://127.0.0.1:18080;
        proxy_read_timeout 60s;
    }

    location ^~ /payment/return/ {
        proxy_pass http://127.0.0.1:18080;
        proxy_read_timeout 60s;
    }

    location ^~ /api/admin/ {
        proxy_pass http://127.0.0.1:18084;
        proxy_read_timeout 60s;
    }

    location ^~ /api/ {
        proxy_pass http://127.0.0.1:18080;
        proxy_read_timeout 60s;
    }

    location ^~ /org/ {
        proxy_pass http://127.0.0.1:18080;
        proxy_read_timeout 60s;
    }

    location ^~ /user/ {
        proxy_pass http://127.0.0.1:18080;
        proxy_read_timeout 60s;
    }

    location ^~ /invite/ {
        proxy_pass http://127.0.0.1:18080;
        proxy_read_timeout 60s;
    }

    location ^~ /chat/ {
        proxy_pass http://127.0.0.1:18080;
        proxy_buffering off;
        proxy_read_timeout 3600s;
    }

    location ^~ /appointment/ {
        proxy_pass http://127.0.0.1:18080;
        proxy_read_timeout 60s;
    }

    location ^~ /courses/ {
        proxy_pass http://127.0.0.1:18080;
        proxy_read_timeout 60s;
    }

    location ^~ /live/ {
        proxy_pass http://127.0.0.1:18080;
        proxy_read_timeout 60s;
    }

    location = /livekit {
        proxy_pass http://127.0.0.1:7880/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }

    location ^~ /livekit/ {
        proxy_pass http://127.0.0.1:7880/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }

    location ^~ /payment/ {
        proxy_pass http://127.0.0.1:18080;
        proxy_read_timeout 60s;
    }

    location / {
        root $DEPLOY_PATH/current/frontend/ai-tutor-web;
        try_files \$uri \$uri/ /index.html;
    }
}
EOF

  rm -f /etc/nginx/sites-enabled/default
  ln -sf "$NGINX_CONF" /etc/nginx/sites-enabled/ai-platform.conf
  nginx -t
  systemctl enable nginx >/dev/null 2>&1 || true
  systemctl reload nginx || systemctl restart nginx
  echo "[deploy_java_jars] nginx frontend config reloaded"
}

wait_for_service() {
  service_name="$1"
  port="$2"
  unit="ai-platform@$service_name.service"
  deadline=$((SECONDS + START_TIMEOUT_SECONDS))

  while [ "$SECONDS" -lt "$deadline" ]; do
    if systemctl is-active --quiet "$unit" && ss -ltn | awk '{print $4}' | grep -Eq "(:|\\])$port$"; then
      echo "[deploy_java_jars] $unit is active on port $port"
      return 0
    fi
    sleep 2
  done

  echo "[deploy_java_jars] $unit failed to become ready on port $port" >&2
  systemctl --no-pager --full status "$unit" >&2 || true
  journalctl -u "$unit" -n 120 --no-pager >&2 || true
  return 1
}

for item in "${SERVICES[@]}"; do
  service_name="${item%%:*}"
  unit="ai-platform@$service_name.service"
  systemctl enable "$unit" >/dev/null
done

for item in "${SERVICES[@]}"; do
  service_name="${item%%:*}"
  port="${item##*:}"
  unit="ai-platform@$service_name.service"

  echo "[deploy_java_jars] Restarting $unit"
  systemctl restart "$unit"
  wait_for_service "$service_name" "$port"
done

setup_nginx
echo "[deploy_java_jars] Deployment completed: $RELEASE_DIR"
