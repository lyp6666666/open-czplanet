#!/usr/bin/env bash
set -euo pipefail

api_base="${QA_API_BASE_URL:-http://localhost:8080}"
web_base="${QA_WEB_BASE_URL:-http://localhost:5173}"

check_url() {
  local url="$1"
  local name="$2"
  local attempts=30
  local sleep_s=1
  for _ in $(seq 1 "$attempts"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "$name ok: $url"
      return 0
    fi
    sleep "$sleep_s"
  done
  echo "$name not ready: $url" >&2
  return 1
}

check_url "${api_base}/v3/api-docs" "backend"

if [[ "${QA_REQUIRE_WEB:-false}" == "true" ]]; then
  check_url "${web_base}/" "frontend"
fi
