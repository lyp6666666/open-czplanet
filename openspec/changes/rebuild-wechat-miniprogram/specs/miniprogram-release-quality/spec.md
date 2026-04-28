### Requirement: 小程序重建 MUST 有明确质量门禁
任何进入提测或发布的小程序改动 MUST 通过类型检查、微信构建和核心冒烟链路。

#### Scenario: 代码提测
- **WHEN** 小程序端代码准备提测
- **THEN** `npm run type-check` MUST 通过
- **AND** `npm run build:mp-weixin` MUST 通过

#### Scenario: 核心业务冒烟
- **WHEN** 小程序准备发布
- **THEN** 团队 MUST 完成学生发布需求、教师申请、学生通过申请、教师支付、聊天解锁、试课合作、课程生成的冒烟验证
- **AND** MUST 记录失败项和遗留风险

#### Scenario: 开发环境 Mock
- **WHEN** 在开发环境使用 Mock 登录或 Mock 支付
- **THEN** 小程序 MUST 明确展示开发态标识
- **AND** 生产包 MUST NOT 暴露 Mock 操作

#### Scenario: 发布前配置检查
- **WHEN** 小程序提交审核或发布
- **THEN** MUST 检查 AppID、服务域名、隐私协议、支付开关、API Base URL 和包体积
- **AND** MUST 确认关键接口域名已加入微信小程序合法域名
