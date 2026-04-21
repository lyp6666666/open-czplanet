#!/bin/sh

set -eu

if [ "$#" -eq 0 ]; then
  echo "用法：bash skills/scripts/changed-area-check.sh <path> [path...]"
  exit 1
fi

for path in "$@"; do
  echo "== $path =="
  case "$path" in
    ai-tutor-web/src/pages/chat/*|ai-tutor-web/src/ui/chat/*|ai-tutor-web/src/stores/chatRealtime.ts)
      echo "区域：聊天前端"
      echo "先读：skills/references/business-flows.md"
      echo "先读：skills/references/gotchas.md"
      echo "验证：cd ai-tutor-web && npm run test"
      ;;
    ai-tutor-web/*)
      echo "区域：用户端 Web"
      echo "先读：skills/references/module-map.md"
      echo "先读：skills/references/business-flows.md"
      echo "验证：cd ai-tutor-web && npm run typecheck"
      ;;
    ai-tutor-admin-web/*)
      echo "区域：管理端 Web"
      echo "先读：skills/references/module-map.md"
      echo "先读：skills/references/testing-matrix.md"
      echo "验证：cd ai-tutor-admin-web && npm run typecheck && npm run lint"
      ;;
    ai-tutor-miniprogram/*)
      echo "区域：小程序"
      echo "先读：skills/references/module-map.md"
      echo "先读：skills/references/business-flows.md"
      echo "验证：cd ai-tutor-miniprogram && npm run type-check"
      ;;
    tutor-appointment-service/*)
      echo "区域：appointment 服务"
      echo "先读：skills/references/module-map.md"
      echo "先读：skills/references/business-flows.md"
      echo "先读：skills/references/gotchas.md"
      echo "验证：./mvnw -pl tutor-appointment-service test"
      ;;
    videoCall-IM-service/*)
      echo "区域：IM 服务"
      echo "先读：skills/references/module-map.md"
      echo "先读：skills/references/business-flows.md"
      echo "先读：skills/references/gotchas.md"
      echo "验证：./mvnw -pl videoCall-IM-service test"
      ;;
    payment-service/*)
      echo "区域：支付服务"
      echo "先读：skills/references/business-flows.md"
      echo "先读：skills/references/testing-matrix.md"
      echo "先读：skills/references/gotchas.md"
      echo "验证：./mvnw -pl payment-service test"
      ;;
    ai-tutor-admin/*)
      echo "区域：管理后端"
      echo "先读：skills/references/module-map.md"
      echo "先读：skills/references/testing-matrix.md"
      echo "验证：./mvnw -pl ai-tutor-admin test"
      ;;
    ai-tutor-gateway/*|ai-tutor-common/*|ai-tutor-mq/*)
      echo "区域：公共认证或基础设施"
      echo "先读：skills/references/module-map.md"
      echo "先读：skills/references/gotchas.md"
      echo "验证：运行最近受影响的后端模块测试"
      ;;
    qa/automation/*|e2e-tests/*)
      echo "区域：QA 或 E2E"
      echo "先读：skills/references/testing-matrix.md"
      echo "验证：运行最近受影响的测试套件"
      ;;
    openspec/*|docs/*)
      echo "区域：规格或文档"
      echo "先读：skills/references/gotchas.md"
      echo "验证：在相信文档前，先和当前代码对照"
      ;;
    *)
      echo "区域：未知"
      echo "先读：skills/references/module-map.md"
      echo "先读：skills/references/gotchas.md"
      echo "验证：手动检查最近的模块入口"
      ;;
  esac
  echo
done
