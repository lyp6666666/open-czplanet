#!/usr/bin/env bash
set -euo pipefail

# 用途：
# 1) 将前端 public/banners 迁移到 MinIO（bucket 内路径：banners/）
# 2) 可选：设置 bucket 为 public read（便于直接访问）
#
# 依赖：
# - 本机已安装 Docker（通过 docker run 使用 minio/mc，无需额外安装 mc）
#
# 环境变量（可在 .env 或命令行传入）：
# - MINIO_ENDPOINT        默认 http://127.0.0.1:9000
# - MINIO_ACCESS_KEY      默认 minioadmin
# - MINIO_SECRET_KEY      默认 minioadmin
# - MINIO_BUCKET          默认 ai-tutor-assets
#
# 示例：
#   MINIO_ENDPOINT=http://127.0.0.1:9000 \
#   MINIO_ACCESS_KEY=minioadmin \
#   MINIO_SECRET_KEY=minioadmin \
#   MINIO_BUCKET=ai-tutor-assets \
#   bash scripts/minio_sync_assets.sh

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BANNERS_DIR="$ROOT_DIR/ai-tutor-web/public/banners"

MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://host.docker.internal:9000}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-minioadmin}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-minioadmin}"
MINIO_BUCKET="${MINIO_BUCKET:-ai-tutor-assets}"

if [[ ! -d "$BANNERS_DIR" ]]; then
  echo "banners 目录不存在：$BANNERS_DIR" >&2
  exit 1
fi

docker run --rm \
  --entrypoint /bin/sh \
  -v "$BANNERS_DIR:/data/banners:ro" \
  quay.io/minio/mc:latest \
  -lc "
    mc alias set local '$MINIO_ENDPOINT' '$MINIO_ACCESS_KEY' '$MINIO_SECRET_KEY';
    mc mb -p 'local/$MINIO_BUCKET' || true;
    mc anonymous set download 'local/$MINIO_BUCKET' || true;
    mc mirror --overwrite /data/banners 'local/$MINIO_BUCKET/banners';
    mc ls 'local/$MINIO_BUCKET/banners';
  "

echo "已同步 banners 到 MinIO：bucket=$MINIO_BUCKET path=banners/"
