# 模块地图

先用这个文件把请求或变更文件映射到正确的模块边界。

## 仓库整体形态

- 后端父工程：`pom.xml`
- 用户端 Web：`ai-tutor-web`
- 管理端 Web：`ai-tutor-admin-web`
- 小程序：`ai-tutor-miniprogram`
- 核心后端服务：
  - `ai-tutor-gateway`
  - `tutor-appointment-service`
  - `videoCall-IM-service`
  - `payment-service`
  - `ai-tutor-admin`
- 公共后端模块：
  - `ai-tutor-common`
  - `ai-tutor-mq`
- QA 与 e2e：
  - `qa/automation`
  - `e2e-tests`
- 规格与方案：
  - `openspec`
  - `docs`

## 后端模块

### `ai-tutor-gateway`

- 职责：
  外部统一入口网关、JWT 解析、路由级身份提取
- 这些场景优先从这里开始：
  Bearer token 解析、网关鉴权、路由转发、Nacos 路由配置
- 关键文件：
  `ai-tutor-gateway/src/main/java/com/ai/tutor/gateway/security/JwtClaimsService.java`

### `ai-tutor-common`

- 职责：
  公共响应模型、异常、请求上下文、频控、身份签名辅助、共享事件
- 这些场景优先从这里开始：
  `RequestHolder`、全局错误风格、Feign 身份透传、共享抽象
- 关键文件：
  - `ai-tutor-common/src/main/java/com/ai/tutor/common/security/FeignIdentityRequestInterceptor.java`
  - `ai-tutor-common/src/main/java/com/ai/tutor/common/security/GatewayIdentityInterceptor.java`

### `ai-tutor-mq`

- 职责：
  MQ 安全调用辅助和事务自动配置
- 这些场景优先从这里开始：
  安全调用注解或 MQ 侧共享基础设施

### `tutor-appointment-service`

- 职责：
  用户登录与资料、教师/学生/机构发单流程、收藏、日程、上传、教师认证、联系方式
- 这些场景优先从这里开始：
  `/user/*`、`/api/v1/parent/*`、`/api/v1/tutor/*`、`/api/v1/org/*`、日程、资源上传
- 关键文件：
  - `controller/UserController.java`
  - `interceptor/RoleInterceptor.java`
  - `config/WebConfig.java`

### `videoCall-IM-service`

- 职责：
  聊天房间生命周期、消息持久化、SSE 实时流、已读回执、合作提案、联系方式解锁，以及部分佣金与退款邻接流程
- 重要提醒：
  这不是纯 IM 模块。很多聊天后的业务动作也在这里。
- 这些场景优先从这里开始：
  `/chat/*`、未读数、流式推送、消息卡片、合作状态
- 关键文件：
  - `chat/controller/ChatController.java`
  - `chat/controller/ChatRoomController.java`
  - `chat/controller/ChatStreamController.java`

### `payment-service`

- 职责：
  支付订单生命周期、收银台/预支付、云购收集成、回调通知、退款应用服务
- 这些场景优先从这里开始：
  收银台、订单状态轮询、支付回调、退款支付状态
- 关键文件：
  `payment-service/src/main/java/com/ai/tutor/payment/service/YungouosPaymentAppService.java`

### `ai-tutor-admin`

- 职责：
  管理端认证、仪表盘、需求审核、认证审核、退款、机构、用户、支付记录
- 这些场景优先从这里开始：
  `/api/admin/*` 接口或管理侧审核逻辑

## 前端模块

### `ai-tutor-web`

- 职责：
  面向教师、学生、机构、聊天、日程、支付、课程的主用户端 Web 应用
- 路由入口：
  `ai-tutor-web/src/router/index.ts`
- 这些场景优先从这里开始：
  主产品 UI、鉴权路由、需求浏览、聊天页、收银台页
- 重要 store：
  - `src/stores/auth.ts`
  - `src/stores/chatRealtime.ts`

### `ai-tutor-admin-web`

- 职责：
  管理端 UI，包括仪表盘、需求、用户、认证、机构、退款、支付
- 路由入口：
  `ai-tutor-admin-web/src/router/index.ts`

### `ai-tutor-miniprogram`

- 职责：
  Uni-app 小程序流程，包括首页、我的、发单、聊天、引导
- 路由入口：
  `ai-tutor-miniprogram/src/pages.json`

## 按文件模式快速判断

- `ai-tutor-web/src/pages/chat` 或 `src/ui/chat`
  读取 `business-flows.md` 与 `gotchas.md` 中的聊天章节
- `ai-tutor-web/src/pages/pay` 或 `src/api/payment.ts`
  读取 `business-flows.md` 与 `testing-matrix.md` 中的支付章节
- `tutor-appointment-service/src/main/java/.../interceptor`
  读取 `gotchas.md` 中与认证、角色相关的说明
- `videoCall-IM-service/src/main/java/.../chat`
  读取 `business-flows.md` 中与聊天、合作相关的流程说明
- `payment-service/src/main/java`
  读取 `business-flows.md` 中与支付、退款相关的章节
- `qa/automation/tests`
  读取 `testing-matrix.md`

## 规格与现实

- `openspec` 适合拿来理解意图、范围和计划中的变更
- 当前实现可能已经领先于部分规格文档
- 如果在实现任务中发现 spec 和代码不一致：
  当前行为以代码为准，然后把不一致记录到 `gotchas.md`
