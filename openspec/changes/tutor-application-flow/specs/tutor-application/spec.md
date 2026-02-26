## ADDED Requirements

### Requirement: 申请支持双向发起并绑定业务上下文
系统 MUST 支持教师向学员、学员向教师发起申请。每笔申请 MUST 绑定发起方与接收方的用户标识，并 MUST 绑定业务上下文（例如需求 demandId 或教员 tutorId），以便后续审核、支付与聊天解锁流程可追溯。

#### Scenario: 教师从需求发起申请
- **WHEN** 教师在需求入口提交申请内容并携带 demandId
- **THEN** 系统 MUST 创建一笔申请，记录 senderRole = `TEACHER`、receiverRole = `STUDENT`、contextType = `DEMAND`、contextId = demandId，且 status = `PENDING`

#### Scenario: 学员从教员入口发起申请
- **WHEN** 学员在教员入口提交申请内容并携带 tutorId
- **THEN** 系统 MUST 创建一笔申请，记录 senderRole = `STUDENT`、receiverRole = `TEACHER`、contextType = `TUTOR`、contextId = tutorId，且 status = `PENDING`

#### Scenario: 申请内容非法
- **WHEN** 发起方提交申请且内容为空或超过系统上限
- **THEN** 系统 MUST 拒绝创建并返回明确错误

### Requirement: 申请创建支持幂等
系统 MUST 支持以客户端幂等键（例如 clientRequestId）实现“创建申请”的幂等，避免因重试导致重复申请。

#### Scenario: 使用相同幂等键重复提交
- **WHEN** 发起方以相同 clientRequestId 重复调用“创建申请”
- **THEN** 系统 MUST 返回同一笔申请的 applicationId 与当前状态，且 MUST NOT 创建新的申请记录

### Requirement: 申请状态机受控且可审计
系统 MUST 将申请状态限制在受控状态机内，并记录关键时间点以便审计与风控。

#### Scenario: 状态流转受控
- **WHEN** 申请状态为 `PENDING` 且接收方选择“通过”
- **THEN** 系统 MUST 将状态变更为 `ACCEPTED` 并记录 decidedAt

#### Scenario: 非法状态流转被拒绝
- **WHEN** 申请状态不为 `PENDING` 仍尝试执行“通过/拒绝”
- **THEN** 系统 MUST 拒绝并返回明确错误

### Requirement: 仅接收方可通过或拒绝申请
系统 MUST 仅允许申请接收方对 `PENDING` 状态的申请执行“通过/拒绝”操作。

#### Scenario: 接收方通过申请
- **WHEN** 接收方对 `PENDING` 申请执行“通过”
- **THEN** 系统 MUST 将申请状态更新为 `ACCEPTED`

#### Scenario: 接收方拒绝申请
- **WHEN** 接收方对 `PENDING` 申请执行“拒绝”
- **THEN** 系统 MUST 将申请状态更新为 `REJECTED`

#### Scenario: 非接收方尝试处理申请
- **WHEN** 非接收方用户对申请执行“通过/拒绝”
- **THEN** 系统 MUST 拒绝并返回明确错误

### Requirement: 申请详情对双方可见并包含解锁前置信息
系统 MUST 允许申请发起方与接收方查询申请详情，并 MUST 在详情中提供后续解锁链路所需的关键信息（例如：是否需要教师支付中介费、当前是否已满足进入聊天条件）。

#### Scenario: 双方查询申请详情
- **WHEN** 申请发起方或接收方查询申请详情
- **THEN** 系统 MUST 返回申请基础信息（参与方、上下文、内容、状态、时间点）以及 paymentPayerRole = `TEACHER` 与 chatAccessStatus
