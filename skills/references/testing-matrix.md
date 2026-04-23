# 测试矩阵

针对受影响区域，选择“最小但有意义”的验证方式。

## 后端公共层或认证相关

- 适用范围：
  网关鉴权、公共身份透传、请求拦截器、角色检查
- 最低验证要求：
  运行最接近的模块测试
- 常见目标：
  - `./mvnw -pl tutor-appointment-service test`
  - `./mvnw -pl ai-tutor-admin test`
  - 与拦截器或认证控制器相关的定向测试

## Appointment 或用户/资料改动

- 适用范围：
  `/user/*`、资料更新、需求、收藏、日程、上传
- 最低验证要求：
  `./mvnw -pl tutor-appointment-service test`
- 还应考虑：
  如果 UI 也改了，补跑用户端 Web 的类型检查或测试

## 聊天或实时通信改动

- 适用范围：
  房间列表、消息页面、SSE 流、未读数、已读回执、合作提案
- 最低验证要求：
  `./mvnw -pl videoCall-IM-service test`
- 同时可考虑：
  `cd ai-tutor-web && npm run test`
  如果改到了前端聊天页或 store
- 可选 QA 目标：
  `qa/automation/tests/api/test_chat_smoke.py`
  `qa/automation/tests/e2e/test_chat_e2e.py`

## 支付或退款改动

- 适用范围：
  预支付、收银台 UI、订单轮询、回调通知、退款应用服务
- 最低验证要求：
  `./mvnw -pl payment-service test`
- 同时可考虑：
  QA 支付 smoke / regression
  如果状态在管理端有展示，也要覆盖管理端退款路径

## 管理端改动

- 适用范围：
  `ai-tutor-admin` 或 `ai-tutor-admin-web`
- 最低验证要求：
  - 后端：`./mvnw -pl ai-tutor-admin test`
  - 前端：`cd ai-tutor-admin-web && npm run typecheck && npm run lint`

## 用户端 Web 改动

- 适用范围：
  `ai-tutor-web/src`
- 最低验证要求：
  `cd ai-tutor-web && npm run typecheck`
- 如果可用，也建议执行：
  `npm run lint`
  `npm run test`

## 实时课堂 / LiveKit / 远端音视频改动

- 适用范围：
  `live-class-service`、`ai-tutor-web/src/pages/live/*`、`ai-tutor-web/src/modules/live/livekit.ts`、`Dockerfile/livekit/*`、`nginx /livekit` 转发、云安全组
- 最低验证要求：
  `cd ai-tutor-web && PLAYWRIGHT_BASE_URL=https://huoyue.online PLAYWRIGHT_API_BASE_URL=https://huoyue.online OPS_VERIFY_TOKEN=DevOpsVerifyTokenForE2E npx playwright test e2e/live-classroom.spec.ts --project=chromium -g "teacher and student can join same livekit room with media permissions"`
- 如果怀疑网络层或云防火墙：
  同时在 `111.228.20.88` 上抓包 `tcpdump`，确认浏览器是否真正回包到 `50000-50100/udp` 或 `7881/tcp`
- 通过标准：
  - 页面状态进入 `connected`
  - 双方都收到 `participant:connected`
  - 双方都收到远端 `track:subscribed` 的 `video/audio`
  - 不再出现双方都显示“等待对方加入”

## 小程序改动

- 适用范围：
  `ai-tutor-miniprogram/src`
- 最低验证要求：
  `cd ai-tutor-miniprogram && npm run type-check`

## QA 或 E2E 改动

- 适用范围：
  `qa/automation` 或 `e2e-tests`
- 最低验证要求：
  运行最近、最相关的测试套件或单个测试文件

## 跨模块规则

如果一次改动跨越多个区域：

- 每个被影响的模块至少验证一次
- 优先组合成“一组定向后端测试 + 一组定向前端或 QA 检查”
- 如果你没法执行验证，就明确说明跳过了什么，以及为什么跳过
