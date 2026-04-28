#!/bin/sh

set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

MANAGE_INFRA="${MANAGE_INFRA:-auto}"
AUTO_PUBLIC_TUNNEL="${AUTO_PUBLIC_TUNNEL:-0}"

echo "[dev_local_up] 本地开发模式"
echo "[dev_local_up] MANAGE_INFRA=$MANAGE_INFRA"
echo "[dev_local_up] AUTO_PUBLIC_TUNNEL=$AUTO_PUBLIC_TUNNEL"

MANAGE_INFRA="$MANAGE_INFRA" sh scripts/dev_all_up.sh

case "$AUTO_PUBLIC_TUNNEL" in
  1|true|yes|auto|always)
    sh scripts/local_public_tunnel.sh start
    ;;
  0|false|no|never)
    echo "[dev_local_up] 跳过公网隧道（AUTO_PUBLIC_TUNNEL=$AUTO_PUBLIC_TUNNEL）"
    ;;
  *)
    echo "[dev_local_up] 不支持的 AUTO_PUBLIC_TUNNEL=$AUTO_PUBLIC_TUNNEL，可选值：1/0 true/false yes/no auto/always/never"
    exit 1
    ;;
esac
