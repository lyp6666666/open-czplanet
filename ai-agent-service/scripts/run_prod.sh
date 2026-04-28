#!/usr/bin/env sh
set -e

cd "$(dirname "$0")/.."

HOST="${AI_AGENT_HOST:-127.0.0.1}"
PORT="${AI_AGENT_PORT:-18086}"
PATH="${UV_INSTALL_DIR:-$HOME/.local/bin}:$PATH"
export PATH

sh scripts/bootstrap_env.sh
if [ -n "${AI_AGENT_ENV_FILE:-}" ] && [ -f "$AI_AGENT_ENV_FILE" ]; then
  # dev_all_up uses launchd on macOS; source the generated env file so Nacos-derived
  # AI_AGENT_* values are available inside the launched Python process.
  . "$AI_AGENT_ENV_FILE"
elif [ -n "${NACOS_SERVER_ADDR:-}" ]; then
  eval "$(uv run --active --python .venv/bin/python python scripts/export_nacos_env.py)"
fi
exec uv run --active --python .venv/bin/python uvicorn app.main:app --host "$HOST" --port "$PORT"
