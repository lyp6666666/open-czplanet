#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

export E2E_GATEWAY_SIGN_SECRET="${E2E_GATEWAY_SIGN_SECRET:-0123456789abcdef0123456789abcdef}"
export E2E_BROKERAGE_ADMIN_TOKEN="${E2E_BROKERAGE_ADMIN_TOKEN:-E2E_ADMIN_TOKEN}"

MYSQL_HOST="${E2E_MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${E2E_MYSQL_PORT:-3306}"
MYSQL_USER="${E2E_MYSQL_USER:-root}"
MYSQL_PASSWORD="${E2E_MYSQL_PASSWORD:-Aa123456}"
MYSQL_DB="${E2E_MYSQL_DB:-ai_tutor}"

IM_PORT="${E2E_IM_PORT:-18082}"
PAYMENT_PORT="${E2E_PAYMENT_PORT:-18083}"

JDBC_URL="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB}?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

mkdir -p .logs .pids

docker_compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    docker compose "$@"
    return
  fi
  if command -v docker-compose >/dev/null 2>&1; then
    docker-compose "$@"
    return
  fi
  echo "[run_refund_flow_e2e] 未检测到 docker compose 或 docker-compose"
  exit 1
}

docker_compose_cmd -f Dockerfile/docker-compose.yml up -d

mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DB}" < sqlDoc/migrations/20260404_brokerage_order_refund_and_trial_fields.sql
mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DB}" < sqlDoc/migrations/20260404_refund_request_create.sql
mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DB}" < sqlDoc/migrations/20260404_payment_refund_create.sql
mysql -h "${MYSQL_HOST}" -P "${MYSQL_PORT}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" "${MYSQL_DB}" < sqlDoc/migrations/20260404_course_enrollment_create.sql

kill_if_running() {
  local pidFile="$1"
  if [ -f "$pidFile" ]; then
    local pid
    pid="$(cat "$pidFile" || true)"
    if [ -n "$pid" ]; then
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pidFile"
  fi
}

cleanup() {
  kill_if_running ".pids/e2e_im.pid"
  kill_if_running ".pids/e2e_payment.pid"
}

trap cleanup EXIT

./mvnw -q -pl ai-tutor-common -DskipTests install

(./mvnw -pl videoCall-IM-service -DskipTests spring-boot:run -Dspring-boot.run.arguments="--server.port=${IM_PORT} --spring.cloud.nacos.config.enabled=false --spring.cloud.nacos.discovery.enabled=false --gateway.sign.secret=${E2E_GATEWAY_SIGN_SECRET} --security.gateway-identity.enabled=true --spring.datasource.url=${JDBC_URL} --spring.datasource.username=${MYSQL_USER} --spring.datasource.password=${MYSQL_PASSWORD} --spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver --spring.data.redis.host=localhost --spring.data.redis.port=6379 --spring.data.redis.password=123456 --brokerage.admin-token=${E2E_BROKERAGE_ADMIN_TOKEN}" > .logs/e2e_im.log 2>&1 & echo $! > .pids/e2e_im.pid)

(./mvnw -pl payment-service -DskipTests spring-boot:run -Dspring-boot.run.arguments="--server.port=${PAYMENT_PORT} --spring.cloud.nacos.config.enabled=false --spring.cloud.nacos.discovery.enabled=false --gateway.sign.secret=${E2E_GATEWAY_SIGN_SECRET} --security.gateway-identity.enabled=true --spring.datasource.url=${JDBC_URL} --spring.datasource.username=${MYSQL_USER} --spring.datasource.password=${MYSQL_PASSWORD} --spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver --spring.data.redis.host=localhost --spring.data.redis.port=6379 --spring.data.redis.password=123456 --integration.brokerage.remote.enabled=true --spring.cloud.openfeign.client.config.videoCall-IM-service.url=http://localhost:${IM_PORT} --payment.yungouos.base-url=mock://yungouos --payment.yungouos.app-key=TEST_KEY --payment.yungouos.wechat-mch-id=MOCK_MCH --payment.yungouos.app-id=MOCK_APP --payment.yungouos.notify-url=http://localhost:${PAYMENT_PORT}/payment/notify/yungouos" > .logs/e2e_payment.log 2>&1 & echo $! > .pids/e2e_payment.pid)

for i in {1..60}; do
  if lsof -ti tcp:${IM_PORT} -sTCP:LISTEN >/dev/null 2>&1 && lsof -ti tcp:${PAYMENT_PORT} -sTCP:LISTEN >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

export E2E_IM_BASE_URL="http://localhost:${IM_PORT}"
export E2E_PAYMENT_BASE_URL="http://localhost:${PAYMENT_PORT}"
export E2E_MYSQL_URL="${JDBC_URL}"
export E2E_MYSQL_USER="${MYSQL_USER}"
export E2E_MYSQL_PASSWORD="${MYSQL_PASSWORD}"

./mvnw -q -pl e2e-tests -DskipTests=false test
