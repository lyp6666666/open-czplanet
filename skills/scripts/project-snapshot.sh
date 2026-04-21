#!/bin/sh

set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname "$0")/../.." && pwd)"

echo "仓库：$ROOT_DIR"
echo
echo "顶层模块："
find "$ROOT_DIR" -maxdepth 1 -mindepth 1 -type d \
  \( -name .git -o -name node_modules -o -name .logs -o -name .pids \) -prune -o -type d -print \
  | sed "s#^$ROOT_DIR/##" \
  | sort

echo
echo "来自 pom.xml 的后端模块："
sed -n '/<modules>/,/<\/modules>/p' "$ROOT_DIR/pom.xml" | sed -n 's/.*<module>\(.*\)<\/module>.*/- \1/p'

echo
echo "关键入口文件："
for path in \
  "ai-tutor-web/src/router/index.ts" \
  "ai-tutor-admin-web/src/router/index.ts" \
  "ai-tutor-miniprogram/src/pages.json" \
  "ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/JwtClaimsService.java" \
  "tutor-appointment-service/src/main/java/com/ai/tutor/appointment/controller/UserController.java" \
  "videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/chat/controller/ChatStreamController.java" \
  "payment-service/src/main/java/com/ai/tutor/payment/service/YungouosPaymentAppService.java"; do
  if [ -f "$ROOT_DIR/$path" ]; then
    echo "- $path"
  fi
done

echo
echo "skill 参考文档："
find "$ROOT_DIR/skills/references" -maxdepth 1 -type f | sed "s#^$ROOT_DIR/##" | sort
