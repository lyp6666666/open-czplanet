#!/usr/bin/env sh
set -e

cd "$(dirname "$0")/.."

PYTHON_BIN="${PYTHON_BIN:-python3}"

if [ ! -d ".venv" ]; then
  "$PYTHON_BIN" -m venv .venv
fi

if [ ! -x ".venv/bin/python" ]; then
  echo ".venv/bin/python not found" >&2
  exit 1
fi

.venv/bin/python -m ensurepip --upgrade >/dev/null 2>&1 || true
.venv/bin/python -m pip install -q -r requirements.txt
