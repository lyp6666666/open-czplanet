#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

export E2E_GATEWAY_BASE_URL="${E2E_GATEWAY_BASE_URL:-http://localhost:18080}"
export E2E_IM_BASE_URL="${E2E_IM_BASE_URL:-http://localhost:18082}"
export E2E_PAYMENT_BASE_URL="${E2E_PAYMENT_BASE_URL:-http://localhost:18083}"
export E2E_ADMIN_BASE_URL="${E2E_ADMIN_BASE_URL:-http://localhost:18084}"
export E2E_GATEWAY_SIGN_SECRET="${E2E_GATEWAY_SIGN_SECRET:-DevGatewaySignSecret_1234567890_abcd}"

if [ -z "${E2E_MYSQL_URL:-}" ]; then
  MYSQL_HOST="${E2E_MYSQL_HOST:-127.0.0.1}"
  MYSQL_PORT="${E2E_MYSQL_PORT:-3306}"
  MYSQL_DB="${E2E_MYSQL_DB:-ai_tutor}"
  export E2E_MYSQL_URL="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
fi
export E2E_MYSQL_USER="${E2E_MYSQL_USER:-root}"
export E2E_MYSQL_PASSWORD="${E2E_MYSQL_PASSWORD:-Aa123456}"

echo "[run_live_class_flow_e2e] gateway=${E2E_GATEWAY_BASE_URL}"
echo "[run_live_class_flow_e2e] mysql=${E2E_MYSQL_URL}"

./mvnw -q -pl e2e-tests -am -Dtest=LiveClassFlowE2eTest -Dsurefire.failIfNoSpecifiedTests=false test
