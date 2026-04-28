### Requirement: 教师端 MUST 支持入驻与认证状态治理
用户切换教师版前 MUST 完成教师资料和认证状态判断，小程序 MUST 对未提交、待审核、通过、拒绝给出明确下一步。

#### Scenario: 新用户申请成为教师
- **WHEN** 用户从我的页点击切换教师版且没有教师资料
- **THEN** 小程序 MUST 进入教师入驻向导
- **AND** MUST 采集基础资料、教学信息和认证资料
- **AND** MUST 支持图片上传和失败重试

#### Scenario: 教师资料待审核
- **WHEN** 用户教师资料已提交但仍待审核
- **THEN** 小程序 MUST 展示审核中状态
- **AND** MUST NOT 允许进入教师需求广场

#### Scenario: 教师审核被拒绝
- **WHEN** 用户教师认证被拒绝
- **THEN** 小程序 MUST 展示拒绝原因
- **AND** MUST 允许用户重新编辑并提交

### Requirement: 教师端 MUST 支持浏览需求和申请沟通
审核通过教师 MUST 能浏览学生需求、查看详情、收藏需求并发起申请。

#### Scenario: 浏览需求广场
- **WHEN** 教师进入教师首页
- **THEN** 小程序 MUST 调用 `/api/v1/parent/jobs/feed`
- **AND** MUST 支持关键词、授课形式、城市、科目和预算筛选

#### Scenario: 教师查看需求详情
- **WHEN** 教师点击需求
- **THEN** 小程序 MUST 调用 `/api/v1/parent/jobs/{id}/view`
- **AND** MUST 展示发布者摘要、需求字段、预算、时间和授课形式

#### Scenario: 教师发起申请
- **WHEN** 审核通过教师在需求详情点击申请沟通
- **THEN** 小程序 MUST 校验教师状态
- **AND** MUST 提交申请内容
- **AND** 成功后 MUST 进入发出的申请详情
