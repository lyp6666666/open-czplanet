## ADDED Requirements

### Requirement: 客户端可建立实时消息流连接
系统 MUST 提供实时消息流接口，认证通过的用户可以建立长连接以接收与自己相关的新消息事件。

#### Scenario: 建立连接成功
- **WHEN** 已登录用户发起实时消息流连接请求
- **THEN** 系统 MUST 建立连接并返回可持续接收事件的响应

#### Scenario: 未认证连接被拒绝
- **WHEN** 未登录用户发起实时消息流连接请求
- **THEN** 系统 MUST 拒绝该请求

### Requirement: 新消息 MUST 实时推送给会话参与方
当 room 内产生新消息时，系统 MUST 向该消息的接收方推送实时事件；系统 MAY 同步向发送方推送以支持多端一致性。

#### Scenario: 接收方在线收到推送
- **WHEN** 用户 A 向用户 B 发送一条消息且用户 B 在线并已建立消息流连接
- **THEN** 系统 MUST 向用户 B 推送包含该消息的事件

#### Scenario: 接收方不在线不阻塞发送
- **WHEN** 用户 A 向用户 B 发送一条消息且用户 B 未建立消息流连接
- **THEN** 系统 MUST 仍然成功落库该消息且 MUST 不因推送失败影响发送结果

### Requirement: 事件数据 MUST 可被客户端用于渲染消息
系统推送的事件数据 MUST 包含渲染消息所需的最小信息集，包括消息 id、roomId、发送者 uid、接收者 uid、发送时间与消息体。

#### Scenario: 推送事件包含最小字段集
- **WHEN** 系统推送一条新消息事件
- **THEN** 事件数据 MUST 包含 msgId、roomId、fromUid、toUid、sendTime、body 字段
