# 提案：微信小程序业务完善与 Mock 集成

## 为什么 (Why)
当前微信小程序端的登录和支付功能依赖真实的 AppID 和商户号，导致在没有正式账号的情况下无法进行端到端的业务自测。此外，目前的预约流程缺乏与订单的关联，无法形成完整的闭环。为了支持开发环境自测并完善业务逻辑，我们需要引入 Mock 机制并优化预约-支付流程。

## 变更内容 (What Changes)
- **Mock 登录**：在 `WechatAuthService` 中增加 Mock 逻辑，允许使用特定的 Code 跳过微信验证直接登录。
- **Mock 支付**：在 `PaymentAppService` 和前端支付逻辑中增加 Mock 支持，允许在无真实商户号的情况下模拟支付成功。
- **预约流程完善**：优化“导师详情页”，支持选择预约时间，并在创建订单时关联预约信息，形成完整的业务闭环。

## 能力 (Capabilities)

### 新增能力 (New Capabilities)
- `mock-infrastructure`: 提供 Mock 环境的基础设施支持（Mock 配置、Mock 策略）。

### 修改能力 (Modified Capabilities)
- `wechat-auth`: 修改微信认证逻辑以支持 Mock 登录。
- `payment-service`: 修改支付服务以支持 Mock 支付流程。
- `tutor-booking`: 完善导师预约逻辑，支持预约与订单的关联。

## 影响 (Impact)
- **后端服务**：`tutor-appointment-service`, `payment-service`, `videoCall-IM-service`
- **前端项目**：`ai-tutor-miniprogram`
- **配置**：新增 Mock 相关的配置项（如 `wechat.mock.enabled`）。
