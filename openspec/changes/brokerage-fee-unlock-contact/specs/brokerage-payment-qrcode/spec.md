## Requirements

### Requirement: 系统提供独立的收款码支付页面
系统 MUST 提供独立页面展示中介费支付信息，并支持微信/支付宝收款码切换。

#### Scenario: 打开支付页
- **GIVEN** 教师拥有一笔中介费订单
- **WHEN** 教师打开支付页并携带订单标识
- **THEN** 系统 MUST 展示订单金额、订单状态、支付说明与收款码

### Requirement: 教师可提交“已支付”凭证
系统 MUST 允许教师在支付页提交“我已完成支付”，并可附带凭证（图片 URL）或备注。

#### Scenario: 提交凭证成功
- **GIVEN** 订单状态为 `PENDING`
- **WHEN** 教师提交支付凭证
- **THEN** 系统 MUST 将订单状态更新为 `PROOF_SUBMITTED` 并在页面展示“待平台确认”

### Requirement: 页面可查询并实时反映订单状态
系统 MUST 支持查询订单状态并在页面刷新后保持一致展示。
