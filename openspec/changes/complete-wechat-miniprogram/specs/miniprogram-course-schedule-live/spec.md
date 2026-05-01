## ADDED Requirements

### Requirement: 小程序 MUST 支持我的合作列表
学生和教师 MUST 能查看自己的长期合作，并按阶段理解下一步。

#### Scenario: 查看我的合作
- **WHEN** 用户进入我的合作
- **THEN** 小程序 MUST 调用 `/courses/my`
- **AND** MUST 展示对方姓名头像、授课方式、课程状态、课时费、频次、下一步和退款/AI 摘要

#### Scenario: 合作处于待支付
- **WHEN** 合作状态为 `WAIT_PAY`
- **THEN** 教师侧 MUST 展示去支付或查看支付进度
- **AND** 学生侧 MUST 展示等待教师支付

### Requirement: 小程序 MUST 支持合作详情和课节列表
用户 MUST 能从合作详情查看合作下所有课节，并对单节课执行相应操作。

#### Scenario: 查看合作详情
- **WHEN** 用户进入某个合作详情
- **THEN** 小程序 MUST 调用 `/courses/{courseId}`
- **AND** MUST 调用 `/api/v1/schedule/courses/{courseId}/events`
- **AND** MUST 按时间展示该合作下全部课节

#### Scenario: 新增课节
- **WHEN** 当前合作允许新增课节
- **THEN** 小程序 MUST 展示课节名称、时间、价格和备注表单
- **AND** MUST 调用 `/api/v1/schedule/events`

#### Scenario: 响应课节
- **WHEN** 用户收到待确认课节
- **THEN** 小程序 MUST 提供确认和拒绝
- **AND** MUST 调用 `/api/v1/schedule/events/{eventId}/response`

#### Scenario: 调课和确认改期
- **WHEN** 用户对已确认课节发起调课
- **THEN** 小程序 MUST 调用 `/appointment/{id}/reschedule`
- **AND** 对方 MUST 能调用 `/appointment/{id}/confirmReschedule` 确认改期

### Requirement: 小程序 MUST 支持单节课详情
单节课详情 MUST 展示课节状态、支付状态、课堂状态和可执行动作。

#### Scenario: 单节课即将开始
- **WHEN** 课节状态允许进入课堂
- **THEN** 小程序 MUST 展示进入课堂准备页入口

#### Scenario: 单节课已完成
- **WHEN** 课节完成且存在 AI 总结
- **THEN** 小程序 MUST 展示查看课后总结入口

#### Scenario: 单节课异常
- **WHEN** 课节取消、拒绝、超时或课堂异常
- **THEN** 小程序 MUST 展示异常原因和可恢复动作

### Requirement: 小程序 MUST 支持全局课程表
学生和教师 MUST 能通过课程表查看所有课节安排。

#### Scenario: 查看课程表
- **WHEN** 用户进入课程表页面
- **THEN** 小程序 MUST 调用 `/api/v1/schedule/events`
- **AND** MUST 支持今天、本周和本月视图
- **AND** MUST 展示待确认、已确认、进行中、已完成和异常课节

#### Scenario: 点击课程表课节
- **WHEN** 用户点击课程表中的课节
- **THEN** 小程序 MUST 打开单节课详情
- **AND** 如果可上课 MUST 提供进入课堂入口

### Requirement: 小程序 MUST 支持线上课堂入口
线上课节 MUST 能从小程序进入课堂准备页，并根据当前能力进入原生课堂或 H5 课堂。

#### Scenario: 打开课堂准备页
- **WHEN** 用户点击进入线上课堂
- **THEN** 小程序 MUST 调用 `/live/sessions/by-course/{courseId}/prepare`
- **AND** MUST 展示是否可加入、阻塞原因、设备权限和对方信息

#### Scenario: 小程序使用 H5 课堂
- **WHEN** 小程序暂不原生承接 LiveKit 课堂
- **THEN** 小程序 MUST 跳转 H5 课堂桥接页
- **AND** MUST 携带安全上下文或通过后端换取 join token
- **AND** 用户返回后 MUST 刷新课程和课节状态

### Requirement: 小程序 MUST 支持课后 AI 总结
课程结束后，用户 MUST 能在小程序查看 AI 总结状态和内容。

#### Scenario: 查看课后总结
- **WHEN** 用户从聊天卡片、合作详情或单节课详情点击课后总结
- **THEN** 小程序 MUST 调用真实 AI 总结接口
- **AND** MUST 展示生成中、成功、失败和无权限状态
