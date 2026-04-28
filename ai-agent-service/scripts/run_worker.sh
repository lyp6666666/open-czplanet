#!/usr/bin/env sh
set -e

cd "$(dirname "$0")/.."
PATH="${UV_INSTALL_DIR:-$HOME/.local/bin}:$PATH"
export PATH
sh scripts/bootstrap_env.sh
if [ -n "${AI_AGENT_ENV_FILE:-}" ] && [ -f "$AI_AGENT_ENV_FILE" ]; then
  . "$AI_AGENT_ENV_FILE"
elif [ -n "${NACOS_SERVER_ADDR:-}" ]; then
  eval "$(uv run --active --python .venv/bin/python python scripts/export_nacos_env.py)"
fi
exec uv run --active --python .venv/bin/python python -m app.worker
