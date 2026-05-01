## ADDED Requirements

### Requirement: 小程序 MUST 以角色工作台组织页面
小程序 MUST 使用统一 TabBar 承接学生、教师和游客状态，并根据当前角色展示对应工作台。

#### Scenario: 游客打开首页
- **WHEN** 未登录用户打开小程序首页
- **THEN** 小程序 MUST 展示学生视角公开内容
- **AND** MUST 允许浏览教师与公开需求摘要
- **AND** MUST 在触发发布需求、收藏、申请、聊天、支付时引导登录

#### Scenario: 学生打开首页
- **WHEN** 学生登录后进入首页
- **THEN** 小程序 MUST 展示找老师、发布需求、我的需求、申请待办和我的合作入口

#### Scenario: 教师打开首页
- **WHEN** 已认证教师登录后进入首页
- **THEN** 小程序 MUST 展示需求广场、发出的申请、待支付信息费、我的合作和课程表入口

### Requirement: 小程序 MUST 使用分包控制包体积
小程序 MUST 将学生、教师、交易流程、课程履约页面拆分为分包，避免主包过大。

#### Scenario: 构建微信小程序
- **WHEN** 执行微信小程序构建
- **THEN** 主包 MUST 只包含首页、消息、我的和基础组件
- **AND** 学生、教师、申请支付聊天、课程课堂页面 MUST 位于对应分包或可被独立懒加载

### Requirement: 小程序 MUST 使用真实接口作为验收依据
小程序所有正式业务页面 MUST 通过真实后端接口加载与提交数据。

#### Scenario: 页面加载业务数据
- **WHEN** 用户进入任意业务页面
- **THEN** 小程序 MUST 调用真实后端接口获取数据
- **AND** MUST 展示加载中、成功、空状态和失败重试

#### Scenario: 开发态 Mock
- **WHEN** 开发态使用 Mock 数据或 Mock 支付
- **THEN** 小程序 MUST 明确展示开发态标识
- **AND** 生产构建 MUST 禁用 Mock 登录、Mock 支付和静态假数据验收入口

### Requirement: 小程序 MUST 统一登录、角色和权限守卫
登录成功后 MUST 初始化完整用户上下文，学生写操作和教师专属操作 MUST 使用统一守卫。

#### Scenario: 登录成功
- **WHEN** 用户通过手机号或微信登录成功
- **THEN** 小程序 MUST 保存 JWT
- **AND** MUST 调用 `/user/me`
- **AND** MUST 根据后端资料初始化当前角色、教师状态、头像、昵称和联系方式

#### Scenario: 受保护动作回跳
- **WHEN** 未登录用户从详情页触发发起申请
- **THEN** 小程序 MUST 跳转登录
- **AND** 登录成功后 MUST 回到原页面并恢复原动作上下文

#### Scenario: 教师未认证访问教师页面
- **WHEN** 未认证教师访问需求广场或发起申请
- **THEN** 小程序 MUST 跳转教师入驻或教师状态页
- **AND** MUST NOT 调用需要已认证教师权限的提交接口

### Requirement: 小程序 MUST 统一页面状态和错误处理
所有核心页面 MUST 展示明确的加载、空、错误、无权限、未登录和提交中状态。

#### Scenario: 接口返回业务错误
- **WHEN** 后端返回非 0 业务错误或 HTTP 错误
- **THEN** 小程序 MUST 展示后端返回的真实错误文案
- **AND** MUST 保留用户已填写内容
- **AND** 对可恢复错误 MUST 提供重试入口
