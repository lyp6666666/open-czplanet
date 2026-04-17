# Business Flows

Read the section that matches the feature you are touching.

## 1. Authentication And Identity

- Public login entry is mainly in `tutor-appointment-service`
- Main endpoints:
  - `/user/sendcode`
  - `/user/loginOrRegister`
  - `/user/wechatLogin`
  - `/user/me`
  - `/user/batch`
- Identity path:
  gateway parses JWT -> common identity signer/validator propagates uid and role -> service interceptors use `RequestHolder`
- If auth work spans services, inspect:
  - gateway JWT parsing
  - `ai-tutor-common` identity signing
  - service interceptors and request attributes

## 2. Teacher Demand Browsing And Start Chat

- User web route area:
  `ai-tutor-web/src/pages/tutor/*`
- Backend area:
  `tutor-appointment-service` for job data, `videoCall-IM-service` for room creation and messaging
- Typical flow:
  teacher logs in -> browses parent jobs -> opens detail -> starts room -> enters chat
- When changing this flow:
  inspect both job APIs and chat room APIs, not just one side

## 3. Student Or Parent Posting And Managing Demand

- User web route area:
  `ai-tutor-web/src/pages/student/*`
- Miniprogram area:
  `ai-tutor-miniprogram/src/pages/post`, `pages/my-jobs`
- Backend area:
  `tutor-appointment-service` parent job posting controllers and services
- Typical flow:
  login -> post demand -> edit/view own demand -> attract teacher contact

## 4. Organization Flow

- User web route area:
  `ai-tutor-web/src/pages/org/*`
- Backend area:
  organization auth and organization job posting controllers in `tutor-appointment-service`
- Typical flow:
  org login -> manage org jobs -> browse tutors/favorites -> public profile

## 5. Chat, Realtime, Unread, Collaboration

- User web route area:
  `ai-tutor-web/src/pages/chat/*`
- Frontend realtime store:
  `ai-tutor-web/src/stores/chatRealtime.ts`
- Backend area:
  `videoCall-IM-service`
- Typical flow:
  list rooms -> load room messages -> send message -> SSE receive -> unread update -> read ack
- Important reminder:
  chat pages also render structured business cards such as collaboration proposals and unlock/contact states

## 6. Payment, Cashier, Brokerage

- User web route area:
  `ai-tutor-web/src/pages/pay/*`
- Backend area:
  `payment-service`
- Cross-service dependency:
  payment may need brokerage order info from IM-side integration
- Typical flow:
  business action creates payable context -> prepay/cashier page -> poll order status -> notify callback -> finalize business state
- In the current shared remote test environment:
  public callback hits `huoyue.online` on `111.229.64.41`, then `nginx` proxies to the real business host `111.228.20.88`
- The app is not served from `huoyue.online` today.
  laptop browser testing usually happens through the SSH tunnel at `localhost:5173`
- Payment success is not complete until IM-side unlock logs appear, such as `brokerage_payment_success` and `tutor_application_paid`
- When changing payment:
  inspect both `payment-service` and any IM/admin integration that consumes the result
  also read `references/payment-remote-testing.md` for the shared two-server callback topology and live verification routine

## 7. Refund And Dispute Flows

- Admin backend and admin web both participate
- Chat-side or IM-side state may also participate depending on refund type
- Payment-side status changes may finalize or gate refund actions
- When changing refund logic:
  trace the end-to-end state machine before editing a single controller

## 8. Admin Moderation And Dashboard

- Admin web route area:
  `ai-tutor-admin-web/src/pages/*`
- Admin backend area:
  `ai-tutor-admin`
- Typical domains:
  verification review, job moderation, users, organizations, refunds, payments, dashboard stats

## 9. Home, Discovery, Recommendations

- Main web home:
  `ai-tutor-web/src/pages/HomePage.vue`
- Main backend area:
  home guest service in `tutor-appointment-service`
- Relevant for:
  guest homepage cards, hot tabs, banner, hot demands, hot tutors, suggestions

## 10. How To Read A Cross-Module Request

If a feature sounds simple but contains one of these words, widen the search:

- "login", "token", "role"
  also inspect gateway and common identity
- "chat", "message", "unread", "realtime"
  also inspect SSE, read ack, and room page APIs
- "pay", "cashier", "refund", "brokerage"
  also inspect IM integration and QA tests
- "admin"
  inspect both `ai-tutor-admin` and `ai-tutor-admin-web`
