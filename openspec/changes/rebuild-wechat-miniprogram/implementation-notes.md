# 实现进展记录

## 2026-04-27 第一批落地

### 已改代码

- 新增 `ai-tutor-miniprogram/src/types/domain.ts`
  - 定义用户、教师资料、学生资料、角色、教师状态、分页基础类型。
- 新增 `ai-tutor-miniprogram/src/utils/role.ts`
  - 修正前端对后端 `userType` 的理解：`1=教师`，`2=学生`。
  - 增加教师状态推导：`NONE`、`INCOMPLETE`、`PENDING`、`APPROVED`、`REJECTED`。
- 修改 `ai-tutor-miniprogram/src/stores/user.ts`
  - 登录后统一等待 `/user/me` 刷新用户上下文。
  - 微信 Mock 登录必须显式开启 `VITE_ENABLE_MP_MOCK_LOGIN=true` 且仅开发环境生效。
  - 角色切换改为依赖真实教师资料与认证状态。
  - 退出登录额外清理 `tutorStatus`。
- 修改 `ai-tutor-miniprogram/src/pages/home/index.vue`
  - 移除未登录打开首页强制跳转“我的”的逻辑。
- 修改 `ai-tutor-miniprogram/src/pages/home/components/ParentHome.vue`
  - 学生首页允许游客浏览教师列表，发起受保护动作时再登录。
- 新增 `ai-tutor-miniprogram/src/api/application.ts`
  - 封装申请创建、列表、详情、处理、进入聊天接口。
- 新增 `ai-tutor-miniprogram/src/api/payment.ts`
  - 封装 `/payment/prepay` 和 `/payment/orders/{orderNo}`。
- 修改 `ai-tutor-miniprogram/src/pages/job/detail.vue`
  - 教师需求详情不再直接 `/chat/room` 创建聊天室，改为创建申请。
- 修改 `ai-tutor-miniprogram/src/pages/chat/room.vue`
  - 信息费支付从旧 `/payment/create` 切换到当前 `/payment/prepay`，并使用 `tradeType=JSAPI`。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 构建仍有 uView/Sass `@import` 与 legacy JS API 弃用告警，非本次阻塞。

### 仍需继续

- 申请中心页面尚未创建，当前“申请已发送”后只 toast，不进入详情页。
- 聊天页还没有完整使用 `chatAccessStatus` 阻断输入。
- 支付成功后还未轮询 `/payment/orders/{orderNo}` 并调用申请进入聊天。
- 教师入驻真实认证状态还依赖 `/user/me` 现有字段，后端最好补充更稳定的状态摘要字段。

## 2026-04-27 第二批落地

### 对齐 Web 主流程

- 新增 `pages/application/list.vue`
  - 对齐 Web `ApplicationCenterPage.vue` 的“收到/发出申请、未读数、游标分页、状态展示”能力。
  - 小程序适配为单列移动工作台：顶部深色状态区、分段 tabs、申请卡片列表、下拉刷新/触底加载。
- 新增 `pages/application/detail.vue`
  - 对齐 Web `ApplicationDetailPage.vue` 的申请详情、通过/拒绝、进入聊天、支付 gating。
  - 小程序适配为固定底部主操作栏，避免用户在长页面中找不到下一步。
- 新增 `pages/pay/cashier.vue`
  - 对齐 Web 收银台支付上下文，使用 `/payment/prepay`、`uni.requestPayment`、`/payment/orders/{orderNo}` 轮询。
  - 小程序适配为单订单支付票据页，强调金额、订单号、状态和返回申请。
- 修改 `pages/chat/list.vue`
  - 从单纯聊天列表升级为“申请中心 + 聊天会话”的消息工作台。
  - 展示申请未读数，引导用户理解“先申请/支付，再聊天”的业务顺序。
- 修改 `pages/job/detail.vue` 和 `pages/tutor/detail.vue`
  - 发起申请后尽量跳转申请详情或申请中心，减少用户发完申请后没有反馈的断点。
- 修改 `pages.json`
  - 注册申请中心、申请详情和支付页路由。

### UI 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 启动 H5 预览后使用 Playwright 在 `390x844` 移动视口下做了截图验证：
  - `tmp/miniprogram-ui/application-list-v2.png`
  - `tmp/miniprogram-ui/application-detail-v2.png`
  - `tmp/miniprogram-ui/cashier-v2.png`
- 验证发现并修复：固定底部操作栏里的 `u-button` 在 H5/小程序预览中表现不稳定，已替换为自定义按钮，避免端差异导致 UI 变形。

### 仍需继续

- 需要继续按 Web 主功能补齐：我的课程、课程详情、试课合作、正式课表、退款入口、收藏中心、学生需求编辑。
- 需要给新增申请/支付页补充单测或小程序自动化测试脚本。
- 真机/微信开发者工具仍需做最终验证，H5 截图只覆盖布局和基础交互结构。

## 2026-04-27 第三批落地

### 对齐 Web 课程与试课合作主流程

- 新增 `src/api/course.ts`
  - 封装 `/courses/my`、`/courses/{courseId}`、`/courses/by-room/{roomId}`、`/courses/{courseId}/trial-result`、`/courses/{courseId}/trial-refund/apply`。
- 新增 `src/api/schedule.ts`
  - 封装 `/api/v1/schedule/courses/{courseId}/events`、课节响应、取消、正式课表提交接口。
- 修改 `src/api/chat.ts`
  - 增加合作提案创建与响应接口。
  - 增加聊天退费状态和申请退费接口封装。
- 新增 `pages/course/list.vue`
  - 对齐 Web “我的合作/我的课程”核心能力：阶段筛选、合作进度、费用/频次、退款/AI 摘要提示、下一步动作提示。
  - 小程序适配为移动工作台：顶部状态区、横向筛选、单列课程卡片。
- 新增 `pages/course/detail.vue`
  - 对齐 Web `CourseDetailPage.vue` 的课程基础信息、课节列表、试课结果、正式课表、试课失败退费入口。
  - 小程序适配为固定底部操作栏 + 底部 Sheet 表单，减少长页面操作跳失。
- 修改 `pages/chat/room.vue`
  - 重写聊天室 UI：申请卡片、支付卡片、联系方式解锁、合作提案、合作状态、退款状态。
  - 增加聊天未解锁输入阻断。
  - 增加“发起试课合作”底部表单，并支持接收方接受/拒绝合作提案。
  - 合作接受后支持按 room 查询课程并跳转课程详情。
- 修改 `pages/me/index.vue`
  - 增加“我的合作”入口。
- 修改 `pages.json`
  - 注册课程列表与课程详情页面。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 构建仍只有 uView/Sass `@import` 与 legacy JS API 弃用告警。
- 启动 H5 预览后使用 Playwright + Mock API 在 `390x844` 移动视口下完成截图验证：
  - `tmp/miniprogram-ui/course-list-v1.png`
  - `tmp/miniprogram-ui/course-detail-v1.png`
  - `tmp/miniprogram-ui/chat-room-v1.png`
- 验证结论：课程列表、课程详情底部操作栏、聊天室卡片和输入栏均非空，主要文字没有明显溢出或遮挡。

### 仍需继续

- 聊天图片消息、发送中/失败重试、已读回执、事件同步还未补齐。
- 课程真后端联调还需覆盖：合作提案接受后课程自动创建、学生提交正式课表、教师试课失败退费。
- 学生需求编辑、收藏、教师入驻完整资料与认证上传仍需继续按 Web 主功能补齐。

## 2026-04-27 第四批落地

### 聊天可用性补强

- 新增 `src/api/assets.ts`
  - 使用小程序 `uni.uploadFile` 封装 `/api/v1/assets/upload`，支持聊天图片上传。
- 修改 `src/api/chat.ts`
  - 增加 `sendImage`、`ackRead`、`ackDelivered`、`reportTyping`、`syncRealtimeEvents`。
- 修改 `pages/chat/room.vue`
  - 支持图片消息展示、点击预览。
  - 支持选择图片、上传图片、发送图片消息。
  - 文本和图片发送增加本地临时消息、发送中状态、失败状态、点击重试。
  - 拉取消息后自动上报已读与送达。
  - 接入 `/chat/events/sync`，有新事件时刷新消息；支持对方输入中提示。
  - 底部输入区扩展为“图片 / 合作 / 输入 / 发送”，并保持小程序移动端可用。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- H5 预览 + Playwright Mock 数据截图：
  - `tmp/miniprogram-ui/chat-room-v1.png`
- 验证结论：图片气泡、合作提案卡片、底部输入区在 `390x844` 移动视口下没有明显溢出或遮挡。

### 仍需继续

- 聊天列表未读数需要与真实后端 ackRead 返回联调确认。
- 图片上传需要在微信开发者工具或真机验证 `uni.uploadFile` 域名白名单、临时文件路径和真实素材预览。
- 学生需求编辑、收藏、教师入驻完整资料与认证上传仍需继续补齐。

## 2026-04-27 第五批落地

### 学生需求与收藏

- 修改 `src/api/jobs.ts`
  - 增加 `/api/v1/parent/jobs/{id}` 更新接口。
- 新增 `src/api/favorites.ts`
  - 封装教师收藏需求、学生收藏老师、批量检查收藏状态、收藏分页接口。
- 重写 `pages/post/index.vue`
  - 发布/编辑共用表单。
  - 支持科目、年级、学生性别、授课方式、城市/地址、每周频次、可上课时间、预算、老师性别偏好、需求描述、教员要求。
  - 线上授课自动清理线下地址；线下/均可要求城市和地址。
  - 编辑模式读取 `/api/v1/parent/jobs/{id}` 并保存到 `PUT /api/v1/parent/jobs/{id}`。
- 重写 `pages/my-jobs/index.vue`
  - 移动端需求管理工作台。
  - 支持游标分页、下拉刷新、编辑入口、关闭/重新打开需求。
- 修改 `pages/job/detail.vue`
  - 教师端支持收藏/取消收藏需求，并加载收藏状态。
- 修改 `pages/tutor/detail.vue`
  - 学生端支持收藏/取消收藏老师，并加载收藏状态。
  - 移除主操作区旧“预约课程”入口，避免继续引导到过时直付预约链路；主流程改为先申请沟通再确认试课合作。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- H5 预览 + Playwright Mock 数据截图：
  - `tmp/miniprogram-ui/my-jobs-v1.png`
  - `tmp/miniprogram-ui/post-edit-v1.png`
- 验证结论：我的需求卡片操作区、编辑需求表单、固定底部保存按钮在移动端视口下可用；H5 fullPage 截图中固定底部按钮显示在当前视口位置，属于截图机制表现。

### 仍需继续

- 收藏分页中心页面仍未做，可继续补“我的收藏”聚合入口。
- 教师入驻认证上传仍使用页面内上传函数，下一批应改为复用 `assetsApi.uploadImage`，并补实名认证/学历认证提交流程。
- 需求详情学生本人视角可继续补“编辑/关闭”快捷操作。

## 2026-04-27 第六批落地

### 收藏中心与入驻上传收敛

- 新增 `pages/favorites/index.vue`
  - 学生角色展示收藏老师：头像、科目、城市、简介、取消收藏、查看老师。
  - 教师角色展示收藏需求：标题、科目、授课方式、预算、描述、取消收藏、查看需求。
  - 支持游标分页、下拉刷新、触底加载和空状态。
- 修改 `pages.json`
  - 注册 `pages/favorites/index`。
- 修改 `pages/me/index.vue`
  - 增加“我的收藏”入口，并按角色展示“管理收藏的老师/需求”。
- 修改 `pages/tutor/onboarding/index.vue`
  - 头像和证书上传从页面内 `uni.uploadFile` 收敛为复用 `assetsApi.uploadImage`。
  - 保持原有提交用户资料和学历认证提交流程。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- H5 预览 + Playwright Mock 数据截图：
  - `tmp/miniprogram-ui/favorites-student-v1.png`
  - `tmp/miniprogram-ui/favorites-tutor-v1.png`
- 验证结论：学生收藏老师、教师收藏需求两种卡片在 `390x844` 移动视口下均无明显溢出或遮挡。

### 仍需继续

- 教师入驻还缺实名认证材料上传与 `/teacher/verification/realname/submit` 提交流程。
- 需求详情学生本人视角可继续补“编辑/关闭”快捷操作。
- 收藏中心目前按收藏 ID 再拉详情，真实后端大量收藏时可考虑补聚合详情接口减少请求数。

## 2026-04-27 第七批落地

### 教师入驻实名认证闭环

- 修改 `pages/tutor/onboarding/index.vue`
  - 第三步从单一“学历证明”扩展为“认证资料”：毕业院校、学历、学历证明、身份证人像面、身份证国徽面。
  - 头像、学历证明、身份证图片统一复用 `assetsApi.uploadImage`，避免页面内重复维护上传实现。
  - 提交时先保存用户/教师资料，再调用 `/teacher/verification/education/submit` 提交学历认证，最后调用 `/teacher/verification/realname/submit` 提交实名认证。
  - 增加提交中防重复、证件材料缺失校验、证件上传成功态标记。
  - 将底部 `u-button` 替换为页面内自定义触控按钮，规避 H5/小程序预览中按钮退化为纯文字的端差异。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- H5 预览 + Playwright Mock 数据截图：
  - `tmp/miniprogram-ui/onboarding-certification-v1.png`
- 验证结论：认证资料第 3 步在 `390x844` 移动视口下无主要内容溢出，证件上传成功态和底部提交按钮可见、清晰、可触控。

### 仍需继续

- 需要在微信开发者工具/真机验证 `uni.uploadFile` 的域名白名单、临时文件路径和真实证件图片预览。
- 需求详情学生本人视角可继续补“编辑/关闭/收到申请”快捷操作。
- 教师状态页仍需按未提交、待审核、通过、拒绝四态继续精修。

## 2026-04-28 第八批落地

### 角色准入守卫与教师状态反馈

- 新增 `src/utils/tutorGuard.ts`
  - 收敛教师准入守卫：未登录跳登录，未入驻/资料不完整跳入驻，待审核/已拒绝跳教师状态页，只有审核通过的教师才能继续教师侧关键动作。
- 新增 `src/utils/studentGuard.ts`
  - 收敛学生写操作守卫：非学生身份尝试发布需求、管理需求、收藏老师、向老师发起申请时，统一提示并引导切回学生端。
- 修改 `pages/home/components/TutorHome.vue`
  - 当教师资料未审核通过时，不再渲染需求广场，而是展示准入拦截卡片。
  - 待审核、已拒绝、未提交三种状态分别提供“查看审核状态 / 重新完善资料 / 去入驻”动作，并允许快速切回学生端浏览老师。
- 修改 `pages/tutor/status.vue`
  - 重建为四态状态页：未提交、待完善、审核中、审核通过、审核拒绝。
  - 以真实用户状态为主、路由参数为辅，并展示基础资料、教学履历、身份认证、学历认证的明细状态。

## 2026-04-28 第十八批落地

### 开发联调与请求基座收口

- 修改 `src/utils/request.ts`
  - 将请求基座地址从模块初始化常量收敛为动态读取，新增 `getBaseUrl`、`setBaseUrl`、`clearBaseUrl`。
  - 运行时优先读取本地存储中的 `ai_tutor_api_base_url`，其次读取 `VITE_API_BASE_URL`，最后回落到默认地址。
  - `request` 与 `resolveImageUrl` 改为按请求时/解析时读取最新地址，避免开发态切换 API 地址后必须重启应用。
- 修改 `src/api/assets.ts`
  - 图片上传接口改为调用 `getBaseUrl()`，确保聊天、头像、认证材料上传与新的联调地址保持一致。
- 修改 `src/pages/me/index.vue`
  - “我的”页补充更贴近网页端的说明性入口文案，强化课程、收藏、邮箱提醒和角色切换的语义。
  - 增加角色状态提示文案，区分学生端、教师已通过、待审核、已拒绝等状态。
  - 在 `import.meta.env.DEV` 下新增“开发联调”区域，可查看当前 API 地址、编辑保存、恢复默认地址；生产构建不显示。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 启动 H5 预览 `http://127.0.0.1:5181/`，通过 in-app browser 做页面结构检查：
  - 学生首页 DOM 已包含搜索、科目筛选、课时费筛选、空态和刷新动作。
  - H5 控制台仍存在 `u-icon` 组件解析告警，以及页面请求失败时的 `console.error(Object)` 输出；不影响 mp-weixin 构建，但会影响 H5 截图观感，后续可继续治理。

### 仍需继续

- “我的”页仍未达到网页端资料编辑深度，目前以入口聚合和联调能力为主。
- 仍需继续完成真机/微信开发者工具验证：输入框键盘、下拉刷新、触底加载、底部固定操作区遮挡。
- 仍需补两类收口：
  - 教师首页、学生首页的截图级 UI 验证；
  - 统一空态/错误态与长列表体验在真机端的最终检查。

## 2026-04-28 第十九批落地

### 首页密度与“我的”页工作台重构

- 修改 `src/pages/home/components/ParentHome.vue`
  - 继续压缩学生首页首屏密度：Hero、搜索框、筛选区与老师卡片统一收紧间距、字号和标签尺寸。
  - 横向科目筛选仍保留滚动结构，但将 chip 尺寸进一步调小，降低 iPhone 12/13 视口下的拥挤感。
- 修改 `src/pages/home/components/TutorHome.vue`
  - 同步压缩教师首页的搜索、筛选、预算区与需求卡片密度，保持学生/教师首页视觉节奏一致。
  - 保留“城市、科目、授课形式、预算”四类筛选，但让首屏更偏工具台，而不是大块卡片堆叠。
- 重写 `src/pages/me/index.vue`
  - 从入口聚合页升级为“资料工作台”：
    - 未登录态保留登录卡，但补充资料/合作说明；
    - 登录态支持头像上传、基础资料编辑、学生资料编辑、教师资料编辑；
    - 教师态可直接在“我的”页提交实名认证与学历认证；
    - 保留合作、收藏、邮箱提醒、开发联调入口。
  - 这一版更接近网页端 `MePage.vue` 的“资料 + 状态 + 动作”组合，而不是只放导航入口。
- 修改 `src/types/domain.ts`
  - 补充 `TeacherProfile` 与 `StudentProfile` 缺失字段，支撑“我的”页资料编辑与认证状态展示。
- 修改 `project.config.json`
  - 将 `miniprogramRoot` 从 `dist/dev/mp-weixin/` 修正为当前实际构建产物目录 `dist/build/mp-weixin/`，解决微信开发者工具模拟器启动失败。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 微信开发者工具验证：
  - 已成功打开项目 `ai-tutor-miniprogram`。
  - 修复 `miniprogramRoot` 后，模拟器可正常启动，不再报找不到 `app.json`。
  - 已完成页面启动检查：
    - `pages/home/index` 可在模拟器正常渲染，能看到游客 Banner、搜索、筛选、空态与 TabBar。
    - `pages/me/index` 未登录态可正常渲染，能看到资料工作台说明、登录卡、输入框与主按钮。

### 当前观察

- 学生首页在微信开发者工具 iPhone 12/13 (Pro) 75% 视口下，横向科目筛选仍略显紧凑，后续还可继续精修。
- “我的”页已具备资料编辑工作台形态，但本轮验证停在未登录态；登录后教师资料、认证按钮与表单键盘体验仍需继续验证。
- 调试器仍能看到若干 warning/error 计数，需在下一轮继续定位具体来源。

## 2026-04-28 第二十批落地

### 首页错误态与密度继续收口

- 修改 `src/pages/home/components/ParentHome.vue`
  - 去掉列表请求失败时覆盖筛选区的 `uni.showToast`，改为页内状态承接。
  - 继续压缩 Hero、搜索框、横向筛选 chip、老师卡片和标签的尺寸与间距。
  - 将“失败提示条 + 空态卡片”的双层提示，收敛为单一错误状态卡。
- 修改 `src/pages/home/components/TutorHome.vue`
  - 同步去掉需求列表失败时的悬浮 toast，改为页内错误状态。
  - 同步收紧筛选区和需求卡片密度，保持学生/教师首页视觉一致。
  - 同样将双层失败提示收敛为单一错误状态卡。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 微信开发者工具验证：
  - `pages/home/index` 在 iPhone 12/13 (Pro) 75% 视口下可正常渲染。
  - 学生首页已确认不再出现覆盖筛选区的失败 toast。
  - 失败时只保留单一错误状态卡，观感比上一轮更干净。
  - “我的”页未登录态仍能正常渲染登录卡、输入框、协议勾选与主按钮。
- H5 in-app browser 验证：
  - 重新拉起了 `http://127.0.0.1:5181/` 本地预览服务。
  - 可正常查看 `pages/me/index` 未登录态结构。
  - 首页与“我的”页都出现过 dev server 重启后的动态模块短暂加载失败，属于 H5 开发态不稳定现象，不影响 mp-weixin 构建结果。

### 当前观察

- 首页当前在开发者工具里的主要问题已从“toast 遮挡”收敛到“纯数据失败时的最终观感”，这部分已经进入可接受区间。
- 开发者工具里切换 tab 后，渲染层截图偶发滞后，需要继续以刷新页和真实点击联动做验证。
- “我的”页登录后态、输入框键盘弹起、长列表/长表单滚动与底部遮挡，还需要继续做一轮端侧验证。
  - 拒绝态展示驳回原因，并支持直接回到入驻页继续修改提交。
- 修改 `pages/job/detail.vue`
  - 教师收藏需求、发起沟通统一接入教师准入守卫，审核未通过时不再误入申请流程。
- 修改 `pages/tutor/detail.vue`
  - 收藏老师、向老师发起申请统一接入学生身份守卫。
- 修改 `pages/post/index.vue` 与 `pages/my-jobs/index.vue`
  - 发布/编辑需求、我的需求列表统一要求学生身份。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- H5 预览 + Playwright Mock 数据截图：
  - `tmp/miniprogram-ui/tutor-home-guard-pending-v1.png`
  - `tmp/miniprogram-ui/tutor-status-rejected-v1.png`
  - `tmp/miniprogram-ui/tutor-status-approved-v1.png`
- 验证结论：教师首页待审核拦截卡、教师状态页通过/拒绝态在 `390x844` 视口下布局稳定，主要内容无有效节点溢出；构建仍只有 uView/Sass 弃用告警，非本批阻塞。

### 仍需继续

- 登录前动作回跳仍未统一落地，受保护页面还不能在登录或切角色后精确恢复到原操作。
- 学生本人需求详情还缺“收到申请 / 编辑 / 关闭”快捷操作。
- 微信开发者工具与真机仍需验证教师状态切换、上传与支付链路的端侧行为。

## 2026-04-28 第九批落地

### 学生本人需求详情快捷操作

- 修改 `pages/job/detail.vue`
  - 当需求详情识别为“当前登录学生本人发布”时，展示“编辑需求 / 关闭或重新打开 / 收到的申请”操作区。
  - “收到的申请”直接跳转申请中心，并按 `contextType=DEMAND + contextId` 过滤到当前需求相关申请。
  - 关闭/重新打开需求直接复用 `PUT /api/v1/parent/jobs/{id}` 状态切换，减少用户在“我的需求”和详情页之间来回切换。
- 修改 `pages/application/list.vue`
  - 支持从路由参数读取 `tab/contextType/contextId`，在申请中心做本地过滤展示，便于从某个需求详情快速查看对应收到的申请。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 构建仍只有 uView/Sass 弃用告警，非本批阻塞。

### 仍需继续

- 登录前动作回跳仍未统一落地，受保护页面还不能在登录或切角色后精确恢复到原操作。
- 申请中心当前按前端本地过滤需求申请，后续可以考虑补后端按 `contextId/contextType` 过滤接口，减少大列表下无关数据加载。
- 微信开发者工具与真机仍需验证教师状态切换、上传与支付链路的端侧行为。

## 2026-04-28 第十批落地

### 登录前动作回跳

- 新增 `src/utils/authRedirect.ts`
  - 收敛待恢复动作的存取、当前页面 URL 推导、跳登录页和登录后恢复逻辑。
- 修改 `src/utils/tutorGuard.ts` 与 `src/utils/studentGuard.ts`
  - 未登录时不再只跳到“我的”页，而是记录当前目标页面与目标角色，登录后恢复到原页面继续操作。
  - 学生守卫在切角色前也会记录当前目标页面，避免切回学生端后丢失原上下文。
- 修改 `src/utils/request.ts`
  - 401/登录过期时统一写入待恢复动作后跳转登录，减少请求失效后用户丢上下文的问题。
- 修改 `pages/me/index.vue`
  - 登录成功后自动消费待恢复动作。
  - `onShow` 时如果已经处于登录态但仍有待恢复动作，也会自动继续回跳，兼容 H5/小程序端的事件时序差异。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- H5 回跳验证：
  - 通过脚本模拟“我的页已登录态 + 待恢复动作”，页面成功自动恢复到老师详情页。
  - 验证结果：`location.href` 最终为 `#/pages/tutor/detail?id=1001`，`ai_tutor_pending_redirect` 已被清空。

### 仍需继续

- 目前已覆盖登录前动作回跳，但“登录后自动打开申请弹窗/继续未完成表单提交”这类更细粒度动作恢复还未做。
- 申请中心当前按前端本地过滤需求申请，后续可以考虑补后端按 `contextId/contextType` 过滤接口，减少大列表下无关数据加载。
- 微信开发者工具与真机仍需验证教师状态切换、上传与支付链路的端侧行为。

## 2026-04-28 第十一批落地

### 学生首页对齐 Web 浏览流

- 新增 `src/api/parentTutors.ts`
  - 独立收敛学生找老师列表接口，封装 `/api/v1/parent/tutors/page`。
- 重写 `pages/home/components/ParentHome.vue`
  - 从轻量老师列表升级为移动端老师浏览工作台。
  - 增加游客提示、关键词搜索、科目筛选、课时费区间筛选、刷新、下拉刷新、触底加载。
  - 老师卡片补齐城市、学校、学历、经验、课时费、科目标签、亮点信息，信息密度向 Web 端老师列表靠拢。
  - 保持“游客先浏览、写操作时再登录”的小程序适配策略，与 Web 主流程一致。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 构建仍只有已有的 uView/Sass `@import` 与 legacy JS API 弃用告警。

### 仍需继续

- 学生首页还可以继续补“城市/授课方式”筛选，以更完整贴近 Web 筛选维度。
- H5/微信开发者工具还需补这一页的截图级验证，确认筛选区和长卡片在小程序视口下的最终观感。
- 账号设置页、教师首页需求广场、统一页面空态/错误态仍需继续完善。

## 2026-04-28 第十二批落地

### 教师首页需求广场对齐

- 重写 `pages/home/components/TutorHome.vue`
  - 在教师审核通过态下，将原来的简化需求列表升级为移动端需求广场工作台。
  - 增加关键词搜索、授课方式筛选、预算区间筛选、刷新、下拉刷新、触底加载。
  - 需求卡片补齐预算、授课方式、频次、学历要求、年级/学段标签、发布者提示和机构单提醒。
  - 保留教师准入守卫卡片，确保未入驻、待审核、已拒绝三态的首页行为仍与业务规则一致。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 构建仍只有已有的 uView/Sass `@import` 与 legacy JS API 弃用告警。

### 仍需继续

- 教师首页还缺城市和科目筛选，离 Web 端完整筛选维度还差一步。
- 学生首页、教师首页都还需要补微信开发者工具或 H5 的截图级 UI 验证。
- 账号设置页、统一页面空态/错误态、真机端长列表体验仍需继续完善。

## 2026-04-28 第十三批落地

### 教师首页筛选补齐 + 邮箱设置页重建

- 新增 `src/components/AppStateCard.vue`
  - 抽出统一页面状态卡片，先用于首页空态/错误态与账号设置入口态，减少后续页面重复实现。
- 修改 `pages/home/components/TutorHome.vue`
  - 在原有关键词、授课方式、预算筛选基础上，补齐城市和科目筛选。
  - 需求广场筛选口径与 Web `feedDemands` 参数进一步对齐。
- 修改 `pages/home/components/ParentHome.vue`
  - 空态切换为复用统一状态卡片，减少页面局部状态样式分叉。
- 重写 `pages/account/email.vue`
  - 对齐 Web 邮箱设置页的信息层次和状态表达。
  - 完整支持主邮箱绑定、课后总结邮箱绑定、验证码发送、邮箱验证、移除总结邮箱。
  - 增加未登录引导、加载失败提示、成功/失败反馈、提醒说明区。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 使用本地 H5 页面做了 DOM 级检查：
  - 学生首页关键节点已渲染：游客提示、搜索、科目筛选、课时费筛选、空态卡片。
  - 邮箱设置页关键节点已渲染：Hero、未登录引导、邮箱设置入口态。
- 构建仍只有已有的 uView/Sass `@import` 与 legacy JS API 弃用告警。

### 仍需继续

- 首页目前完成了 DOM 级验证，但浏览器插件截图超时，仍需补微信开发者工具或更稳定的截图级 UI 验证。
- 统一状态组件目前只接入了首页和邮箱页，`favorites`、`my-jobs`、`application` 等页还可继续收敛。
- 真机端长列表滚动、下拉刷新和输入框交互仍需继续做端侧体验验证。

## 2026-04-28 第十四批落地

### 统一状态组件继续铺开

- 修改 `pages/favorites/index.vue`
  - 登录引导、加载失败、空列表状态统一接入 `AppStateCard`。
- 修改 `pages/my-jobs/index.vue`
  - 需求加载失败、空列表状态统一接入 `AppStateCard`。
- 修改 `pages/application/list.vue`
  - 登录引导、申请列表加载失败、空列表状态统一接入 `AppStateCard`。
- 修改 `pages/application/detail.vue`
  - 加载中、详情加载失败状态统一接入 `AppStateCard`。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 构建仍只有已有的 uView/Sass `@import` 与 legacy JS API 弃用告警。

### 仍需继续

- 统一状态组件还可继续接入课程、支付、发布需求等页面，进一步消除页面状态表现差异。
- 真机端长列表滚动、下拉刷新和输入框交互仍需继续做端侧体验验证。
- 首页与账号页仍需补更稳定的截图级 UI 验证产物。

## 2026-04-28 第十五批落地

### 课程、支付、发布需求页状态统一

- 修改 `pages/course/list.vue`
  - 登录引导、加载中、加载失败、空列表状态统一接入 `AppStateCard`。
- 修改 `pages/course/detail.vue`
  - 加载中、详情加载失败状态统一接入 `AppStateCard`。
- 修改 `pages/pay/cashier.vue`
  - 支付单创建失败时增加统一状态卡和“重新获取”入口，补足支付页的失败恢复路径。
- 修改 `pages/post/index.vue`
  - 发布/编辑需求页的加载态和表单错误态统一接入 `AppStateCard`，减少散落的状态块实现。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 构建仍只有已有的 uView/Sass `@import` 与 legacy JS API 弃用告警。

### 仍需继续

- 真机端长列表滚动、下拉刷新和输入框交互仍需继续做端侧体验验证。
- 首页与账号页仍需补更稳定的截图级 UI 验证产物。
- 申请中心、课程、支付等页虽然状态样式已统一，但细节交互仍可继续做端侧打磨。

## 2026-04-28 第十六批落地

### 请求层错误展示收敛

- 修改 `src/utils/request.ts`
  - 新增 `silentError` 选项，允许页面已经具备完整错误卡时，不再额外弹出全局 toast。
- 修改以下读接口，接入 `silentError: true`
  - `src/api/parentTutors.ts`
  - `src/api/course.ts`
  - `src/api/schedule.ts`
  - `src/api/application.ts`
  - `src/api/favorites.ts`

### 效果

- 首页、收藏、申请、课程等页面在请求失败时，优先由页面内错误卡承接，不再同时出现“toast + 页面错误态”的双重提示。
- 用户失败体验更安静，也更符合小程序工作台页面的使用预期。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 构建仍只有已有的 uView/Sass `@import` 与 legacy JS API 弃用告警。

### 仍需继续

- 真机端长列表滚动、下拉刷新和输入框交互仍需继续做端侧体验验证。
- 首页与账号页仍需补更稳定的截图级 UI 验证产物。
- 聊天、支付和上传等强端侧能力还需要继续做微信开发者工具/真机联调。

## 2026-04-28 第十七批落地

### 长列表页面配置补齐 + `jobs` 链路静默错误

- 修改 `src/pages.json`
  - 为以下已实现 `onPullDownRefresh` 的页面补齐 `enablePullDownRefresh: true`
  - `pages/home/index`
  - `pages/post/index`
  - `pages/my-jobs/index`
  - `pages/application/list`
  - `pages/course/list`
  - `pages/favorites/index`
- 修改 `src/api/jobs.ts`
  - `mineDemands`
  - `feedDemands`
  - `getDemand`
  - `getDemandView`
  - 上述读接口统一接入 `silentError: true`

### 效果

- 已实现下拉刷新的长列表页，页面配置和代码行为终于一致，后续更适合直接进入微信开发者工具/真机验证。
- `jobs` 相关页面失败时，优先由页面内错误态承接，不再额外弹全局 toast。

### 验证

- `cd ai-tutor-miniprogram && npm run type-check`：通过。
- `cd ai-tutor-miniprogram && npm run build:mp-weixin`：通过。
- 构建仍只有已有的 uView/Sass `@import` 与 legacy JS API 弃用告警。

### 仍需继续

- 真机端长列表滚动、下拉刷新和输入框交互仍需继续做端侧体验验证。
- 首页与账号页仍需补更稳定的截图级 UI 验证产物。
- 聊天、支付和上传等强端侧能力还需要继续做微信开发者工具/真机联调。
