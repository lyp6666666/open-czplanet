#!/bin/sh

set -eu

if [ "$#" -eq 0 ]; then
  echo "usage: bash skills/scripts/changed-area-check.sh <path> [path...]"
  exit 1
fi

for path in "$@"; do
  echo "== $path =="
  case "$path" in
    ai-tutor-web/src/pages/chat/*|ai-tutor-web/src/ui/chat/*|ai-tutor-web/src/stores/chatRealtime.ts)
      echo "area: chat frontend"
      echo "read: skills/references/business-flows.md"
      echo "read: skills/references/gotchas.md"
      echo "validate: cd ai-tutor-web && npm run test"
      ;;
    ai-tutor-web/*)
      echo "area: user web"
      echo "read: skills/references/module-map.md"
      echo "read: skills/references/business-flows.md"
      echo "validate: cd ai-tutor-web && npm run typecheck"
      ;;
    ai-tutor-admin-web/*)
      echo "area: admin web"
      echo "read: skills/references/module-map.md"
      echo "read: skills/references/testing-matrix.md"
      echo "validate: cd ai-tutor-admin-web && npm run typecheck && npm run lint"
      ;;
    ai-tutor-miniprogram/*)
      echo "area: miniprogram"
      echo "read: skills/references/module-map.md"
      echo "read: skills/references/business-flows.md"
      echo "validate: cd ai-tutor-miniprogram && npm run type-check"
      ;;
    tutor-appointment-service/*)
      echo "area: appointment service"
      echo "read: skills/references/module-map.md"
      echo "read: skills/references/business-flows.md"
      echo "read: skills/references/gotchas.md"
      echo "validate: ./mvnw -pl tutor-appointment-service test"
      ;;
    videoCall-IM-service/*)
      echo "area: IM service"
      echo "read: skills/references/module-map.md"
      echo "read: skills/references/business-flows.md"
      echo "read: skills/references/gotchas.md"
      echo "validate: ./mvnw -pl videoCall-IM-service test"
      ;;
    payment-service/*)
      echo "area: payment service"
      echo "read: skills/references/business-flows.md"
      echo "read: skills/references/testing-matrix.md"
      echo "read: skills/references/gotchas.md"
      echo "validate: ./mvnw -pl payment-service test"
      ;;
    ai-tutor-admin/*)
      echo "area: admin backend"
      echo "read: skills/references/module-map.md"
      echo "read: skills/references/testing-matrix.md"
      echo "validate: ./mvnw -pl ai-tutor-admin test"
      ;;
    ai-tutor-gateway/*|ai-tutor-common/*|ai-tutor-mq/*)
      echo "area: shared auth or infra"
      echo "read: skills/references/module-map.md"
      echo "read: skills/references/gotchas.md"
      echo "validate: run the nearest affected backend module tests"
      ;;
    qa/automation/*|e2e-tests/*)
      echo "area: qa or e2e"
      echo "read: skills/references/testing-matrix.md"
      echo "validate: run the nearest affected test suite"
      ;;
    openspec/*|docs/*)
      echo "area: specs or docs"
      echo "read: skills/references/gotchas.md"
      echo "validate: compare against current code before trusting docs"
      ;;
    *)
      echo "area: unknown"
      echo "read: skills/references/module-map.md"
      echo "read: skills/references/gotchas.md"
      echo "validate: inspect nearest module entrypoint manually"
      ;;
  esac
  echo
done
