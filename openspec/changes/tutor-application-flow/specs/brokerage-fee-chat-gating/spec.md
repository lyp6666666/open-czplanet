## ADDED Requirements

### Requirement: 申请通过后需要教师支付中介费以开启聊天
系统 MUST 将“开启聊天”与“教师支付中介费”绑定。对于状态为 `ACCEPTED` 的申请，系统 MUST 要求由教师完成中介费支付后，双方才可进入聊天。

#### Scenario: 教师发起申请且被通过
- **WHEN** 教师发起的申请被学员通过且申请状态变更为 `ACCEPTED`
- **THEN** 系统 MUST 将该申请标记为 chatAccessStatus = `PAYMENT_REQUIRED`

#### Scenario: 学员发起申请且被通过
- **WHEN** 学员发起的申请被教师通过且申请状态变更为 `ACCEPTED`
- **THEN** 系统 MUST 将该申请标记为 chatAccessStatus = `PAYMENT_REQUIRED`

### Requirement: 通过后系统可为申请生成中介费订单且付款人为教师
系统 MUST 支持为状态为 `ACCEPTED` 的申请生成一笔中介费订单，并 MUST 强制订单付款人为教师。

#### Scenario: 通过后创建或获取订单
- **WHEN** 一笔申请状态变更为 `ACCEPTED`
- **THEN** 系统 MUST 创建或获取与该申请绑定的中介费订单，并 MUST 返回 orderId 且 payerRole = `TEACHER`

#### Scenario: 非通过状态不可创建订单
- **WHEN** 申请状态不是 `ACCEPTED` 仍尝试创建中介费订单
- **THEN** 系统 MUST 拒绝并返回明确错误

### Requirement: 未支付前禁止双方进入聊天
系统 MUST 在后端统一拦截聊天进入/创建请求，确保未支付中介费时无法通过任何入口进入聊天。

#### Scenario: 教师尝试进入聊天但未支付
- **WHEN** 教师请求进入与该申请相关的 1v1 聊天，且该申请 chatAccessStatus = `PAYMENT_REQUIRED`
- **THEN** 系统 MUST 拒绝进入聊天，并 MUST 返回 paymentRequired = true 与 orderId

#### Scenario: 学员尝试进入聊天但未支付
- **WHEN** 学员请求进入与该申请相关的 1v1 聊天，且该申请 chatAccessStatus = `PAYMENT_REQUIRED`
- **THEN** 系统 MUST 拒绝进入聊天，并 MUST 返回 waitingForTeacherPayment = true

### Requirement: 支付完成后解锁聊天并返回可进入的会话标识
系统 MUST 在确认中介费订单完成支付后解锁聊天，并 MUST 支持为双方创建或复用既有 1v1 会话。

#### Scenario: 支付完成后解锁
- **WHEN** 与申请绑定的中介费订单状态变更为 `PAID`
- **THEN** 系统 MUST 将该申请标记为 chatAccessStatus = `CHAT_ENABLED`

#### Scenario: 解锁后进入聊天
- **WHEN** 申请 chatAccessStatus = `CHAT_ENABLED` 且任一方请求进入聊天
- **THEN** 系统 MUST 返回可进入的 roomId（会话不存在时 MUST 创建，存在时 MUST 复用）
