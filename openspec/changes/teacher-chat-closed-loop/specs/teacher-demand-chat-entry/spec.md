## ADDED Requirements

### Requirement: 教师可从需求入口发起沟通
系统 MUST 支持教师从需求列表、需求详情与首页热门需求发起与需求发布者的 1v1 沟通，会话不存在时 MUST 创建并返回 roomId，存在时 MUST 返回既有 roomId。

#### Scenario: 从需求列表发起沟通
- **WHEN** 教师在需求列表点击“立即沟通”
- **THEN** 系统 MUST 获取或创建与该需求发布者的 room 并返回 roomId

#### Scenario: 从需求详情发起沟通
- **WHEN** 教师在需求详情点击“立即沟通”
- **THEN** 系统 MUST 获取或创建与该需求发布者的 room 并返回 roomId

#### Scenario: 从首页热门需求发起沟通
- **WHEN** 教师在首页热门需求卡片点击“立即沟通”
- **THEN** 系统 MUST 获取或创建与该需求发布者的 room 并返回 roomId

### Requirement: 发起沟通接口支持携带首条招呼语
系统 MUST 提供“发起沟通”接口以支持客户端携带可选的首条招呼语文本。系统 MUST 在同一次请求内完成 room 获取/创建，并在满足“首次建立联系”的条件时发送招呼语消息。

#### Scenario: 携带招呼语且为首次建立联系
- **WHEN** 教师调用发起沟通接口并携带非空 greeting，且 room 内尚无消息
- **THEN** 系统 MUST 创建或获取 room，并 MUST 发送一条文本消息作为首条消息

#### Scenario: 携带招呼语但会话已存在消息
- **WHEN** 教师调用发起沟通接口并携带非空 greeting，且 room 内已有消息
- **THEN** 系统 MUST 仅返回 roomId 且 MUST NOT 自动发送重复招呼语

#### Scenario: 未携带招呼语
- **WHEN** 教师调用发起沟通接口且 greeting 为空
- **THEN** 系统 MUST 返回 roomId 且 MUST NOT 自动发送任何消息
