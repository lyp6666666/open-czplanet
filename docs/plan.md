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

---

## 11. 需求广场（BOSS 双栏）V2：信息密度提升 + 强校验 + 更真实数据（本次变更专用超详细 Plan）

### 11.1 变更背景（为什么要做）
当前教师端“需求广场”已具备“双栏形态”，但信息表达更像“卡片列表”，不够 BOSS：
- 关键筛选维度（地点/授课方式/授课频次/学历要求）分散，不能一眼判断是否匹配
- 详情页缺少“发布者信息”（头像/名字/身份），无法建立信任与沟通动机
- 发布端字段约束不足（例如线下却不填地址），会导致教师端筛选与沟通体验劣化
- 测试数据偏“演示级”，真实度不足导致体验评审与自测成本高

### 11.2 产品目标（做到什么程度算成功）
**目标 A：信息架构对齐 BOSS**
- 需求详情信息分层与密度接近 BOSS：标题下“关键条件一行看懂”，下面再看描述与发布者
- 线下需求必须明确“工作地址”，线上需求不展示地址区块，避免空白
- 详情区块高度随内容自适应：没有大块留白；短内容时只剩背景/容器，不出现“为了凑高度而留空”的空段

**目标 B：数据质量可控**
- 学生端发布需求：关键字段不允许缺失（前端强校验 + 后端强校验）
- 线下必填地址（至少 city + address），否则无法发布
- 引入“授课频次（每周几次）”的结构化字段，支持展示与筛选
- 引入“发布者身份（学生家长/学生本人）”字段，支持展示

**目标 C：可直接联调自测**
- 提供一批“真实风格”的学生用户与需求数据，且 user/profile/job_posting 绑定关系真实可用
- 测试数据覆盖线上/线下/多城市/多学段/多预算/多频次/多学历要求

### 11.3 非目标（本次不做，避免范围爆炸）
- 不做复杂地图/距离筛选、LBS 地理编码
- 不做“发布者在线状态/最后活跃”与更多社交指标
- 不做推荐算法/热度排序（仅保留 latest 或占位）

---

### 11.4 新的信息架构与交互（对标 BOSS）

#### 11.4.1 详情区（右侧）展示顺序（必须按此层级）
1) **标题（岗位名称）**
2) **标题下关键信息行（紧凑展示）**
   - 地点（线上：显示“线上”；线下/可线下：显示城市）
   - 授课方式（线上/线下/线上+线下）
   - 授课频次（例如“每周 2 次”）
   - 学历要求（例如“本科/211/985/不限”）
3) **需求描述**
4) **发布者信息区**
   - 头像 + 名字
   - 身份标签（“学生家长 / 学生本人”）
5) **工作地址区块（仅线下/可线下展示）**
   - 展示 address（优先展示“city + address”）
   - 若 address 较长，允许换行；若缺失（理论上不会缺），展示“—”但同时在日志/监控中提示数据质量

#### 11.4.2 左侧列表卡片（轻量但“像 BOSS”）
建议左侧卡片结构（信息密度更高但仍可扫）：
- 第一行：标题（左） + 预算（右，红色强调）
- 第二行：地点 / 授课方式 / 每周几次 / 学历要求（与右侧一致）
- 第三行：描述摘要（最多 2 行）
选中态：边框高亮 + 背景浅主色（已实现类似效果，V2 做更贴近 BOSS 的行内布局）

#### 11.4.3 空白与高度策略（避免“大块空白”）
- 右侧详情内部按“区块是否有内容”决定是否渲染，不渲染就不占高度
- 右侧容器可以设置 `min-height` 以保证视觉稳定，但内容区块不应通过 padding/margin 造空白
- 短内容时：详情卡片结束后自然留背景，不加虚假占位区块

---

### 11.5 数据模型/数据库改造（必须对齐业务）

#### 11.5.1 student_job_posting 新增字段（满足展示 + 筛选）
新增两列：
- `frequency_per_week`（int）：授课频次（每周几次）
- `publisher_identity`（varchar(16)）：发布者身份枚举：`PARENT`（学生家长）、`STUDENT_SELF`（学生本人）

DDL（兼容老数据的策略）：
1) 先新增列并给默认值，保证历史数据可读可展示
2) 后端发布接口强校验，确保新数据不再缺失

示例 DDL（MySQL）：
```sql
ALTER TABLE student_job_posting
  ADD COLUMN frequency_per_week int NOT NULL DEFAULT 2 COMMENT '授课频次（每周几次）',
  ADD COLUMN publisher_identity varchar(16) NOT NULL DEFAULT 'PARENT' COMMENT '发布者身份：PARENT/ STUDENT_SELF';
CREATE INDEX idx_frequency_per_week ON student_job_posting (frequency_per_week);
CREATE INDEX idx_publisher_identity ON student_job_posting (publisher_identity);
```

#### 11.5.2 student_profile（可选增强，不阻塞本次）
现阶段发布者身份以 `student_job_posting.publisher_identity` 为准，避免“个人资料与发布时身份不一致”的歧义。
若未来要做“档案级身份”，可在 `student_profile` 增加默认身份，但不是本次必须项。

---

### 11.6 后端改造方案（接口/校验/返回字段）

#### 11.6.1 发布/编辑强校验（Create/Update）
校验规则（前后端一致）：
- 必填：`subjectId/title/description/classMode/frequencyPerWeek/stageCode`
- `educationRequirement` 必须显式选择（可以选择“不限”，后端可落库为 `null` 或 `UNLIMITED`，但前端必须有选择行为）
- 预算规则：允许不填；若填必须满足 `budgetMin <= budgetMax` 且均为正数
- 授课方式相关：
  - classMode = online：`city/address` 不要求（可留空）
  - classMode = offline 或 both：`city` 必填，`address` 必填（本次按你要求强制）

实现方式：
- DTO 上用 `jakarta.validation` 做基础非空（例如 `@NotNull`/`@NotBlank`）
- Service 层做“条件必填”校验（offline/both 时必填地址）+ 预算区间校验
- 错误码：统一走 `PARAMS_ERROR` 并给出可读提示（前端直接展示）

#### 11.6.2 feed 筛选增强（教师端用）
在现有 `GET /api/v1/parent/jobs/feed` 基础上：
- 新增可选筛选参数：`frequencyPerWeek`（整型）
- SQL 条件：`frequency_per_week = #{frequencyPerWeek}`（可扩展为区间，但 MVP 先做等值）

#### 11.6.3 需求详情返回“发布者信息”
为满足右侧详情“头像/名字/身份”展示，需要返回发布者 summary：
- `publisher: { uid, displayName, avatar, identityLabel }`
- `identityLabel` 由 `publisher_identity` 映射：`PARENT -> 学生家长`，`STUDENT_SELF -> 学生本人`

接口策略（两种可选，建议选 A）：
- A（推荐，低风险）：新增教师端专用详情接口 `GET /api/v1/parent/jobs/{id}/view` 返回 `DemandViewVO`
  - 学生端编辑仍用原 `GET /api/v1/parent/jobs/{id}`（只返回 posting 本身）
  - 教师端列表右侧详情与详情页都切到 `/view`
- B（改动更大）：把原 detail 接口直接升级为 VO（会影响学生端编辑回显与前端 types）

---

### 11.7 前端改造方案（页面/组件/校验/展示）

#### 11.7.1 教师端需求广场（/tutor/jobs）
UI 改造点：
- 左侧列表卡片信息结构按 11.4.2 调整（标题+预算、条件行、描述摘要）
- 右侧详情按 11.4.1 调整（标题下条件行、描述、发布者信息、线下地址区块）
- 区块渲染遵循“有内容才渲染”，避免空白
- 若详情请求失败：右侧展示错误提示但不影响左侧滚动与选中态

数据对接：
- 左侧 feed 仍用 `feedDemands`
- 右侧详情切换到 `getDemandView`（新增 API）

#### 11.7.2 教师端独立详情页（/tutor/jobs/:id）
目标：与右侧详情一致（复用同一组件/同一展示结构），避免两个地方维护两套 UI。
方案：抽一个 `DemandDetailPanel` 组件（既可在右侧用，也可在详情页单独用）。

#### 11.7.3 学生端发布需求（/student/post）强校验与字段补齐
新增/强化字段：
- 授课方式（必选）：online/offline/both（已存在但需改为必选）
- 授课频次（必选）：每周 1~7 次（建议下拉）
- 学历要求（必选但允许选“不限”）
- 学段（必选）：PRESCHOOL/PRIMARY/JUNIOR/SENIOR/OTHER（已存在但需改为必选）
- 线下地址（条件必填）：当选择 offline/both 时必须填 city + address
- 发布者身份（必选）：学生家长 / 学生本人（写入 publisherIdentity）

交互规则：
- 当授课方式从 offline/both 切到 online：自动清空 city/address（避免误展示）
- 当从 online 切到 offline/both：立即提示填写地址，并在提交时阻断

#### 11.7.4 学生端编辑需求（/student/jobs/:id/edit）
需要回显并可编辑新增字段：
- 授课频次、发布者身份
同时保持原字段兼容，编辑校验规则与发布一致。

---

### 11.8 更真实的测试数据（可直接导入）

目标：让你无需手工造数据就能评审“像不像 BOSS”。

数据设计原则：
- 城市覆盖：北京/上海/广州/深圳/杭州
- 学段覆盖：小学/初中/高中/兴趣
- 授课方式覆盖：线上/线下/线上+线下
- 频次覆盖：每周 1/2/3/5 次
- 学历要求覆盖：不限/本科/211/985/海归/QS50
- 描述“像真实家长/学生写的”，包含痛点、目标、时间偏好、孩子情况

交付形式：
- 新增一个 `sqlDoc/seed_dev_data.sql`（可反复执行、幂等），包含：
  - `user`：至少 12 个学生端用户（含手机号/昵称/头像）+ 6 个教师端用户
  - `student_profile/teacher_profile`：与 userId 一一对应
  - `student_job_posting`：至少 30 条需求贴，parent_id 指向真实学生用户
  - （可选）`room/message`：预置 2~3 个会话与几条消息，用于 chat 列表演示

---

### 11.9 测试策略（必须覆盖：单测 + 联调 + 关键回归）

#### 11.9.1 后端单测
- 发布校验：
  - offline/both 缺 city/address 返回 PARAMS_ERROR
  - frequencyPerWeek 缺失/越界返回 PARAMS_ERROR
  - budgetMin > budgetMax 返回 PARAMS_ERROR
- feed 筛选：
  - frequencyPerWeek 过滤生效
- detail view：
  - 返回 publisher.displayName/avatar/identityLabel 不为空（至少 name 有兜底）

#### 11.9.2 前端单测（vitest）
- 发布页校验：offline/both 不填地址按钮禁用/提示出现
- 详情区渲染：线上不展示地址区块；线下展示地址区块
- 关键字段展示：地点/方式/频次/学历要求在标题下出现且顺序正确

#### 11.9.3 手工联调验收（按角色）
学生端：
- 选择线下/可线下 → 不填地址无法发布
- 发布成功后在“我的需求”里能回显并可编辑新增字段
教师端：
- 进入需求广场默认选中第一条，右侧详情即时渲染
- 选中不同需求，右侧无闪屏/选中态正确
- 详情点击“立即沟通”可创建会话并进入聊天页

#### 11.9.4 回归点（防止旧功能被破坏）
- 收藏/取消收藏仍可用
- 搜索/筛选仍可用（不因新增字段导致空结果异常）
- 双端切换弹窗逻辑仍可用（退出后登录新端）
