#!/bin/sh

set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname "$0")/../.." && pwd)"

echo "repo: $ROOT_DIR"
echo
echo "top-level modules:"
find "$ROOT_DIR" -maxdepth 1 -mindepth 1 -type d \
  \( -name .git -o -name node_modules -o -name .logs -o -name .pids \) -prune -o -type d -print \
  | sed "s#^$ROOT_DIR/##" \
  | sort

echo
echo "backend modules from pom.xml:"
sed -n '/<modules>/,/<\/modules>/p' "$ROOT_DIR/pom.xml" | sed -n 's/.*<module>\(.*\)<\/module>.*/- \1/p'

echo
echo "key entry files:"
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
echo "skill references:"
find "$ROOT_DIR/skills/references" -maxdepth 1 -type f | sed "s#^$ROOT_DIR/##" | sort
