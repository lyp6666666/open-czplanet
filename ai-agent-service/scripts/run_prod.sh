#!/usr/bin/env sh
set -e

cd "$(dirname "$0")/.."

HOST="${AI_AGENT_HOST:-127.0.0.1}"
PORT="${AI_AGENT_PORT:-18086}"

. .venv/bin/activate
exec uvicorn app.main:app --host "$HOST" --port "$PORT"
