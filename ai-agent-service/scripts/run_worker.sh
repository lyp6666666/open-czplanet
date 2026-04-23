#!/usr/bin/env sh
set -e

cd "$(dirname "$0")/.."
exec .venv/bin/python -m app.worker
