## ADDED Requirements

### Requirement: 小程序 MUST 有构建和静态质量门禁
任何提测版本 MUST 通过类型检查和微信小程序构建。

#### Scenario: 提测前检查
- **WHEN** 小程序准备提测
- **THEN** `npm run type-check` MUST 通过
- **AND** `npm run build:mp-weixin` MUST 通过

#### Scenario: 生产构建检查
- **WHEN** 构建生产小程序
- **THEN** API Base URL MUST NOT 指向 `localhost` 或 `127.0.0.1`
- **AND** Mock 登录和 Mock 支付 MUST 被禁用
- **AND** AppID、合法域名、支付开关和隐私协议 MUST 完成检查

### Requirement: 小程序 MUST 通过真实接口主流程冒烟
小程序核心业务验收 MUST 使用真实后端接口和测试账号完成。

#### Scenario: 学生到教师申请闭环
- **WHEN** 使用测试学生发布线上需求
- **THEN** 测试教师 MUST 能在需求广场看到该需求
- **AND** MUST 能发起申请
- **AND** 测试学生 MUST 能通过申请

#### Scenario: 支付到聊天解锁闭环
- **WHEN** 教师申请被学生通过
- **THEN** 教师 MUST 能创建信息费支付单
- **AND** 支付成功后 MUST 能进入聊天室
- **AND** 聊天输入 MUST 可用

#### Scenario: 聊天到课程闭环
- **WHEN** 聊天已解锁
- **THEN** 用户 MUST 能发起合作提案
- **AND** 对方接受后 MUST 生成课程合作
- **AND** 双方 MUST 能在我的合作看到该课程

#### Scenario: 课程到课程表闭环
- **WHEN** 合作生成后创建课节或提交正式课表
- **THEN** 合作详情 MUST 展示课节
- **AND** 全局课程表 MUST 展示同一课节
- **AND** 点击课节 MUST 能进入单节课详情

### Requirement: 小程序 MUST 覆盖关键异常分支
测试 MUST 覆盖支付、聊天、申请、课节、退款和课堂入口的异常分支。

#### Scenario: 支付异常
- **WHEN** 用户取消支付、支付失败或订单超时
- **THEN** 小程序 MUST 保持未解锁状态
- **AND** MUST 提供恢复入口

#### Scenario: 聊天未解锁
- **WHEN** 用户进入未支付聊天室
- **THEN** 小程序 MUST 禁用普通输入
- **AND** MUST 展示等待支付或去支付引导

#### Scenario: 线下试课退款证据缺失
- **WHEN** 用户提交线下试课不通过但缺少必要证据
- **THEN** 小程序 MUST 阻止提交
- **AND** MUST 提示缺少图片、录屏 URL 或录屏时长

#### Scenario: 课堂不可进入
- **WHEN** live prepare 返回未到时间、未支付或课堂已结束
- **THEN** 小程序 MUST 展示真实阻塞原因
- **AND** MUST NOT 进入课堂

### Requirement: 小程序提测 MUST 输出验证记录
每次小程序提测 MUST 附带测试环境、账号、数据 ID 和结果记录。

#### Scenario: 输出测试报告
- **WHEN** 完成一轮小程序冒烟
- **THEN** 报告 MUST 包含测试环境、学生账号、教师账号、需求 ID、申请 ID、订单号、房间 ID、课程 ID、课节 ID
- **AND** MUST 标记每个失败项属于前端、后端、支付环境、微信权限、测试数据或配置问题
