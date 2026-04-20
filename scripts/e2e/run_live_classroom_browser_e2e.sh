#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

export PLAYWRIGHT_BASE_URL="${PLAYWRIGHT_BASE_URL:-http://127.0.0.1:5173}"
export PLAYWRIGHT_API_BASE_URL="${PLAYWRIGHT_API_BASE_URL:-http://127.0.0.1:5173}"
export OPS_VERIFY_TOKEN="${OPS_VERIFY_TOKEN:-DevOpsVerifyTokenForE2E}"

echo "[run_live_classroom_browser_e2e] base=${PLAYWRIGHT_BASE_URL}"
echo "[run_live_classroom_browser_e2e] api=${PLAYWRIGHT_API_BASE_URL}"

cd ai-tutor-web
npx playwright install --with-deps chromium firefox webkit
node scripts/run-live-classroom-playwright.mjs
