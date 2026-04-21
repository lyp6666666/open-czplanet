#!/usr/bin/env sh
set -e

cd "$(dirname "$0")/.."

PYTHON_BIN="${PYTHON_BIN:-python3}"

if [ ! -d ".venv" ]; then
  "$PYTHON_BIN" -m venv .venv
fi

. .venv/bin/activate
pip install -q -r requirements.txt
