## ADDED Requirements

### Requirement: 用户可上报会话已读进度
系统 MUST 提供会话已读上报接口，用户可以在打开会话或读到最新消息时上报 lastReadMsgId，系统 MUST 持久化该进度。

#### Scenario: 上报已读进度成功
- **WHEN** 用户在 room 内上报 lastReadMsgId
- **THEN** 系统 MUST 保存该 room+uid 的已读进度

#### Scenario: 上报越权被拒绝
- **WHEN** 用户对非自己参与的 room 上报 lastReadMsgId
- **THEN** 系统 MUST 拒绝该请求

### Requirement: 系统可返回会话维度未读数
系统 MUST 能够为用户返回其会话列表中每个 room 的未读消息数。

#### Scenario: 会话列表返回未读数
- **WHEN** 用户请求会话列表
- **THEN** 返回数据 MUST 包含每个 room 的 unreadCount 字段

#### Scenario: 已读后未读数归零
- **WHEN** 用户上报已读进度为该 room 的最新消息
- **THEN** 该 room 对该用户的 unreadCount MUST 为 0
