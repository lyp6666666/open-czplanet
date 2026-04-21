## ADDED Requirements

### Requirement: 试课改期入口必须从我的课程进入

系统 MUST 将试课改期入口放在“我的课程”的试课阶段中，而 MUST NOT 允许通过聊天直接发起改期。

#### Scenario: 试课阶段展示改期入口
- **WHEN** 长期课程处于 `TRIAL_SCHEDULED`
- **THEN** 系统 MUST 在“我的课程”中展示试课改期入口

### Requirement: 试课改期必须复用双方可视化时间选择器

系统 MUST 在发起试课改期时复用试课合作提案的双列时间选择器和冲突校验逻辑。

#### Scenario: 发起试课改期
- **WHEN** 用户点击试课改期
- **THEN** 系统 MUST 展示双方当日日程的双列时间视图
- **AND** MUST 支持重新选择日期、开始时间和结束时间
- **AND** MUST 执行与试课合作提案相同的冲突校验

### Requirement: 同一时刻只能存在一个待处理试课改期提案

系统 MUST 保证同一门课程在任意时刻只能存在一个待处理试课改期提案。

#### Scenario: 已有待处理改期单时再次发起
- **WHEN** 某课程已有状态为 `PENDING` 的试课改期提案
- **THEN** 系统 MUST 拒绝新的改期提案创建

### Requirement: 改期结果必须通过系统消息同步给对方

系统 MUST 在一方发起试课改期后通过聊天系统消息通知对方，并允许对方在消息中同意或拒绝。

#### Scenario: 发起改期后发送系统消息
- **WHEN** 一方提交试课改期提案
- **THEN** 聊天中 MUST 发送待处理改期系统消息

#### Scenario: 对方同意改期
- **WHEN** 接收方同意改期提案
- **THEN** 系统 MUST 更新试课时间
- **AND** 将课程阶段恢复为 `TRIAL_SCHEDULED`

#### Scenario: 对方拒绝改期
- **WHEN** 接收方拒绝改期提案
- **THEN** 系统 MUST 保留原试课时间
- **AND** 聊天中 MUST 发送改期被拒绝的系统消息

### Requirement: 试课开始前允许取消，取消优先级高于改期

系统 MUST 允许任一方在试课开始前取消试课，并在取消时使待处理改期提案失效。

#### Scenario: 试课开始前取消
- **WHEN** 任一方在试课开始前发起取消
- **THEN** 系统 MUST 作废当前试课事件
- **AND** 将课程阶段回退到 `COMMUNICATING`
- **AND** 聊天中 MUST 发送“试课已取消，已回到沟通阶段”的系统消息

#### Scenario: 取消时存在待处理改期提案
- **WHEN** 试课被取消且该课程存在状态为 `PENDING` 的改期提案
- **THEN** 系统 MUST 将该改期提案状态更新为 `INVALIDATED`
