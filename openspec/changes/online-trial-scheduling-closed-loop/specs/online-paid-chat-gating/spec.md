## ADDED Requirements

### Requirement: 申请通过后必须教师支付信息费才能解锁线上聊天闭环

系统 MUST 在聊天申请被同意后，将聊天、合作提案、试课安排、试课改期和正式排课能力统一绑定到教师信息费支付结果。

#### Scenario: 申请同意后进入待支付状态
- **WHEN** 任一方发起聊天申请且另一方同意
- **THEN** 系统 MUST 将聊天准入状态更新为 `PAYMENT_REQUIRED`

#### Scenario: 未支付前教师不可聊天
- **WHEN** 教师尝试进入聊天或发送消息，且聊天准入状态为 `PAYMENT_REQUIRED`
- **THEN** 系统 MUST 拒绝操作，并提示需先支付信息费

#### Scenario: 未支付前学生不可聊天
- **WHEN** 学生尝试进入聊天或发送消息，且聊天准入状态为 `PAYMENT_REQUIRED`
- **THEN** 系统 MUST 拒绝操作，并提示等待教师支付信息费

#### Scenario: 未支付前不可发起合作
- **WHEN** 任一方尝试发起合作提案，且聊天准入状态为 `PAYMENT_REQUIRED`
- **THEN** 系统 MUST 拒绝操作

#### Scenario: 未支付前不可排课
- **WHEN** 任一方尝试创建试课、改期试课或提交正式课表，且聊天准入状态为 `PAYMENT_REQUIRED`
- **THEN** 系统 MUST 拒绝操作

#### Scenario: 教师支付成功后解锁聊天
- **WHEN** 教师信息费订单支付成功
- **THEN** 系统 MUST 将聊天准入状态更新为 `CHAT_ENABLED`

#### Scenario: 课程正式结课后聊天继续开放
- **WHEN** 长期课程进入正式结课状态
- **THEN** 系统 MUST 允许聊天继续进行
- **AND** 聊天页面 MUST 展示“课程已结课”标签
- **AND** 系统 MUST NOT 再允许排课相关动作
