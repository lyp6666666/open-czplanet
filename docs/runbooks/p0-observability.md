# P0 观测体系 Runbook

## service-down

1. 先确认对应服务进程、端口和注册中心状态是否正常。
2. 再看 `AI Tutor - Service Overview` 中最近 15 分钟的 QPS、5xx 和 JVM 指标。
3. 最后去 Loki 按 `service` 和 `requestId` 检索错误日志，定位是否是配置、依赖或发布引起。

## high-http-5xx

1. 在 Grafana 先确认是单服务还是多服务同时升高。
2. 对照最近发布、网关流量、依赖错误率判断是否为级联故障。
3. 用 Loki 按 `service=<服务名>` 且关键词 `ERROR` 检索近 10 分钟日志。

## high-latency

1. 检查线程池、数据库连接池、Redis 和 MQ 是否有饱和。
2. 若仅单接口升高，优先看 해당接口的下游调用和 SQL。
3. 若全站升高，优先排查宿主机 CPU、内存和网络。

## payment-unlock-gap

1. 对比 `payment-service` 与 `videoCall-IM-service` 最近 10 分钟指标。
2. 检查支付回调是否成功落库，支付成功事件是否被消费。
3. 检查聊天房间 `PAYMENT_REQUIRED -> CHAT_ENABLED` 状态迁移日志和重试情况。

## chat-message-failed

1. 先看失败原因标签是否集中在 `access_denied`、`persist_error` 或 `push_error`。
2. `access_denied` 优先查门禁状态和支付/解锁链路。
3. `persist_error` 优先查数据库或消息表写入。
4. `push_error` 优先查 SSE 会话、网络和前端连接稳定性。

## trial-proposal-expired

1. 检查最近 1 小时试课提案创建量与消息投递量是否匹配。
2. 检查消息通知是否到达对端，以及是否存在排课入口交互问题。
3. 如过期量主要集中在某一端角色，回看对应页面引导和提醒逻辑。

## weekly-schedule-timeout

1. 查试课通过后的正式课表提交链路是否有前端阻塞或权限问题。
2. 查 `CourseEnrollmentService.processWeeklyScheduleTimeouts` 是否正常执行。
3. 对照相关课程日志确认是否为提醒缺失还是状态机异常。

## refund-spike

1. 先看退款申请、审批通过和退款成功是否同时升高。
2. 再按课程、试课、信息费场景拆分，确认是否为某一业务环节集中异常。
3. 必要时回看最近履约质量、投诉和支付问题。

## high-host-cpu

1. 在 `AI Tutor - Infra & Dependency` 看是否为单容器或全机升高。
2. 若是单容器升高，继续定位到服务级 JVM、线程池和慢接口。
3. 若是全机升高，检查批任务、日志采集和其他后台任务。

## redis-down

1. 先确认 Redis 容器与 exporter 是否都存活。
2. 再确认连接密码、端口和网络是否变更。
3. 若 Redis 本身不可用，优先评估登录、验证码、会话、队列等链路影响。
