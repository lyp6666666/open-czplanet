## ADDED Requirements

### Requirement: 学生端 MUST 支持找老师闭环
学生 MUST 能浏览教师、筛选教师、查看教师详情、收藏教师并发起沟通申请。

#### Scenario: 浏览教师列表
- **WHEN** 学生或游客进入找老师页面
- **THEN** 小程序 MUST 调用 `/api/v1/parent/tutors/page`
- **AND** MUST 展示教师姓名、头像、城市、学校、科目、课时费、教龄和简介
- **AND** MUST 支持关键词、科目、城市、价格和授课方式筛选

#### Scenario: 查看教师详情
- **WHEN** 用户点击教师卡片
- **THEN** 小程序 MUST 调用真实教师详情接口
- **AND** MUST 展示教师资料、认证摘要、可授课方式、价格、简介、历史课程或评价摘要

#### Scenario: 发起教师申请
- **WHEN** 已登录学生在教师详情点击发起沟通
- **THEN** 小程序 MUST 要求选择授课形式 `ONLINE` 或 `OFFLINE`
- **AND** MUST 提交申请内容到真实申请接口
- **AND** 成功后 MUST 进入申请详情页

### Requirement: 学生端 MUST 支持发布和管理需求
学生 MUST 能发布线上或线下需求，并管理自己发布的需求。

#### Scenario: 发布线上需求
- **WHEN** 学生提交授课形式为线上的需求
- **THEN** 小程序 MUST 校验科目、年级、预算、频次、可上课时间和需求描述
- **AND** MUST 调用 `/api/v1/parent/jobs`
- **AND** 成功后 MUST 进入我的需求或需求详情

#### Scenario: 发布线下需求
- **WHEN** 学生提交授课形式为线下的需求
- **THEN** 小程序 MUST 额外校验城市和地址
- **AND** MUST 调用 `/api/v1/parent/jobs`
- **AND** 后续页面 MUST 明确线下为信息撮合和线下履约模式

#### Scenario: 查看我的需求
- **WHEN** 学生进入我的需求
- **THEN** 小程序 MUST 调用 `/api/v1/parent/jobs/mine`
- **AND** MUST 展示需求状态、授课形式、预算、频次、申请数和更新时间
- **AND** MUST 支持分页、下拉刷新、编辑和关闭/重开

#### Scenario: 查看需求收到的申请
- **WHEN** 学生从某个需求点击收到的申请
- **THEN** 小程序 MUST 进入申请中心
- **AND** MUST 携带 `contextType` 和 `contextId` 过滤申请列表

### Requirement: 学生端 MUST 支持申请处理
学生作为申请接收方时 MUST 能通过或拒绝教师申请，并看到后续支付与聊天状态。

#### Scenario: 学生通过教师申请
- **WHEN** 学生在申请详情点击通过
- **THEN** 小程序 MUST 调用 `/chat/application/{applicationId}/decision`
- **AND** MUST 展示等待教师支付信息费
- **AND** MUST NOT 直接开放聊天输入

#### Scenario: 学生拒绝教师申请
- **WHEN** 学生拒绝申请
- **THEN** 小程序 MUST 调用真实申请决策接口
- **AND** MUST 展示申请已结束
- **AND** MUST 不再展示支付或合作入口

### Requirement: 学生端 MUST 支持合作履约待办
学生 MUST 能从我的页、消息页和我的合作进入待处理课程事项。

#### Scenario: 学生存在待确认试课
- **WHEN** 学生打开我的合作或消息待办
- **THEN** 小程序 MUST 展示待确认试课结果
- **AND** MUST 提供确认合适或不合适入口

#### Scenario: 学生存在待提交正式课表
- **WHEN** 试课通过后课程进入待正式课表状态
- **THEN** 小程序 MUST 提醒学生提交正式周课表
- **AND** MUST 调用 `/api/v1/schedule/courses/{courseId}/weekly-schedule`
