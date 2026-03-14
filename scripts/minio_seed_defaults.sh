#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DEFAULT_AVATAR_FILE="$ROOT_DIR/ai-tutor-web/public/avatars/default-avatar.svg"

MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://host.docker.internal:9000}"
MINIO_ACCESS_KEY="${MINIO_ACCESS_KEY:-minioadmin}"
MINIO_SECRET_KEY="${MINIO_SECRET_KEY:-minioadmin}"
MINIO_BUCKET="${MINIO_BUCKET:-ai-tutor-assets}"

if [[ ! -f "$DEFAULT_AVATAR_FILE" ]]; then
  echo "默认头像文件不存在：$DEFAULT_AVATAR_FILE" >&2
  exit 1
fi

docker run --rm \
  --entrypoint /bin/sh \
  -v "$DEFAULT_AVATAR_FILE:/data/default-avatar.svg:ro" \
  quay.io/minio/mc:latest \
  -lc "
    mc alias set local '$MINIO_ENDPOINT' '$MINIO_ACCESS_KEY' '$MINIO_SECRET_KEY';
    mc mb -p 'local/$MINIO_BUCKET' || true;
    mc anonymous set download 'local/$MINIO_BUCKET' || true;
    mc cp /data/default-avatar.svg 'local/$MINIO_BUCKET/avatars/default.svg';
    mc ls 'local/$MINIO_BUCKET/avatars';
  "

echo "已写入默认头像：bucket=$MINIO_BUCKET objectKey=avatars/default.svg"
