## ADDED Requirements

### Requirement: 教师可配置默认打招呼语并回显
系统 MUST 允许教师配置默认打招呼语文本，并在教师再次进入“我的/简历”或刷新登录态时回显该配置。

#### Scenario: 教师保存默认打招呼语
- **WHEN** 教师在设置入口提交默认打招呼语文本
- **THEN** 系统 MUST 保存该文本并返回更新成功

#### Scenario: 教师清空默认打招呼语
- **WHEN** 教教师将默认打招呼语设置为空并提交
- **THEN** 系统 MUST 保存为空值并返回更新成功

#### Scenario: 教师回显默认打招呼语
- **WHEN** 教师请求获取当前登录用户信息
- **THEN** 返回数据 MUST 包含 defaultGreeting 字段且与最近一次保存一致

### Requirement: 默认打招呼语仅在首次建立联系时自动发送
系统 MUST 仅在教师与目标用户的 room 内尚无任何消息时，才允许把 defaultGreeting 作为自动首条消息发送；系统 MUST 保证幂等，不因重复点击或重试产生多条重复招呼语。

#### Scenario: 首次建立联系自动发送
- **WHEN** 教师发起沟通且 defaultGreeting 非空，且该 room 内尚无消息
- **THEN** 系统 MUST 发送一条文本消息，内容为 defaultGreeting

#### Scenario: 非首次建立联系不再自动发送
- **WHEN** 教师发起沟通且 defaultGreeting 非空，但该 room 内已有消息
- **THEN** 系统 MUST NOT 自动发送任何招呼语消息
