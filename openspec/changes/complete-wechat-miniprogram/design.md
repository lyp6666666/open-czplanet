# 设计：微信小程序端彻底实现

## 1. 设计目标

小程序端必须独立承接完整主流程，而不是只做 Web 的入口集合。用户在小程序里应能完成从找人/找需求到合作履约的闭环：

学生主线：

1. 登录或游客浏览
2. 找老师或发布需求
3. 处理教师申请
4. 等待教师支付信息费
5. 聊天确认试课合作
6. 查看我的合作
7. 查看合作下全部课节
8. 管理试课结果、正式课表、课程表、课后总结

教师主线：

1. 登录并完成入驻认证
2. 浏览需求广场
3. 发起沟通申请
4. 等学生通过
5. 支付信息费
6. 聊天确认试课合作
7. 查看我的合作
8. 管理课节、调课、进入课堂、查看收入/退款状态

## 2. 总体页面架构

### 2.1 TabBar

建议保留 3 个主 Tab，降低小程序底部复杂度：

- 首页：根据当前角色展示学生首页或教师首页
- 消息：聚合申请中心、聊天列表、未读和系统待办
- 我的：资料、角色切换、我的需求/合作/课程表/收藏/设置入口

课程表、我的合作、申请中心不放底部 Tab，但必须在“消息”和“我的”中有清晰入口。

### 2.2 分包建议

- 主包：
  - `pages/home/index`
  - `pages/chat/list`
  - `pages/me/index`
  - 登录组件、角色切换、基础状态组件
- `student` 分包：
  - `pages/student/tutors`
  - `pages/tutor/detail`
  - `pages/post/index`
  - `pages/my-jobs/index`
  - `pages/job/detail`
- `tutor` 分包：
  - `pages/tutor/jobs`
  - `pages/job/detail`
  - `pages/tutor/onboarding/index`
  - `pages/tutor/status`
  - `pages/tutor/profile`
- `workflow` 分包：
  - `pages/application/list`
  - `pages/application/detail`
  - `pages/pay/cashier`
  - `pages/chat/room`
- `course` 分包：
  - `pages/course/list`
  - `pages/course/detail`
  - `pages/course/lesson-detail`
  - `pages/schedule/index`
  - `pages/course/ai-summary`
  - `pages/live/prepare`
  - `pages/live/h5-bridge`

## 3. 页面分类与逻辑

### 3.1 公共页面

#### 首页 `pages/home/index`

职责：

- 未登录时展示学生视角的公开找老师/发需求入口
- 已登录学生展示学生首页
- 已登录教师且通过认证展示教师需求广场摘要
- 教师未通过认证展示入驻/审核状态卡

真实接口：

- 首页公共配置、热门教师、热门需求复用现有首页/教师列表/需求列表接口
- 用户上下文调用 `/user/me`

必须逻辑：

- 游客可浏览，不强制登录
- 受保护动作触发登录并保留回跳
- 角色切换后刷新首页数据

#### 消息 `pages/chat/list`

职责：

- 展示申请中心入口和未读数
- 展示聊天会话列表
- 展示“待支付信息费”“待处理申请”“待确认合作”等业务待办

真实接口：

- `/chat/application/unread`
- `/chat/application/received/page`
- `/chat/application/sent/page`
- `/chat/room/page`

必须逻辑：

- 聊天列表未读数与聊天室已读回执一致
- 点击待支付申请进入支付页
- 点击待确认申请进入申请详情
- 点击会话进入聊天室

#### 我的 `pages/me/index`

职责：

- 登录/退出
- 用户资料
- 角色切换
- 教师认证状态
- 我的需求、我的合作、课程表、收藏、邮箱设置入口

真实接口：

- `/user/me`
- `/user/updateUserInfo`
- `/user/email/*`
- 教师认证提交接口

必须逻辑：

- 学生和教师资料分区展示
- 切教师时必须经过教师状态守卫
- 退出清理 token、用户上下文、轮询与未读状态

### 3.2 学生端页面

#### 找老师 `pages/student/tutors`

职责：

- 搜索和筛选教师
- 列表分页
- 收藏教师
- 进入教师详情

真实接口：

- `/api/v1/parent/tutors/page`
- 收藏教师接口

必须逻辑：

- 游客可看列表
- 收藏和发起申请必须登录
- 筛选条件包括科目、城市、课时费、授课方式

#### 教师详情 `pages/tutor/detail`

职责：

- 展示教师资料、认证、科目、价格、城市、简介、历史课程/评价摘要
- 学生可收藏
- 学生可发起沟通申请

真实接口：

- 教师详情接口
- 收藏教师接口
- `/chat/application` 或后端当前创建申请接口

必须逻辑：

- 发起申请前选择授课形式，且携带 `ONLINE` 或 `OFFLINE`
- 创建申请后进入申请详情
- 若已存在申请，显示当前状态和下一步

#### 发布需求 `pages/post/index`

职责：

- 创建或编辑需求
- 支持线上、线下、均可
- 保存后进入我的需求或需求详情

真实接口：

- `/api/v1/parent/jobs`
- `PUT /api/v1/parent/jobs/{id}`
- `/api/v1/parent/jobs/{id}`

必须逻辑：

- 线上需求校验科目、年级、预算、频次、可上课时间、描述
- 线下需求额外校验城市和地址
- 已进入申请/合作后的锁定字段错误必须展示后端真实提示

#### 我的需求 `pages/my-jobs/index`

职责：

- 展示学生发布过的需求
- 支持状态筛选、分页、编辑、查看收到申请

真实接口：

- `/api/v1/parent/jobs/mine`
- 需求关闭/重开接口

必须逻辑：

- 区分匹配中、沟通中、试课中、合作中、已关闭
- 需求卡片显示授课形式、预算、频次、申请数
- 点击申请数进入申请中心并带 context filter

### 3.3 教师端页面

#### 教师入驻 `pages/tutor/onboarding/index`

职责：

- 基础资料、教学资料、认证资料、图片上传
- 保存草稿和提交审核

真实接口：

- `/user/updateUserInfo`
- `/api/v1/assets/upload`
- `/teacher/verification/education/submit`
- `/teacher/verification/realname/submit`

必须逻辑：

- 未完成资料不能进入需求广场
- 图片上传失败可重试和删除
- 提交后进入教师状态页

#### 教师状态 `pages/tutor/status`

职责：

- 展示未提交、审核中、已通过、被拒绝
- 被拒绝时展示原因并允许重新编辑

真实接口：

- `/user/me`

必须逻辑：

- 通过后可进入教师首页
- 待审核不能发起申请或收藏需求

#### 需求广场 `pages/tutor/jobs`

职责：

- 教师浏览需求
- 搜索、筛选、分页
- 收藏需求
- 进入需求详情

真实接口：

- `/api/v1/parent/jobs/feed`
- 收藏需求接口

必须逻辑：

- 仅审核通过教师可进入
- 筛选包括线上/线下、科目、城市、预算
- 列表显示发布者类型、预算、频次、授课形式和摘要

#### 需求详情 `pages/job/detail`

职责：

- 教师查看完整需求并发起申请
- 学生作为 owner 时编辑/关闭需求、查看申请

真实接口：

- 教师视角 `/api/v1/parent/jobs/{id}/view`
- 学生视角 `/api/v1/parent/jobs/{id}`
- `/chat/application`

必须逻辑：

- 教师发起申请必须校验认证状态
- 申请成功后进入发出的申请详情
- 已申请需求显示申请状态

### 3.4 申请、支付与聊天页面

#### 申请中心 `pages/application/list`

职责：

- 发出的申请/收到的申请
- 状态、未读、筛选、分页

真实接口：

- `/chat/application/sent/page`
- `/chat/application/received/page`
- `/chat/application/unread`

必须逻辑：

- 支持从需求详情带 context filter 进入
- 申请卡片显示对方、上下文、状态、聊天解锁状态

#### 申请详情 `pages/application/detail`

职责：

- 展示申请详情
- 接收方通过/拒绝
- 申请通过后进入支付或聊天

真实接口：

- `/chat/application/{applicationId}`
- `/chat/application/{applicationId}/decision`
- `/chat/application/{applicationId}/enter-chat`

必须逻辑：

- 通过申请后不能误判聊天已解锁
- 教师待支付时显示支付入口
- 学生侧显示等待教师支付

#### 信息费支付 `pages/pay/cashier`

职责：

- 创建支付单
- 发起微信支付
- 轮询订单状态
- 支付成功后进入聊天

真实接口：

- `/payment/prepay`
- `/payment/orders/{orderNo}`
- `/chat/application/{applicationId}/enter-chat`

必须逻辑：

- 生产必须使用 JSAPI `payParams`
- 需要 openid 时必须先完成微信登录/绑定
- 支付取消、失败、超时可恢复
- Dev Mock 支付必须只在开发/体验版显示

#### 聊天室 `pages/chat/room`

职责：

- 展示历史消息
- 文本/图片收发
- 申请、支付、合作、退款系统卡片
- 聊天解锁控制
- 发起合作提案

真实接口：

- `/chat/public/msg/page`
- `/chat/msg`
- 图片上传接口
- `/chat/read/ack`
- `/chat/delivery/ack`
- `/chat/events/sync`
- `/chat/collaboration/proposal`
- `/chat/collaboration/proposal/{proposalId}/response`
- `/courses/by-room/{roomId}`

必须逻辑：

- 未支付时禁用普通输入和合作按钮
- 发送失败保留本地消息并可重试
- 合作提案接受后刷新卡片并可查看合作
- 同步申请/支付/合作/退款状态消息

### 3.5 课程、课表与课堂页面

#### 我的合作 `pages/course/list`

职责：

- 展示长期合作列表
- 按阶段筛选
- 显示当前待办

真实接口：

- `/courses/my`
- 补充用户批量接口用于显示对方姓名头像

必须逻辑：

- 学生显示“授课老师”
- 教师显示“学生”
- 区分待支付、沟通中、试课、待学生确认、待正式课表、正式授课、退款、已结束

#### 合作详情 `pages/course/detail`

职责：

- 展示当前合作
- 展示该合作下全部课节
- 提供试课结果、正式课表、新增课节、调课、取消、确认改期、结课入口

真实接口：

- `/courses/{courseId}`
- `/api/v1/schedule/courses/{courseId}/events`
- `/api/v1/schedule/events`
- `/api/v1/schedule/events/{eventId}/response`
- `/api/v1/schedule/events/{eventId}/cancel`
- `/appointment/{id}/reschedule`
- `/appointment/{id}/confirmReschedule`
- `/courses/{courseId}/trial-result`
- `/api/v1/schedule/courses/{courseId}/weekly-schedule`
- `/courses/{courseId}/trial-refund/apply`

必须逻辑：

- 课节按时间分组
- 单节课可进入详情
- 不同状态显示不同主操作
- 线下试课失败需要证据图片和录屏信息
- 线上待上课程显示进入课堂入口

#### 单节课详情 `pages/course/lesson-detail`

职责：

- 展示单节课完整信息、状态、支付状态、直播状态、AI 总结状态
- 承接调课、取消、确认、结课、进入课堂、查看总结

真实接口：

- 课节详情可先由课程课节列表数据承接，必要时补 `/api/v1/schedule/events/{eventId}`
- 直播状态 `/live/sessions/by-course/{courseId}`
- AI 总结接口

必须逻辑：

- READY_TO_START/IN_PROGRESS 显示上课入口
- COMPLETED 显示课后总结入口
- PENDING 显示确认/拒绝
- RESCHEDULE_PENDING 显示确认改期

#### 课程表 `pages/schedule/index`

职责：

- 全局课程日历
- 今天/本周/本月视图
- 待确认与即将上课提醒

真实接口：

- `/api/v1/schedule/events`
- `/live/sessions/by-course/{courseId}` 或提醒接口

必须逻辑：

- 支持学生/教师共用
- 点击课节进入单节课详情
- 可从空白时间创建日程或从合作创建课节
- 显示待确认、已确认、进行中、已完成、异常

#### 线上课堂入口 `pages/live/prepare`

职责：

- 小程序课堂准备页
- 检查课堂是否可进入
- 原生课堂或 H5 课堂跳转

真实接口：

- `/live/sessions/by-course/{courseId}/prepare`
- `/live/sessions/{sessionId}/join-token`
- `/live/sessions/{sessionId}/device-report`

必须逻辑：

- 如果小程序原生暂不承接 LiveKit，则跳转 H5 课堂并带安全 token
- 展示未到时间、未支付阻塞、设备权限、课堂已结束等状态
- 返回后刷新课程/课节状态

#### AI 总结 `pages/course/ai-summary`

职责：

- 展示课后总结、课堂摘要、关键点、作业建议

真实接口：

- 复用 Web 端 AI 总结接口或 `/live/sessions/{sessionId}/ai/result`

必须逻辑：

- 支持生成中、成功、失败、无权限
- 支持从聊天卡片、合作详情、单节课详情进入

## 4. 真实接口原则

- 小程序页面加载必须调用真实后端接口，禁止以静态假数据作为验收依据
- Mock 只允许用于 UI 截图和开发态兜底，必须有显式环境标识
- 所有提交类动作必须防重复，并展示后端返回的真实错误
- 所有支付类动作必须保留订单号和状态，可刷新恢复
- 所有长轮询/定时器必须在页面卸载时停止

## 5. 测试验证设计

### 5.1 静态与构建

- `cd ai-tutor-miniprogram && npm run type-check`
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`
- 生产构建检查 API Base URL、Mock 登录、Mock 支付、AppID、合法域名

### 5.2 页面冒烟

使用 H5 预览或微信开发者工具完成：

- 学生首页、找老师、教师详情、发布需求、我的需求
- 教师首页、需求广场、需求详情、入驻状态
- 申请中心、申请详情、支付页、聊天页
- 我的合作、合作详情、单节课详情、课程表、AI 总结、课堂准备

### 5.3 真实接口联调

使用测试环境后门账号或 dev login：

- 学生账号创建线上需求
- 教师账号看到该需求并申请
- 学生通过申请
- 教师创建支付单并完成开发态支付成功或真实微信支付
- 聊天解锁
- 发起合作提案
- 对方接受
- 课程生成
- 创建课节/提交正式课表
- 课程表出现课节
- 进入课堂准备页
- 课后总结状态可展示

### 5.4 异常分支

- 未登录触发受保护动作
- 教师未认证进入需求广场
- 申请拒绝
- 信息费支付取消/失败/超时
- 聊天未解锁发送消息
- 合作提案拒绝/失效
- 课节拒绝/取消/调课
- 线下试课失败退款证据缺失
- AI 总结失败

### 5.5 验收输出

每次提测必须输出：

- 测试环境
- 测试账号
- 测试数据 ID：需求、申请、订单、房间、课程、课节
- 每条主流程步骤结果
- 失败项、截图或日志
- 不能闭环的真实原因：前端缺失、接口缺失、数据缺失、支付环境缺失或微信权限缺失
