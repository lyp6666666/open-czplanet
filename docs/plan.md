# 家教直聘（BOSS直聘式）MVP 超详细 Plan

## 0. 目标与约束（先统一口径）

### 0.1 产品愿景（一句话）
做“家教届的 BOSS 直聘”：学生/家长发布需求，老师像“求职者”一样浏览需求并直接开聊，达成预约与成单。

### 0.2 本阶段（MVP）必须闭环的 5 条主链路
1. 教师端：注册/登录 → 完善资料 → 浏览/搜索家教需求 → 进入需求详情 → “立即沟通”开聊（进入会话页）
2. 学生/家长端：注册/登录 → 完善资料 → 发布需求 → 在“我的需求”里编辑需求
3. 双端：会话列表 → 消息列表 → 发送消息（保证基础可用：文字消息）
4. 双端：我的（个人信息查看/编辑）→ 退出登录
5. 运营/增长最低配：未登录首页（已有）→ 引导注册/登录 → 角色切换入口清晰

### 0.3 MVP 暂不做（避免范围爆炸，P1/P2 再上）
- WebSocket 实时推送、已读回执、未读数、撤回、@、多媒体（图片/文件）
- 支付、保证金、平台抽成、订单履约、评价体系、风控实名
- 推荐/算法、复杂筛选（距离、地图）、企业认证等重机制
- 多端（APP/小程序）适配：先把 Web MVP 打穿

---

## 1. 现状盘点（基于你当前仓库的真实实现）

### 1.1 前端已实现（ai-tutor-web）
- 路由仅 3 个入口：Home + 教师登录 + 学生登录：[router/index.ts](file:///Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-web/src/router/index.ts#L1-L27)
- 登录/注册页已具备手机号验证码登录/注册，且支持角色切换 UI：[AuthPage.vue](file:///Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-web/src/pages/AuthPage.vue#L1-L194)
- 首页是“未登录首页 Guest Home”聚合展示：热门词、科目树、Banner、热门服务/需求/老师等：[HomePage.vue](file:///Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-web/src/pages/HomePage.vue#L1-L65) + [stores/home.ts](file:///Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-web/src/stores/home.ts#L36-L176)
- API 层已统一封装 Bearer Token：[http.ts](file:///Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-web/src/api/http.ts#L1-L55)

### 1.2 后端已实现（Spring Boot 聚合启动）
聚合启动模块同时依赖“预约服务 + IM 服务”：[ai-tutor-starter/pom.xml](file:///Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-starter/pom.xml#L20-L65)

#### 登录/注册与资料（预约服务 tutor-appointment-service）
- 登录/注册、验证码：[/user/loginOrRegister, /user/sendcode]：[UserController](file:///Users/bytedance/lyp/project/huoyue/ai_platform/tutor-appointment-service/src/main/java/com/ai/tutor/appointment/controller/UserController.java#L28-L114)
- 登录时会自动创建 teacher_profile / student_profile（保证后续业务可用）：[UserServiceImpl](file:///Users/bytedance/lyp/project/huoyue/ai_platform/tutor-appointment-service/src/main/java/com/ai/tutor/appointment/service/impl/UserServiceImpl.java#L57-L117)
- 更新资料接口已存在：[/user/updateUserInfo]：[UserController](file:///Users/bytedance/lyp/project/huoyue/ai_platform/tutor-appointment-service/src/main/java/com/ai/tutor/appointment/controller/UserController.java#L86-L92)

#### 岗位/需求（已具备完整 CRUD + feed）
- 学生/家长“需求贴”：[/api/v1/parent/jobs]：[ParentJobPostingController](file:///Users/bytedance/lyp/project/huoyue/ai_platform/tutor-appointment-service/src/main/java/com/ai/tutor/appointment/controller/ParentJobPostingController.java#L18-L60)
- 老师“服务贴”：[/api/v1/tutor/services]：[TutorJobPostingController](file:///Users/bytedance/lyp/project/huoyue/ai_platform/tutor-appointment-service/src/main/java/com/ai/tutor/appointment/controller/TutorJobPostingController.java#L18-L60)
- 数据库表已存在：student_job_posting / teacher_job_posting：[huoyue.sql](file:///Users/bytedance/lyp/project/huoyue/ai_platform/sqlDoc/huoyue.sql#L43-L117)

#### IM（已具备会话创建 + 会话列表 + 消息分页 + 发消息）
- 会话：POST /chat/room、GET /chat/room/page：[ChatRoomController](file:///Users/bytedance/lyp/project/huoyue/ai_platform/videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/chat/controller/ChatRoomController.java#L21-L41)
- 消息：GET /chat/public/msg/page、POST /chat/msg：[ChatController](file:///Users/bytedance/lyp/project/huoyue/ai_platform/videoCall-IM-service/src/main/java/com/ai/tutor/videocallimservice/chat/controller/ChatController.java#L20-L49)
- DB 表已存在：room / message：[huoyue.sql](file:///Users/bytedance/lyp/project/huoyue/ai_platform/sqlDoc/huoyue.sql#L177-L242)

#### 统一鉴权（全路径拦截）
CollectorInterceptor + JwtInterceptor 已对 /** 生效（登录/验证码/未登录首页除外）：[WebConfig](file:///Users/bytedance/lyp/project/huoyue/ai_platform/tutor-appointment-service/src/main/java/com/ai/tutor/appointment/config/WebConfig.java#L10-L45)

### 1.3 当前最大缺口（为什么“后端有了但产品不可用”）
- 前端缺：个人中心/资料编辑、需求贴发布/编辑/我的列表、教师端需求广场/搜索/详情、会话列表/聊天页
- 后端缺：没有“获取当前用户资料”的接口（前端无法回显资料），IM 返回的会话/消息缺少对方基础信息（昵称/头像/身份），产品体验会很“裸”
- 体系缺：角色准入规则（teacher/student）没有强约束（存在误用风险：学生调用老师接口）

---

## 2. 参考 BOSS 直聘的“可复用设计套路”（落到你这个 MVP）

由于目标是“家教届 BOSS 直聘”，MVP 只要抓住 BOSS 的 3 个核心体验点即可：

1. 强搜索 + 强列表：用户一进入就能“搜/筛/刷列表”，每张卡片都有明确下一步（立即沟通）
2. 强沟通入口：全局可达的“消息/沟通”入口，会话列表就是用户的工作台
3. 强身份与资料：登录后要能快速完善资料，否则“无法匹配/无法沟通/无法发布”

你当前的 UI 基色已是 BOSS 风格的“强主色”（#00bebd），可以沿用，但需要把“信息架构”和“页面分层”补齐。

---

## 3. 信息架构（IA）与路由规划（MVP 必须具备）

### 3.1 全局 Layout（BOSS 风格最重要的结构）
推荐结构（Web 端）：
- 顶部导航：Logo + 城市切换 + 搜索框 + 角色入口（“我要当家教/我要找家教”）+ 消息 + 我的
- 主体：列表/详情双栏（优先实现“列表页”，详情页可单页）

### 3.2 路由地图（建议）
公共：
- /（已存在）：未登录首页（Guest Home）
- /auth/tutor、/auth/student（已存在）

登录后（按角色分流）：
- /tutor/jobs：教师端“需求广场”（看家长需求贴）
- /tutor/jobs/:id：需求详情 + 立即沟通
- /student/post：学生/家长“发布需求”
- /student/jobs/mine：我的需求（列表）
- /student/jobs/:id/edit：编辑需求
- /chat：会话列表
- /chat/:roomId：聊天页
- /me：我的（资料查看/编辑、退出登录）

### 3.3 页面准入规则（前端路由守卫 + 后端校验）
- 未登录：只能访问 /、/auth/*
- 已登录 + TEACHER：允许 /tutor/*、/chat*、/me
- 已登录 + STUDENT：允许 /student/*、/chat*、/me

---

## 4. 关键业务流程（把“能用”拆成明确步骤）

### 4.1 教师端：看需求 → 立即沟通
1. 进入 /tutor/jobs
2. 默认加载：/api/v1/parent/jobs/feed（支持 city/subject/mode + 游标分页）
3. 列表卡片展示：标题、预算区间、城市/授课方式、发布时间（MVP 够用）
4. 点击卡片进入详情 /tutor/jobs/:id：调用 /api/v1/parent/jobs/{id}
5. 点击“立即沟通”：POST /chat/room（targetUid=parentId）→ 跳转 /chat/:roomId

### 4.2 学生端：发需求 → 编辑需求
1. 进入 /student/post（发布页）
2. 表单字段（对齐你 DB/DTO）：科目、标题、描述、授课方式、城市/地址、预算、时间段
3. 发布：POST /api/v1/parent/jobs → 返回 id → 进入“我的需求”或详情
4. 我的需求：GET /api/v1/parent/jobs/mine（游标分页）
5. 编辑：GET /api/v1/parent/jobs/{id} 回显 → PUT /api/v1/parent/jobs/{id}

### 4.3 会话与聊天（双端一致）
1. /chat：GET /chat/room/page（游标分页）
2. /chat/:roomId：GET /chat/public/msg/page（roomId + cursor）
3. 发送：POST /chat/msg（roomId + body）

---

## 5. 接口与数据（对照“现有”与“需要补齐”）

### 5.1 你已具备、前端直接对接即可的接口
- 登录：POST /user/loginOrRegister
- 发送验证码：POST /user/sendcode
- 更新资料：POST /user/updateUserInfo
- 家长需求贴：POST/PUT/GET + mine/feed（见 ParentJobPostingController）
- IM：/chat/room、/chat/room/page、/chat/public/msg/page、/chat/msg
- 未登录首页：/api/v1/public/**

### 5.2 MVP 强烈建议补齐的后端接口（否则体验会很“简陋”）
1. 获取当前登录用户信息（用于“我的”页回显 + 角色分流）
   - 建议：GET /user/me（返回 LoginUserVO + teacher/student profile 简版）
2. 批量获取用户基础信息（用于会话列表/聊天页展示昵称头像）
   - 方案 A（推荐）：IM 返回里直接带 otherUser（uid/name/avatar/userType）
   - 方案 B：新增 GET /user/batch?ids=1,2,3（前端在 /chat 页面补齐信息）
3. 需求广场搜索能力（老师端需要“搜索”）
   - 在 /api/v1/parent/jobs/feed 增加 q（keyword）与排序 sort（latest/recommend）

### 5.3 角色准入（后端必须有硬校验）
- /api/v1/parent/jobs/** 只允许 STUDENT
- /api/v1/tutor/services/** 只允许 TEACHER
- /student/* 页面调用的接口同理
实现方式建议：在 appointment 模块新增 RoleInterceptor（路径前缀 -> 允许 role 列表）。

---

## 6. 前端 Coding 计划（Vue3/Vite/Pinia，按 MVP 顺序）

### 6.1 基础工程能力补齐（P0，所有页面共享）
1. 新增“登录态初始化”：
   - 从 localStorage 读取 token + role/userId（目前 token 存在，但缺少“当前用户”拉取与路由分流的确定性）
2. 新增全局路由守卫：
   - 未登录拦截到 /auth/*
   - 已登录按 role 进入各自工作台
3. 新增统一布局组件（Boss 风格）：
   - Header：Logo / 城市 / 搜索 / 消息 / 我的
   - Content：容器宽度、卡片样式、列表空态

### 6.2 教师端（需求广场/详情/开聊）
1. 页面：/tutor/jobs（列表）
   - 顶部：搜索框（q）+ 基础筛选（科目/授课方式/城市）
   - 列表：卡片（标题、预算、城市/方式、更新时间）+ “立即沟通”按钮
2. 页面：/tutor/jobs/:id（详情）
   - 展示详情信息 + CTA “立即沟通”
3. API 对接：
   - 调用 /api/v1/parent/jobs/feed 与 /api/v1/parent/jobs/{id}
   - 立即沟通：POST /chat/room → 跳聊天页

### 6.3 学生端（发布/我的/编辑）
1. 页面：/student/post（发布需求）
2. 页面：/student/jobs/mine（我的需求列表）
3. 页面：/student/jobs/:id/edit（编辑需求）
4. API 对接：
   - POST/PUT/GET /api/v1/parent/jobs
   - GET /api/v1/parent/jobs/mine

### 6.4 IM（会话列表/聊天页）
1. 页面：/chat（会话列表）
2. 页面：/chat/:roomId（消息列表 + 发送框）
3. 体验最低配（Boss 风格“像聊天”）：
   - 会话列表按 activeTime 排序
   - 消息列表区分左右（fromUid==me）
   - 发送后本地插入一条（optimistic）+ 回包纠正

### 6.5 我的（资料编辑 + 退出登录）
1. 页面：/me
2. 表单字段按角色展示：
   - TEACHER：姓名、学历、教学经验、价格、简介（对齐 teacher_profile）
   - STUDENT：姓名、孩子年龄、地址、需求描述、预算（对齐 student_profile）
3. 保存：POST /user/updateUserInfo
4. 退出：清 token 并跳转 /（或 /auth/role）

### 6.6 测试（MVP 必要的“护城河”）
前端已有 vitest/msw 基础设施（见 src/test），建议补齐：
- API client 单测（请求参数、错误处理）
- Pinia store 单测（分页合并、状态恢复）
- 关键页面最小渲染测试（列表空态、错误态）

---

## 7. 后端 Coding 计划（按“产品可用性”优先级）

### 7.1 P0：补齐“获取当前用户”接口
新增 GET /user/me：
- 返回字段：uid、name、avatar、phone、userType(role)、profile 简版（用于前端回显）
- 与 JWT/RequestHolder 绑定（不依赖前端传 phone）

### 7.2 P0：IM 展示信息补齐（二选一，但要定一种）
方案 A（推荐）：改造 IM 的 ChatRoomItemResp / ChatMessageResp
- ChatRoomItemResp 增加 otherUser { uid, name, avatar, userType }
- ChatMessageResp.fromUser 增加 name/avatar（或前端按 uid 统一补齐）

方案 B：预约服务新增用户批量查询接口
- GET /user/batch（ids）返回 uid->name/avatar/userType
- IM 保持最小字段，前端在 /chat 页面二次请求补齐

### 7.3 P0：接口角色准入（硬校验）
实现建议：
- 在 appointment 模块新增 RoleInterceptor（路径前缀 -> 允许 role 列表）
- 或在 JwtInterceptor 里增加“role 白名单映射”
验收标准：
- teacher 调用 /api/v1/parent/jobs POST/PUT 必须 403/NO_AUTH
- student 调用 /api/v1/tutor/services POST/PUT 必须 403/NO_AUTH

### 7.4 P0：需求广场支持 keyword 搜索
在 StudentJobPostingService.listPublished 增加 q（title/description like）与 sort（create_time/热度占位）。

### 7.5 P1：统一返回 DTO（避免直接暴露 Entity）
目前 detail 接口直接返回实体（StudentJobPosting/TeacherJobPosting），建议在 MVP 后半段做 DTO 包装：
- 防止字段变更破坏前端
- 方便补齐“发布者昵称/头像/是否在线”等展示字段

---

## 8. 迭代节奏（把 MVP 拆成可交付的里程碑）

### Milestone A：登录后分流 + 我的资料（“先有身份”）
- 前端：/me + 路由守卫 + 登录态初始化
- 后端：GET /user/me

### Milestone B：学生端发布/编辑需求（“先有供给”）
- 前端：/student/post + /student/jobs/mine + edit
- 后端：角色准入（STUDENT-only）+ 必要字段校验/错误码统一

### Milestone C：教师端需求广场 + 搜索（“能刷列表”）
- 前端：/tutor/jobs + detail
- 后端：feed 支持 q + sort，角色准入（TEACHER-only）

### Milestone D：开聊 + 会话列表 + 聊天页（“像 BOSS 一样沟通”）
- 前端：/chat + /chat/:roomId + 立即沟通链路打通
- 后端：IM 返回补齐对方信息（或 user/batch）确保 UI 可展示

完成 Milestone D 即达成你描述的 MVP 核心能力。

---

## 9. 验收清单（上线前必须逐条过）

### 9.1 功能验收
- 教师：能从需求广场进入任意需求，点击立即沟通，进入聊天页并发送消息
- 学生：能发布需求、在“我的需求”里编辑并保存成功
- 双端：消息页能看到会话列表；进入会话能看到历史消息分页；能持续发送消息
- 我的：资料可回显、可保存；退出登录有效

### 9.2 安全与稳定性（最低限度）
- 未登录访问受限接口统一返回未登录错误码
- 非本角色调用接口返回无权限
- 发送验证码/发消息具备频控（你已实现部分频控，继续沿用）

---

## 10. 本 Plan 的“下一步”输出物（你确认后我再开始执行）
确认本 Plan 后，我会按 Milestone A→D 的顺序输出：
- 需要新增/调整的后端接口清单与字段定义（对齐现有 DTO/表结构）
- 前端页面与组件拆分图（到文件级别）
- 路由/状态/接口的对接方式（含错误态/空态/分页策略）

