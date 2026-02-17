# [OPEN] 调试记录：需求页编译报错 + 首页热门需求异常 + 角色首页模块显示规则 + 端到端闭环

## 症状
- 进入“需求”页面时报 Vite/Vue 编译错误：Unclosed block，定位到 TutorJobsPage.vue 的 style 区域
- 首页“热门需求”模块异常（用户截图：列表为空/展示异常）
- 额外需求：教师登录隐藏“热门服务/推荐教师”，学生登录隐藏“热门需求”，未登录维持原样
- 需要跑通端到端：学生发布需求 → 教师筛选需求 → 发起沟通 → 消息双栏打开会话
- 新增：同一手机号教师端登录过后，再登录学生端时报 MyBatis ReflectionException（StudentProfile 缺少 school getter）
- 新增：需要支持同手机号双端（数据隔离），切换端需弹窗确认；头像下拉也要提供“切换端”
- 新增：需求页改成左右双栏（左列表右详情，默认选中第一个）

## 假设（可证伪）
1. TutorJobsPage.vue 的 `<style scoped>` 存在缺失的 `}` / `</style>`，导致 PostCSS 解析报 Unclosed block。
2. 首页热门需求异常是因为登录态下走了不同 UI 分支/参数（如 city、tabId、page cursor），导致请求参数不正确或响应被当成空列表。
3. 首页模块显示规则需要基于 auth store 的 userType，但当前首页/顶栏对登录态判断与路由框架切换导致渲染分支错位。
4. 端到端闭环失败点可能在：后端 DB 未迁移新增字段/收藏表导致接口 500；或前端 feed/发布参数与后端字段不一致导致发布/筛选无数据。
5. 消息双栏嵌套路由后，chatRoom 页面的返回/选中态与 query(otherUid) 传递存在差异，导致会话打开失败或信息缺失。
6. StudentProfile 实体字段与 StudentProfileMapper.xml 不一致（mapper 插入 school/grade/parent_contact，但实体缺字段），导致首次创建学生资料失败。
7. 当前后端把 user.userType 当作“唯一端”，切换端时会覆盖，需确保“资料表按 userId 分端创建”并且鉴权完全以 JWT role 为准。
8. 前端缺少“切换端”交互：在登录页/头像菜单触发切换时未清理当前 token 或未提示确认，导致端身份混乱。

## 证据收集计划
1. 静态检查 TutorJobsPage.vue：确认 style block 的闭合情况并定位缺失点（不改业务逻辑）。
2. 启动前后端并复现：
   - 教师登录 → /tutor/jobs
   - 学生登录 → /student/post 发布需求
   - 教师筛选 → 立即沟通 → /chat/:roomId
3. 对关键请求/响应做最小侵入的埋点上报（通过 Debug Server），用于确认参数/响应结构与渲染分支。

## 状态
- 当前：[OPEN]

## 已执行（本次提交内）
- 修复 TutorJobsPage.vue 样式块未闭合导致的 Unclosed block 编译失败
- 首页热门区块按角色隐藏：
  - 教师：隐藏“热门服务”“推荐老师”，保留“热门需求”
  - 学生：隐藏“热门需求”，保留“热门服务”“推荐老师”
  - 未登录：全部显示
- 后端文档补充数据库升级 SQL（旧库需要 ALTER/新表）
- 待处理：双端切换弹窗、StudentProfile 映射修复、需求页双栏改造、端到端联调验证
