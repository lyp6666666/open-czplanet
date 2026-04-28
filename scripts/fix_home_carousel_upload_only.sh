#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

APP_REMOTE_USER="${APP_REMOTE_USER:-root}"
APP_REMOTE_HOST="${APP_REMOTE_HOST:-111.228.20.88}"
APP_REMOTE_PORT="${APP_REMOTE_PORT:-22}"
APP_REMOTE_PATH="${APP_REMOTE_PATH:-/opt/ai-platform}"
PROXY_REMOTE_USER="${PROXY_REMOTE_USER:-root}"
PROXY_REMOTE_HOST="${PROXY_REMOTE_HOST:-111.229.64.41}"
PROXY_REMOTE_PORT="${PROXY_REMOTE_PORT:-22}"
PROXY_REMOTE_PATH="${PROXY_REMOTE_PATH:-/opt/ai-platform}"
PUBLIC_DOMAIN="${PUBLIC_DOMAIN:-huoyue.online}"
PUBLIC_ADMIN_PORT="${PUBLIC_ADMIN_PORT:-5174}"
PUBLIC_ADMIN_BASE_PATH="${PUBLIC_ADMIN_BASE_PATH:-/admin/}"
TLS_CERT_PATH="${TLS_CERT_PATH:-}"
TLS_KEY_PATH="${TLS_KEY_PATH:-}"
CLIENT_MAX_BODY_SIZE="${CLIENT_MAX_BODY_SIZE:-25m}"

echo "[fix_home_carousel_upload_only] 仅修复首页轮播图上传链路"
echo "[fix_home_carousel_upload_only] app=${APP_REMOTE_USER}@${APP_REMOTE_HOST}:${APP_REMOTE_PORT}"
echo "[fix_home_carousel_upload_only] proxy=${PROXY_REMOTE_USER}@${PROXY_REMOTE_HOST}:${PROXY_REMOTE_PORT}"
echo "[fix_home_carousel_upload_only] publicDomain=$PUBLIC_DOMAIN adminPath=$PUBLIC_ADMIN_BASE_PATH adminPort=$PUBLIC_ADMIN_PORT"
echo "[fix_home_carousel_upload_only] clientMaxBodySize=$CLIENT_MAX_BODY_SIZE"

ssh -p "$APP_REMOTE_PORT" "${APP_REMOTE_USER}@${APP_REMOTE_HOST}" \
  "mkdir -p '$APP_REMOTE_PATH/scripts'"

scp -P "$APP_REMOTE_PORT" \
  scripts/ensure_nginx_client_max_body_size.sh \
  "${APP_REMOTE_USER}@${APP_REMOTE_HOST}:${APP_REMOTE_PATH}/scripts/"

ssh -p "$APP_REMOTE_PORT" "${APP_REMOTE_USER}@${APP_REMOTE_HOST}" \
  "set -eu
  cd '$APP_REMOTE_PATH'
  for conf in /etc/nginx/sites-available/ai-platform.conf /etc/nginx/sites-enabled/ai-platform.conf /etc/nginx/sites-available/ai-tutor-app-host.conf /etc/nginx/sites-enabled/ai-tutor-app-host.conf; do
    if [ -e \"\$conf\" ]; then
      NGINX_CONF=\"\$conf\" CLIENT_MAX_BODY_SIZE='$CLIENT_MAX_BODY_SIZE' sh scripts/ensure_nginx_client_max_body_size.sh
      exit 0
    fi
  done
  echo '[fix_home_carousel_upload_only] 未找到应用机 Nginx 站点配置'
  exit 1"

ssh -p "$PROXY_REMOTE_PORT" "${PROXY_REMOTE_USER}@${PROXY_REMOTE_HOST}" \
  "mkdir -p '$PROXY_REMOTE_PATH/scripts'"

scp -P "$PROXY_REMOTE_PORT" \
  scripts/ensure_nginx_client_max_body_size.sh \
  "${PROXY_REMOTE_USER}@${PROXY_REMOTE_HOST}:${PROXY_REMOTE_PATH}/scripts/"

ssh -p "$PROXY_REMOTE_PORT" "${PROXY_REMOTE_USER}@${PROXY_REMOTE_HOST}" \
  "set -eu
  cd '$PROXY_REMOTE_PATH'
  for conf in /etc/nginx/sites-available/ai-tutor-public-domain.conf /etc/nginx/sites-enabled/ai-tutor-public-domain.conf; do
    if [ -e \"\$conf\" ]; then
      NGINX_CONF=\"\$conf\" CLIENT_MAX_BODY_SIZE='$CLIENT_MAX_BODY_SIZE' sh scripts/ensure_nginx_client_max_body_size.sh
      exit 0
    fi
  done
  echo '[fix_home_carousel_upload_only] 未找到代理机 Nginx 站点配置'
  exit 1"

echo "[fix_home_carousel_upload_only] 已就地更新两台服务器的 Nginx 上传限制并执行 reload"
echo "[fix_home_carousel_upload_only] 用户端入口: http://$PUBLIC_DOMAIN/"
echo "[fix_home_carousel_upload_only] 管理端入口: http://$PUBLIC_DOMAIN$PUBLIC_ADMIN_BASE_PATH"
