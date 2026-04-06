## 背景

当前平台已具备「信息费（brokerage\_order）支付 → 解锁聊天」的能力，并已接入 YunGouOS 扫码支付（payment\_order + /payment/prepay + notify 回调闭环）。但业务上仍缺少：

「申请退费」在聊天内的闭环（申请 → 管理端审核可查看聊天记录 → 审批通过 → 原路退款）

- 「达成合作」后展示双方联系方式，并使「申请退费」按钮失效
- 「试课期」与「我的课程」状态视图
- 「试课不通过」仅退 60% 信息费（带说明与图片证据）并走管理端审核闭环

本变更在不改变现有支付通道（YunGouOS 扫码）前提下，补齐支付与退款的业务闭环，并提供可验收的接口、校验与测试用例。

## 目标

- 完成信息费的两类退款闭环
  - 聊天阶段「申请退费」：100% 退款，申请后立即关闭聊天
  - 试课阶段「试课不通过」：60% 退款，需上传图片证据
- 管理端可见退款申请清单与详情
  - 审核时可查看双方聊天记录（不脱敏，用于确认是否泄露联系方式）
  - 试课退款可查看说明与图片证据
- 合作达成（双方确认）后：
  - 聊天页面弹出双方联系方式（默认电话=注册手机号）
  - 「申请退费」按钮失效并给出固定提示文案
- 新增「我的课程」页面（教师为主，同时可扩展学生视角）
  - 按栏展示课程/合作的状态：申请中、待支付、沟通中、退费审批中、已退费、试课中、开课中、已结课、试课退费审批中

## 非目标（本次不做）

- 引入新的支付通道（仅 YunGouOS 扫码）
- 引入独立的课程收费/课时计费（试课退款仍以信息费为标的）
- 自动识别“泄露联系方式”的内容审核（由管理员通过聊天记录人工判定）

## 现状对齐（代码库已有能力）

- payment-service 已具备 payment\_order 模型、YunGouOS 下单/回调/查单
- videoCall-IM-service 已具备：
  - tutor\_application + chatAccessStatus（PAYMENT\_REQUIRED/CHAT\_ENABLED）
  - collaboration\_proposal（发起合作/同意/拒绝）
  - CONTACT\_UNLOCKED 系统消息能力与“合作提案已同意+订单已支付”后解锁联系方式的服务（但当前仅教师侧可查看）
  - 系统消息 bizType= BROKERAGE\_REFUND\_REQUEST 时会自动关闭 room（ChatServiceImpl 已实现）
- ai-tutor-admin 已有「退款纠纷」的雏形接口，但与 brokerage\_order.status 枚举不一致且未触发真实退款

## 核心概念与状态机

### 1）信息费订单（brokerage\_order）

信息费订单是后续聊天、合作、退款与试课退款的共同标的。现有状态仅覆盖支付前后，本次扩展为可表达退款流程：

- PENDING：待支付（信息费订单已生成，未完成支付）
- PAID：已支付（解锁聊天）
- REFUND\_REVIEW：退费审批中（聊天退费申请）
- TRIAL\_REFUND\_REVIEW：试课退费审批中
- REFUNDED：已退费（无论聊天退费/试课退费最终都落到该状态）
- REJECTED / CANCELED：维持原语义（若已存在使用）

约束：

- brokerage\_order.status 从 PAID 才允许进入退款申请状态
- 退款成功后 MUST 进入 REFUNDED 且不可再次申请任何退款

### 2）退款申请（新增 refund\_request）

新增退款申请表用于承载“申请原因、证据、审批记录、退款金额/比例”等信息，避免污染 brokerage\_order 表，同时使管理端可按申请维度审计。

refund\_request.type：

- CHAT\_INFO\_FEE：聊天阶段申请退费（100%）
- TRIAL\_INFO\_FEE：试课不通过退费（60%）

refund\_request.status：

- PENDING：待审核
- APPROVED：审核通过（进入发起退款/已退款）
- REJECTED：审核拒绝

说明：

- 管理端列表以 refund\_request.status=PENDING 为主，并同步展示对应 brokerage\_order.status（用于课程页状态映射）

### 3）课程视图（新增 course\_enrollment）

新增 course\_enrollment 用于驱动「我的课程」页面的稳定展示与状态流转（避免每次列表都做复杂跨表推导）。

course\_enrollment.status：

- APPLYING：申请中（tutor\_application=PENDING）
- WAIT\_PAY：待支付（tutor\_application=ACCEPTED 且 chatAccessStatus=PAYMENT\_REQUIRED / brokerage\_order=PENDING）
- COMMUNICATING：沟通中（chatAccessStatus=CHAT\_ENABLED / brokerage\_order=PAID 且未合作达成、未发起退款）
- REFUND\_REVIEW：退费审批中（聊天退款申请）
- REFUNDED：已退费
- TRIALING：试课中（合作达成后开始试课期，trialEndAt=trialStartAt+7d）
- TRIAL\_REFUND\_REVIEW：试课退费审批中
- TEACHING：开课中（试课期结束且未发起试课退款）
- FINISHED：已结课（本次提供接口但不强制上线 UI；可先作为占位）

生成与推进规则：

- course\_enrollment 以 tutor\_application 为主键语义（applicationId 唯一）
- 当合作提案状态变更为 ACCEPTED（双方确认）时：
  - course\_enrollment.status MUST 进入 TRIALING
  - trialStartAt=now，trialEndAt=now+7d
- 定时推进（或懒加载推进）：
  - 当 status=TRIALING 且 now > trialEndAt 且不存在 TRIAL\_INFO\_FEE 的 PENDING 申请：status 自动推进为 TEACHING

## 关键业务规则

### 规则 A：聊天阶段「申请退费」按钮

按钮展示与可点击：

- MUST 在聊天解锁后（brokerage\_order=PAID 且 room.status=1）显示按钮
- MUST 在以下任一情况禁用：
  - 双方合作已达成（collaboration\_proposal=ACCEPTED）
  - 已存在任意 PENDING 的退款申请（refund\_request.status=PENDING）
  - brokerage\_order.status=REFUNDED

Hover 文案：

- 可点击时：`申请退费意味着合作失败，聊天功能会立即关闭，相关费用将在6个小时内退回`
- 不可点击（合作达成后）：`发起合作后无法再退换信息费用，请尽快完成试课，并前往我的课程中查看详情`

点击后效果（聊天退费申请）：

- MUST 创建 refund\_request(type=CHAT\_INFO\_FEE, status=PENDING)
- MUST 将 brokerage\_order.status 更新为 REFUND\_REVIEW
- MUST 发送一条系统消息（msgType=8，bizType=BROKERAGE\_REFUND\_REQUEST），以触发房间关闭逻辑
- MUST 立即关闭聊天（room.status=closed）

### 规则 B：合作达成后展示联系方式

- 合作提案支持双方确认：一方发起，另一方同意后状态=ACCEPTED（现有能力）
- 当状态变更为 ACCEPTED 后：
  - 前端 MUST 弹出对方联系方式（双方都能看到对方电话）
  - 后端 MUST 提供受控接口返回联系方式（避免通过普通文本消息传播）
  - 「申请退费」按钮 MUST 变为不可用，并展示固定提示文案
- 合作达成后聊天功能继续可用（允许继续发送消息）

### 规则 C：试课不通过退款（60%）

入口：

- 「我的课程」页面，教师在 status=TRIALING 或 TEACHING（由产品最终确认，默认 TRIALING）可发起“试课不通过”

提交校验：

- MUST 要求填写说明 reason（非空，长度限制）
- MUST 要求至少 1 张图片证据（URL 列表）
- MUST 确保申请人是课程的教师且该课程未 REFUNDED
- MUST 确保当前在试课期内（now <= trialEndAt），否则返回明确错误（避免“过期后再退”）

提交后：

- refund\_request(type=TRIAL\_INFO\_FEE, percent=60, amount=orderAmount\*0.6, status=PENDING)
- brokerage\_order.status = TRIAL\_REFUND\_REVIEW
- course\_enrollment.status = TRIAL\_REFUND\_REVIEW

审核通过后：

- MUST 发起部分退款（60%），并记录退款单（payment\_refund）
- brokerage\_order.status = REFUNDED
- course\_enrollment.status = REFUNDED

## 接口（对外与对内）

### 1）用户端（videoCall-IM-service）

#### 1.1 查询聊天页退款按钮状态

- `GET /chat/refund/state?roomId={roomId}`
  - 返回：`canApply`、`disableReasonCode`、`hoverText`

#### 1.2 申请聊天退费（100%）

- `POST /chat/refund/apply`
  - 入参：`roomId`、`reason`（可选，长度限制）
  - 校验：
    - room 存在且双方身份匹配
    - 必须已支付信息费（brokerage\_order=PAID）
    - 合作未达成（proposal != ACCEPTED）
    - 不存在 PENDING 申请
  - 结果：创建 refund\_request + 关闭聊天

#### 1.3 我的课程列表

- `GET /courses/my?page=&size=&role=TEACHER|STUDENT`
  - 返回课程列表（含 applicationId、roomId、对方信息摘要、status、trialEndAt、refundState）

#### 1.4 试课不通过退款申请（60%）

- `POST /courses/{courseId}/trial-refund/apply`
  - 入参：`reason`、`evidenceImageUrls[]`
  - 校验：见规则 C

#### 1.5 获取对方联系方式（合作达成后）

- `GET /chat/contact/unlocked?roomId={roomId}&targetUid={targetUid}`
  - 变更：支持双方查询对方；校验 proposal=ACCEPTED 且 brokerage\_order=PAID 且无退款中/已退费

### 2）管理端（ai-tutor-admin）

#### 2.1 退款申请列表

- `GET /api/admin/refund/requests?page=&size=&type=&status=`

#### 2.2 退款申请详情（含聊天记录、证据）

- `GET /api/admin/refund/requests/{requestId}`

#### 2.3 审核通过

- `POST /api/admin/refund/requests/{requestId}/approve`
  - 触发 payment-service 发起退款（全额/60%）
  - 幂等：重复审批不重复退款

#### 2.4 审核拒绝

- `POST /api/admin/refund/requests/{requestId}/reject`
  - 入参：`reason`（必填）

### 3）支付域（payment-service）

#### 3.1 发起退款（内部接口）

- `POST /payment/refund`
  - 入参：`paymentOrderNo` 或 `contextType/contextId`，`refundAmountFen`，`reason`，`requestId`
  - 校验：
    - payment\_order.status=SUCCESS
    - refundAmountFen <= payment\_order.amount
    - 幂等：按 requestId 或（paymentOrderNo + refundAmountFen）保证不重复发起
  - 输出：refundNo、refundStatus

## 数据库变更（migrations）

### 1）扩展 brokerage\_order

- 增加 status 枚举值：REFUND\_REVIEW/TRIAL\_REFUND\_REVIEW/REFUNDED（以及与现有代码保持一致的命名）
- 增加字段（建议）：
  - `refund_locked` tinyint：是否已进入退款流程（避免并发）
  - `refunded_amount_fen` bigint：已退款金额（支持部分退款 60% 的可追溯）

### 2）新增 refund\_request

建议字段：

- id（PK）
- brokerage\_order\_id（唯一或一对多；本次约束同一订单最多 1 个 PENDING）
- course\_id（nullable；试课退款必填）
- room\_id（nullable；聊天退款必填）
- applicant\_uid、applicant\_role
- type（CHAT\_INFO\_FEE/TRIAL\_INFO\_FEE）
- status（PENDING/APPROVED/REJECTED）
- reason（text/varchar）
- evidence\_images\_json（text，存 URL 数组）
- refund\_percent（int，聊天=100，试课=60）
- refund\_amount\_fen（bigint）
- admin\_uid、admin\_note、decided\_at
- created\_at、updated\_at

索引：

- (status, type, created\_at)
- (brokerage\_order\_id, status)

### 3）新增 payment\_refund

用于记录与 YunGouOS 的退款交互与幂等。

建议字段：

- refund\_no（唯一）
- payment\_order\_no
- provider（YUNGOUOS）
- provider\_refund\_no（可空）
- refund\_amount\_fen
- status（PENDING/SUCCESS/FAILED）
- request\_id（业务幂等键，对应 refund\_request.id）
- fail\_reason
- created\_at、updated\_at

### 4）新增 course\_enrollment

建议字段：

- id（PK）
- application\_id（unique）
- room\_id
- proposal\_id（合作提案 id）
- teacher\_uid、student\_uid
- status（APPLYING/WAIT\_PAY/COMMUNICATING/REFUND\_REVIEW/REFUNDED/TRIALING/TRIAL\_REFUND\_REVIEW/TEACHING/FINISHED）
- trial\_start\_at、trial\_end\_at
- created\_at、updated\_at

## 服务改动点（落点）

- videoCall-IM-service
  - 新增 refund\_request/course\_enrollment 相关实体、mapper、service、controller
  - 扩展 BrokerageOrderStatus 枚举并对齐 mapper SQL
  - 修正聊天发送前置校验：合作达成后仍允许继续聊天
  - 扩展 ContactUnlockService：支持双方查看对方联系方式
- ai-tutor-admin
  - 新增退款申请后端 API（替换/兼容现有 disputes API）
  - 审核通过时调用 payment-service 内部退款接口
  - 详情页增加试课退款说明与图片证据展示
- payment-service
  - 扩展 YunGouOS client 支持 refund
  - 新增 payment\_refund 表与退款编排服务（幂等、错误处理、审计）
- ai-tutor-web
  - 聊天页新增“申请退费”按钮、tooltip 与禁用态逻辑
  - 合作达成后弹出联系方式（双方）
  - 新增“我的课程”页与顶部导航入口，支持教师发起试课退款申请（含图片上传）

## 测试策略

- payment-service
  - 单元测试：退款幂等（重复 approve 不重复发起），金额校验，mock:// baseUrl 路径下模拟成功/失败
  - 集成测试：创建 payment\_order(success) → 发起 refund → 写入 payment\_refund → 返回成功
- videoCall-IM-service
  - 单元/集成测试：
    - 申请聊天退款后必须 closeRoom（复用既有 ChatServiceImplRefundCloseTest 扩展覆盖 controller/service 层）
    - 合作达成后仍可发送消息（回归测试）
    - 试课退款：证据校验、试课期校验、状态推进校验
- ai-tutor-admin
  - Service 测试：approve 调用 payment-service（mock feign）并更新 refund\_request/brokerage\_order 状态

