# YunGouOS 支付联调与验收清单

## 1. 环境准备

- 申请/配置 YunGouOS 商户与渠道（微信/支付宝）并获取：
  - `appKey`
  - `wechatMchId` / `alipayMchId`
- 配置回调地址 `notifyUrl`（公网可达，HTTPS），并在 YunGouOS 控制台完成配置
- 确认收银台域名/页面可访问（PC 端扫码展示稳定）

## 2. 配置检查（后端）

确保以下配置按环境注入（建议配置中心/环境变量）：

- `payment.enabled=true`
- `payment.yungouos.appKey=...`
- `payment.yungouos.wechatMchId=...`（启用微信）
- `payment.yungouos.alipayMchId=...`（启用支付宝）
- `payment.yungouos.notifyUrl=https://<domain>/payment/notify/yungouos`
- `payment.yungouos.nativePayType=2`（推荐：直接返回二维码图片地址）
- `payment.eventRetryDelayMs=60000`（可选：事件补偿间隔）

启动时若配置缺失会直接失败（用于防止线上运行期才暴露问题）。

## 3. 数据库迁移

按顺序执行迁移脚本（或合并到统一迁移流程）：

- `sqlDoc/migrations/20260309_payment_order_extend_yungouos.sql`
- `sqlDoc/migrations/20260310_payment_order_event_delivery.sql`

验收点：
- `payment_order` 新增字段与索引存在
- 插入/更新不影响旧业务

## 4. 手工闭环验证（推荐顺序）

### 4.1 下单出码

1. 构造一笔 `brokerage_order`（状态 `PENDING`，payer_uid 为当前登录用户）
2. 业务页点击“去支付”，应新开收银台页 `/pay/cashier?...`
3. 收银台默认微信支付出码，切换支付宝应刷新二维码

验收点：
- `payment_order` 写入：`order_no`、`status=PENDING`、`context_type/context_id`、`channel`、`amount`、`pay_data`、`expire_time`

### 4.2 扫码支付 + 回调闭环

1. 使用微信/支付宝扫码完成支付
2. 观察回调接口 `/payment/notify/yungouos` 有请求到达（可通过日志/网关记录）
3. 订单状态应更新为 `SUCCESS`

验收点（数据库）：
- `payment_order.status=SUCCESS`
- `transaction_id/provider_order_no` 记录第三方流水信息（如回调有返回）
- `notify_verified=1`，`notify_count` 增加，`last_notify_time` 更新
- `success_time` 写入

### 4.3 支付成功事件与业务解锁

1. 支付成功后应发布 `payment-success-topic`
2. 业务服务消费后：
   - `brokerage_order.status=PAID`
   - `brokerage_order.pay_method`（若可写入）更新为 `WECHAT/ALIPAY`
   - 申请/聊天解锁逻辑正常触发

验收点：
- 重复回调/重复事件不造成重复解锁（幂等）
- 若 MQ 暂时不可用，`payment_order.event_sent=0`，恢复后补偿任务可自动补投递

## 5. 管理端验收

### 5.1 后端接口

- `GET /api/admin/payment/orders` 支持分页与筛选
- `GET /api/admin/payment/orders/{orderNo}` 返回详情字段

### 5.2 前端页面（ai-tutor-admin-web）

- 菜单“付款记录”可访问
- 列表支持筛选、分页
- 点击订单号进入详情，能看到：
  - 渠道/状态/金额
  - 第三方单号/交易号
  - 回调次数、验签通过、最后回调时间
  - payData（JSON 格式化展示）

## 6. 回滚与兜底

- 回滚开关：`payment.enabled=false`
  - 下单接口会提示支付禁用
  - 回调接口返回成功（避免第三方持续重试打爆系统）
- 入口兜底：
  - 仍可暂时保留旧的 `/pay/brokerage` 页面作为兼容入口，但默认引导到新收银台页

