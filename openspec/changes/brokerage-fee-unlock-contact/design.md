## Overview

本变更在“合作提案已同意”后引入中介费支付与联系方式解锁能力，形成可变现闭环。Phase 1 采用“跳转收款码页面 + 用户提交已支付凭证 + 管理端确认到账”的方式落地；后续可替换为微信/支付宝真实支付。

## Key Flows

### 1) 合作达成 -> 生成中介费订单 -> 聊天提示去支付

1. 学生在合作提案卡片中点击“同意”，合作提案状态变为 `ACCEPTED`。
2. 服务端为该 `proposalId` 生成一笔中介费订单（幂等：同一提案最多一笔有效订单）。
3. 服务端向该房间投递系统消息 `BROKERAGE_REQUIRED`（结构化消息），教师端显示“去支付”入口。

### 2) 教师支付 -> 提交凭证 -> 管理端确认 -> 解锁联系方式

1. 教师进入支付页查看收款码并完成转账。
2. 教师在支付页点击“我已支付”提交凭证（图片 URL 或文字备注）。
3. 管理端审核并将订单置为 `PAID`。
4. 服务端向房间投递系统消息 `CONTACT_UNLOCKED`，教师端出现“查看对方联系方式”按钮。
5. 教师点击按钮调用“联系方式查询”接口获取并展示对方联系方式（不通过普通文本消息传播）。

## Data Model

### BrokerageOrder

- `id`: 主键
- `proposalId`: 合作提案 id（唯一约束：同一提案仅一笔有效订单）
- `roomId`: 房间 id
- `payerUid`: 付款人 uid（教师）
- `amountFen`: 中介费金额（分）
- `payMethod`: `WECHAT | ALIPAY`
- `status`: `PENDING | PROOF_SUBMITTED | PAID | REJECTED | CANCELED`
- `proofUrl`: 支付凭证（可选）
- `proofNote`: 支付备注（可选）
- `createdAt/updatedAt/paidAt`

## APIs

### Brokerage Order

- `POST /brokerage/orders/by-proposal/{proposalId}`
  - 幂等创建或获取订单（当合作提案为 `ACCEPTED` 时允许）。
  - 返回订单详情。

- `GET /brokerage/orders/{orderId}`
  - 返回订单详情（仅付款人/管理员可见）。

- `POST /brokerage/orders/{orderId}/submit-proof`
  - 付款人提交凭证，状态流转为 `PROOF_SUBMITTED`。

- `POST /admin/brokerage/orders/{orderId}/mark-paid`
  - 管理端确认到账，状态流转为 `PAID`，并触发系统消息投递。

### Contact Unlock

- `GET /chat/rooms/{roomId}/contact?targetUid=xxx`
  - 条件：请求者必须为房间参与者；存在 `ACCEPTED` 合作提案；且该提案对应中介费订单为 `PAID`；请求者为教师。
  - 返回：`phone/wechat/qq` 等字段（按现有用户资料字段选择）。

## IM Structured Messages

复用现有 `SYSTEM` 消息机制，通过 `bizType` 承载新卡片与通知：

- `BROKERAGE_REQUIRED`
  - 字段：`orderId/proposalId/amountFen/status/payerUid`

- `BROKERAGE_STATUS`
  - 字段：`orderId/proposalId/status/actorUid`

- `CONTACT_UNLOCKED`
  - 字段：`proposalId/orderId`

前端通过 `type` 分支渲染卡片，并跳转支付页或拉取联系方式。

## Frontend Integration

- 新增路由：`/pay/brokerage?orderId=...`
- 聊天页：
  - 识别并渲染 `BROKERAGE_REQUIRED / CONTACT_UNLOCKED` 类型消息。
  - 教师端显示“去支付/查看联系方式”按钮；学生端显示等待提示。

## Phaseing

Phase 1：收款码 + 凭证 + 审核确认（本次实现）。

Phase 2：接入真实支付（统一下单/回调验签/对账），替换订单状态流转来源。
