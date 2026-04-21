## ADDED Requirements

### Requirement: 试课结束后必须自动进入待学生决策状态

系统 MUST 在试课结束时间自然到达后自动将课程切换到待学生决策状态。

#### Scenario: 试课自然结束
- **WHEN** 当前时间到达试课结束时间
- **THEN** 系统 MUST 将课程阶段更新为 `TRIAL_WAIT_STUDENT_DECISION`

### Requirement: 学生必须选择是否继续

系统 MUST 在试课结束后要求学生显式选择“通过，继续上课”或“不通过，结束合作”。

#### Scenario: 学生选择不通过
- **WHEN** 学生在试课结束后选择“不通过”
- **THEN** 系统 MUST 将课程状态更新为 `TRIAL_FAILED`
- **AND** 系统 MUST 关闭聊天

#### Scenario: 学生选择通过
- **WHEN** 学生在试课结束后选择“通过”
- **THEN** 系统 MUST 要求其提交正式固定课表或进入待补充正式课表状态

### Requirement: 正式固定课表只能由学生提交

系统 MUST 限制正式固定课表的提交权限为学生侧。

#### Scenario: 教师尝试提交正式课表
- **WHEN** 教师提交正式固定课表
- **THEN** 系统 MUST 拒绝操作

### Requirement: 正式固定课表必须在一周视图中选择且所有时长一致

系统 MUST 允许学生在一周视图中选择一个或多个固定时段，并要求同一课程下所有固定时段时长一致。

#### Scenario: 选择多日固定课表
- **WHEN** 学生选择周内多个固定时段
- **THEN** 系统 MUST 允许保存多日规则

#### Scenario: 固定时段时长不一致
- **WHEN** 学生选择的多个固定时段时长不一致
- **THEN** 前端 MUST 提示错误
- **AND** 后端 MUST 拒绝保存

### Requirement: 正式固定课表需要进行前后端双重冲突校验

系统 MUST 在正式固定课表提交前，对双方日程进行前后端双重冲突校验。

#### Scenario: 正式课表与本人冲突
- **WHEN** 学生选择的固定时段与本人日程冲突
- **THEN** 系统 MUST 提示并阻止提交

#### Scenario: 正式课表与对方冲突
- **WHEN** 学生选择的固定时段与教师日程冲突
- **THEN** 系统 MUST 提示并阻止提交

### Requirement: 学生可稍后补选正式课表，但必须在24小时内完成

系统 MUST 允许学生在试课通过后稍后提交正式课表，但 MUST 设置 24 小时截止时间。

#### Scenario: 通过后稍后选择
- **WHEN** 学生选择试课通过但暂不立即提交正式课表
- **THEN** 系统 MUST 将课程阶段更新为 `TRIAL_WAIT_WEEKLY_SCHEDULE`
- **AND** MUST 写入 24 小时决策截止时间

### Requirement: 截止前需发送提醒且完成选择后需去重

系统 MUST 在截止前 12 小时、6 小时、1 小时分别发送聊天提醒，并在学生完成正式课表选择后停止后续提醒。

#### Scenario: 到达12小时提醒点
- **WHEN** 距离截止时间还有 12 小时且学生尚未提交正式课表
- **THEN** 聊天中 MUST 发送提醒系统消息

#### Scenario: 已提交正式课表后不再提醒
- **WHEN** 学生已成功提交正式课表
- **THEN** 系统 MUST NOT 再发送后续 6 小时和 1 小时提醒

### Requirement: 超时未提交正式课表应视为试课失败

系统 MUST 在学生超过 24 小时仍未提交正式课表时自动判定试课失败。

#### Scenario: 超时未提交
- **WHEN** 当前时间超过正式课表截止时间且课程仍处于 `TRIAL_WAIT_WEEKLY_SCHEDULE`
- **THEN** 系统 MUST 将课程状态更新为 `TRIAL_FAILED`
- **AND** 系统 MUST 关闭聊天
- **AND** 教师端 MUST 展示可退 80% 信息费的状态提示

### Requirement: 正式结课后聊天允许继续但不可继续排课

系统 MUST 在课程正式结课后保留聊天能力，并阻止进一步排课。

#### Scenario: 正式结课
- **WHEN** 课程状态更新为 `FINISHED`
- **THEN** 系统 MUST 保留聊天能力
- **AND** 聊天页面 MUST 展示“课程已结课”标签
- **AND** 系统 MUST NOT 允许发起新的合作、试课、改期或正式排课
