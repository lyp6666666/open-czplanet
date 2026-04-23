#!/bin/sh

set -eu

WEB_PORT="${WEB_PORT:-5173}"
ADMIN_WEB_PORT="${ADMIN_WEB_PORT:-5174}"
GATEWAY_PORT="${GATEWAY_PORT:-18080}"
ADMIN_PORT="${ADMIN_PORT:-18084}"
ADMIN_BASE_PATH="${ADMIN_BASE_PATH:-/admin/}"
NGINX_CONF="${NGINX_CONF:-/etc/nginx/sites-available/ai-tutor-app-host.conf}"
PUBLIC_HOST="${PUBLIC_HOST:-huoyue.online}"

if [ "$(id -u)" != "0" ]; then
  echo "[setup_app_host_nginx] 请使用 root 执行"
  exit 1
fi

cat >"$NGINX_CONF" <<EOF
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;

    access_log /var/log/nginx/ai-tutor-app-host.access.log;
    error_log /var/log/nginx/ai-tutor-app-host.error.log warn;

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
        proxy_pass http://127.0.0.1:$ADMIN_WEB_PORT;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 300s;
    }

    location ^~ /payment/notify/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_set_header Authorization \$http_authorization;
        proxy_read_timeout 60s;
    }

    location ^~ /payment/return/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_set_header Authorization \$http_authorization;
        proxy_read_timeout 60s;
    }

    location ^~ /api/admin/ {
        proxy_pass http://127.0.0.1:$ADMIN_PORT;
        proxy_set_header Authorization \$http_authorization;
        proxy_read_timeout 60s;
    }

    location ^~ /api/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_set_header Authorization \$http_authorization;
        proxy_read_timeout 60s;
    }

    location ^~ /org/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_set_header Authorization \$http_authorization;
        proxy_read_timeout 60s;
    }

    location ^~ /user/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_set_header Authorization \$http_authorization;
        proxy_read_timeout 60s;
    }

    location ^~ /invite/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_set_header Authorization \$http_authorization;
        proxy_read_timeout 60s;
    }

    location ^~ /chat/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_set_header Authorization \$http_authorization;
        proxy_buffering off;
        proxy_read_timeout 3600s;
    }

    location = /livekit {
        proxy_pass http://127.0.0.1:7880/;
        proxy_http_version 1.1;
        proxy_set_header Host $PUBLIC_HOST;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }

    location ^~ /livekit/ {
        proxy_pass http://127.0.0.1:7880/;
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }

    location ^~ /payment/ {
        proxy_pass http://127.0.0.1:$GATEWAY_PORT;
        proxy_set_header Authorization \$http_authorization;
        proxy_read_timeout 60s;
    }

    location = /ops {
        return 302 /ops/;
    }

    location = /ops/ {
        return 302 /ops/grafana/;
    }

    location ^~ /ops/grafana/ {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $PUBLIC_HOST;
        proxy_set_header X-Forwarded-Port 443;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 300s;
    }

    location = /ops/prometheus {
        return 302 /ops/prometheus/;
    }

    location ^~ /ops/prometheus/ {
        rewrite ^/ops/prometheus/(.*)$ /\$1 break;
        proxy_pass http://127.0.0.1:9090;
        proxy_set_header Host $PUBLIC_HOST;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $PUBLIC_HOST;
        proxy_set_header X-Forwarded-Port 443;
        proxy_read_timeout 300s;
    }

    location / {
        proxy_pass http://127.0.0.1:$WEB_PORT;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 300s;
    }
}
EOF

rm -f /etc/nginx/sites-enabled/default
rm -f /etc/nginx/sites-enabled/ai-tutor-payment-callback.conf
ln -sf "$NGINX_CONF" /etc/nginx/sites-enabled/ai-tutor-app-host.conf
nginx -t
systemctl enable nginx >/dev/null 2>&1 || true
systemctl restart nginx

echo "[setup_app_host_nginx] 完成"
echo "用户端本机入口: http://127.0.0.1/"
echo "管理端本机入口: http://127.0.0.1$ADMIN_BASE_PATH"
