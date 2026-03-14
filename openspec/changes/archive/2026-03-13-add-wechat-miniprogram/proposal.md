# 提案：增加微信小程序端

## 为什么（Why）
为了扩大用户群并提高可访问性，我们需要推出 AI 导师平台的微信小程序版本。微信是中国占主导地位的超级应用，原生的小程序版本可以实现无缝的社交分享、更便捷的登录（微信授权）以及原生的支付集成，从而显著降低学生和导师的使用门槛。

## 变更内容（What Changes）
- **新前端项目**：使用 Uni-app (Vue 3 + TypeScript) 创建 `ai-tutor-miniprogram`，在针对微信小程序的同时启用跨平台能力。
- **后端认证**：在 `user-service` / `tutor-appointment-service` 中实现微信登录（code2Session）。
- **UI/UX 适配**：将现有的 `ai-tutor-web` 页面（首页、消息、我的）移植并优化到小程序环境，确保符合微信设计规范。
- **支付集成**：将现有的后端微信支付支持连接到新的小程序前端。

## 能力（Capabilities）

### 新增能力
- `wechat-miniprogram-frontend`：核心小程序应用结构、页面和组件。
- `wechat-auth`：微信登录的后端实现（处理 `code`、`openid`、`session_key`）以及绑定到现有用户账户。

### 修改能力
- `user-profile`：更新用户数据模型以存储微信 OpenID 和 UnionID。
- `payment-service`：验证并确保 `WechatPaymentStrategy` 支持小程序支付流程（JSAPI）。

## 影响（Impact）
- **新目录**：`/ai-tutor-miniprogram`
- **后端服务**：`tutor-appointment-service`，`payment-service`
- **数据库**：`user` 表（增加 `wechat_openid` 列）
