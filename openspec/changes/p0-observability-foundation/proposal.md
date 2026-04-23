## Why

当前仓库已经具备部分观测基础，但仍停留在“能看少量业务指标”的阶段，尚不满足真实上线的 P0 观测要求：

- 已有 Prometheus + Grafana 本地基础设施，以及业务 KPI 看板草案。
- `tutor-appointment-service`、`videoCall-IM-service`、`live-class-service` 已部分接入 Actuator/Prometheus。
- `ai-tutor-gateway` 与 `payment-service` 仍未形成完整的指标暴露基线。
- 当前 Prometheus 仅抓取少数服务，且缺少主机、容器、JVM、MySQL、Redis、RabbitMQ、网关流量、错误率、日志集中检索、告警联动。

如果直接上线，团队将面临以下高风险：

- 服务异常后无法在 5 分钟内判断是应用、资源、依赖还是网络问题。
- 支付、退款、聊天、直播等关键链路故障无法被自动告警。
- 运维只能登录机器逐台看日志，排障链路慢且不可复制。
- 只有业务看板，没有值班告警和故障定位工具链，Grafana 只能“看”，不能“值班”。

因此需要新增一个独立的 P0 观测体系变更，建立真实上线前必须具备的观测基线。

## What Changes

- 建立统一的 P0 观测分层：指标、日志、告警、链路关联、运行手册。
- 为所有线上关键服务补齐 Micrometer + Prometheus 暴露能力。
- 从“仅业务 KPI”扩展为“业务 + 技术 + 依赖资源”三层看板。
- 引入基础告警机制，确保故障能主动通知，而不是靠人工发现。
- 引入集中日志检索与 requestId/traceId 关联能力，缩短问题定位路径。
- 形成面向本项目的验收口径、告警清单、值班和应急说明。

## Scope

### In Scope

- 网关、预约、IM、支付、管理后台、直播课堂、AI Agent 等关键服务的指标暴露
- Prometheus 抓取配置与标签规范
- Grafana 生产运行看板与业务看板分层
- Alertmanager 或等价告警通道接入
- 主机 / 容器 / JVM / MySQL / Redis / RabbitMQ / HTTP 指标接入
- 日志集中采集与检索
- requestId / traceId 贯穿网关与后端服务
- 上线前联调、验收、故障演练

### Out of Scope

- 完整 APM 商业化平台替换
- 全量分布式 tracing 深度采样优化

说明：本次变更不包含 Arthas，P0 重点聚焦“发现问题、定位问题、量化业务闭环损耗”的基础能力。

## Impact

- **代码**：多个后端服务需要补依赖、配置、拦截器或 filter、日志字段、公共封装。
- **部署**：需要新增或补全 Prometheus、Grafana、Alertmanager、日志采集组件及 exporter。
- **配置**：Nacos / 环境变量中需要新增 observability 相关配置项。
- **运维**：需要建立仪表盘、告警路由、值班和应急手册。
- **测试**：需要增加观测链路验证、故障注入、告警演练。
