# YunGouOS 支付接入说明（微信/支付宝扫码 + 闭环）

## 1. 接入范围

- 支付方式：微信扫码、支付宝扫码
- 支付闭环：统一下单出码 → 前端收银台展示与轮询 → YunGouOS 异步通知 → 验签/校验/幂等入库 → 发布支付成功事件 → 业务域解锁
- 管理端：付款记录列表/详情查询（用于对账与排障）

## 2. 关键接口

### 2.1 统一下单（收银台出码）

- `POST /payment/prepay`
- 入参：
  - `contextType`：当前实现为 `BROKERAGE_ORDER`
  - `contextId`：业务订单ID（中介费订单ID）
  - `channel`：`WECHAT` / `ALIPAY`
- 返回：`orderNo`、`amountFen`、`expireTime`、`qrCodeUrl`（优先）/`codeUrl`

### 2.2 查单（收银台轮询）

- `GET /payment/orders/{orderNo}`
- 返回：`status`（`PENDING/SUCCESS/FAILED/CLOSED`）、`amountFen`、`expireTime`、`successTime`

### 2.3 YunGouOS 异步回调

- `POST /payment/notify/yungouos`
- 说明：
  - 回调由第三方平台发起，不携带用户登录态
  - 系统会进行验签 + 金额校验 + 幂等更新订单 + 发布支付成功事件
  - 回调处理成功后返回 `SUCCESS`，避免第三方重复回调

## 3. 配置项（payment-service）

统一使用 `PaymentProperties`（`payment.*`）配置，建议通过配置中心或环境变量按环境注入：

- `payment.enabled`：是否启用支付能力（默认 `true`）

YunGouOS 聚合支付配置（核心）：

- `payment.yungouos.appKey`：YunGouOS 商户密钥（用于签名/验签，禁止硬编码与落库）
- `payment.yungouos.baseUrl`：YunGouOS API 基址（默认 `https://api.pay.yungouos.com`）
- `payment.yungouos.wechatMchId`：微信渠道商户号（`mch_id`）
- `payment.yungouos.alipayMchId`：支付宝渠道商户号（`mch_id`）
- `payment.yungouos.notifyUrl`：回调地址（公网可达）
- `payment.yungouos.returnUrl`：同步跳转地址（可选）
- `payment.yungouos.nativePayType`：扫码返回类型
  - `1`：返回支付链接（前端自行生成二维码）
  - `2`：返回二维码图片地址（前端直接展示图片，收银台默认采用）

## 4. 数据库设计（payment_order）

### 4.1 主键与唯一约束

- `order_no`：商户订单号（系统支付单号，唯一）

### 4.2 业务关联（闭环关键）

- `context_type/context_id`：业务上下文关联（本次为 `BROKERAGE_ORDER + brokerageOrderId`）

### 4.3 第三方与审计字段（排障/对账）

- `provider`：支付提供方（当前固定 `YUNGOUOS`）
- `provider_order_no`：第三方系统单号（如 YunGouOS `orderNo`）
- `transaction_id`：第三方交易号（如 `pay_no`）
- `pay_data`：支付要素（JSON：二维码图片地址/支付链接等）
- `expire_time`：支付要素过期时间
- `success_time`：支付成功时间
- `notify_count`：回调接收次数（便于判断是否存在重复回调）
- `last_notify_time`：最后一次回调接收时间
- `notify_verified`：回调验签是否通过（0/1）

迁移脚本：
- `sqlDoc/migrations/20260309_payment_order_extend_yungouos.sql`

## 5. 前端收银台（ai-tutor-web）

- 页面路由：`/pay/cashier?contextType=BROKERAGE_ORDER&contextId=<orderId>`
- 特性：
  - 新开浏览器标签页
  - 微信/支付宝切换出码
  - 倒计时与过期处理
  - 轮询查单，成功后通过 `postMessage` 通知业务页刷新

## 6. 管理端付款记录（ai-tutor-admin / ai-tutor-admin-web）

后端接口：
- `GET /api/admin/payment/orders`：分页列表（支持筛选）
- `GET /api/admin/payment/orders/{orderNo}`：详情

前端页面：
- `ai-tutor-admin-web` 新增“付款记录”菜单与列表/详情页

## 7. 测试策略

- 单元测试：
  - 回调验签与金额校验分支
  - 支付成功状态机幂等（重复回调不重复发布事件）
- 消费端测试：
  - `PaymentConsumer` 收到支付成功事件后调用业务服务落库并触发解锁

