# P0 观测体系实施方案

本文档按模块给出建议改造顺序、打点入口和看板/告警联动方式。

## 实施顺序

1. 先扩展公共指标封装
2. 再补 `payment-service` 与 `ai-tutor-gateway` 的 Actuator/Prometheus 基线
3. 再按业务优先级补打点：
   - 注册激活
   - 申请漏斗
   - 支付解锁
   - 聊天可用性
   - 试课与长期转化
   - 退款
4. 最后补 Grafana 和告警

## 1. 公共层改造

目标文件：

- [BizKpiMetrics.java](/Users/luyipeng/project/ai_platform/ai-platform/ai-tutor-common/src/main/java/com/ai/tutor/common/metrics/BizKpiMetrics.java)

建议做法：

- 继续保留现在的“统一封装 + 无注册表时 no-op”模式。
- 新增方法时按业务域分组，避免散乱：
  - `incUserLogin`
  - `incProfileCompleted`
  - `incPaymentOrderCreated`
  - `incPaymentSuccess`
  - `incChatUnlock`
  - `incChatUnlockFailed`
  - `incChatMessageSent`
  - `incChatMessageFailed`
  - `incTrialProposalCreated`
  - `incTrialProposalDecision`
  - `incTrialProposalExpired`
  - `incTrialScheduled`
  - `incTrialDecision`
  - `incWeeklyScheduleSubmitted`
  - `incWeeklyScheduleTimeout`
  - `incRefundRequest`
  - `incRefundReview`

约束：

- 所有 labels 都必须经过 `safeEnum`
- 严禁引入 `userId/orderNo/roomId/applicationId` 等高基数 label
- 金额统一用分

## 2. `tutor-appointment-service`

### 2.1 注册激活

建议入口：

- [UserServiceImpl.java](/Users/luyipeng/project/ai_platform/ai-platform/tutor-appointment-service/src/main/java/com/ai/tutor/appointment/service/impl/UserServiceImpl.java)

建议打点：

- `userLoginOrRegister`
  - `isNew=true`：已存在 `incUserRegister`
  - `isNew=false`：新增 `incUserLogin`
- 资料保存 service
  - 新增 `incProfileCompleted`
  - 实现上要先有一个 `isProfileCompletedBefore/After` 判断函数

建议补充的实现细节：

- 不要在 controller 打点。
- 资料完成判断必须稳定，不要依赖前端页面文案。
- 资料完成状态可先通过实时计算完成，后续若频繁使用再落库缓存字段。

### 2.2 供需发布

建议入口：

- 学生/机构发布需求的 service 实现

建议打点：

- 发布成功：`incJobPostCreated`
- 关闭/撤销/完成：`incJobPostClosed`
- 详情查询成功：`incJobDetailView`

## 3. `videoCall-IM-service`

### 3.1 申请漏斗

建议入口：

- [TutorApplicationService.java](/Users/luyipeng/project/ai_platform/ai-platform/videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/chat/service/TutorApplicationService.java)

建议打点：

- `create`
  - 已存在 `incCommApply`
  - 可新增 `context_type` 维度
- 申请审批方法
  - 在 `PENDING -> ACCEPTED/REJECTED` 成功后打 `incCommApplyDecision`
- 申请详情方法
  - 成功查询后打 `incCommApplyDetailView`

幂等注意：

- `clientRequestId` 命中旧记录时绝不重复打点
- update 行数 <= 0 时不要打 decision

### 3.2 聊天可用性

建议入口：

- [ChatServiceImpl.java](/Users/luyipeng/project/ai_platform/ai-platform/videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/chat/service/impl/ChatServiceImpl.java)
- [ChatRoomServiceImpl.java](/Users/luyipeng/project/ai_platform/ai-platform/videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/chat/service/impl/ChatRoomServiceImpl.java)
- `SseSessionManager`

建议打点：

- 房间创建/进入成功：`incChatRoomEnter`
- 消息持久化成功：`incChatMessageSent`
- 因门禁/推送/持久化失败：`incChatMessageFailed`
- SSE 推送成功或事件落表成功：`incChatRealtimeDelivered`

幂等注意：

- 以“消息 insert 成功”作为 sent 计数唯一依据
- 同一条消息重推不要重复算 sent

### 3.3 支付后聊天解锁

建议入口：

- [CourseEnrollmentService.java](/Users/luyipeng/project/ai_platform/ai-platform/videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/chat/service/CourseEnrollmentService.java)
- `PaymentConsumer`

建议打点：

- `onApplicationAccepted`
  - 可选打 `WAIT_PAY` 漏斗埋点
- `onPaymentSuccess`
  - 课程状态从 `WAIT_PAY -> COMMUNICATING` 成功后打课程沟通恢复指标
- `tutorApplicationMapper.updateChatAccessStatus(... CHAT_ENABLED)` 成功后打 `incChatUnlock`
- 支付事件消费失败、状态更新失败、重试耗尽时打 `incChatUnlockFailed`

关键告警：

- 10 分钟内 `payment_success_total` 明显大于 `chat_unlock_total`

### 3.4 试课提案与试课转化

建议入口：

- [CollaborationProposalService.java](/Users/luyipeng/project/ai_platform/ai-platform/videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/chat/service/CollaborationProposalService.java)
- [CourseEnrollmentService.java](/Users/luyipeng/project/ai_platform/ai-platform/videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/chat/service/CourseEnrollmentService.java)

建议打点：

- `createAndSend`
  - insert 成功后 `incTrialProposalCreated`
- `respondAndSend`
  - `PENDING -> ACCEPTED/REJECTED` 成功后 `incTrialProposalDecision`
- 提案过期任务
  - `incTrialProposalExpired`
- `onCollaborationAccepted`
  - 试课日程真正落地成功后 `incTrialScheduled`
- 改期创建/处理
  - `incTrialRescheduleCreated`
  - `incTrialRescheduleDecision`
- `markTrialCanceled`
  - 状态更新成功后 `incTrialCancel`
- `processEndedTrials`
  - 状态更新成功后 `incTrialFinished`
- `submitTrialResult`
  - PASS/FAIL 成功后 `incTrialDecision`
- `confirmWeeklyScheduleSubmitted`
  - 状态更新成功后 `incWeeklyScheduleSubmitted`
- `processWeeklyScheduleTimeouts`
  - 超时更新成功后 `incWeeklyScheduleTimeout`

幂等注意：

- 定时任务必须依赖 update 成功行数判断是否首次迁移
- 只要状态没变成功，就不能打点

## 4. `payment-service`

### 4.1 支付订单

建议入口：

- [PaymentOrderServiceImpl.java](/Users/luyipeng/project/ai_platform/ai-platform/payment-service/src/main/java/com/ai/tutor/payment/service/impl/PaymentOrderServiceImpl.java)

建议打点：

- `createOrReusePending`
  - 仅新建 order 时 `incPaymentOrderCreated`
- `updateSuccess`
- `updateSuccessFromNotify`
- `updateSuccessFromProviderQuery`
  - 三个方法中，只有 update 成功时打：
    - `incPaymentSuccess`
    - `addPaymentInfoFeeAmountFen`

实现建议：

- 把“打支付成功指标 + 打金额指标 + 标记 event sent/fail”收口到一个私有方法，避免三处复制后口径跑偏。
- `channel` 统一使用订单已有字段，不做额外推断。

### 4.2 退款

建议入口：

- `PaymentRefundAppService.refund`

建议打点：

- 申请创建成功：`incRefundRequest`
- 提供方返回成功并落库成功：`incRefund`、`addRefundAmountFen`
- 提供方失败不打 success，只打 error log

## 5. `ai-tutor-admin`

### 5.1 退款审批

建议入口：

- `AdminRefundRequestService`
- `AdminRefundServiceImpl`

建议打点：

- 审批通过/拒绝成功：`incRefundReview(decision)`

说明：

- 管理端只统计“审批动作”，最终退款成功仍以后端支付域成功为准，避免一个退款被记两次成功。

## 6. Grafana 面板落地建议

### 6.1 Business KPI Dashboard

新增面板组：

- 注册激活漏斗
  - 验证码发送
  - 注册成功
  - 登录成功
  - 资料完成
- 申请与支付漏斗
  - 申请创建
  - 申请通过
  - 订单创建
  - 支付成功
  - 聊天解锁
- 试课与长期转化漏斗
  - 试课提案创建
  - 试课安排
  - 试课通过
  - 正式课表提交
- 退款风险
  - 退款申请
  - 退款审批通过/拒绝
  - 退款成功笔数
  - 退款金额

### 6.2 Payment & Fulfillment Dashboard

新增专项面板：

- `支付成功 -> 聊天解锁` 对比
- `试课安排 -> 试课通过 -> 正式课表提交` 对比
- `退款申请 -> 退款审批 -> 退款成功` 对比
- `聊天消息失败` 与 `SSE 成功投递` 对比

## 7. 首批建议落地顺序

为了最快拿到价值，建议按这个顺序实施：

1. `payment-service` 接入 Actuator/Prometheus
2. `ai-tutor-gateway` 接入 Actuator/Prometheus
3. 扩展 `BizKpiMetrics`
4. 补 `payment-service` 的订单创建、支付成功指标
5. 补 `videoCall-IM-service` 的聊天解锁、试课提案、试课决策指标
6. 补 `tutor-appointment-service` 的登录与资料完成指标
7. 升级 Grafana dashboard
8. 增加 3 条首批关键告警：
   - 支付成功后未解锁
   - 聊天消息失败突增
   - 退款金额突增
