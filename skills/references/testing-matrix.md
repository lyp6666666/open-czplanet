# Testing Matrix

Choose the smallest meaningful validation that matches the touched area.

## Backend Shared Or Auth

- Touches:
  gateway auth, common identity propagation, request interceptors, role checks
- Minimum validation:
  run the nearest module tests
- Usual targets:
  - `./mvnw -pl tutor-appointment-service test`
  - `./mvnw -pl ai-tutor-admin test`
  - specific tests around interceptors or auth controllers

## Appointment Or User/Profile Changes

- Touches:
  `/user/*`, profile updates, jobs, favorites, schedule, upload
- Minimum validation:
  `./mvnw -pl tutor-appointment-service test`
- Also consider:
  user web typecheck/test if UI changed

## Chat Or Realtime Changes

- Touches:
  room list, message page, SSE stream, unread counts, read ack, collaboration proposal
- Minimum validation:
  `./mvnw -pl videoCall-IM-service test`
- Also consider:
  `cd ai-tutor-web && npm run test`
  if frontend chat page or store changed
- QA candidates:
  `qa/automation/tests/api/test_chat_smoke.py`
  `qa/automation/tests/e2e/test_chat_e2e.py`

## Payment Or Refund Changes

- Touches:
  prepay, cashier UI, order polling, notify callback, refund app service
- Minimum validation:
  `./mvnw -pl payment-service test`
- Also consider:
  QA payment smoke and regression
  admin refund paths if state is surfaced there

## Admin Changes

- Touches:
  `ai-tutor-admin` or `ai-tutor-admin-web`
- Minimum validation:
  - backend: `./mvnw -pl ai-tutor-admin test`
  - frontend: `cd ai-tutor-admin-web && npm run typecheck && npm run lint`

## User Web Changes

- Touches:
  `ai-tutor-web/src`
- Minimum validation:
  `cd ai-tutor-web && npm run typecheck`
- Also run when available:
  `npm run lint`
  `npm run test`

## Miniprogram Changes

- Touches:
  `ai-tutor-miniprogram/src`
- Minimum validation:
  `cd ai-tutor-miniprogram && npm run type-check`

## QA Or E2E Changes

- Touches:
  `qa/automation` or `e2e-tests`
- Minimum validation:
  run the nearest relevant suite or test file

## Cross-Module Rule

If a change spans more than one of these areas:

- validate each touched module at least once
- prefer one focused backend suite plus one focused frontend or QA check
- if you cannot run validation, say exactly what was skipped and why
