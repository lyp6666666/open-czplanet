#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

STOP_INFRA="${STOP_INFRA:-1}"

echo "[dev_local_down] 本地关闭模式"
echo "[dev_local_down] STOP_INFRA=$STOP_INFRA"

STOP_INFRA="$STOP_INFRA" sh scripts/dev_all_down.sh
bash scripts/ssh_tunnel.sh stop >/dev/null 2>&1 || true
bash scripts/nacos_tunnel.sh stop >/dev/null 2>&1 || true
