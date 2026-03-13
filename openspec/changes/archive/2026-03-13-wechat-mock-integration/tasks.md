## 1. 登录 Mock 支持

- [x] 1.1 修改 `WechatProperties` 增加 `mock.enabled` 配置项
- [x] 1.2 在 `WechatAuthServiceImpl` 中增加 Mock 判断逻辑，支持使用 `mock_code` 登录
- [x] 1.3 前端 `user.ts` 适配，允许在开发环境使用 Mock Code 登录

## 2. 支付 Mock 支持

- [x] 2.1 修改 `WechatPaymentStrategy`，增加 Mock 判断逻辑，生成 Mock 支付参数
- [x] 2.2 前端 `detail.vue` 适配，识别 Mock 支付参数并模拟支付成功回调

## 3. 预约流程完善

- [x] 3.1 前端 `detail.vue` 增加预约时间选择和备注输入弹窗
- [x] 3.2 调用 `TutorAppointmentService` 创建预约
- [x] 3.3 修改 `BrokerageOrderService.createDirectOrder` 支持传入 `appointmentId`
- [x] 3.4 前端联调：创建预约 -> 创建订单 -> 支付
