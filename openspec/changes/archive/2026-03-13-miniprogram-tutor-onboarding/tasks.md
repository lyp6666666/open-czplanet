## 1. 基础准备

- [ ] 1.1 封装 `tutor-application` 相关 API 请求 (`getApplyStatus`, `apply`)
- [ ] 1.2 修改 `userStore`，增加 `tutorStatus` 状态和检查逻辑

## 2. 教师入驻页面开发

- [ ] 2.1 创建 `pages/tutor/onboarding/index` 页面结构
- [ ] 2.2 实现步骤一：基本信息表单（头像上传、姓名、简介）
- [ ] 2.3 实现步骤二：教学偏好表单（科目、年级、价格）
- [ ] 2.4 实现步骤三：教育背景表单（学校、学历、证书上传）
- [ ] 2.5 实现表单校验与提交逻辑

## 3. 状态管理与流程串联

- [ ] 3.1 实现“审核中”状态页面 `pages/tutor/status/pending`
- [ ] 3.2 实现“审核拒绝”状态页面 `pages/tutor/status/rejected`
- [ ] 3.3 修改 `userStore.switchRole` 逻辑，集成状态跳转
- [ ] 3.4 联调测试：从申请到审核通过的全流程
