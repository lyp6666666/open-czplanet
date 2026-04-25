#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

PUBLIC_DOMAIN="${PUBLIC_DOMAIN:-}"
ADMIN_DOMAIN="${ADMIN_DOMAIN:-}"
NACOS_SERVER_ADDR="${NACOS_SERVER_ADDR:-111.228.20.88:8848}"
NACOS_GRPC_CHECK="${NACOS_GRPC_CHECK:-warn}"
FRONTEND_HOST="${FRONTEND_HOST:-127.0.0.1}"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"
TLS_CERT_PATH="${TLS_CERT_PATH:-}"
TLS_KEY_PATH="${TLS_KEY_PATH:-}"
LEGACY_SYSTEMD_SERVICES="${LEGACY_SYSTEMD_SERVICES:-nginx}"
WEB_PORT="${WEB_PORT:-5173}"
ADMIN_WEB_PORT="${ADMIN_WEB_PORT:-5174}"
GATEWAY_PORT="${GATEWAY_PORT:-18080}"

if [ "$(id -u)" != "0" ]; then
  echo "[setup_remote_test_host] 请使用 root 执行"
  exit 1
fi

if [ ! -f "$ROOT_DIR/scripts/dev_all_up.sh" ]; then
  echo "[setup_remote_test_host] 未找到 scripts/dev_all_up.sh，请确认当前目录是仓库根目录"
  exit 1
fi

if [ -z "$PUBLIC_DOMAIN" ]; then
  echo "[setup_remote_test_host] 缺少 PUBLIC_DOMAIN，例如：PUBLIC_DOMAIN=pay.example.com"
  exit 1
fi

ensure_pkg() {
  pkg="$1"
  if dpkg -s "$pkg" >/dev/null 2>&1; then
    return 0
  fi
  apt-get update
  apt-get install -y "$pkg"
}

ensure_bin() {
  bin_name="$1"
  pkg_name="$2"
  if command -v "$bin_name" >/dev/null 2>&1; then
    return 0
  fi
  ensure_pkg "$pkg_name"
}

ensure_bin nginx nginx
ensure_bin lsof lsof
ensure_bin nc netcat-openbsd
ensure_bin curl curl

echo "[setup_remote_test_host] 停止当前应用栈"
STOP_INFRA=0 sh scripts/dev_all_down.sh || true

echo "[setup_remote_test_host] 停止遗留 systemd 服务"
for svc in $LEGACY_SYSTEMD_SERVICES; do
  systemctl stop "$svc" >/dev/null 2>&1 || true
done

echo "[setup_remote_test_host] 清理遗留 Java / Node / Vite 进程"
pkill -f 'org.springframework.boot.loader|spring-boot:run|java' >/dev/null 2>&1 || true
pkill -f 'vite --host|npm run dev|node.*vite' >/dev/null 2>&1 || true

for port in 80 443 18080 18081 18082 18083 18084 5173 5174; do
  pid="$(lsof -ti tcp:"$port" -sTCP:LISTEN 2>/dev/null | sed -n '1p' || true)"
  if [ -n "$pid" ]; then
    echo "[setup_remote_test_host] 停止端口占用 port=$port pid=$pid"
    kill "$pid" >/dev/null 2>&1 || true
    sleep 1
    if lsof -ti tcp:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
      kill -9 "$pid" >/dev/null 2>&1 || true
    fi
  fi
done

if [ -z "$TLS_CERT_PATH" ] && [ -f "/etc/letsencrypt/live/$PUBLIC_DOMAIN/fullchain.pem" ]; then
  TLS_CERT_PATH="/etc/letsencrypt/live/$PUBLIC_DOMAIN/fullchain.pem"
fi
if [ -z "$TLS_KEY_PATH" ] && [ -f "/etc/letsencrypt/live/$PUBLIC_DOMAIN/privkey.pem" ]; then
  TLS_KEY_PATH="/etc/letsencrypt/live/$PUBLIC_DOMAIN/privkey.pem"
fi

NGINX_CONF="/etc/nginx/sites-available/ai-tutor-test.conf"

cat >"$NGINX_CONF" <<EOF
server {
    listen 80;
    listen [::]:80;
    server_name $PUBLIC_DOMAIN;
    client_max_body_size 25m;

    access_log /var/log/nginx/ai-tutor-test.access.log;
    error_log /var/log/nginx/ai-tutor-test.error.log warn;

    location /api/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location /org/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /user/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /chat/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location /livekit/ {
        proxy_pass http://127.0.0.1:7880/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location /payment/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location / {
        proxy_pass http://127.0.0.1:$WEB_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
EOF

if [ -n "$TLS_CERT_PATH" ] && [ -n "$TLS_KEY_PATH" ] && [ -f "$TLS_CERT_PATH" ] && [ -f "$TLS_KEY_PATH" ]; then
  cat >>"$NGINX_CONF" <<EOF

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name $PUBLIC_DOMAIN;
    client_max_body_size 25m;

    ssl_certificate $TLS_CERT_PATH;
    ssl_certificate_key $TLS_KEY_PATH;

    access_log /var/log/nginx/ai-tutor-test-ssl.access.log;
    error_log /var/log/nginx/ai-tutor-test-ssl.error.log warn;

    location /api/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location /org/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /user/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /chat/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location /livekit/ {
        proxy_pass http://127.0.0.1:7880/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location /payment/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location / {
        proxy_pass http://127.0.0.1:$WEB_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
EOF
fi

if [ -n "$ADMIN_DOMAIN" ]; then
  cat >>"$NGINX_CONF" <<EOF

server {
    listen 80;
    listen [::]:80;
    server_name $ADMIN_DOMAIN;
    client_max_body_size 25m;

    access_log /var/log/nginx/ai-tutor-admin-test.access.log;
    error_log /var/log/nginx/ai-tutor-admin-test.error.log warn;

    location /api/admin/ {
        proxy_pass http://127.0.0.1:18084;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location / {
        proxy_pass http://127.0.0.1:$ADMIN_WEB_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
EOF

  if [ -n "$TLS_CERT_PATH" ] && [ -n "$TLS_KEY_PATH" ] && [ -f "$TLS_CERT_PATH" ] && [ -f "$TLS_KEY_PATH" ]; then
    cat >>"$NGINX_CONF" <<EOF

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name $ADMIN_DOMAIN;
    client_max_body_size 25m;

    ssl_certificate $TLS_CERT_PATH;
    ssl_certificate_key $TLS_KEY_PATH;

    access_log /var/log/nginx/ai-tutor-admin-test-ssl.access.log;
    error_log /var/log/nginx/ai-tutor-admin-test-ssl.error.log warn;

    location /api/admin/ {
        proxy_pass http://127.0.0.1:18084;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location / {
        proxy_pass http://127.0.0.1:$ADMIN_WEB_PORT;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
EOF
  fi
fi

rm -f /etc/nginx/sites-enabled/default
ln -sf "$NGINX_CONF" /etc/nginx/sites-enabled/ai-tutor-test.conf
nginx -t
systemctl enable nginx >/dev/null 2>&1 || true
systemctl restart nginx

echo "[setup_remote_test_host] 使用旧 Nacos 拉起测试栈"
MANAGE_INFRA=never \
AUTO_NACOS_TUNNEL=never \
SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" \
NACOS_SERVER_ADDR="$NACOS_SERVER_ADDR" \
NACOS_GRPC_CHECK="$NACOS_GRPC_CHECK" \
FRONTEND_HOST="$FRONTEND_HOST" \
sh scripts/dev_all_up.sh

echo
echo "[setup_remote_test_host] 完成"
echo "用户端入口: http://$PUBLIC_DOMAIN/"
if [ -n "$TLS_CERT_PATH" ] && [ -n "$TLS_KEY_PATH" ] && [ -f "$TLS_CERT_PATH" ] && [ -f "$TLS_KEY_PATH" ]; then
  echo "用户端 HTTPS: https://$PUBLIC_DOMAIN/"
  echo "支付 notify-url: https://$PUBLIC_DOMAIN/payment/notify/yungouos"
  echo "支付 return-url: https://$PUBLIC_DOMAIN/payment/return/yungouos"
else
  echo "警告: 未检测到 TLS 证书，当前只配置了 HTTP；若第三方支付要求 HTTPS，请补证书后重跑。"
  echo "支付 notify-url: http://$PUBLIC_DOMAIN/payment/notify/yungouos"
  echo "支付 return-url: http://$PUBLIC_DOMAIN/payment/return/yungouos"
fi
if [ -n "$ADMIN_DOMAIN" ]; then
  echo "管理端入口: http://$ADMIN_DOMAIN/"
fi
echo "Nacos: $NACOS_SERVER_ADDR"
echo "如服务注册异常，请确认旧 Nacos 的 9848/9849 对本机可达。"
