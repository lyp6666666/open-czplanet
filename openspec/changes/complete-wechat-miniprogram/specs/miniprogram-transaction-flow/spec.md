## ADDED Requirements

### Requirement: 小程序 MUST 通过申请机制收口首次沟通
学生和教师之间的首次沟通 MUST 先进入申请流程，申请通过后再按支付状态解锁聊天。

#### Scenario: 查看申请中心
- **WHEN** 用户进入申请中心
- **THEN** 小程序 MUST 调用发出和收到申请接口
- **AND** MUST 展示状态、上下文、对方、聊天权限和未读信息

#### Scenario: 申请通过后等待支付
- **WHEN** 申请被接收方通过且后端要求信息费
- **THEN** 小程序 MUST 展示 `PAYMENT_REQUIRED`
- **AND** 教师侧 MUST 显示支付入口
- **AND** 学生侧 MUST 显示等待教师支付

### Requirement: 信息费支付 MUST 使用真实微信支付闭环
小程序生产支付 MUST 使用后端 `/payment/prepay` 返回的微信 JSAPI 参数，并根据订单状态驱动聊天解锁。

#### Scenario: 教师创建支付单
- **WHEN** 教师点击支付信息费
- **THEN** 小程序 MUST 调用 `/payment/prepay`
- **AND** MUST 展示订单号、金额、渠道、截止时间和当前状态

#### Scenario: 发起微信支付
- **WHEN** 后端返回 `payParams`
- **THEN** 小程序 MUST 调用 `uni.requestPayment`
- **AND** MUST 在支付发起后轮询 `/payment/orders/{orderNo}`

#### Scenario: 支付成功
- **WHEN** 订单状态返回 `SUCCESS` 或 `PAID`
- **THEN** 小程序 MUST 调用或刷新 `/chat/application/{applicationId}/enter-chat`
- **AND** MUST 进入聊天室
- **AND** 聊天输入区 MUST 变为可用

#### Scenario: 支付取消或失败
- **WHEN** 用户取消支付、支付失败或订单超时
- **THEN** 小程序 MUST 保留订单状态
- **AND** MUST 展示重新支付、刷新状态或返回申请详情入口
- **AND** MUST NOT 解锁聊天

### Requirement: 聊天室 MUST 承接业务卡片和状态同步
聊天室 MUST 支持普通消息、图片消息、业务系统卡片、未读已读和增量同步。

#### Scenario: 进入聊天室
- **WHEN** 用户进入聊天室
- **THEN** 小程序 MUST 调用真实消息分页接口
- **AND** MUST 启动事件同步或短轮询
- **AND** MUST 调用已读回执清理未读数

#### Scenario: 聊天未解锁
- **WHEN** 当前房间仍处于支付前状态
- **THEN** 小程序 MUST 禁用文本、图片和合作提案输入
- **AND** MUST 展示支付或等待支付提示

#### Scenario: 发送文本或图片
- **WHEN** 用户发送文本或图片
- **THEN** 小程序 MUST 插入本地发送中消息
- **AND** 成功后 MUST 替换为服务端消息
- **AND** 失败后 MUST 保留失败状态并提供重试

### Requirement: 小程序 MUST 支持试课合作提案
聊天解锁后，双方 MUST 能发起、处理和查看试课合作提案。

#### Scenario: 发起合作提案
- **WHEN** 用户在已解锁聊天中点击合作
- **THEN** 小程序 MUST 展示课时费、试课日期、开始时间、结束时间和备注表单
- **AND** SHOULD 查询双方可用时间
- **AND** MUST 调用 `/chat/collaboration/proposal`

#### Scenario: 接受合作提案
- **WHEN** 接收方点击接受
- **THEN** 小程序 MUST 调用 `/chat/collaboration/proposal/{proposalId}/response`
- **AND** MUST 刷新聊天卡片
- **AND** MUST 能跳转到生成后的课程合作

#### Scenario: 拒绝合作提案
- **WHEN** 接收方点击拒绝
- **THEN** 小程序 MUST 调用真实响应接口
- **AND** MUST 展示提案已拒绝
- **AND** MUST 允许重新发起新提案
