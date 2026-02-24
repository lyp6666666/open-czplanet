## Requirements

### Requirement: 合作提案同意后可生成中介费订单
系统 MUST 在合作提案状态为 `ACCEPTED` 的前提下，允许为该提案生成一笔中介费订单，用于后续支付与解锁流程。

#### Scenario: 幂等创建
- **GIVEN** 存在 proposalId = P 的合作提案，且状态为 `ACCEPTED`
- **WHEN** 教师或系统请求“按提案创建订单”
- **THEN** 系统 MUST 返回同一笔订单（若已存在），不得创建重复有效订单

#### Scenario: 提案未同意不可创建
- **GIVEN** proposalId = P 的提案状态不是 `ACCEPTED`
- **WHEN** 请求“按提案创建订单”
- **THEN** 系统 MUST 拒绝并返回明确错误

### Requirement: 订单状态机可追溯
系统 MUST 将订单状态限制在受控状态机内，并记录关键时间点。

#### Scenario: 状态流转
- **GIVEN** 订单状态为 `PENDING`
- **WHEN** 付款人提交支付凭证
- **THEN** 订单状态 MUST 变更为 `PROOF_SUBMITTED`

- **GIVEN** 订单状态为 `PROOF_SUBMITTED`
- **WHEN** 管理端确认到账
- **THEN** 订单状态 MUST 变更为 `PAID` 且记录 `paidAt`

### Requirement: 订单权限控制
系统 MUST 仅允许订单付款人或管理员查询订单详情与状态；仅允许付款人提交凭证；仅允许管理员确认到账。
