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

echo "[fix_home_carousel_upload_only] 仅修复首页轮播图上传链路"
echo "[fix_home_carousel_upload_only] app=${APP_REMOTE_USER}@${APP_REMOTE_HOST}:${APP_REMOTE_PORT}"
echo "[fix_home_carousel_upload_only] proxy=${PROXY_REMOTE_USER}@${PROXY_REMOTE_HOST}:${PROXY_REMOTE_PORT}"
echo "[fix_home_carousel_upload_only] publicDomain=$PUBLIC_DOMAIN adminPath=$PUBLIC_ADMIN_BASE_PATH adminPort=$PUBLIC_ADMIN_PORT"

ssh -p "$APP_REMOTE_PORT" "${APP_REMOTE_USER}@${APP_REMOTE_HOST}" \
  "mkdir -p '$APP_REMOTE_PATH/scripts'"

scp -P "$APP_REMOTE_PORT" \
  scripts/setup_app_host_nginx.sh \
  "${APP_REMOTE_USER}@${APP_REMOTE_HOST}:${APP_REMOTE_PATH}/scripts/"

ssh -p "$APP_REMOTE_PORT" "${APP_REMOTE_USER}@${APP_REMOTE_HOST}" \
  "cd '$APP_REMOTE_PATH' && ADMIN_BASE_PATH='$PUBLIC_ADMIN_BASE_PATH' sh scripts/setup_app_host_nginx.sh"

ssh -p "$PROXY_REMOTE_PORT" "${PROXY_REMOTE_USER}@${PROXY_REMOTE_HOST}" \
  "mkdir -p '$PROXY_REMOTE_PATH/scripts'"

scp -P "$PROXY_REMOTE_PORT" \
  scripts/setup_public_domain_proxy.sh \
  "${PROXY_REMOTE_USER}@${PROXY_REMOTE_HOST}:${PROXY_REMOTE_PATH}/scripts/"

ssh -p "$PROXY_REMOTE_PORT" "${PROXY_REMOTE_USER}@${PROXY_REMOTE_HOST}" \
  "cd '$PROXY_REMOTE_PATH' && PUBLIC_DOMAIN='$PUBLIC_DOMAIN' UPSTREAM_HOST='$APP_REMOTE_HOST' TARGET_ENTRY_PORT='80' USER_WEB_PORT='80' ADMIN_WEB_PORT='$PUBLIC_ADMIN_PORT' ADMIN_API_PORT='18084' ADMIN_BASE_PATH='$PUBLIC_ADMIN_BASE_PATH' TLS_CERT_PATH='$TLS_CERT_PATH' TLS_KEY_PATH='$TLS_KEY_PATH' sh scripts/setup_public_domain_proxy.sh"

echo "[fix_home_carousel_upload_only] 已更新两台服务器的 Nginx 配置并执行 reload"
echo "[fix_home_carousel_upload_only] 用户端入口: http://$PUBLIC_DOMAIN/"
echo "[fix_home_carousel_upload_only] 管理端入口: http://$PUBLIC_DOMAIN$PUBLIC_ADMIN_BASE_PATH"
