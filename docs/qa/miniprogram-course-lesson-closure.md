# 微信小程序课程 / 课节闭环收口记录

更新时间：2026-05-01

## 当前实现状态

### 已完成的前端动作

- 合作详情：
  - 查看合作详情
  - 查看合作下课节列表
  - 确认/拒绝待确认课节
  - 提交试课结果
  - 提交正式课表
  - 申请试课退款
  - 新增课节
- 单节课详情：
  - 确认/拒绝课节
  - 取消课节
  - 发起调课
  - 确认改期
  - 标记结课
  - 进入课堂准备页
  - 进入课后 AI 总结
- 课程表：
  - 今天 / 本周 / 本月视图
  - 点击课节进入详情
- 课堂承接：
  - `prepare -> joinToken -> launch -> webview`
  - 返回后自动刷新合作详情 / 课节详情 / 课程表

### 本轮收口调整

- 课程/课节动作成功后统一重新拉取上下文：
  - 不再只乐观更新单条课节
  - 改为重新同步课程状态、课节列表、课堂状态
- 调课前端校验补充：
  - 必填改期日期
  - 结束时间必须晚于开始时间
- 已新增 QA 自动化骨架：
  - `qa/automation/api/course_client.py`
  - `qa/automation/api/schedule_client.py`
  - `qa/automation/api/appointment_client.py`
  - `qa/automation/api/live_client.py`
  - `qa/automation/tests/api/test_course_lesson_smoke.py`

## 尚未完成的真实闭环

以下项目“前端已实现，但尚未完成真实环境验收记录”：

- 确认课节
- 拒绝课节
- 提交试课结果
- 提交正式课表
- 新增课节
- 发起调课
- 确认改期
- 取消课节
- 标记结课
- 课后 AI 总结状态切换

## 当前阻塞

### 1. 自动化资产不足

现已补充课程/课节自动化骨架，并已在当前工作机完成基础环境搭建与收集校验。

现状：

- `python3 -m py_compile` 已通过
- 已创建 `qa/automation/.venv`
- 已安装 `pytest` / `requests` / `playwright` 等依赖
- `pytest --collect-only tests/api/test_course_lesson_smoke.py` 已通过，共收集 7 条用例

因此当前结论是：

- 自动化代码已落仓且可执行
- 下一步重点从“能不能跑”转为“是否拿到可登录验证码与可用 QA 数据”

### 2. 测试环境数据依赖

课程/课节动作的真实验证需要至少具备：

- 已生成的课程 ID
- 可操作的课节 ID
- 可调课 / 可结课 / 可提交试课结果的课程状态

目前仓库内已有 QA 地图和账号，但还缺一份“面向小程序课程闭环”的具体数据清单。

已确认的现成 QA seed：

- 课程：`982001`
- 教师：`910103 / 18611721003`
- 学生：`910003 / 18611720003`
- 已确认试听课：`983001`
- 已完成课节：`983002`
- 已结束课堂：`984002`

### 3. 当前测试环境阻塞

117 测试机当前状态：

- `GET /actuator/health` 返回 `200`
- `POST /user/sendcode` 返回 `200`
- `GET /internal/debug/sms-code` 返回 `404`
- 本地后门教师 `29999999999 / 1886`、学生 `19999999999 / 1668` 均可登录成功
- 但后门账号下：
  - `GET /courses/my` 返回空数组
  - `GET /api/v1/schedule/events` 返回空数组
- 课程 smoke 依赖的 QA seed 链路在 117 当前未命中：
  - JWT 直签 `910103 / 18611721003`、`910003 / 18611720003` 后
  - `GET /user/me` 返回 `40400 / 请求数据不存在`
  - `GET /courses/982001` 返回 `40400 / 课程不存在`
  - `GET /api/v1/schedule/courses/982001/events` 返回空数组
  - `GET /live/sessions/by-course/982001` 返回 `40400 / 未找到课堂`
  - `GET /live/sessions/984002/ai/result` 返回 `40400 / 未找到课堂`

因此当前自动化登录需要两种方式二选一：

1. 测试环境补开 debug 短信读取接口
2. 使用 `QA_SMS_CODE=<手工收到的验证码>` 运行

但即使登录问题绕过，课程 / 课节闭环当前仍会被“117 未导入课程 seed”阻塞。

## 本轮真实执行结果

### 自动化执行

- 已执行：
  - `cd qa/automation && .venv/bin/python -m pytest --collect-only tests/api/test_course_lesson_smoke.py`
  - 结果：成功收集 7 条用例
- 已执行：
  - `QA_LOGIN_MODE=jwt` + `QA_JWT_SECRET=LypJwtSecretKey123LypJwtSecretKey123`
  - 直接对 117 跑 `tests/api/test_course_lesson_smoke.py`
- 初次结果：
  - 6 failed / 1 skipped
  - 根因均指向 `982001` 课程链路不存在
- 已调整自动化：
  - 在 `test_course_lesson_smoke.py` 中增加课程 seed 探测
  - 当 QA 环境缺少课程 seed 时，用 `skip` 明确收口，而不是误报脚本失败

### 当前结论

- 小程序端课程 / 课节 / 真实课堂承接代码：已实现并完成本地类型检查、微信构建
- QA 自动化脚本：已落仓，可直接复跑
- 117 测试环境：当前只能验证后门账号登录；课程 / 课节 / AI 总结闭环缺少可操作 seed，未形成真实业务闭环

## 下一步建议执行顺序

1. 在 QA 自动化环境安装依赖后执行 `tests/api/test_course_lesson_smoke.py`
2. 用 QA 账号跑一轮课程动作闭环
3. 回填 `miniprogram-test-record-template.md`
4. 将失败项归因到前端 / 后端 / 配置 / 数据
