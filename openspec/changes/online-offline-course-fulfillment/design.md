## Context

当前仓库已经具备以下基础能力，可作为本次方案的复用底座：

- 需求发布、教师沟通、支付解锁聊天已有基础链路
- `videoCall-IM-service` 已承接师生 1v1 房间、消息、首次沟通与聊天状态
- `payment-service` 已具备支付订单、回调、退款等支付域基础能力
- `live-class-service` 已具备直播课会话、参与者、课堂事件等基础模型
- `ai-tutor-web` 已有聊天页、课程页、直播准备页等前端页面基础
- `e2e-tests` 与 `qa/automation` 已具备 Java/Python 两套自动化验证能力

本次变更的重点不是单点功能，而是让“授课形式”成为整个用户旅程的根字段，并据此拆出线下撮合与线上履约两套状态机。

## Goals / Non-Goals

**Goals:**

- 在线下与线上两种授课模式之间建立明确边界，并使用户在关键入口主动选择
- 将“授课形式不可切换”落实为前后端一致的硬约束
- 为线上模式建立长期课程与短期课节模型，支持滚动课表与灵活排课
- 为线上模式建立逐节课支付与教师结算闭环，并将未支付阻塞后续预约/上课
- 为线上模式建立试课判断、退课、课程结束与聊天关闭闭环
- 形成可执行研发计划，要求增量提交、中文注释和端到端测试验收

**Non-Goals:**

- 不在本期支持线上与线下互转、课程履约中途切换模式
- 不在本期支持多人课程、拼课、小班课
- 不在本期实现复杂自动分账清算系统，可先落手动提现或准实时结算
- 不在本期实现完整 AI 学习画像算法，只预留事件与数据沉淀能力

## Decisions

### 1. 授课形式是强约束字段，创建后不可切换

- 决策：
  - `teaching_mode` 将存在于需求、沟通关系、长期课程、课节订单等核心实体
  - 当需求发布完成或学生主动联系教师后，授课形式 MUST 固化为 `ONLINE` 或 `OFFLINE`
  - 后续任何页面只允许查看当前模式说明，MUST NOT 提供切换入口
- 理由：
  - 授课形式直接决定平台责任、收费方式、课表模型与聊天治理，切换会引发历史数据冲突
- 落地：
  - 前端不展示切换控件
  - 后端对更新接口增加防重写校验，返回明确错误码
  - 数据迁移阶段要为历史数据补默认值或兼容空值

### 2. 线下与线上采用双状态机

- 线下：
  - 保持现有信息费模式
  - 平台不承接后续课程、课节、课后支付与质量保障
- 线上：
  - 从“沟通”升级为“长期课程”
  - 所有履约行为围绕 `Course` 与 `Lesson` 展开
- 理由：
  - 只有拆成双状态机，才能同时满足现有撮合业务与未来平台化履约

### 3. 长期课程 Course 与短期课节 Lesson 分层建模

- 决策：
  - `Course` 表示长期教学关系
  - `Lesson` 表示该课程下的单次上课实例
  - 我的课程只展示 `Course`
  - 进入课程详情后再展示 `Lesson` 列表及其操作
- 理由：
  - 避免“课程”和“单节课”混为一个对象，便于做长期评估、排课和支付

### 4. 第一节固定为试课，试课后必须做继续/退课判断

- 决策：
  - 线上课程创建后首节 MUST 标记为 `TRIAL`
  - 试课结束且支付后，学生 MUST 选择“合适，继续上课”或“不合适，退课”
  - 若退课，课程 MUST 结束，未开始课节 MUST 取消，聊天 MUST 关闭或转只读
- 理由：
  - 试课是线上长期课程的关键转化节点，必须有显式状态才能驱动后续履约

### 5. 支付采用“课后支付 + 后续阻塞”的 MVP 方案

- 决策：
  - 每节课完成后生成待支付账单
  - 学生支付上一节课后，才能预约/进入下一节课
  - 平台从教师收入中抽取 10% 服务费，学生端不展示抽成拆分
- 理由：
  - 与当前业务设想一致，上线成本最低
- 风险：
  - 存在最后一节坏账风险
- 预留：
  - 后续可升级到绑卡、预授权或课前冻结

### 6. 教师结算先采用“进入可提现余额”的方案

- 决策：
  - 学生支付成功后，课节订单拆出平台服务费与教师收入
  - 教师收入先进入余额，由教师发起微信提现
- 理由：
  - 比逐笔自动打款更稳，便于对账和异常处理

### 7. 聊天权限由课程状态驱动

- 决策：
  - 线下聊天只受信息费解锁约束
  - 线上聊天受沟通状态、课程状态、退课状态共同约束
  - 退课或课程终止后，聊天 MUST 关闭或只读
- 理由：
  - 聊天已经不是单纯 IM 功能，而是履约权限的一部分

## Domain Model

### 核心实体

- `student_job_posting`
  - 新增 `teaching_mode`
- `tutor_application` / 沟通关系
  - 新增 `teaching_mode`
  - 新增 `online_course_status_snapshot` 便于列表展示
- `course`
  - `course_id`
  - `student_id`
  - `teacher_id`
  - `application_id`
  - `teaching_mode`
  - `course_name`
  - `subject`
  - `goal`
  - `lesson_price_fen`
  - `lesson_duration_minutes`
  - `trial_required`
  - `course_status`
  - `weekly_schedule_rule`
  - `next_lesson_at`
- `lesson`
  - `lesson_id`
  - `course_id`
  - `lesson_type` (`TRIAL` / `NORMAL` / `EXTRA`)
  - `scheduled_start_at`
  - `scheduled_end_at`
  - `lesson_status`
  - `price_fen`
  - `payment_status`
  - `reschedule_source_lesson_id`
- `lesson_payment_order`
  - `payment_order_no`
  - `lesson_id`
  - `student_id`
  - `teacher_id`
  - `total_amount_fen`
  - `platform_fee_rate`
  - `platform_fee_amount_fen`
  - `teacher_income_amount_fen`
  - `payment_status`
- `teacher_settlement`
  - `settlement_id`
  - `teacher_id`
  - `payment_order_no`
  - `settlement_status`
  - `settlement_amount_fen`
- `trial_decision`
  - `course_id`
  - `lesson_id`
  - `decision` (`FIT` / `UNFIT`)
  - `reason`
- `chat_permission_snapshot`
  - `room_id`
  - `permission_status`
  - `closed_reason`

## State Machines

### 1. 授课形式分流状态

- `UNSELECTED`
- `ONLINE_SELECTED`
- `OFFLINE_SELECTED`

约束：

- 仅在需求发布前或首次联系教师前允许从 `UNSELECTED` 进入某一模式
- 一旦进入 `ONLINE_SELECTED` 或 `OFFLINE_SELECTED`，MUST NOT 再回到未选择，也 MUST NOT 在两者之间切换

### 2. 线上长期课程状态

- `PENDING_CONFIRMATION`
- `TRIAL_PENDING`
- `TRIAL_IN_PROGRESS`
- `TRIAL_DONE_WAIT_PAY`
- `TRIAL_DONE_WAIT_DECISION`
- `ACTIVE`
- `PAUSED`
- `FINISHED`
- `DROPPED`

### 3. 线上课节状态

- `PENDING`
- `CONFIRMED`
- `RESCHEDULE_PENDING`
- `CANCELLED`
- `IN_CLASS`
- `COMPLETED_WAIT_PAY`
- `PAID`
- `SETTLED`
- `ABSENT_STUDENT`
- `ABSENT_TEACHER`
- `DISPUTE`

## Execution Plan

本次研发按四个模块拆分，建议每个模块独立开发、独立提交、独立测试，避免大批量混改。

### 模块一：授课形式分流

- 目标：
  - 发布需求、联系教师时必须选择 `线上/线下`
  - 已选授课形式不可切换
- 涉及：
  - `ai-tutor-web`
  - `tutor-appointment-service`
  - `videoCall-IM-service`
  - SQL 迁移
- 提交建议：
  - 单独一个 Git commit
  - 提交说明使用中文，例如：`feat: 新增授课形式分流与不可切换约束`
- 中文注释要求：
  - 关键状态机字段、校验逻辑、错误码处增加中文注释
  - 页面上的业务判断分支使用简短中文注释说明“为何不能切换”

### 模块二：线上课程/课节系统

- 目标：
  - 建立长期课程与短期课节体系
  - 支持试课、滚动课表、加课、删课、调课
- 涉及：
  - `videoCall-IM-service`
  - `live-class-service`
  - `ai-tutor-web`
  - SQL 迁移
- 提交建议：
  - 单独一个 Git commit
  - 提交说明使用中文，例如：`feat: 新增长期课程与课节排课体系`
- 中文注释要求：
  - 课程状态推进、课节滚动生成、调课覆盖规则增加中文注释

### 模块三：课后支付结算

- 目标：
  - 每节课完成后生成待支付订单
  - 支付上一节课后才能预约/进入下一节
  - 教师收入进入可提现余额
- 涉及：
  - `payment-service`
  - `videoCall-IM-service`
  - `live-class-service`
  - `ai-tutor-web`
- 提交建议：
  - 单独一个 Git commit
  - 提交说明使用中文，例如：`feat: 新增线上课后支付与教师结算闭环`
- 中文注释要求：
  - 订单状态推进、10% 服务费计算、阻塞规则加中文注释

### 模块四：试课退课与聊天权限

- 目标：
  - 试课后学生必须选择继续或退课
  - 退课后课程终止、课节取消、聊天关闭
- 涉及：
  - `videoCall-IM-service`
  - `ai-tutor-web`
  - `ai-tutor-admin`
  - `payment-service`（若涉及试课费用退款）
- 提交建议：
  - 单独一个 Git commit
  - 提交说明使用中文，例如：`feat: 新增试课退课闭环与聊天权限治理`
- 中文注释要求：
  - 聊天关闭原因、试课判定、退课后的状态回收增加中文注释

## Test Strategy

### 单元测试

- 授课形式不可切换校验
- 课程/课节状态机推进
- 课节滚动生成与调课逻辑
- 服务费计算与结算金额计算
- 试课继续/退课判定逻辑
- 聊天关闭幂等逻辑

### 集成测试

- 线上沟通后创建课程并自动生成试课
- 课节完成后生成待支付订单
- 支付成功后下一节恢复可预约/可进入
- 退课后未开始课节取消且聊天只读/关闭

### 端到端全流程测试

至少覆盖以下 3 条主链路：

1. 线上继续链路
   - 学生发布需求选择线上
   - 教师接单并沟通
   - 创建课程
   - 生成试课
   - 试课完成
   - 学生支付试课
   - 学生确认“合适”
   - 下一节课可预约/可进入

2. 线上退课链路
   - 学生主动联系教师并选择线上
   - 创建课程
   - 试课完成
   - 学生支付试课
   - 学生确认“不合适”
   - 课程结束
   - 后续课节全部取消
   - 聊天关闭

3. 课后未支付阻塞链路
   - 线上课程正常创建
   - 第一节课完成但学生未支付
   - 学生尝试预约下一节失败
   - 学生尝试进入下一节课堂失败
   - 学生完成支付
   - 下一节恢复可预约/可进入

### 测试落地建议

- Java 侧在 `e2e-tests` 补充跨服务 E2E
- Python 侧在 `qa/automation/tests/e2e` 增加面向前台的 UI/API 混合验证
- 若本地环境不稳定，至少要保证一条服务级 E2E 和一条前端自动化跑通

## Risks / Trade-offs

- 风险：历史数据没有授课形式字段
  - 应对：迁移脚本为历史记录补默认值，并在前端对历史记录展示“旧版模式”
- 风险：课后支付模式存在最后一节坏账
  - 应对：MVP 先用阻塞后续课节，后续预留绑卡/预授权
- 风险：课程、课节、直播、支付跨服务事件复杂
  - 应对：优先采用事件驱动 + 幂等更新，所有状态推进节点保留操作日志
- 风险：聊天权限与课程状态双向依赖，容易出现脏状态
  - 应对：由统一应用服务负责关闭聊天与回收课节，前端仅展示结果

## Open Questions

- 试课是否允许单独定价，还是默认与正式课同价
- 课后支付是否要设置超时自动催付或自动关闭课程
- 教师提现是人工审核还是自动打款
- 退课后是否允许学生再次向同一教师发起新的线上沟通
