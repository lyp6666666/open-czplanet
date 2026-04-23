## ADDED Requirements

### Requirement: 注册与身份链路必须暴露转化型业务指标

系统 MUST 为登录、注册、验证码和资料完善链路采集业务指标，以支持判断新用户是否成功进入业务闭环。

#### Scenario: 验证码发送成功
- **WHEN** 系统真实触发一次验证码发送动作
- **THEN** 系统 MUST 增加 `ai_tutor_biz_sms_code_send_total`

#### Scenario: 新用户注册成功
- **WHEN** 用户首次注册成功并创建用户记录
- **THEN** 系统 MUST 增加 `ai_tutor_biz_user_register_total`
- **AND** MUST 记录标签 `role=teacher|student|org`

#### Scenario: 老用户登录成功
- **WHEN** 已存在用户完成一次登录
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_user_login_total`
- **AND** SHOULD 记录标签 `role=teacher|student|org`

#### Scenario: 首次资料完善完成
- **WHEN** 用户首次把最低可用资料补齐并达到业务准入门槛
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_profile_completed_total`
- **AND** SHOULD 记录标签 `role=teacher|student|org`

### Requirement: 需求供给链路必须暴露供需侧业务指标

系统 MUST 为学生/家长发需求、教师浏览需求、机构发布需求等供需动作采集核心指标，以支持判断供给是否充足、匹配是否顺畅。

#### Scenario: 学生需求发布成功
- **WHEN** 学生或家长成功创建一条需求
- **THEN** 系统 MUST 增加 `ai_tutor_biz_job_post_created_total`
- **AND** MUST 记录标签 `publisher_role=student|org`

#### Scenario: 学生需求状态变更
- **WHEN** 需求从可见变为关闭、取消、完成等终态
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_job_post_closed_total`
- **AND** SHOULD 记录标签 `close_reason=filled|cancelled|expired|other`

#### Scenario: 教师浏览需求详情
- **WHEN** 教师成功打开需求详情页且后端返回有效详情
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_job_detail_view_total`

### Requirement: 聊天申请链路必须暴露申请漏斗指标

系统 MUST 为双向申请及其审批链路采集业务指标，以支持判断申请创建、接收、审批和解锁前的损耗。

#### Scenario: 创建申请成功
- **WHEN** 教师或学生成功创建申请
- **THEN** 系统 MUST 增加 `ai_tutor_biz_comm_apply_total`
- **AND** MUST 记录标签 `initiator=teacher|student|org`
- **AND** MUST 记录标签 `context_type=demand|tutor`

#### Scenario: 申请被通过或拒绝
- **WHEN** 接收方对 `PENDING` 申请执行通过或拒绝
- **THEN** 系统 MUST 增加 `ai_tutor_biz_comm_apply_decision_total`
- **AND** MUST 记录标签 `initiator=teacher|student|org`
- **AND** MUST 记录标签 `decision=approved|rejected`

#### Scenario: 申请详情被查看
- **WHEN** 发起方或接收方成功打开申请详情
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_comm_apply_detail_view_total`
- **AND** SHOULD 记录标签 `viewer_role=teacher|student|org`

### Requirement: 信息费支付与聊天解锁链路必须暴露闭环指标

系统 MUST 为“申请通过 -> 待支付 -> 支付成功 -> 聊天解锁”链路采集完整业务指标，以支持判断资金转化和聊天门禁是否打通。

#### Scenario: 信息费订单创建成功
- **WHEN** 系统成功创建一笔信息费订单
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_payment_order_created_total`
- **AND** SHOULD 记录标签 `biz_type=info_fee`

#### Scenario: 信息费订单支付成功
- **WHEN** 信息费订单状态首次从 `PENDING` 变为 `SUCCESS`
- **THEN** 系统 MUST 增加 `ai_tutor_biz_payment_success_total`
- **AND** MUST 增加 `ai_tutor_biz_payment_info_fee_amount_cents_total`
- **AND** MUST 记录标签 `biz_type=info_fee`
- **AND** SHOULD 记录标签 `channel=yungouos|wechat|alipay|other`

#### Scenario: 支付成功后聊天解锁
- **WHEN** 支付成功后聊天准入状态首次进入 `CHAT_ENABLED`
- **THEN** 系统 MUST 增加 `ai_tutor_biz_chat_unlock_total`
- **AND** MUST 记录标签 `unlock_reason=payment_success`

#### Scenario: 支付成功但聊天未解锁
- **WHEN** 支付成功后下游未能完成聊天解锁
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_chat_unlock_failed_total`

### Requirement: 聊天实时与互动链路必须暴露可用性和活跃指标

系统 SHOULD 为聊天房间创建、消息发送、SSE 投递和消息失败重试采集业务指标，以支持判断“可聊天”是否真的等于“能稳定聊”。

#### Scenario: 聊天房间创建成功
- **WHEN** 系统成功创建房间或返回已存在房间
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_chat_room_enter_total`

#### Scenario: 用户发送消息成功
- **WHEN** 一条文本或图片消息成功持久化
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_chat_message_sent_total`
- **AND** SHOULD 记录标签 `message_type=text|image|system|proposal`

#### Scenario: SSE 事件成功投递
- **WHEN** 一次实时事件成功写入或推送给在线客户端
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_chat_realtime_delivered_total`

#### Scenario: 消息发送失败
- **WHEN** 消息因权限、门禁、存储或推送异常导致失败
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_chat_message_failed_total`
- **AND** SHOULD 记录标签 `reason=access_denied|persist_error|push_error|other`

### Requirement: 试课合作提案链路必须暴露状态机指标

系统 MUST 为试课合作提案创建、过期、通过、拒绝采集业务指标，以支持判断教师与学生是否能顺利进入试课安排。

#### Scenario: 试课合作提案创建成功
- **WHEN** 任一方在聊天中成功创建试课合作提案
- **THEN** 系统 MUST 增加 `ai_tutor_biz_trial_proposal_created_total`
- **AND** MUST 记录标签 `initiator=teacher|student`

#### Scenario: 试课合作提案处理完成
- **WHEN** 接收方同意或拒绝试课合作提案
- **THEN** 系统 MUST 增加 `ai_tutor_biz_trial_proposal_decision_total`
- **AND** MUST 记录标签 `decision=accepted|rejected`

#### Scenario: 试课合作提案超时过期
- **WHEN** 提案 12 小时未处理并自动过期
- **THEN** 系统 MUST 增加 `ai_tutor_biz_trial_proposal_expired_total`

### Requirement: 试课安排、改期、取消必须暴露履约前指标

系统 MUST 为试课排期、改期、取消采集业务指标，以支持判断试课阶段是否顺利推进。

#### Scenario: 试课安排成功
- **WHEN** 合作提案同意后系统成功写入试课事件
- **THEN** 系统 MUST 增加 `ai_tutor_biz_trial_scheduled_total`

#### Scenario: 发起改期成功
- **WHEN** 任一方成功创建试课改期提案
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_trial_reschedule_created_total`

#### Scenario: 改期被同意或拒绝
- **WHEN** 改期提案被处理
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_trial_reschedule_decision_total`
- **AND** SHOULD 记录标签 `decision=accepted|rejected|invalidated`

#### Scenario: 试课取消成功
- **WHEN** 任一方在试课开始前取消试课
- **THEN** 系统 MUST 增加 `ai_tutor_biz_trial_cancel_total`
- **AND** MUST 记录标签 `cancel_by=teacher|student`

### Requirement: 试课后决策与正式排课必须暴露关键转化指标

系统 MUST 为试课结束后的学生决策、正式课表提交、超时失败采集业务指标，以支持判断试课是否真正转化为正式履约。

#### Scenario: 试课自然结束进入待决策
- **WHEN** 试课结束时间到达并自动进入待学生决策
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_trial_finished_total`

#### Scenario: 学生选择通过或不通过
- **WHEN** 学生完成试课后决策
- **THEN** 系统 MUST 增加 `ai_tutor_biz_trial_decision_total`
- **AND** MUST 记录标签 `decision=passed|failed`

#### Scenario: 正式课表提交成功
- **WHEN** 学生成功提交正式固定课表
- **THEN** 系统 MUST 增加 `ai_tutor_biz_weekly_schedule_submitted_total`

#### Scenario: 试课通过后 24 小时超时未排正式课
- **WHEN** 课程因超时未提交正式课表而转为失败
- **THEN** 系统 MUST 增加 `ai_tutor_biz_weekly_schedule_timeout_total`

### Requirement: 退款链路必须暴露争议与结果指标

系统 MUST 为退款申请、审核、成功、失败采集业务指标，以支持判断退款风险和人工处理负载。

#### Scenario: 用户发起退款申请
- **WHEN** 用户成功提交退款申请
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_refund_request_total`

#### Scenario: 管理端处理退款申请
- **WHEN** 管理员批准或拒绝退款申请
- **THEN** 系统 SHOULD 增加 `ai_tutor_biz_refund_review_total`
- **AND** SHOULD 记录标签 `decision=approved|rejected`

#### Scenario: 退款成功
- **WHEN** 退款状态首次变为成功
- **THEN** 系统 MUST 增加 `ai_tutor_biz_refund_total`
- **AND** MUST 增加 `ai_tutor_biz_refund_amount_cents_total`

### Requirement: 业务指标必须避免高基数并声明来源服务

系统 MUST 为每一个业务指标限定低基数标签，并明确由哪个服务、哪个状态变更点负责采集。

#### Scenario: 指标设计评审
- **WHEN** 团队新增一个业务指标
- **THEN** 指标定义 MUST 包含 `metric name`、`type`、`labels`、`owner service`、`trigger point`、`PromQL`
- **AND** MUST NOT 使用 `userId`、`roomId`、`orderNo`、`phone` 等高基数标签
