#!/usr/bin/env bash
set -euo pipefail

# 用途：
# 1) 将创智星球品牌 logo 资源同步到 MinIO（bucket 内路径：brand/）
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
#   bash scripts/minio_sync_brand_assets.sh

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BRAND_DIR="$ROOT_DIR/ai-tutor-web/public/brand"

MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://127.0.0.1:9000}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-minioadmin}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-minioadmin}"
MINIO_BUCKET="${MINIO_BUCKET:-ai-tutor-assets}"

if [[ ! -d "$BRAND_DIR" ]]; then
  echo "brand 目录不存在：$BRAND_DIR" >&2
  exit 1
fi

docker run --rm \
  --network host \
  --entrypoint /bin/sh \
  -v "$BRAND_DIR:/data/brand:ro" \
  quay.io/minio/mc:latest \
  -lc "
    mc alias set local '$MINIO_ENDPOINT' '$MINIO_ACCESS_KEY' '$MINIO_SECRET_KEY';
    mc mb -p 'local/$MINIO_BUCKET' || true;
    mc anonymous set download 'local/$MINIO_BUCKET' || true;
    mc mirror --overwrite /data/brand 'local/$MINIO_BUCKET/brand';
    mc ls 'local/$MINIO_BUCKET/brand';
  "

echo "已同步 brand 资源到 MinIO：bucket=$MINIO_BUCKET path=brand/"
