#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

MANAGE_INFRA="${MANAGE_INFRA:-auto}"

echo "[dev_local_up] 本地开发模式"
echo "[dev_local_up] MANAGE_INFRA=$MANAGE_INFRA"

MANAGE_INFRA="$MANAGE_INFRA" sh scripts/dev_all_up.sh
