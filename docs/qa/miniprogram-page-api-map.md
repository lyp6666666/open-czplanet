# 微信小程序页面 / 接口映射表

更新时间：2026-05-01

## 学生端 / 教师端共用主链路

| 页面 | 角色 | 主要动作 | 真实接口 |
| --- | --- | --- | --- |
| 合作详情 `src/pages/course/detail.vue` | 学生 / 教师 | 拉取课程详情 | `GET /courses/{courseId}` |
| 合作详情 `src/pages/course/detail.vue` | 学生 / 教师 | 拉取课节列表 | `GET /api/v1/schedule/courses/{courseId}/events` |
| 合作详情 `src/pages/course/detail.vue` | 学生 / 教师 | 拉取直播课状态 | `GET /live/sessions/by-course/{courseId}` |
| 合作详情 `src/pages/course/detail.vue` | 教师 | 提交试课结果 | `POST /courses/{courseId}/trial-result` |
| 合作详情 `src/pages/course/detail.vue` | 学生 | 申请试课退款 | `POST /courses/{courseId}/trial-refund/apply` |
| 合作详情 `src/pages/course/detail.vue` | 学生 | 提交正式课表 | `POST /api/v1/schedule/courses/{courseId}/weekly-schedule` |
| 合作详情 `src/pages/course/detail.vue` | 教师 | 新增课节 | `POST /api/v1/schedule/events` |
| 单节课详情 `src/pages/course/lesson-detail.vue` | 学生 / 教师 | 拉取课程详情并匹配当前课节 | `GET /courses/{courseId}` + `GET /api/v1/schedule/courses/{courseId}/events` |
| 单节课详情 `src/pages/course/lesson-detail.vue` | 学生 | 确认 / 拒绝课节 | `POST /api/v1/schedule/events/{eventId}/response` |
| 单节课详情 `src/pages/course/lesson-detail.vue` | 教师 / 学生 | 取消课节 | `POST /api/v1/schedule/events/{eventId}/cancel` |
| 单节课详情 `src/pages/course/lesson-detail.vue` | 教师 | 发起调课 | `POST /appointment/{appointmentId}/reschedule` |
| 单节课详情 `src/pages/course/lesson-detail.vue` | 学生 | 确认改期 | `POST /appointment/{appointmentId}/confirmReschedule` |
| 单节课详情 `src/pages/course/lesson-detail.vue` | 教师 | 标记结课 | `POST /appointment/{appointmentId}/complete` |
| 课程表 `src/pages/schedule/index.vue` | 学生 / 教师 | 拉取日历事件 | `GET /api/v1/schedule/events?startAt=&endAt=&includePending=` |
| AI 总结 `src/pages/course/ai-summary.vue` | 学生 / 教师 | 拉取课后总结 | `GET /live/sessions/{sessionId}/ai/result` |

## 真实课堂承接链路

| 页面 | 角色 | 主要动作 | 真实接口 |
| --- | --- | --- | --- |
| 课堂准备页 `src/pages/live/prepare.vue` | 学生 / 教师 | 拉取课程维度课堂状态 | `GET /live/sessions/by-course/{courseId}` |
| 课堂准备页 `src/pages/live/prepare.vue` | 学生 / 教师 | 申请进入课堂 token | `POST /live/sessions/by-course/{courseId}/prepare` |
| 课堂发射页 `src/pages/live/launch.vue` | 学生 / 教师 | 承接 prepare 返回的 `joinToken/serverUrl` | 无新增接口，消费 prepare 结果 |
| 课堂 webview `src/pages/live/webview.vue` | 学生 / 教师 | 打开 H5 bridge/classroom | 无新增接口，小程序透传参数到 Web |
| H5 bridge `ai-tutor-web/src/pages/live/LiveMpBridgePage.vue` | 学生 / 教师 | 优先使用小程序已签发 token | 若 query 已带 token，不再二次签发 |
| H5 classroom `ai-tutor-web/src/pages/live/LiveClassroomPage.vue` | 学生 / 教师 | 缺 token 时补签发 | `POST /live/sessions/by-course/{courseId}/prepare` |

## 117 测试环境实测说明

### 可直接验证

- 后门教师登录：`POST /user/loginOrRegister`
  - `phone=29999999999`
  - `code=1886`
  - `userRoleEnum=TEACHER`
- 后门学生登录：`POST /user/loginOrRegister`
  - `phone=19999999999`
  - `code=1668`
  - `userRoleEnum=STUDENT`

### 当前环境缺口

2026-05-01 对 `http://117.72.111.39:18080` 的实测结果：

- 后门教师 / 学生账号能登录，并能返回 `user/me`
- 但 `/courses/my` 为空，`/api/v1/schedule/events` 为空
- 课程 smoke 依赖的 QA seed 链路当前未命中：
  - `GET /courses/982001` -> `40400 / 课程不存在`
  - `GET /live/sessions/by-course/982001` -> `40400 / 未找到课堂`
  - `GET /live/sessions/984002/ai/result` -> `40400 / 未找到课堂`

结论：

- 小程序前端课程 / 课节 / 课堂承接代码已对接真实接口；
- 117 当前还能验证“后门登录 + 课堂支付联调基础账号存在”；
- 但要完成课程/课节/AI 总结的真实闭环，仍需测试环境重新导入 QA 课程 seed，或提供一套可操作的真实课程账号与业务 ID。
