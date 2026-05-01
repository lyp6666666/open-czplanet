# 微信小程序页面与真实接口映射表

本文档用于落实 OpenSpec `7.1` 和 `7.2`：

- 输出小程序页面到接口映射表
- 为每个页面记录加载接口、提交接口、轮询接口、跳转依赖

说明：

- 仅记录当前小程序端已经接入或已预留的真实接口
- 不包含假数据接口
- `页面状态`、`下一步动作`、`跳转依赖` 以当前实现为准

## 1. 公共与账号

### `/pages/home/index`

- 页面职责：
  - 根据当前角色展示学生首页或教师首页
  - 登录后恢复待回跳动作
- 加载接口：
  - 无页面级接口；数据由子组件分别加载
- 提交接口：
  - 无
- 轮询接口：
  - 无
- 跳转依赖：
  - `resumePendingRedirect()`

### `/pages/me/index`

- 页面职责：
  - 登录、微信登录、角色切换、资料编辑、快捷入口
- 加载接口：
  - `/user/me`
  - `/user/email/summary`
- 提交接口：
  - `/user/loginOrRegister`
  - `/user/wechatLogin`
  - `/user/updateUserInfo`
  - `/user/email`
  - `/user/email/code`
  - `/user/email/verify`
- 轮询接口：
  - 无
- 跳转依赖：
  - 登录后 `resumePendingRedirect()`
  - 我的需求、我的合作、课程表、收藏、邮箱设置

### `/pages/account/email`

- 页面职责：
  - 邮箱绑定与验证
- 加载接口：
  - `/user/email/summary`
- 提交接口：
  - `/user/email`
  - `/user/email/code`
  - `/user/email/verify`
- 轮询接口：
  - 无
- 跳转依赖：
  - `/pages/me/index`

## 2. 学生端

### `/pages/tutor/detail`

- 页面职责：
  - 查看老师信息、收藏、发起申请
- 加载接口：
  - `/user/card?uid={id}`
  - `/api/v1/parent/favorites/tutors/check`
- 提交接口：
  - `/api/v1/parent/favorites/tutors/{tutorId}`
  - `/chat/application/start-chat`
- 轮询接口：
  - 无
- 跳转依赖：
  - 登录/切角色后支持 `__intent=open-tutor-apply`
  - 申请成功跳 `/pages/application/detail`
  - 若直接建房成功则跳 `/pages/chat/room`

### `/pages/post/index`

- 页面职责：
  - 发布或编辑需求
- 加载接口：
  - `/api/v1/parent/jobs/{id}`
- 提交接口：
  - `/api/v1/parent/jobs`
  - `/api/v1/parent/jobs/{id}`
- 轮询接口：
  - 无
- 跳转依赖：
  - `/pages/my-jobs/index`

### `/pages/my-jobs/index`

- 页面职责：
  - 我的需求列表、状态管理、进入需求详情
- 加载接口：
  - `/api/v1/parent/jobs/mine`
- 提交接口：
  - `/api/v1/parent/jobs/{id}`
- 轮询接口：
  - 无
- 跳转依赖：
  - `/pages/job/detail`
  - `/pages/post/index`

## 3. 教师端

### `/pages/job/detail`

- 页面职责：
  - 查看需求详情、收藏需求、发起申请、需求方查看收到的申请
- 加载接口：
  - `/api/v1/parent/jobs/{id}/view`
  - `/api/v1/tutor/favorites/demands/check`
- 提交接口：
  - `/api/v1/tutor/favorites/demands/{demandId}`
  - `/chat/application`
  - `/api/v1/parent/jobs/{id}`（需求拥有者关闭/重开）
- 轮询接口：
  - 无
- 跳转依赖：
  - 登录/切角色后支持 `__intent=open-demand-apply`
  - 申请成功跳 `/pages/application/detail`
  - 需求拥有者可跳 `/pages/application/list?tab=received&contextType=DEMAND&contextId={id}`

### `/pages/tutor/onboarding/index`

- 页面职责：
  - 教师入驻与认证资料提交
- 加载接口：
  - `/user/me`
- 提交接口：
  - `/user/updateUserInfo`
  - `/api/v1/assets/upload`
  - `/teacher/verification/education/submit`
  - `/teacher/verification/realname/submit`
- 轮询接口：
  - 无
- 跳转依赖：
  - `/pages/tutor/status`

### `/pages/tutor/status`

- 页面职责：
  - 展示教师审核状态
- 加载接口：
  - `/user/me`
- 提交接口：
  - 无
- 轮询接口：
  - 无
- 跳转依赖：
  - `/pages/tutor/onboarding/index`
  - `/pages/home/index`

## 4. 申请、支付、聊天

### `/pages/application/list`

- 页面职责：
  - 收到/发出申请列表、上下文筛选、状态筛选
- 加载接口：
  - `/chat/application/received/page`
  - `/chat/application/sent/page`
  - `/chat/application/unread`
- 提交接口：
  - 无
- 轮询接口：
  - 无
- 跳转依赖：
  - `/pages/application/detail`

### `/pages/application/detail`

- 页面职责：
  - 查看申请详情、通过/拒绝、进入支付、进入聊天、进入合作、拒绝后重走流程
- 加载接口：
  - `/chat/application/{applicationId}`
- 提交接口：
  - `/chat/application/{applicationId}/decision`
  - `/chat/application/{applicationId}/enter-chat`
  - `/courses/by-room/{roomId}`（进入合作前查询）
- 轮询接口：
  - 无
- 跳转依赖：
  - `/pages/pay/cashier`
  - `/pages/chat/room`
  - `/pages/course/detail`
  - `/pages/job/detail`
  - `/pages/tutor/detail`

### `/pages/pay/cashier`

- 页面职责：
  - 信息费支付、订单轮询、支付后进入聊天
- 加载接口：
  - `/payment/prepay`
  - `/payment/orders/{orderNo}`
- 提交接口：
  - `/payment/prepay`
  - `/payment/dev/orders/{orderNo}/mock-success`
  - `/chat/application/{applicationId}/enter-chat`
- 轮询接口：
  - `/payment/orders/{orderNo}`
- 跳转依赖：
  - `/pages/chat/room`
  - `/pages/application/detail`

### `/pages/chat/list`

- 页面职责：
  - 会话列表、申请未读、业务待办
- 加载接口：
  - `/chat/room/page`
  - `/chat/application/unread`
  - `/chat/application/received/page`
  - `/chat/application/sent/page`
- 提交接口：
  - 无
- 轮询接口：
  - `/chat/room/page`
  - `/chat/application/unread`
  - `/chat/application/received/page`
  - `/chat/application/sent/page`
- 跳转依赖：
  - `/pages/chat/room`
  - `/pages/application/list`

### `/pages/chat/room`

- 页面职责：
  - 聊天、图片发送、申请处理、支付信息费、合作提案、查看合作
- 加载接口：
  - `/chat/public/msg/page`
  - `/chat/events/sync`
  - `/courses/by-room/{roomId}`
- 提交接口：
  - `/chat/msg`
  - `/api/v1/assets/upload`
  - `/chat/application/{applicationId}/decision-message`
  - `/payment/prepay`
  - `/payment/orders/{orderNo}`
  - `/chat/collaboration/proposal`
  - `/chat/collaboration/proposal/{proposalId}/response`
  - `/chat/read/ack`
  - `/chat/delivery/ack`
  - `/chat/typing`
- 轮询接口：
  - `/chat/public/msg/page`
  - `/chat/events/sync`
  - `/payment/orders/{orderNo}`
- 跳转依赖：
  - `/pages/course/detail`

## 5. 合作、课节、课程表、课堂

### `/pages/course/list`

- 页面职责：
  - 我的合作列表
- 加载接口：
  - `/courses/my`
  - `/user/batch`
- 提交接口：
  - 无
- 轮询接口：
  - 无
- 跳转依赖：
  - `/pages/course/detail`

### `/pages/course/detail`

- 页面职责：
  - 合作详情、课节列表、试课结果、正式周课表、退费、新增课节、进入聊天、AI 摘要
- 加载接口：
  - `/courses/{courseId}`
  - `/api/v1/schedule/courses/{courseId}/events`
  - `/api/v1/schedule/availability/day`
- 提交接口：
  - `/courses/{courseId}/trial-result`
  - `/api/v1/schedule/courses/{courseId}/weekly-schedule`
  - `/courses/{courseId}/trial-refund/apply`
  - `/api/v1/assets/upload`
  - `/api/v1/schedule/events`
- 轮询接口：
  - 无
- 跳转依赖：
  - `/pages/chat/room`
  - `/pages/course/lesson-detail`
  - `/pages/course/ai-summary`

### `/pages/course/lesson-detail`

- 页面职责：
  - 单节课详情、确认/拒绝、调课、确认改期、取消、结课、进入课堂、查看总结
- 加载接口：
  - `/courses/{courseId}`
  - `/api/v1/schedule/courses/{courseId}/events`
  - `/live/sessions/by-course/{courseId}`
- 提交接口：
  - `/api/v1/schedule/events/{eventId}/response`
  - `/api/v1/schedule/events/{eventId}/cancel`
  - `/appointment/{id}/reschedule`
  - `/appointment/{id}/confirmReschedule`
  - `/appointment/{id}/complete`
- 轮询接口：
  - 无
- 跳转依赖：
  - `/pages/live/prepare`
  - `/pages/course/ai-summary`

### `/pages/schedule/index`

- 页面职责：
  - 课程表，支持日/周/月视图、点击课节详情
- 加载接口：
  - `/api/v1/schedule/events`
- 提交接口：
  - 无
- 轮询接口：
  - 无
- 跳转依赖：
  - `/pages/course/lesson-detail`

### `/pages/live/prepare`

- 页面职责：
  - 课堂准备、自检、AI 选项、join token、支付阻塞处理
- 加载接口：
  - `/live/sessions/by-course/{courseId}/prepare`
- 提交接口：
  - `/live/sessions/{sessionId}/device-report`
  - `/live/sessions/{sessionId}/ai/options`
  - `/live/sessions/{sessionId}/join-token`
- 轮询接口：
  - 页面回到前台后重新请求 `/prepare`
- 跳转依赖：
  - `/pages/live/launch`
  - `/pages/pay/cashier`

### `/pages/live/launch`

- 页面职责：
  - 真实课堂承接页，负责跳转 H5 bridge
- 加载接口：
  - 无
- 提交接口：
  - 无
- 轮询接口：
  - 无
- 跳转依赖：
  - `/pages/live/webview`

### `/pages/live/webview`

- 页面职责：
  - H5 课堂容器
- 加载接口：
  - H5 页面自身加载
- 提交接口：
  - 无
- 轮询接口：
  - 无
- 跳转依赖：
  - H5 `/live/mp-bridge`

### `/pages/course/ai-summary`

- 页面职责：
  - AI 课后总结、失败重试
- 加载接口：
  - `/api/v1/schedule/courses/{courseId}/events`
  - `/live/sessions/by-course/{courseId}`
  - `/live/sessions/{sessionId}/ai/result`
- 提交接口：
  - `/live/sessions/{sessionId}/ai/result/retry`
- 轮询接口：
  - 手动刷新
- 跳转依赖：
  - 无

## 6. 已识别的后续缺口

以下项目仍建议继续实现或补齐：

- 页面地图文档与页面分包说明
- 更系统的状态工具与类型抽离
- 更多角色守卫覆盖面审计
- 教师需求广场筛选项补齐
- 真机联调与 E2E 记录
- 支付、聊天、课堂的异常分支验收清单
