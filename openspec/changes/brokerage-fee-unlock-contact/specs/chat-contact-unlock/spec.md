## Requirements

### Requirement: 中介费支付完成后方可查看对方联系方式
系统 MUST 仅在满足“合作提案已同意 + 中介费订单已支付”的条件下允许教师查看对方联系方式。

#### Scenario: 未支付不可查看
- **GIVEN** 合作提案状态为 `ACCEPTED` 且订单状态不是 `PAID`
- **WHEN** 教师请求查看联系方式
- **THEN** 系统 MUST 拒绝并返回明确错误

#### Scenario: 已支付可查看
- **GIVEN** 合作提案状态为 `ACCEPTED` 且订单状态为 `PAID`
- **WHEN** 教师请求查看对方联系方式
- **THEN** 系统 MUST 返回对方联系方式字段集合

### Requirement: 联系方式不得通过普通文本消息传播
系统 MUST 避免在普通文本消息中下发联系方式，联系方式展示 MUST 通过受控接口返回并在前端受控展示。
