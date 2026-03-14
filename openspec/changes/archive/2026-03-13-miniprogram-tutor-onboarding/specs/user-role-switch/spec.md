## 新增需求 (ADDED Requirements)

### 需求：教师入驻状态校验
系统应在用户尝试切换为教师角色时校验其入驻状态。

#### 场景：未入驻用户切换
- **当** 用户尝试切换为教师角色且后端返回 `teacherStatus=NONE` 时
- **那么** 系统应跳转至“教师入驻”页面（`pages/tutor/onboarding`）
- **且** 不直接进入教师首页

#### 场景：已入驻用户切换
- **当** 用户尝试切换为教师角色且后端返回 `teacherStatus=APPROVED` 时
- **那么** 系统应正常进入教师首页（`TutorHome`）
