## 1. 基线收口

- [ ] 1.1 梳理线上关键服务、端口、owner、环境标签与 scrape job 命名
- [ ] 1.2 统一 observability 命名规范：dashboard、alert、label、日志字段
- [ ] 1.3 评审并冻结 P0 范围：指标、日志、告警、requestId、runbook
- [ ] 1.4 冻结业务指标设计模板：`metric/type/labels/owner service/trigger point/PromQL/dashboard/alert`

## 2. 业务指标定义与打点方案

- [ ] 2.1 注册激活漏斗：验证码发送、注册成功、登录成功、资料完成
- [ ] 2.2 供需漏斗：需求发布、需求关闭、需求详情浏览
- [ ] 2.3 申请漏斗：申请创建、申请详情查看、通过/拒绝
- [ ] 2.4 支付解锁漏斗：订单创建、支付成功、聊天解锁、解锁失败
- [ ] 2.5 聊天互动：房间进入、消息发送成功、消息失败、SSE 投递
- [ ] 2.6 试课漏斗：提案创建、处理、过期、试课安排、改期、取消
- [ ] 2.7 长期转化：试课结束、试课通过/不通过、正式课表提交、超时失败
- [ ] 2.8 退款漏斗：退款申请、退款审批、退款成功、退款金额
- [ ] 2.9 为每个指标补充 owner service、状态机触发点、低基数 labels 和 PromQL

## 3. 应用指标暴露

- [ ] 3.1 为 `ai-tutor-gateway` 引入 `spring-boot-starter-actuator` 与 `micrometer-registry-prometheus`
- [ ] 3.2 为 `payment-service` 引入 `spring-boot-starter-actuator` 与 `micrometer-registry-prometheus`
- [ ] 3.3 为 `ai-tutor-gateway`、`payment-service`、`ai-tutor-admin`、`videoCall-IM-service`、`live-class-service`、`tutor-appointment-service` 统一 `management.*` 暴露配置
- [ ] 3.4 评估 `ai-agent-service` 的 Prometheus 暴露方案并完成接入
- [ ] 3.5 在 `ai-tutor-common` 增补通用业务指标封装，并按服务拆分调用点
- [ ] 3.6 Prometheus 抓取配置覆盖全部关键服务
- [ ] 3.7 验证所有关键服务 `/actuator/prometheus` 返回 200，Targets 为 UP

## 4. 基础设施与依赖采集

- [ ] 4.1 接入 `node-exporter`
- [ ] 4.2 接入 `cadvisor` 或等价容器指标采集
- [ ] 4.3 接入 `mysqld_exporter`
- [ ] 4.4 接入 `redis_exporter`
- [ ] 4.5 接入 RabbitMQ 指标采集
- [ ] 4.6 在 Prometheus 中统一管理 job、targets 与 labels

## 5. Grafana 看板

- [ ] 5.1 保留并升级现有 `Business KPI` 看板
- [ ] 5.2 新增“注册与供需漏斗”面板组
- [ ] 5.3 新增“申请与支付解锁漏斗”面板组
- [ ] 5.4 新增“试课与正式课转化漏斗”面板组
- [ ] 5.5 新增 `Service Overview` 运行态总览看板
- [ ] 5.6 新增 `Infra & Dependency` 依赖资源看板
- [ ] 5.7 新增 `Payment & Fulfillment` 关键链路专项看板
- [ ] 5.8 为 dashboard 增加 `env/service/instance` 变量与 drill-down 入口

## 6. 告警闭环

- [ ] 6.1 选定并接入 `Alertmanager` 或 `Grafana Alerting`
- [ ] 6.2 配置服务可用性、错误率、延迟、JVM、DB、Redis、MQ 告警
- [ ] 6.3 配置支付成功后未解锁、消息失败突增、试课提案过期突增、正式课表超时突增、退款突增等业务告警
- [ ] 6.4 为每条 P1/P2 告警补充 runbook 链接与处理说明
- [ ] 6.5 联调告警通知链路并验证恢复通知

## 7. 日志与链路关联

- [ ] 7.1 统一服务日志格式与字段
- [ ] 7.2 接入 `Loki + Promtail` 或等价集中日志方案
- [ ] 7.3 在网关生成并透传 `requestId`
- [ ] 7.4 后端服务接入 MDC，日志自动输出 `requestId`
- [ ] 7.5 为支付、退款、IM、试课状态迁移、正式课表超时关键动作补结构化日志
- [ ] 7.6 评估 `traceId/OpenTelemetry` 演进路径并形成后续计划

## 8. 验收与演练

- [ ] 8.1 编写观测体系验收清单
- [ ] 8.2 演练服务下线、Redis 不可用、MQ 堆积、支付回调失败、支付成功未解锁、试课超时未排课等场景
- [ ] 8.3 验证“告警 -> 看板 -> 日志”闭环定位路径
- [ ] 8.4 把观测验收加入上线前门禁

## 9. 后续演进

- [ ] 9.1 评估 OpenTelemetry 全链路 tracing 的接入窗口
- [ ] 9.2 评估更细粒度的业务异常画像与容量预测告警
