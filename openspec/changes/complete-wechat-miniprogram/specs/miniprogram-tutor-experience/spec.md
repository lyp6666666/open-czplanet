## ADDED Requirements

### Requirement: 教师端 MUST 支持入驻和认证治理
用户切换教师身份前 MUST 完成入驻资料和认证状态判断。

#### Scenario: 新用户成为教师
- **WHEN** 用户从我的页点击成为教师
- **THEN** 小程序 MUST 进入教师入驻向导
- **AND** MUST 采集基础资料、教学资料和认证资料
- **AND** MUST 支持图片上传、失败重试和删除

#### Scenario: 教师资料待审核
- **WHEN** 教师资料提交后仍处于待审核
- **THEN** 小程序 MUST 展示审核中状态
- **AND** MUST NOT 允许进入需求广场、收藏需求或发起申请

#### Scenario: 教师审核被拒绝
- **WHEN** 后端返回教师审核拒绝状态
- **THEN** 小程序 MUST 展示拒绝原因
- **AND** MUST 允许重新编辑并提交

### Requirement: 教师端 MUST 支持需求广场
已认证教师 MUST 能浏览学生需求、筛选需求、查看需求详情和收藏需求。

#### Scenario: 浏览需求广场
- **WHEN** 已认证教师进入需求广场
- **THEN** 小程序 MUST 调用 `/api/v1/parent/jobs/feed`
- **AND** MUST 支持关键词、授课形式、科目、城市和预算筛选
- **AND** MUST 展示需求标题、预算、频次、授课形式、发布者类型和描述摘要

#### Scenario: 查看教师视角需求详情
- **WHEN** 教师点击需求
- **THEN** 小程序 MUST 调用教师视角需求详情接口
- **AND** MUST 展示需求字段、发布者摘要、预算、时间、地点和对教师要求

#### Scenario: 收藏需求
- **WHEN** 教师在需求详情点击收藏
- **THEN** 小程序 MUST 调用真实收藏接口
- **AND** MUST 回显已收藏状态

### Requirement: 教师端 MUST 支持发起沟通申请
已认证教师 MUST 能向学生需求发起沟通申请，并跟踪申请状态。

#### Scenario: 教师发起申请
- **WHEN** 教师在需求详情点击申请沟通
- **THEN** 小程序 MUST 校验教师认证状态
- **AND** MUST 展示申请内容表单
- **AND** MUST 调用真实申请创建接口
- **AND** 成功后 MUST 进入发出的申请详情

#### Scenario: 教师申请已通过
- **WHEN** 学生通过教师申请
- **THEN** 教师申请详情 MUST 展示去支付信息费入口
- **AND** 支付前聊天室 MUST 禁止普通聊天和合作提案

### Requirement: 教师端 MUST 支持履约工作台
教师 MUST 能查看自己的合作、课节、课程表和履约待办。

#### Scenario: 教师查看我的合作
- **WHEN** 教师进入我的合作
- **THEN** 小程序 MUST 调用 `/courses/my` 并传教师角色
- **AND** MUST 展示学生信息、课程状态、下一节课、待确认课节和退款状态

#### Scenario: 教师存在待上课程
- **WHEN** 教师打开课程表或我的合作
- **THEN** 小程序 MUST 展示即将上课提醒
- **AND** MUST 提供进入课堂准备页或 H5 课堂入口
