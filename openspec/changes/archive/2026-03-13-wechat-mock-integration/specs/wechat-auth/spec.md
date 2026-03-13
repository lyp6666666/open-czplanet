## 新增需求 (ADDED Requirements)

### 需求：Mock 登录支持
系统应支持在配置为 Mock 模式时，使用特定的 Code 进行登录，跳过真实的微信验证。

#### 场景：使用 Mock Code 登录
- **当** 系统配置为 Mock 模式（`appid=wx_mock_appid`）时
- **且** 用户使用特定的 Code（如 `mock_login_code`）请求登录
- **那么** 系统应直接返回预设的 OpenID（如 `mock_openid_123456`）和模拟的 Token
- **且** 不应调用真实的微信接口

#### 场景：正常登录流程保持不变
- **当** 系统配置为真实 AppID 时
- **那么** 系统应继续调用微信接口进行验证
