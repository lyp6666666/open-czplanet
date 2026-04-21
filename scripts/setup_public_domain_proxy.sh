#!/bin/sh

set -eu

PUBLIC_DOMAIN="${PUBLIC_DOMAIN:-}"
UPSTREAM_HOST="${UPSTREAM_HOST:-111.228.20.88}"
USER_WEB_PORT="${USER_WEB_PORT:-80}"
ADMIN_PORT="${ADMIN_PORT:-5174}"
ADMIN_BASE_PATH="${ADMIN_BASE_PATH:-/admin/}"
NGINX_CONF="${NGINX_CONF:-/etc/nginx/sites-available/ai-tutor-public-domain.conf}"
TLS_CERT_PATH="${TLS_CERT_PATH:-}"
TLS_KEY_PATH="${TLS_KEY_PATH:-}"

if [ "$(id -u)" != "0" ]; then
  echo "[setup_public_domain_proxy] 请使用 root 执行"
  exit 1
fi

if [ -z "$PUBLIC_DOMAIN" ]; then
  echo "[setup_public_domain_proxy] 缺少 PUBLIC_DOMAIN，例如：PUBLIC_DOMAIN=huoyue.online"
  exit 1
fi

if [ -z "$TLS_CERT_PATH" ] && [ -f "/etc/letsencrypt/live/$PUBLIC_DOMAIN/fullchain.pem" ]; then
  TLS_CERT_PATH="/etc/letsencrypt/live/$PUBLIC_DOMAIN/fullchain.pem"
fi
if [ -z "$TLS_KEY_PATH" ] && [ -f "/etc/letsencrypt/live/$PUBLIC_DOMAIN/privkey.pem" ]; then
  TLS_KEY_PATH="/etc/letsencrypt/live/$PUBLIC_DOMAIN/privkey.pem"
fi

if [ -n "$TLS_CERT_PATH" ] && [ -n "$TLS_KEY_PATH" ] && [ -f "$TLS_CERT_PATH" ] && [ -f "$TLS_KEY_PATH" ]; then
  cat >"$NGINX_CONF" <<EOF

server {
    listen 80;
    listen [::]:80;
    server_name $PUBLIC_DOMAIN www.$PUBLIC_DOMAIN _;
    return 301 https://\$host\$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name $PUBLIC_DOMAIN www.$PUBLIC_DOMAIN _;

    ssl_certificate $TLS_CERT_PATH;
    ssl_certificate_key $TLS_KEY_PATH;

    access_log /var/log/nginx/ai-tutor-public-domain-ssl.access.log;
    error_log /var/log/nginx/ai-tutor-public-domain-ssl.error.log warn;

    proxy_http_version 1.1;
    proxy_set_header Host \$host;
    proxy_set_header X-Real-IP \$remote_addr;
    proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto \$scheme;
    proxy_set_header X-Forwarded-Host \$host;
    proxy_set_header X-Forwarded-Port \$server_port;

    location / {
        proxy_pass http://$UPSTREAM_HOST:$USER_WEB_PORT;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 300s;
    }
}
EOF
else
  cat >"$NGINX_CONF" <<EOF
server {
    listen 80;
    listen [::]:80;
    server_name $PUBLIC_DOMAIN www.$PUBLIC_DOMAIN _;

    access_log /var/log/nginx/ai-tutor-public-domain.access.log;
    error_log /var/log/nginx/ai-tutor-public-domain.error.log warn;

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
        proxy_pass http://$UPSTREAM_HOST:$USER_WEB_PORT;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 300s;
    }

    location / {
        proxy_pass http://$UPSTREAM_HOST:$USER_WEB_PORT;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 300s;
    }
}
EOF
fi

rm -f /etc/nginx/sites-enabled/default
rm -f /etc/nginx/sites-enabled/ai-tutor-payment-domain.conf
ln -sf "$NGINX_CONF" /etc/nginx/sites-enabled/ai-tutor-public-domain.conf
nginx -t
systemctl enable nginx >/dev/null 2>&1 || true
systemctl restart nginx

echo "[setup_public_domain_proxy] 完成"
echo "用户端入口: http://$PUBLIC_DOMAIN/"
echo "管理端入口: http://$PUBLIC_DOMAIN$ADMIN_BASE_PATH"
if [ -n "$TLS_CERT_PATH" ] && [ -n "$TLS_KEY_PATH" ] && [ -f "$TLS_CERT_PATH" ] && [ -f "$TLS_KEY_PATH" ]; then
  echo "用户端 HTTPS: https://$PUBLIC_DOMAIN/"
  echo "管理端 HTTPS: https://$PUBLIC_DOMAIN$ADMIN_BASE_PATH"
fi
