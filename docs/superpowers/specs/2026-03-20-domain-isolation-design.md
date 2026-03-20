# 领域隔离设计（基于现有模块，不立即拆微服务）

- 日期：2026-03-20
- 项目：ai-platform（家教直聘）
- 设计目标：在不立即拆分现有服务的前提下，完成领域隔离，提升团队并行开发能力与独立发布能力，并为后续微服务拆分做好边界准备。

## 1. 已确认决策

1. 拆分策略：先做领域隔离，不立即拆服务进程。
2. 数据策略：第一阶段不拆库，继续使用共享库 `ai_tutor`。
3. 流量入口：采用 API Gateway 统一入口。
4. 业务优先级：优先解决团队并行开发和发布独立性。
5. 范围：先基于现有模块边界治理，不做全量重构。

## 2. 当前领域盘点（以现有代码为准）

### 2.1 现有后端模块

- `tutor-appointment-service`：账号/资料、供需发布、预约履约、教师认证、组织账号、部分内部门面。
- `videoCall-IM-service`：会话、消息、已读、申请、合作提案、中介费订单、联系人解锁。
- `payment-service`：支付下单、支付回调、订单状态、支付成功事件投递。
- `ai-tutor-admin`：后台管理聚合接口（审核、用户、机构、支付记录、退款等）。
- `ai-tutor-starter`：单进程聚合启动与本地门面实现（单体阶段桥接）。

### 2.2 现有核心数据域

- 身份与账号：`user`, `teacher_profile`, `student_profile`, `organization_profile`, `organization_account`, `user_settings`
- 供需市场：`student_job_posting`, `teacher_job_posting`, `position_post`, `parent_favorite_tutor`, `tutor_favorite_demand`
- 沟通与申请：`room`, `message`, `room_read_state`, `tutor_application`
- 成交协作：`collaboration_proposal`, `brokerage_order`, `application_brokerage_order`
- 支付：`payment_order`
- 履约：`tutor_appointment`, `tutor_review`
- 运营：`sys_admin_user`

### 2.3 当前主要风险

1. `tutor-appointment-service` 业务面过宽，内部边界较弱。
2. 后台与业务可能出现跨域直接查表倾向，弱化了模块自治。
3. 进程虽可分启，但团队协作仍容易按“功能”而非“领域”修改代码。

## 3. 目标边界（阶段一：不拆服务）

## 3.1 领域与模块归属

| 领域 | 模块归属 | 说明 |
|---|---|---|
| Identity（身份与账号） | tutor-appointment-service | 用户登录、角色、资料、认证、用户设置 |
| Marketplace（供需市场） | tutor-appointment-service | 家长需求、教师服务、收藏、科目目录 |
| Booking（履约） | tutor-appointment-service | 预约创建/确认/改期/取消/完成、日程 |
| Conversation（沟通） | videoCall-IM-service | 会话、消息、已读、流式消息、联系人 |
| Deal（成交协作） | videoCall-IM-service | 申请、合作提案、中介费业务订单、解锁判定 |
| Payment（支付） | payment-service | 支付单、渠道下单、回调验签、支付事件 |
| Ops（运营后台） | ai-tutor-admin | 管理员认证、审核与运营聚合视图 |

## 3.2 强约束（必须执行）

1. 单表单写主责：每张表只能有一个“写入主人模块”。
2. 跨域访问只走内部 API / Facade，不允许跨模块直接写 Mapper。
3. `ai-tutor-common` 仅放通用契约（错误码、事件、接口），不放业务实体实现。
4. 后台 `admin` 作为聚合层，优先调用业务内部接口，不新增跨域直写库逻辑。
5. 事件用于领域状态传播，不承载跨域事务一致性。

## 4. API Gateway 入口设计（阶段一）

统一入口为 Gateway；先做路由归一，不改核心业务语义。

- `/identity/**` -> appointment.identity
- `/market/**` -> appointment.marketplace
- `/booking/**` -> appointment.booking
- `/conversation/**` -> im.conversation
- `/deal/**` -> im.deal
- `/payment/**` -> payment
- `/ops/**` -> admin

说明：
- 阶段一可先通过 Gateway 反向代理到当前服务端口。
- 外部 API 路径可保留兼容映射（新旧并存一段时间），避免前端一次性改造风险。

## 5. 跨域协作契约

## 5.1 同步调用契约

- Identity -> Conversation：用户基础信息查询（已有 `internal/facade/users/*` 方向）
- Deal -> Payment：查询可支付业务订单（`BrokerageOrderFacade`）
- Admin -> Identity/Market：通过内部门面读取待审/用户/机构数据

调用规范：
- 必须携带内部令牌（`X-Internal-Token`）
- 统一 `BaseResponse` 语义与错误码
- 内部接口版本化：`/internal/facade/v1/**`

## 5.2 异步事件契约

- `AppointmentAcceptedEvent`：预约确认后触发会话对齐
- `PaymentSuccessEvent`：支付成功后驱动成交域状态推进

约束：
- 事件发布幂等（业务唯一键 + 去重）
- 消费端失败可重试，且必须可观测（失败原因、重试次数）

## 6. 关键业务数据流（阶段一）

1. 用户注册/登录（Identity）
2. 家长发布需求/老师发布服务（Marketplace）
3. 老师发起申请、双方聊天（Conversation + Deal）
4. 生成中介费业务单并发起支付（Deal -> Payment）
5. 支付回调成功，成交状态更新并解锁后续动作（Payment -> Deal）
6. 达成合作后进入预约履约（Booking）

## 7. 错误处理与可观测性

## 7.1 错误处理

1. 跨域调用失败统一映射为明确错误码（超时、鉴权失败、依赖不可用）。
2. 对外接口不透传内部堆栈，只返回业务可判定信息。
3. 内部接口错误消息需包含 `operation` 上下文，便于排障。

## 7.2 可观测性

1. 所有跨域调用打点：QPS、P95、错误率。
2. 事件链路打点：投递成功率、重试次数、最终失败量。
3. 关键链路告警：支付回调失败、内部 token 鉴权失败激增、消息发送失败激增。

## 8. 测试策略

1. 模块内测试：各模块保留并强化单元测试与集成测试。
2. 契约测试：对 `internal/facade` 增加契约比对，防止字段破坏。
3. 端到端回归：覆盖“登录->发布->沟通->支付->预约”最小闭环。
4. 发布门禁：模块级 CI 独立执行，禁止“改 A 只测 B”。

## 9. 6 周落地计划（按角色分工）

## 9.1 第 1 周：边界定标

- 后端 A（appointment 负责人）
  - 输出 Identity/Marketplace/Booking 包级边界和依赖关系。
  - 编制“表归属与写入主责清单”。
- 后端 B（im/payment/admin 负责人）
  - 输出 Conversation/Deal/Payment/Ops 边界和调用现状图。
  - 整理全部内部 API 清单（路径、入参、返回、调用方）。
- QA
  - 梳理主链路用例矩阵和回归优先级。
- DevOps
  - 设计 Gateway 路由草案与环境变量策略。

交付物：领域归属清单 v1、内部 API 目录 v1、网关路由草案。

## 9.2 第 2 周：appointment 内部隔离

- 后端 A
  - 在 appointment 内按 Identity/Marketplace/Booking 拆分包结构。
  - 移除跨子域直接 Mapper 访问（改为 service/facade）。
- 后端 B
  - 配合修正内部调用路径和 DTO。
- QA
  - 增量回归登录、资料、发帖、预约主流程。
- DevOps
  - 准备网关灰度路由配置。

交付物：appointment 领域内聚改造 PR、回归报告 v1。

## 9.3 第 3 周：IM 内部隔离

- 后端 B
  - 在 IM 内按 Conversation/Deal 拆分包和服务边界。
  - 明确 Deal 依赖 Conversation，禁止反向。
- 后端 A
  - 保持 appointment 内部接口向后兼容并补齐必要字段。
- QA
  - 回归会话、消息、申请、提案与中介费链路。
- DevOps
  - Gateway 接入 conversation/deal 前缀路由。

交付物：IM 领域内聚改造 PR、IM 关键链路回归。

## 9.4 第 4 周：admin/payment 契约收敛

- 后端 B
  - admin 统一经内部门面拿数据，减少跨域直连数据访问。
  - payment 只保留支付域能力，业务订单通过 facade 获取。
- 后端 A
  - 配合提供稳定内部接口和版本号。
- QA
  - 回归后台审核、支付回调、订单查询。
- DevOps
  - 增加内部接口鉴权和限流策略。

交付物：admin/payment 契约化 PR、安全策略清单。

## 9.5 第 5 周：独立发布能力建设

- 后端 A/B
  - 模块级 changelog 与发布说明模板。
  - 内部 API 版本化（`/internal/facade/v1`）。
- QA
  - 模块级冒烟测试集合。
- DevOps
  - 拆分 CI/CD：appointment/im/payment/admin 独立流水线。

交付物：模块独立发布流水线、模块冒烟脚本。

## 9.6 第 6 周：稳定性加固与拆分就绪评估

- 后端 A/B
  - 完成跨域契约测试与告警兜底。
  - 输出可拆服务优先级建议。
- QA
  - 全链路回归与缺陷收敛。
- DevOps
  - 发布监控大盘与告警阈值。

交付物：《微服务拆分就绪报告》与优先级建议。

## 10. 非目标（本阶段不做）

1. 不做物理拆库。
2. 不做一次性全链路重构。
3. 不引入分布式事务中间件。
4. 不追求“服务数量最大化”，优先边界清晰和发布独立。

## 11. 阶段完成判定

满足以下条件即视为“领域隔离阶段完成”：

1. 所有核心表有且仅有一个写入主责模块。
2. 新增功能开发可以明确落在单一领域模块。
3. 四个后端模块（appointment/im/payment/admin）可独立构建、测试、发布。
4. 关键跨域调用具备版本、鉴权、监控、告警。
5. 已产出下一阶段微服务拆分顺序与风险清单。

## 12. 未来微服务拆分映射（后续阶段）

当阶段一达标后，可按以下顺序拆分：

1. `identity-service`（从 appointment 中先拆）
2. `marketplace-service`
3. `conversation-service`
4. `deal-service`
5. `booking-service`
6. `ops-service`
7. `payment-service` 保持独立并继续演进

拆分原则：每次只拆一个高价值领域，优先选择跨团队改动频繁且发布冲突高的模块。
