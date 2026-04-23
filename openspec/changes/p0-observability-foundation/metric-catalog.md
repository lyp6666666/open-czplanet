# P0 业务指标字典

本文档是 `p0-observability-foundation` 的实施级补充，目标是把“业务指标”落到开发和 Grafana 可直接使用的粒度。

## 1. 注册与激活漏斗

| Metric | 中文口径 | Type | Labels | Owner Service | 触发方法/位置 | 幂等条件 | PromQL 示例 | 建议告警 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_sms_code_send_total` | 真实触发验证码发送次数 | Counter | `scene`（如保留必须是固定枚举） | `tutor-appointment-service` | `SmsService` 成功发送分支 | 限流命中不计；仅真实发送时计 | `sum(increase(ai_tutor_biz_sms_code_send_total[1d]))` | 单位时间内突增可做 P3 风控告警 |
| `ai_tutor_biz_user_register_total` | 首次注册成功用户数 | Counter | `role` | `tutor-appointment-service` | `UserServiceImpl.userLoginOrRegister` | 仅 `isNew=true` 计数 | `sum by (role) (increase(ai_tutor_biz_user_register_total[1d]))` | 无 |
| `ai_tutor_biz_user_login_total` | 老用户登录成功次数 | Counter | `role` | `tutor-appointment-service` | `UserServiceImpl.userLoginOrRegister` | 仅 `isNew=false` 且登录成功后计 | `sum by (role) (increase(ai_tutor_biz_user_login_total[1d]))` | 登录量突降可做 P3 |
| `ai_tutor_biz_profile_completed_total` | 首次完成最低资料门槛用户数 | Counter | `role` | `tutor-appointment-service` | `updateUserInfo` 对应 service | 只允许从“未完成”首次转“完成” | `sum by (role) (increase(ai_tutor_biz_profile_completed_total[1d]))` | 无 |

效果：

- 可拼出 `验证码 -> 注册 -> 登录 -> 资料完成` 漏斗。
- 可识别投流问题、验证码问题、资料引导问题。

## 2. 供需发布与浏览

| Metric | 中文口径 | Type | Labels | Owner Service | 触发方法/位置 | 幂等条件 | PromQL 示例 | 建议告警 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_job_post_created_total` | 需求发布成功数 | Counter | `publisher_role` | `tutor-appointment-service` | 学生/机构发布需求成功的 service | 仅 insert 成功计数 | `sum by (publisher_role) (increase(ai_tutor_biz_job_post_created_total[1d]))` | 无 |
| `ai_tutor_biz_job_post_closed_total` | 需求关闭数 | Counter | `publisher_role`,`close_reason` | `tutor-appointment-service` | 需求状态流转到终态的 service | 仅首次进入终态 | `sum by (close_reason) (increase(ai_tutor_biz_job_post_closed_total[1d]))` | 关闭率异常升高可做 P3 |
| `ai_tutor_biz_job_detail_view_total` | 需求详情成功浏览数 | Counter | `viewer_role` | `tutor-appointment-service` | 详情查询成功返回路径 | 只统计返回 200 且找到详情 | `sum(increase(ai_tutor_biz_job_detail_view_total[1d]))` | 无 |

效果：

- 能看平台有没有真实供给。
- 能看教师有没有真实消费需求。

## 3. 申请漏斗

| Metric | 中文口径 | Type | Labels | Owner Service | 触发方法/位置 | 幂等条件 | PromQL 示例 | 建议告警 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_comm_apply_total` | 家教申请创建成功数 | Counter | `initiator`,`context_type` | `videoCall-IM-service` | `TutorApplicationService.create` | 仅 insert 成功；命中 `clientRequestId` 幂等返回不重复计 | `sum by (initiator) (increase(ai_tutor_biz_comm_apply_total[1d]))` | 无 |
| `ai_tutor_biz_comm_apply_decision_total` | 家教申请审批结果数 | Counter | `initiator`,`decision` | `videoCall-IM-service` | `TutorApplicationService.decide/accept/reject` 成功流转 | 仅 `PENDING -> ACCEPTED/REJECTED` | `sum by (initiator,decision) (increase(ai_tutor_biz_comm_apply_decision_total[1d]))` | 通过率骤降可做 P3 |
| `ai_tutor_biz_comm_apply_detail_view_total` | 申请详情打开成功数 | Counter | `viewer_role` | `videoCall-IM-service` | 申请详情查询接口 | 只统计真正查询成功 | `sum(increase(ai_tutor_biz_comm_apply_detail_view_total[1d]))` | 无 |

效果：

- 能按教师发起/学生发起拆转化。
- 能定位申请量高但通过率低的问题。

## 4. 支付与聊天解锁

| Metric | 中文口径 | Type | Labels | Owner Service | 触发方法/位置 | 幂等条件 | PromQL 示例 | 建议告警 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_payment_order_created_total` | 信息费待支付订单创建数 | Counter | `biz_type`,`channel` | `payment-service` | `PaymentOrderServiceImpl.createOrReusePending` | 仅新建订单时；复用 pending 不重复计 | `sum(increase(ai_tutor_biz_payment_order_created_total[1d]))` | 无 |
| `ai_tutor_biz_payment_success_total` | 信息费支付成功数 | Counter | `biz_type`,`channel` | `payment-service` | `updateSuccess*` 三个成功方法 | 仅 `PENDING -> SUCCESS` | `sum by (channel) (increase(ai_tutor_biz_payment_success_total[1d]))` | 成功率骤降 P2 |
| `ai_tutor_biz_payment_info_fee_amount_cents_total` | 信息费支付成功金额累计 | Counter | `channel` | `payment-service` | 同上 | 仅首次成功累加 | `sum(increase(ai_tutor_biz_payment_info_fee_amount_cents_total[1d])) / 100` | 金额突降可做 P3 |
| `ai_tutor_biz_chat_unlock_total` | 支付后聊天解锁成功数 | Counter | `unlock_reason` | `videoCall-IM-service` | `CourseEnrollmentService.onPaymentSuccess` / 更新 `CHAT_ENABLED` 成功后 | 仅 `PAYMENT_REQUIRED -> CHAT_ENABLED` | `sum(increase(ai_tutor_biz_chat_unlock_total[1d]))` | 与支付成功数偏差过大 P1 |
| `ai_tutor_biz_chat_unlock_failed_total` | 支付成功但聊天未解锁数 | Counter | `reason` | `videoCall-IM-service` | 支付事件消费失败、状态更新失败、重试超阈值 | 仅真实失败 | `sum(increase(ai_tutor_biz_chat_unlock_failed_total[10m]))` | >0 可直接 P1 |

效果：

- 这是最关键收入闭环。
- 必须用 `支付成功数` 对比 `聊天解锁数`，否则会漏掉“付费成功但业务没落实”。

## 5. 聊天可用性

| Metric | 中文口径 | Type | Labels | Owner Service | 触发方法/位置 | 幂等条件 | PromQL 示例 | 建议告警 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_chat_room_enter_total` | 聊天房间进入/创建成功数 | Counter | `entry_type` | `videoCall-IM-service` | `ChatRoomServiceImpl` 进入房间成功 | 成功返回才计 | `sum(increase(ai_tutor_biz_chat_room_enter_total[1d]))` | 无 |
| `ai_tutor_biz_chat_message_sent_total` | 消息发送成功数 | Counter | `message_type` | `videoCall-IM-service` | `ChatServiceImpl.sendMsg` 持久化成功后 | 以 msg insert 成功为准 | `sum by (message_type) (increase(ai_tutor_biz_chat_message_sent_total[1h]))` | 无 |
| `ai_tutor_biz_chat_message_failed_total` | 消息发送失败数 | Counter | `reason` | `videoCall-IM-service` | `ChatServiceImpl.sendMsg` 失败分支 | 仅真实失败 | `sum by (reason) (increase(ai_tutor_biz_chat_message_failed_total[5m]))` | 短时突增 P2 |
| `ai_tutor_biz_chat_realtime_delivered_total` | 实时事件成功投递数 | Counter | `event_type` | `videoCall-IM-service` | `SseSessionManager` 或事件落库成功处 | 只统计成功推送或成功落事件表 | `sum by (event_type) (increase(ai_tutor_biz_chat_realtime_delivered_total[1h]))` | 大幅下降 P3 |

效果：

- 能区分“没人聊”和“想聊但发不出去”。

## 6. 试课提案与试课安排

| Metric | 中文口径 | Type | Labels | Owner Service | 触发方法/位置 | 幂等条件 | PromQL 示例 | 建议告警 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_trial_proposal_created_total` | 试课提案创建成功数 | Counter | `initiator` | `videoCall-IM-service` | `CollaborationProposalService.createAndSend` | 仅 insert 成功；命中 `clientRequestId` 不重复计 | `sum by (initiator) (increase(ai_tutor_biz_trial_proposal_created_total[1d]))` | 无 |
| `ai_tutor_biz_trial_proposal_decision_total` | 试课提案处理结果数 | Counter | `decision` | `videoCall-IM-service` | `CollaborationProposalService.respondAndSend` | 仅 `PENDING -> ACCEPTED/REJECTED` | `sum by (decision) (increase(ai_tutor_biz_trial_proposal_decision_total[1d]))` | 接受率骤降 P3 |
| `ai_tutor_biz_trial_proposal_expired_total` | 试课提案超时过期数 | Counter | 无 | `videoCall-IM-service` | 12 小时过期任务 | 仅首次过期 | `sum(increase(ai_tutor_biz_trial_proposal_expired_total[1d]))` | 突增 P3 |
| `ai_tutor_biz_trial_scheduled_total` | 试课安排成功数 | Counter | 无 | `videoCall-IM-service` | `CourseEnrollmentService.onCollaborationAccepted` 完成试课落地后 | 仅首次安排成功 | `sum(increase(ai_tutor_biz_trial_scheduled_total[1d]))` | 无 |
| `ai_tutor_biz_trial_reschedule_created_total` | 试课改期提案创建数 | Counter | 无 | 课程域 | 改期提案创建成功处 | 仅新建成功 | `sum(increase(ai_tutor_biz_trial_reschedule_created_total[1d]))` | 无 |
| `ai_tutor_biz_trial_reschedule_decision_total` | 改期处理结果数 | Counter | `decision` | 课程域 | 改期处理成功处 | 仅状态迁移成功 | `sum by (decision) (increase(ai_tutor_biz_trial_reschedule_decision_total[1d]))` | 无 |
| `ai_tutor_biz_trial_cancel_total` | 试课取消成功数 | Counter | `cancel_by` | `videoCall-IM-service`/课程域 | `CourseEnrollmentService.markTrialCanceled` | 仅状态更新成功 | `sum by (cancel_by) (increase(ai_tutor_biz_trial_cancel_total[1d]))` | 取消率突增 P3 |

效果：

- 能看清从聊天到试课的实际履约前损耗。

## 7. 试课后转长期课

| Metric | 中文口径 | Type | Labels | Owner Service | 触发方法/位置 | 幂等条件 | PromQL 示例 | 建议告警 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_trial_finished_total` | 试课自然结束进入待决策数 | Counter | 无 | `videoCall-IM-service` | `CourseEnrollmentService.processEndedTrials` | 仅 `TRIALING -> TRIAL_WAIT_STUDENT_DECISION` | `sum(increase(ai_tutor_biz_trial_finished_total[1d]))` | 无 |
| `ai_tutor_biz_trial_decision_total` | 试课后学生决策结果数 | Counter | `decision` | `videoCall-IM-service` | `CourseEnrollmentService.submitTrialResult` | 仅成功提交 PASS/FAIL | `sum by (decision) (increase(ai_tutor_biz_trial_decision_total[1d]))` | 通过率突降 P3 |
| `ai_tutor_biz_weekly_schedule_submitted_total` | 正式课表提交成功数 | Counter | 无 | `videoCall-IM-service`/课程域 | `CourseEnrollmentService.confirmWeeklyScheduleSubmitted` | 仅首次提交成功 | `sum(increase(ai_tutor_biz_weekly_schedule_submitted_total[1d]))` | 无 |
| `ai_tutor_biz_weekly_schedule_timeout_total` | 24 小时内未排正式课超时数 | Counter | 无 | `videoCall-IM-service`/课程域 | `processWeeklyScheduleTimeouts` | 仅首次超时失败 | `sum(increase(ai_tutor_biz_weekly_schedule_timeout_total[1d]))` | 突增 P3 |

效果：

- 能观察 `试课安排 -> 试课结束 -> 试课通过 -> 正式课表提交` 的长期转化。

## 8. 退款与争议

| Metric | 中文口径 | Type | Labels | Owner Service | 触发方法/位置 | 幂等条件 | PromQL 示例 | 建议告警 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_refund_request_total` | 用户发起退款申请数 | Counter | `refund_type` | `videoCall-IM-service` / `payment-service` | `CourseEnrollmentService.applyTrialRefund` 等申请入口 | 仅申请记录创建成功 | `sum(increase(ai_tutor_biz_refund_request_total[1d]))` | 突增 P3 |
| `ai_tutor_biz_refund_review_total` | 管理端退款审批结果数 | Counter | `decision` | `ai-tutor-admin` | `AdminRefundRequestService` 审批成功后 | 仅审批落库成功 | `sum by (decision) (increase(ai_tutor_biz_refund_review_total[1d]))` | 审批通过率突增 P3 |
| `ai_tutor_biz_refund_total` | 退款成功笔数 | Counter | 无 | `payment-service` 或管理域最终成功点 | 退款首次成功 | `sum(increase(ai_tutor_biz_refund_total[1d]))` | 突增 P2 |
| `ai_tutor_biz_refund_amount_cents_total` | 退款成功金额累计 | Counter | 无 | `payment-service` 或管理域最终成功点 | 首次成功累加金额 | `sum(increase(ai_tutor_biz_refund_amount_cents_total[1d])) / 100` | 金额突增 P2 |

效果：

- 能区分退款诉求增长、审批通过变多、还是最终退款金额异常。
