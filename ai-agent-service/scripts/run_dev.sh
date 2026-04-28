#!/usr/bin/env sh
set -e

cd "$(dirname "$0")/.."
HOST="${AI_AGENT_HOST:-0.0.0.0}"
PORT="${AI_AGENT_PORT:-18086}"
PATH="${UV_INSTALL_DIR:-$HOME/.local/bin}:$PATH"
export PATH

sh scripts/bootstrap_env.sh
exec uv run --active --python .venv/bin/python uvicorn app.main:app --host "$HOST" --port "$PORT" --reload
