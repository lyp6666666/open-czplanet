# Module Map

Use this file first to map a request or changed file to the right module boundaries.

## Repo Shape

- Backend parent: `pom.xml`
- User web: `ai-tutor-web`
- Admin web: `ai-tutor-admin-web`
- Miniprogram: `ai-tutor-miniprogram`
- Core backend services:
  - `ai-tutor-gateway`
  - `tutor-appointment-service`
  - `videoCall-IM-service`
  - `payment-service`
  - `ai-tutor-admin`
- Shared backend modules:
  - `ai-tutor-common`
  - `ai-tutor-mq`
- QA and e2e:
  - `qa/automation`
  - `e2e-tests`
- Specs and plans:
  - `openspec`
  - `docs`

## Backend Modules

### `ai-tutor-gateway`

- Responsibility:
  External entry gateway, JWT parsing, route-level identity extraction
- Start here when work involves:
  Bearer token parsing, gateway auth, route forwarding, Nacos route setup
- Key file:
  `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/JwtClaimsService.java`

### `ai-tutor-common`

- Responsibility:
  Shared response model, exceptions, request context, frequency control, identity-sign helpers, shared events
- Start here when work involves:
  `RequestHolder`, global error style, Feign identity propagation, shared abstractions
- Key files:
  - `ai-tutor-common/src/main/java/com/ai/tutor/common/security/FeignIdentityRequestInterceptor.java`
  - `ai-tutor-common/src/main/java/com/ai/tutor/common/security/GatewayIdentityInterceptor.java`

### `ai-tutor-mq`

- Responsibility:
  MQ security invocation helpers and transaction auto-config
- Start here when work involves:
  Secure invoke annotations or MQ-side shared infra

### `tutor-appointment-service`

- Responsibility:
  User login and profile, teacher/student/org job posting flows, favorites, schedule, upload, teacher verification, contacts
- Start here when work involves:
  `/user/*`, `/api/v1/parent/*`, `/api/v1/tutor/*`, `/api/v1/org/*`, schedule, asset upload
- Key files:
  - `controller/UserController.java`
  - `interceptor/RoleInterceptor.java`
  - `config/WebConfig.java`

### `videoCall-IM-service`

- Responsibility:
  Chat room lifecycle, chat message persistence, SSE realtime stream, read ack, collaboration proposal, contact unlock, some brokerage and refund-adjacent flows
- Important caution:
  This is not a pure IM module. Many post-chat business actions also live here.
- Start here when work involves:
  `/chat/*`, unread counts, stream delivery, message cards, collaboration states
- Key files:
  - `chat/controller/ChatController.java`
  - `chat/controller/ChatRoomController.java`
  - `chat/controller/ChatStreamController.java`

### `payment-service`

- Responsibility:
  Payment order lifecycle, cashier/prepay, YunGouOS integration, notify callback, refund app service
- Start here when work involves:
  Cashier, order status polling, payment notify, refund payment state
- Key file:
  `payment-service/src/main/java/com/ai/tutor/payment/service/YungouosPaymentAppService.java`

### `ai-tutor-admin`

- Responsibility:
  Admin auth, dashboard, job moderation, verification review, refunds, organizations, users, payment records
- Start here when work involves:
  `/api/admin/*` endpoints or admin-side moderation logic

## Frontend Modules

### `ai-tutor-web`

- Responsibility:
  Main user-facing web app for teacher, student, organization, chat, schedule, payment, courses
- Routing entry:
  `ai-tutor-web/src/router/index.ts`
- Start here when work involves:
  Main product UI, auth routing, demand browsing, chat page, cashier page
- Important stores:
  - `src/stores/auth.ts`
  - `src/stores/chatRealtime.ts`

### `ai-tutor-admin-web`

- Responsibility:
  Admin UI for dashboard, jobs, users, verification, organizations, refunds, payments
- Routing entry:
  `ai-tutor-admin-web/src/router/index.ts`

### `ai-tutor-miniprogram`

- Responsibility:
  Uni-app miniprogram flow for home, me, posting, chat, onboarding
- Routing entry:
  `ai-tutor-miniprogram/src/pages.json`

## Fast Path By File Pattern

- `ai-tutor-web/src/pages/chat` or `src/ui/chat`
  Read chat sections in `business-flows.md` and `gotchas.md`
- `ai-tutor-web/src/pages/pay` or `src/api/payment.ts`
  Read payment sections in `business-flows.md` and `testing-matrix.md`
- `tutor-appointment-service/src/main/java/.../interceptor`
  Read auth and role notes in `gotchas.md`
- `videoCall-IM-service/src/main/java/.../chat`
  Read chat and collaboration flow notes in `business-flows.md`
- `payment-service/src/main/java`
  Read payment and refund sections in `business-flows.md`
- `qa/automation/tests`
  Read `testing-matrix.md`

## Specs Versus Reality

- `openspec` is useful for intent, scope, and planned changes
- Current implementation may be ahead of some spec docs
- When spec and code disagree during implementation work:
  trust code for current behavior, then note the mismatch in `gotchas.md`
