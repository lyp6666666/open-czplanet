### Requirement: 小程序 MUST 以申请机制收口沟通入口
学生和教师之间的首次沟通 MUST 通过申请中心处理，申请通过后 MUST 按当前业务规则进入支付和聊天解锁。

#### Scenario: 查看申请中心
- **WHEN** 用户进入消息 Tab 的申请入口
- **THEN** 小程序 MUST 拉取发出的申请和收到的申请
- **AND** MUST 展示申请状态、关联对象、最后更新时间和未读标记

#### Scenario: 通过申请
- **WHEN** 申请接收方点击通过
- **THEN** 小程序 MUST 调用 `/chat/application/{applicationId}/decision`
- **AND** MUST 刷新申请状态
- **AND** MUST NOT 直接开放普通聊天，除非后端确认聊天已解锁

#### Scenario: 拒绝申请
- **WHEN** 申请接收方点击拒绝
- **THEN** 小程序 MUST 调用 `/chat/application/{applicationId}/decision`
- **AND** MUST 展示拒绝后的终态

### Requirement: 教师信息费支付 MUST 驱动聊天解锁
申请通过后，如业务要求教师支付信息费，教师 MUST 支付成功后才能进入普通聊天。

#### Scenario: 教师发起支付
- **WHEN** 教师在已通过申请中点击去支付
- **THEN** 小程序 MUST 调用 `/payment/prepay`
- **AND** MUST 展示订单金额、倒计时、支付渠道和重试入口
- **AND** MUST 使用 `uni.requestPayment` 发起微信小程序支付

#### Scenario: 支付成功
- **WHEN** `/payment/orders/{orderNo}` 返回支付成功
- **THEN** 小程序 MUST 刷新申请详情
- **AND** MUST 调用或进入 `/chat/application/{applicationId}/enter-chat`
- **AND** 聊天输入区 MUST 变为可用

#### Scenario: 支付未完成
- **WHEN** 用户取消支付、支付失败或订单超时
- **THEN** 小程序 MUST 保留当前订单状态
- **AND** MUST 展示重新支付或返回申请详情入口
- **AND** MUST NOT 误判聊天已解锁

### Requirement: 聊天 MUST 支持消息收发、未读已读和业务卡片
聊天室 MUST 支持文本/图片消息、系统业务卡片、未读/已读、增量同步和失败重试。

#### Scenario: 进入聊天室
- **WHEN** 用户进入已解锁聊天室
- **THEN** 小程序 MUST 拉取历史消息
- **AND** MUST 启动增量事件同步
- **AND** MUST 调用已读回执清理未读数

#### Scenario: 聊天未解锁
- **WHEN** 用户进入 `chatAccessStatus=PAYMENT_REQUIRED` 的聊天室
- **THEN** 小程序 MUST 禁用普通输入
- **AND** MUST 展示支付或等待支付引导

#### Scenario: 发送消息失败
- **WHEN** `/chat/msg` 调用失败
- **THEN** 小程序 MUST 保留失败消息
- **AND** MUST 提供重试或删除入口
