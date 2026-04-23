## Context

当前项目是多模块、多服务架构，关键链路横跨：

- `ai-tutor-gateway`
- `tutor-appointment-service`
- `videoCall-IM-service`
- `payment-service`
- `ai-tutor-admin`
- `live-class-service`
- `ai-agent-service`

仓库现状表明已经有一定观测基础：

- 本地 `docker-compose` 已包含 `prometheus` 与 `grafana`
- 已有业务 KPI dashboard：`Dockerfile/grafana/dashboards/ai-tutor-kpi.json`
- `ai-tutor-common` 中已有统一业务打点封装：`BizKpiMetrics`

但现状仍存在明显缺口：

1. 采集覆盖不完整  
   当前 Prometheus 仅抓取 `18081/18082/18084`，未覆盖网关、支付、直播等关键入口和资金链路。

2. 看板结构偏业务、缺生产值班视角  
   现有看板主要关注注册、沟通、支付、退款等业务 KPI，缺少错误率、延迟、资源饱和度和依赖异常面板。

3. 告警机制缺失  
   尚未形成“阈值 -> 通知 -> 值班响应”的闭环。

4. 日志与链路关联薄弱  
   当前仓库未见统一 requestId / traceId 方案，也未见集中日志体系。

## Goals / Non-Goals

**Goals**

- 让每个关键服务都具备统一的 Prometheus 暴露能力。
- 让团队能在 1 分钟内发现故障，在 5 分钟内完成初步归因。
- 让 Grafana 从“业务展示板”升级为“运行态看板 + 业务看板 + 值班入口”。
- 让关键问题可以通过“告警 -> 指标 -> 日志 -> trace/requestId”串起来定位。
- 让支付、退款、聊天、直播等高风险链路具备专项监控与验收口径。

**Non-Goals**

- 不在本次内完成全量 OpenTelemetry 深度治理。
- 不在本次内建设复杂多租户可观测平台。

## Decisions

### 1. 观测体系分四层落地

**Decision**

采用四层观测模型推进：

1. 指标层：Prometheus + Micrometer + Exporters
2. 告警层：Alertmanager 或 Grafana Alerting
3. 日志层：Loki + Promtail 或等价集中日志方案
4. 关联层：requestId / traceId 贯穿网关与下游服务

**Rationale**

只做 Grafana 看板无法形成生产闭环。P0 的关键不是“能看到图”，而是“能主动发现并快速定位”。

### 2. 先补全指标暴露，再扩告警和日志

**Decision**

实施顺序固定为：

1. 应用暴露与抓取
2. 基础技术看板
3. 告警规则
4. 日志集中化
5. requestId / traceId 贯通
6. 验收与演练

**Rationale**

如果没有统一采集基线，后续看板、告警、日志关联都会碎片化。

### 3. 业务与技术看板拆分

**Decision**

Grafana 至少拆为三类 dashboard：

- `AI Tutor - Service Overview`
- `AI Tutor - Infra & Dependency`
- `AI Tutor - Business KPI`

**Rationale**

业务方关注成交、退款、注册；研发和值班人员首先关注错误率、延迟、依赖健康、资源饱和。混在一张图里会降低排障效率。

### 4. 本期告警以高价值、低噪音为准

**Decision**

P0 告警优先覆盖：

- 服务不可用
- 网关 / 支付 / IM / 直播关键接口错误率突增
- 接口 P95/P99 延迟异常
- JVM 堆内存、GC、线程池、连接池异常
- MySQL / Redis / RabbitMQ 不可用或积压
- 退款、支付成功率、回调堆积、消息堆积异常

暂不在 P0 引入海量低价值告警。

**Rationale**

观测体系最怕一开始就“告警风暴”。P0 先保障真问题能被叫醒，再逐步细化。

## Detailed Implementation Path

### Phase 0: 现状收口与命名规范

目标：先统一后续所有观测资产的命名和标签，避免越做越乱。

实现路径：

- 确认服务清单、端口、职责、owner、值班等级
- 统一 Prometheus job 命名：`gateway / appointment / im / payment / admin / live-class / ai-agent`
- 统一标签：`service`、`env`、`instance`、`cluster`
- 统一 dashboard 命名、文件路径、变量规范
- 统一告警严重级别：`P1 / P2 / P3`
- 统一日志字段：`timestamp`、`level`、`service`、`traceId`、`requestId`、`uid`、`path`
- 冻结业务指标设计模板：`metric`、`type`、`labels`、`owner service`、`trigger point`、`PromQL`、`dashboard`、`alert`

效果说明：

- 后续所有看板、告警、日志查询都可以跨服务复用。
- 避免同一种问题在不同服务里用不同名字、不同标签导致查询不可维护。

## Business Metrics Plan

这一节将业务指标从“宽泛概念”细化为“对应状态机中的具体打点点位”。P0 只采能直接支撑经营判断、闭环漏斗和线上异常发现的指标。

### 1. 注册与激活漏斗

目标：判断用户是否只是拿到验证码，还是已经真正进入业务。

指标清单：

| Metric | Type | Labels | Owner Service | Trigger Point | 用途 |
| --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_sms_code_send_total` | Counter | `scene`(可选固定枚举) | `tutor-appointment-service` | 验证码真实发送成功 | 看验证码发放量、异常峰值 |
| `ai_tutor_biz_user_register_total` | Counter | `role` | `tutor-appointment-service` | 首次创建用户成功 | 看新增注册 |
| `ai_tutor_biz_user_login_total` | Counter | `role` | `tutor-appointment-service` | 登录成功 | 看活跃登录 |
| `ai_tutor_biz_profile_completed_total` | Counter | `role` | `tutor-appointment-service` | 首次达到最低资料完备门槛 | 看注册后是否激活 |

具体实现：

- `UserServiceImpl.loginOrRegister` 中区分“首次注册成功”和“已存在用户登录成功”。
- `updateUserInfo` 或资料保存 service 中判断用户是否从“未完成资料”首次进入“资料完成”。
- 资料完成规则先按角色冻结：
  - `teacher`: 姓名/头像/简介/可教学科至少一项主信息完成
  - `student`: 姓名/需求主描述/联系方式可用
  - `org`: 机构名/联系人/简介

效果说明：

- 可看到“验证码 -> 注册 -> 登录 -> 资料完成”的真实漏斗。
- 若注册高但资料完成低，说明 onboarding 卡住，不是投流问题。

### 2. 供需发布与浏览漏斗

目标：判断平台是否有足够供给，以及教师是否真的在消费需求。

指标清单：

| Metric | Type | Labels | Owner Service | Trigger Point | 用途 |
| --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_job_post_created_total` | Counter | `publisher_role` | `tutor-appointment-service` | 学生/机构发布需求成功 | 看供给生产 |
| `ai_tutor_biz_job_post_closed_total` | Counter | `publisher_role`,`close_reason` | `tutor-appointment-service` | 需求关闭/撤销/完成 | 看供给流失与成单后关闭 |
| `ai_tutor_biz_job_detail_view_total` | Counter | `viewer_role` | `tutor-appointment-service` | 需求详情返回成功 | 看教师消费需求意愿 |

具体实现：

- 在需求发布 service 的真正落库成功路径打点，而不是 controller。
- `close_reason` 固定枚举为 `filled|cancelled|expired|other`。
- 详情查看只在成功返回详情时计数，避免 404/鉴权失败污染口径。

效果说明：

- 能看到“发布出来的需求是否被教师看见、需求最终是被满足还是流失”。

### 3. 聊天申请漏斗

目标：判断教师/学生从“有意向”到“被接受”的损耗点。

指标清单：

| Metric | Type | Labels | Owner Service | Trigger Point | 用途 |
| --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_comm_apply_total` | Counter | `initiator`,`context_type` | `videoCall-IM-service` 或申请所属服务 | 创建申请成功 | 看申请发起量 |
| `ai_tutor_biz_comm_apply_decision_total` | Counter | `initiator`,`decision` | 同上 | 接收方处理成功 | 看通过/拒绝 |
| `ai_tutor_biz_comm_apply_detail_view_total` | Counter | `viewer_role` | 同上 | 详情打开成功 | 看接收方是否有处理动作 |

具体实现：

- 只在 `PENDING` 新建成功时打 `comm_apply_total`。
- 只在 `PENDING -> ACCEPTED/REJECTED` 时打 decision，天然幂等。
- `context_type` 固定枚举 `demand|tutor`，不要记录具体 `demandId/tutorId`。

效果说明：

- 能按“教师发起”和“学生发起”比较双方转化。
- 若申请量高但通过率低，可定位供需质量或页面引导问题。

### 4. 信息费支付与聊天解锁漏斗

目标：判断平台最关键的收入和聊天门禁链路是否闭环。

指标清单：

| Metric | Type | Labels | Owner Service | Trigger Point | 用途 |
| --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_payment_order_created_total` | Counter | `biz_type` | `payment-service` | 创建信息费订单成功 | 看支付入口量 |
| `ai_tutor_biz_payment_success_total` | Counter | `biz_type`,`channel` | `payment-service` | 订单首次成功 | 看支付成功量 |
| `ai_tutor_biz_payment_info_fee_amount_cents_total` | Counter | `channel`(可选) | `payment-service` | 信息费成功金额累计 | 看 GMV |
| `ai_tutor_biz_chat_unlock_total` | Counter | `unlock_reason` | `videoCall-IM-service` | 支付后首次进入 `CHAT_ENABLED` | 看支付后业务落实 |
| `ai_tutor_biz_chat_unlock_failed_total` | Counter | `reason` | `videoCall-IM-service` | 已收到支付成功但未完成解锁 | 捕获资金-业务不一致 |

具体实现：

- `payment-service` 在订单状态首次从 `PENDING -> SUCCESS` 时打 `payment_success_total` 和金额。
- `videoCall-IM-service` 在 `chat_access_status` 首次从 `PAYMENT_REQUIRED -> CHAT_ENABLED` 时打 `chat_unlock_total`。
- 当支付成功事件处理失败或重试超过阈值时打 `chat_unlock_failed_total`。
- 必须同时落一张专项看板，展示：
  - 订单创建量
  - 支付成功量
  - 聊天解锁量
  - 解锁失败量
  - `支付成功 -> 解锁成功` 转化率

效果说明：

- 可以直接发现“付了钱但没解锁”的严重业务事故。
- 收入漏斗不再只看支付成功，而是看到是否真正落实到聊天能力。

### 5. 聊天可用性与活跃度

目标：判断“聊天开放”后是否真的稳定可用。

指标清单：

| Metric | Type | Labels | Owner Service | Trigger Point | 用途 |
| --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_chat_room_enter_total` | Counter | `entry_type`(可选) | `videoCall-IM-service` | 房间创建或进入成功 | 看聊天入口使用 |
| `ai_tutor_biz_chat_message_sent_total` | Counter | `message_type` | `videoCall-IM-service` | 消息持久化成功 | 看互动活跃度 |
| `ai_tutor_biz_chat_message_failed_total` | Counter | `reason` | `videoCall-IM-service` | 消息发送失败 | 捕获聊天可用性问题 |
| `ai_tutor_biz_chat_realtime_delivered_total` | Counter | `event_type` | `videoCall-IM-service` | SSE 事件成功投递 | 看实时链路健康 |

具体实现：

- 文本、图片、系统消息、提案卡片统一归到固定枚举 `message_type`。
- `reason` 固定枚举：`access_denied|persist_error|push_error|other`。
- 如果 SSE 只要能写事件表 `chat_realtime_event` 就视为 delivered，需要在文档中明确。

效果说明：

- 可以区分“没人聊天”和“大家想聊但发不出去”。

### 6. 试课合作提案与试课履约前漏斗

目标：判断聊天之后是否进入试课排期，并在何处流失。

指标清单：

| Metric | Type | Labels | Owner Service | Trigger Point | 用途 |
| --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_trial_proposal_created_total` | Counter | `initiator` | `videoCall-IM-service` | 试课提案创建成功 | 看试课意向量 |
| `ai_tutor_biz_trial_proposal_decision_total` | Counter | `decision` | `videoCall-IM-service` | 提案同意/拒绝 | 看提案接受率 |
| `ai_tutor_biz_trial_proposal_expired_total` | Counter | 无 | `videoCall-IM-service` | 提案超时过期 | 看处理效率 |
| `ai_tutor_biz_trial_scheduled_total` | Counter | 无 | `videoCall-IM-service`/课程域 | 试课日程创建成功 | 看真正排上试课的量 |
| `ai_tutor_biz_trial_reschedule_created_total` | Counter | 无 | 课程域 | 改期提案创建 | 看排期稳定性 |
| `ai_tutor_biz_trial_reschedule_decision_total` | Counter | `decision` | 课程域 | 改期处理完成 | 看改期处理 |
| `ai_tutor_biz_trial_cancel_total` | Counter | `cancel_by` | 课程域 | 试课取消成功 | 看履约前流失 |

具体实现：

- 提案创建只在无待处理提案且写入成功时计数。
- `TRIAL_SCHEDULED` 只在第一次安排试课成功时计数，改期不要重复打这个指标。
- 改期与取消要严格依赖状态机迁移成功，不要在前端点击时打点。

效果说明：

- 能看出是“大家聊得起来但不发试课”，还是“发了试课但经常被拒绝/过期/取消”。

### 7. 试课后决策与正式课表漏斗

目标：判断试课是否真正转化为长期课程。

指标清单：

| Metric | Type | Labels | Owner Service | Trigger Point | 用途 |
| --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_trial_finished_total` | Counter | 无 | 课程域 | 试课自然结束 | 看进入决策池的量 |
| `ai_tutor_biz_trial_decision_total` | Counter | `decision` | 课程域 | 学生选择通过/不通过 | 看试课通过率 |
| `ai_tutor_biz_weekly_schedule_submitted_total` | Counter | 无 | 课程域 | 正式课表提交成功 | 看长期转化 |
| `ai_tutor_biz_weekly_schedule_timeout_total` | Counter | 无 | 课程域 | 24 小时内未提交导致失败 | 看后续跟进损耗 |

具体实现：

- `trial_finished_total` 在定时任务或状态切换 job 中打点。
- `trial_decision_total{decision="passed|failed"}` 在学生提交结果成功时打点。
- `weekly_schedule_timeout_total` 必须由自动超时任务在真正把状态改为失败后打点。

效果说明：

- 管理层可以看到“试课安排成功数 -> 试课结束数 -> 通过数 -> 正式课表提交数”的完整成单漏斗。

### 8. 退款与争议风险

目标：让退款不只是财务问题，也变成业务健康信号。

指标清单：

| Metric | Type | Labels | Owner Service | Trigger Point | 用途 |
| --- | --- | --- | --- | --- | --- |
| `ai_tutor_biz_refund_request_total` | Counter | `refund_type`(可选) | `payment-service`/管理域 | 用户发起退款申请 | 看退款需求量 |
| `ai_tutor_biz_refund_review_total` | Counter | `decision` | `ai-tutor-admin` | 管理员审批通过/拒绝 | 看人工审核压力 |
| `ai_tutor_biz_refund_total` | Counter | 无 | `payment-service` | 退款成功 | 看退款成功笔数 |
| `ai_tutor_biz_refund_amount_cents_total` | Counter | 无 | `payment-service` | 退款成功金额累计 | 看退款损失 |

具体实现：

- 退款申请和退款成功要分开，不要只看最终退款成功。
- 管理端审批打点放在审批结果真正落库成功后。

效果说明：

- 可以区分“退款诉求变多”还是“退款审批通过率变高”。
- 退款可与试课失败、支付异常等专项看板联动分析。

## Dashboard And Alert Mapping

为了避免“有指标没用处”，本次直接约束指标到看板和告警：

### Business KPI Dashboard

- 注册漏斗：验证码发送、注册成功、登录成功、资料完成
- 供需漏斗：需求发布、需求详情浏览、申请创建、申请通过
- 收入漏斗：订单创建、支付成功、聊天解锁、退款成功
- 履约漏斗：试课提案创建、试课安排、试课通过、正式课表提交

### Payment & Fulfillment Dashboard

- 信息费订单创建/成功趋势
- 聊天解锁成功/失败趋势
- 退款申请/审批/成功趋势
- `支付成功 -> 聊天解锁` 转化率
- `试课安排 -> 试课通过 -> 正式课表提交` 转化率

### P1/P2 Alert Suggestions

- `支付成功数 > 0` 但 `聊天解锁数` 在 10 分钟窗口显著偏低
- `聊天消息失败数` 在 5 分钟内超过阈值
- `试课提案过期数` 异常升高
- `weekly_schedule_timeout_total` 日增幅异常
- `退款申请数` 或 `退款成功金额` 突增

### Phase 1: 应用指标暴露全覆盖

目标：所有关键服务都能被 Prometheus 抓到基础运行指标。

实现路径：

- 为 `ai-tutor-gateway` 增加 `spring-boot-starter-actuator` 与 `micrometer-registry-prometheus`
- 为 `payment-service` 补齐上述依赖和 `management.endpoints.web.exposure.include=health,info,prometheus`
- 复核 `ai-tutor-admin`、`videoCall-IM-service`、`live-class-service`、`tutor-appointment-service` 暴露配置一致性
- 为 `ai-agent-service` 评估并接入 Prometheus 指标暴露
- 统一 `/actuator/health`、`/actuator/info`、`/actuator/prometheus`
- Prometheus 抓取配置覆盖全部关键服务
- 补充 up、jvm、process、http server requests 等基础面板

效果说明：

- 值班人员可第一时间确认“服务挂了没有、接口是否变慢、JVM 是否异常”。
- 网关与支付服务纳入统一观测后，核心链路不再存在采集盲区。

### Phase 2: 基础设施与依赖资源监控

目标：从“应用自身”扩展到“宿主机、容器、数据库、缓存、MQ”等依赖资源。

实现路径：

- 接入 `node-exporter` 监控 CPU、内存、负载、磁盘、网络
- 接入 `cadvisor` 或等价容器指标采集
- 接入 `mysqld_exporter` 监控连接数、慢查询、QPS、主从/复制状态（如有）
- 接入 `redis_exporter` 监控命中率、内存、阻塞、连接数、key eviction
- 接入 RabbitMQ 官方指标或 exporter，监控 ready/unacked、消费者数、队列堆积
- 如线上使用 Nginx / SLB / API 网关，再补入口层健康指标

效果说明：

- 出现 RT 升高时，可以快速区分是应用代码问题，还是数据库连接池耗尽、Redis 阻塞、MQ 堆积、机器资源打满。
- 资源瓶颈可提前告警，而不是等业务报障。

### Phase 3: 生产运行看板建设

目标：Grafana 从 KPI 看板升级为可值班的分层 dashboard。

实现路径：

- 保留并完善现有 `Business KPI` 看板
- 新增 `Service Overview`：
  - 服务可用性
  - 请求量 QPS
  - 错误率 4xx/5xx
  - P50/P95/P99 RT
  - JVM 堆内存 / GC / 线程
  - 实例维度对比
- 新增 `Infra & Dependency`：
  - 主机 CPU / Memory / Disk / Load
  - 容器资源
  - MySQL / Redis / RabbitMQ 健康
  - MQ 堆积
- 新增 `Payment & Fulfillment` 专项看板：
  - 支付成功率
  - 回调成功率
  - 退款成功率
  - 退款处理时延
  - IM 消息投递延迟
  - 直播建房 / 入房成功率

效果说明：

- 业务、研发、运维能按角色看各自关心的面板。
- 故障判断从“看一张很长的总图”变为“先总览，再钻取专项”。

### Phase 4: 告警闭环建设

目标：问题可被主动发现并通知到人。

实现路径：

- 接入 Alertmanager 或 Grafana Alerting
- 告警按级别配置路由：群通知、电话/短信、值班人升级策略
- 首批规则至少包括：
  - `up == 0`
  - 网关 5xx 比例超阈值
  - 支付成功率低于阈值
  - 接口 P95 持续超阈值
  - JVM heap usage 持续高位
  - Full GC 频繁
  - DB / Redis / RabbitMQ 不可用
  - MQ 队列堆积超过阈值
- 每条告警配套说明：
  - 含义
  - 可能原因
  - 第一处理动作
  - 升级动作

效果说明：

- 从“用户反馈后才知道事故”转为“系统先报警”。
- 值班同学收到告警后，不需要临时问人，能直接按 runbook 处理。

### Phase 5: 日志集中化与链路关联

目标：指标发现问题后，能快速在日志中完成定位。

实现路径：

- 统一服务日志格式为 JSON 或固定结构化 pattern
- 接入 Loki + Promtail 或等价集中日志系统
- 网关生成 `requestId`，写入响应头并向下游透传
- 后端服务读取并写入 MDC，日志自动带 `requestId`
- 如成本允许，逐步接入 `traceId` 并兼容 OpenTelemetry
- 关键业务日志统一打点：
  - 支付回调
  - 退款流转
  - IM 投递失败
  - 直播 token 签发
  - 外部依赖调用异常

效果说明：

- 告警触发后，可以按 `service + requestId + 时间窗口` 迅速查到同一请求的完整链路日志。
- 支付、退款等投诉工单的定位时间会显著缩短。

### Phase 6: 验收、演练与上线门禁

目标：观测体系不是“搭起来”，而是“被验证过能用”。

实现路径：

- 建立验收清单：
  - 所有服务 `/actuator/prometheus` 200
  - Prometheus Targets 全部 UP
  - 关键 dashboard 数据正常
  - 告警触发、抑制、恢复链路打通
  - 日志可按 requestId 检索
- 故障演练：
  - 手动停一个服务
  - 模拟 Redis 不可用
  - 模拟 RabbitMQ 堆积
  - 模拟支付回调失败
  - 模拟 RT 拉高
- 把观测验收纳入上线前门禁

效果说明：

- 确保上线前团队不是“理论上有监控”，而是“实际演练过且能用”。
- 故障响应会从经验驱动转向标准化流程驱动。

## Acceptance Effects

完成本变更后，团队应达到以下效果：

### 对业务

- 支付、退款、聊天、直播等关键链路出现异常时，业务侧可在分钟级获知。
- 业务 KPI 与运行态可以同屏关联，能区分“业务波动”与“系统故障”。

### 对研发

- 能快速判断问题在网关、应用、数据库、缓存、消息队列还是外部依赖。
- 能通过 requestId / traceId 找到一笔请求的关键日志。

### 对运维 / 值班

- 有明确的生产总览、依赖总览、专项看板。
- 有明确的告警规则和应急手册，不再依赖口口相传。

### 对上线质量

- P0 观测能力成为上线前硬门槛，降低“裸奔上线”的概率。

## Risks / Trade-offs

- **风险：初期指标和告警过多，产生噪音**
  - **缓解**：先以少量高价值规则上线，逐步调优阈值。

- **风险：日志集中化带来存储成本**
  - **缓解**：先采集关键服务与 ERROR/WARN，配置保留周期。

- **风险：traceId/requestId 改造涉及多服务**
  - **缓解**：先实现 requestId 贯穿，traceId 作为增强项逐步补齐。
