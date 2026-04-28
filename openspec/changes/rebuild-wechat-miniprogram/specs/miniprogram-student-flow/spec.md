### Requirement: 学生端 MUST 支持找老师和发起申请
学生/家长 MUST 能在小程序中浏览教师、查看详情、收藏教师，并通过申请机制发起沟通。

#### Scenario: 浏览教师列表
- **WHEN** 学生进入首页或教师列表
- **THEN** 小程序 MUST 调用 `/api/v1/parent/tutors/page`
- **AND** MUST 展示教师头像、姓名、城市、科目、价格、简介和认证摘要
- **AND** MUST 支持分页加载和刷新

#### Scenario: 从教师详情发起申请
- **WHEN** 已登录学生在教师详情点击发起沟通申请
- **THEN** 小程序 MUST 展示申请表单
- **AND** MUST 使用教师上下文创建申请
- **AND** 成功后 MUST 跳转申请详情或消息页

#### Scenario: 未登录学生发起申请
- **WHEN** 游客在教师详情点击发起沟通申请
- **THEN** 小程序 MUST 跳转登录
- **AND** 登录成功后 MUST 回到该教师详情并保留发起申请意图

### Requirement: 学生端 MUST 支持发布和管理需求
学生/家长 MUST 能发布需求、查看我的需求、查看需求详情，并进入收到的申请处理流程。

#### Scenario: 发布线上需求
- **WHEN** 学生提交授课形式为线上的需求
- **THEN** 小程序 MUST 校验科目、年级、预算、上课时间、线上授课字段和描述
- **AND** MUST 调用 `/api/v1/parent/jobs`
- **AND** 成功后 MUST 进入我的需求或需求详情

#### Scenario: 发布线下需求
- **WHEN** 学生提交授课形式为线下的需求
- **THEN** 小程序 MUST 校验城市/地址等线下必要字段
- **AND** MUST 在后续流程中按线下信息费与联系方式解锁模式展示

#### Scenario: 查看我的需求
- **WHEN** 学生进入我的需求
- **THEN** 小程序 MUST 调用 `/api/v1/parent/jobs/mine`
- **AND** MUST 支持状态筛选、分页、空状态和失败重试
