### Requirement: 小程序 MUST 支持试课合作提案
聊天解锁后，用户 MUST 能在小程序中发起和处理试课合作提案，提案通过后生成课程入口。

#### Scenario: 发起试课合作
- **WHEN** 用户在已解锁聊天室发起试课合作
- **THEN** 小程序 MUST 查询双方可用时间
- **AND** MUST 展示试课时间选择
- **AND** MUST 调用 `/chat/collaboration/proposal`

#### Scenario: 接收方同意试课合作
- **WHEN** 接收方同意合作提案
- **THEN** 小程序 MUST 调用 `/chat/collaboration/proposal/{proposalId}/response`
- **AND** MUST 刷新聊天系统卡片
- **AND** MUST 在我的课程中展示对应试课课程

### Requirement: 小程序 MUST 支持我的课程主流程
用户 MUST 能查看自己的课程列表、课程详情、试课结果和正式课表动作。

#### Scenario: 查看我的课程
- **WHEN** 用户进入我的课程
- **THEN** 小程序 MUST 调用 `/courses/my`
- **AND** MUST 按课程阶段展示待确认试课、已确认试课、待学生决策、待补正式课表、正式课和已结束

#### Scenario: 学生提交试课通过
- **WHEN** 试课结束后学生选择通过
- **THEN** 小程序 MUST 调用 `/courses/{courseId}/trial-result`
- **AND** MUST 引导学生提交正式课表

#### Scenario: 提交正式课表
- **WHEN** 学生选择每周固定上课时间并提交
- **THEN** 小程序 MUST 调用 `/api/v1/schedule/courses/{courseId}/weekly-schedule`
- **AND** MUST 校验所有固定课节时长一致
- **AND** 成功后课程 MUST 进入正式授课阶段

#### Scenario: 试课不通过
- **WHEN** 学生选择试课不通过
- **THEN** 小程序 MUST 要求填写原因和必要凭证
- **AND** MUST 刷新课程和聊天状态
- **AND** 教师侧 MUST 能看到退款申请入口或退款状态
