### Requirement: 小程序 MUST 提供稳定的应用壳和运行配置
小程序启动时 MUST 初始化 API 地址、鉴权状态、用户上下文、角色和公共配置，并且生产构建 MUST NOT 默认指向本地地址或启用 Mock 支付/登录。

#### Scenario: 有有效 token 启动
- **WHEN** 用户打开小程序且本地存在有效 token
- **THEN** 小程序 MUST 调用 `/user/me` 刷新用户资料
- **AND** MUST 恢复上次角色或按用户资料推导默认角色
- **AND** MUST 刷新未读申请数和未读消息数

#### Scenario: token 失效启动
- **WHEN** `/user/me` 返回 401 或业务未登录错误
- **THEN** 小程序 MUST 清理 token、用户资料和角色缓存
- **AND** MUST 保持游客可浏览状态
- **AND** MUST 在用户触发受保护动作时引导登录

#### Scenario: 生产包配置检查
- **WHEN** 构建微信小程序生产包
- **THEN** API Base URL MUST NOT 为 `localhost` 或 `127.0.0.1`
- **AND** Mock 登录和 Mock 支付入口 MUST 被禁用

### Requirement: 小程序 MUST 统一登录和角色初始化
手机号登录、微信登录和开发 Mock 登录成功后 MUST 进入同一个用户上下文初始化流程。

#### Scenario: 手机号验证码登录成功
- **WHEN** 用户通过 `/user/loginOrRegister` 登录成功
- **THEN** 小程序 MUST 保存 JWT
- **AND** MUST 调用 `/user/me` 获取完整资料
- **AND** MUST 根据资料初始化学生/教师角色和教师状态

#### Scenario: 微信登录成功
- **WHEN** 用户通过 `uni.login` 获取 code 并调用 `/user/wechatLogin` 成功
- **THEN** 小程序 MUST 执行与手机号登录相同的上下文初始化
- **AND** MUST 支持登录前页面或动作回跳

### Requirement: 小程序 MUST 提供角色准入守卫
学生写操作、教师专属页面、支付页、聊天室和课程页 MUST 使用统一守卫判断登录、角色、认证状态和业务权限。

#### Scenario: 未登录触发受保护动作
- **WHEN** 游客点击发布需求、发起申请、收藏、聊天或支付
- **THEN** 小程序 MUST 跳转登录入口
- **AND** 登录成功后 MUST 回到原页面或继续原动作

#### Scenario: 未完成入驻切换教师版
- **WHEN** 用户切换到教师版但未完成教师资料
- **THEN** 小程序 MUST 引导进入教师入驻
- **AND** MUST NOT 允许进入教师需求广场或发起教师申请
