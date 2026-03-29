---
name: "ai-platform-context"
description: "汇总 ai_platform 仓库结构、启动命令、依赖与约定。用于在 vibecoding 前快速对齐上下文；以及在每次修改完成后追加变更摘要到 Change Log。"
---

# AI Platform Context（ai_platform）

## 何时调用

- 开始在本仓库做任何功能/修 bug 前：用于快速获取结构、入口、启动方式与常见坑
- 不确定改动应该落在哪个模块/目录时：用于定位
- 每次 AI 完成修改并通过校验后：把本次变更摘要追加到文末 Change Log

## 仓库结构速览

### 后端（Maven 多模块 / Spring Boot）

- 网关：`ai-tutor-gateway`
- 业务服务：`tutor-appointment-service` / `videoCall-IM-service` / `payment-service`
- 管理端后端：`ai-tutor-admin`
- 通用能力：`ai-tutor-common`
- MQ 相关：`ai-tutor-mq`

### 前端

- 用户端 Web：`ai-tutor-web`（Vue 3 + Vite + Pinia + Vitest）
- 管理端 Web：`ai-tutor-admin-web`（Vue 3 + Vite）
- 小程序/多端：`ai-tutor-miniprogram`（uni-app）

## 本地依赖与一键启动

### Docker Compose（推荐）

- 配置文件：`Dockerfile/docker-compose.yml`
- 启动：
  - `docker compose -f Dockerfile/docker-compose.yml up -d`
- 停止并清数据（会删除 MySQL/Redis/MinIO 卷）：
  - `docker compose -f Dockerfile/docker-compose.yml down -v`

### 默认服务与口令（来自后端启动说明与 compose）

- MySQL：`localhost:3306`
  - root 密码：`Aa123456`
  - 默认库：`ai_tutor`
  - 初始化 SQL：`sqlDoc/huoyue.sql`、`sqlDoc/seed_dev_data.sql`
- Redis：`localhost:6379`，密码 `123456`
- MinIO：API `http://localhost:9000`，Console `http://localhost:9001`，`minioadmin/minioadmin`
  - bucket：`ai-tutor-assets`（public read）
- RabbitMQ：`5672`，Console `http://localhost:15672`

## Nacos（配置中心 + 服务注册）

- Nacos Console：`http://106.52.179.248:8848/nacos`
- Server Address（不带 `/nacos`）：`106.52.179.248:8848`
- 约定：开发环境与生产环境都通过同一套 Nacos 做配置管理与服务注册；环境隔离主要靠 namespace（以及必要时的 group）
- 注意：Nacos 属于内部系统，不要对公网暴露；本仓库不提交 Nacos 账号/密码等敏感信息

### 各服务：Nacos Config（拉配置）

- 每个服务都通过 `spring.config.import=optional:nacos:...` 拉取配置（缺失不阻塞启动，本地 `application.yml` 作为兜底）
- 统一 Nacos 参数：
  - `spring.cloud.nacos.config.server-addr: 106.52.179.248:8848`
  - `spring.cloud.nacos.config.group: DEFAULT_GROUP`
  - `spring.cloud.nacos.config.namespace: ${NACOS_NAMESPACE:3066af4f-57ee-4f4d-80fe-0d2a4e791f7d}`
- 推荐在 Nacos 中维护（示例 DataId）：
  - 公共：`ai-tutor-common.yaml` / `ai-tutor-common-dev.yaml` / `ai-tutor-common-prod.yaml`
  - 服务专用：`${spring.application.name}.yaml` / `${spring.application.name}-dev.yaml` / `${spring.application.name}-prod.yaml`

### 各业务服务：服务注册方式（Discovery）

- 网关：`ai-tutor-gateway`（见 `ai-tutor-gateway/src/main/resources/application.yml`）
- 业务服务：`tutor-appointment-service` / `videoCall-IM-service` / `payment-service`
- 这些服务的 Nacos discovery 地址已在本地配置文件中写死：
  - `spring.cloud.nacos.discovery.server-addr: 106.52.179.248:8848`
- discovery namespace 统一通过环境变量覆盖（默认 DEV）：
  - `spring.cloud.nacos.discovery.namespace: ${NACOS_NAMESPACE:3066af4f-57ee-4f4d-80fe-0d2a4e791f7d}`

### 你本地如何配置（推荐做法）

1) 确保本地中间件用 Docker 起好（MySQL/Redis/MinIO/RabbitMQ），Nacos 不在本地起

2) 选择环境（Config 用：决定 namespace 与 dataId 后缀）

- 默认：各服务 `application.yml` 写死 `spring.profiles.active: dev`
- 需要切换时，用环境变量覆盖：

```bash
SPRING_PROFILES_ACTIVE=dev NACOS_NAMESPACE=3066af4f-57ee-4f4d-80fe-0d2a4e791f7d
SPRING_PROFILES_ACTIVE=prod NACOS_NAMESPACE=be1662a5-65d2-4be8-92ef-31001b7fa427
```

3) 在 Nacos 控制台里放哪些配置（最小可运行集合）

- Group：`DEFAULT_GROUP`
- Namespace：按 `SPRING_PROFILES_ACTIVE` 选择 dev/prod 对应的 namespace id
- Data ID：与 `spring.config.import` 保持一致（建议先从公共的开始）
  - `ai-tutor-common.yaml`
  - `ai-tutor-home.yaml`
  - `ai-tutor-sms.yaml`
  - `ai-tutor-payment.yaml`
  - 可选：按环境补充 `ai-tutor-*-dev.yaml` / `ai-tutor-*-prod.yaml`
- 配置格式：YAML（文件名以 `.yaml` 结尾）

4) 如何验证 Nacos 配置是否生效

- 启动任一服务后观察日志是否出现 Nacos config 加载成功的提示
- 若某配置在 Nacos 缺失，当前是 optional 模式：服务会继续启动并使用本地 `application.yml` 中的默认值（例如 `wechat.*`、`payment.wechat.*` 的 mock 默认值）

5) 如需临时切换 Nacos 地址（仅本次启动生效）

```bash
sh ../mvnw -am spring-boot:run -Dspring-boot.run.arguments="--spring.cloud.nacos.discovery.server-addr=106.52.179.248:8848"
```

## 常用启动与校验命令

### 后端启动（推荐：一键拉起）

- `sh scripts/dev_all_up.sh`
- 停止：`sh scripts/dev_all_down.sh`
- 脚本参数（环境变量覆盖）：
  - `SPRING_PROFILES_ACTIVE`：默认 `dev`
  - `NACOS_NAMESPACE`：默认 DEV 命名空间 `3066af4f-57ee-4f4d-80fe-0d2a4e791f7d`
  - 端口：`GATEWAY_PORT/APPOINTMENT_PORT/IM_PORT/PAYMENT_PORT/ADMIN_PORT`（默认 18080~18084）
  - 日志与 PID：
    - 日志目录：`/.logs/*.log`
    - PID 目录：`/.pids/*.pid`

### Nacos 配置迁移（指南与模板）

- 迁移指南：`docs/nacos/CONFIG_MIGRATION_GUIDE.md`
- 带注释模板：`docs/nacos/templates/*.yaml`

### 后端常用入口（默认端口）
- 网关：`http://localhost:18080`
- 预约服务：`http://localhost:18081`
- IM 服务：`http://localhost:18082`
- 支付服务：`http://localhost:18083`
- Admin：`http://localhost:18084`

### MinIO 资源准备（首次必做）
- 同步前端内置 Banner 等静态资源到 MinIO：
  - `bash scripts/minio_sync_assets.sh`
- 写入默认头像对象（可选）：
  - `bash scripts/minio_seed_defaults.sh`

### 用户端 Web（ai-tutor-web）

- `npm ci`
- `npm run dev`
- 校验：
  - `npm run lint`
  - `npm run typecheck`
  - `npm test`

### 管理端 Web（ai-tutor-admin-web）

- `npm ci`
- `npm run dev`
- 校验：
  - `npm run lint`
  - `npm run typecheck`

### 小程序（ai-tutor-miniprogram）

- `npm ci`
- 开发（微信小程序）：`npm run dev:mp-weixin`
- 类型检查：`npm run type-check`

## 代码修改时的默认原则（用于 vibecoding 对齐）

- 优先改既有模块与既有工具方法，尽量不引入新依赖
- 后端变更优先逐服务启动验证（Config 拉取 + Discovery 注册）
- 修改完成必须执行该模块的 lint/typecheck/test（若项目提供）

## Grafana 业务 KPI 看板（Prometheus）

本仓库支持将关键业务事件以 Prometheus Metrics 的形式暴露，并在 Grafana 上构建“按天聚合”的实时看板。

### 指标暴露（服务侧）

相关服务暴露以下 endpoint（默认端口见“后端常用入口”）：

- `tutor-appointment-service`：`GET /actuator/prometheus`
  - 覆盖：新注册（教师/学生）、验证码发送
- `videoCall-IM-service`：`GET /actuator/prometheus`
  - 覆盖：申请沟通创建/通过/拒绝、信息费支付金额、达成合作次数
- `ai-tutor-admin`：`GET /actuator/prometheus`
  - 覆盖：新注册（机构，管理端创建）、退款次数与退款金额

配置要点（已在各服务 `application.yml` 中默认开启）：

- `management.endpoints.web.exposure.include: health,info,prometheus`
- `management.endpoint.prometheus.enabled: true`

### Prometheus 配置（scrape）

Prometheus 通过 scrape 拉取 Metrics（不建议在线服务用 pushgateway 作为主链路）。

示例 `prometheus.yml`（按实际部署地址替换 targets）：

```yaml
scrape_configs:
  - job_name: ai-tutor
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - tutor-appointment-service:18081
          - videoCall-IM-service:18082
          - ai-tutor-admin:18084
```

### Grafana 配置（数据源 + 面板）

- 新建 Prometheus 数据源，URL 指向 Prometheus（如 `http://prometheus:9090`）
- 新建 Dashboard，按下面 PromQL 创建面板（时间范围建议 `Last 7 days` / `Last 30 days`）

### Grafana 基本页面信息（本地 Docker Compose）

本仓库已在 `Dockerfile/docker-compose.yml` 内集成 Prometheus + Grafana，并通过 provisioning 自动完成数据源与看板导入。

启动命令（仅启动监控栈）：

```bash
docker compose -f Dockerfile/docker-compose.yml up -d prometheus grafana
```

页面入口：

- Grafana：`http://localhost:3000`
  - 默认账号密码：`admin / huoyue`
  - 默认看板：`AI Tutor KPI`
  - 默认文件夹：`AI Tutor`
- Prometheus：`http://localhost:9090`
  - Targets 检查页：`http://localhost:9090/targets`

Prometheus 抓取目标（Mac 上 Docker 访问宿主机端口使用 `host.docker.internal`）：

- `host.docker.internal:18081/actuator/prometheus`（tutor-appointment-service）
- `host.docker.internal:18082/actuator/prometheus`（videoCall-IM-service）
- `host.docker.internal:18084/actuator/prometheus`（ai-tutor-admin）

常见问题排查（最小闭环）：

- Grafana 看板没有曲线：先打开 `http://localhost:9090/targets` 确保对应 target 为 `UP`
- Targets 为 `DOWN`：确认后端服务已启动且本机能访问 `http://localhost:18081/actuator/prometheus`（18082/18084 同理）
- 指标没增长：需要先触发业务动作（注册/发验证码/发申请/审批/支付/退款等），PromQL 才会出现非 0 的按天 increase

### 指标列表与 PromQL（按天聚合）

说明：

- 全部为 Counter（单调递增），按天口径统一使用 `increase(metric[1d])`
- 金额类指标单位为“分”，在 PromQL 中 `/ 100` 转为“元”

新注册用户数（教师/学生/机构）：

- 总计：`sum(increase(ai_tutor_biz_user_register_total[1d]))`
- 分角色：`sum by (role) (increase(ai_tutor_biz_user_register_total[1d]))`

申请沟通数量（教师主动/学生主动）：

- 总计：`sum(increase(ai_tutor_biz_comm_apply_total[1d]))`
- 分发起方：`sum by (initiator) (increase(ai_tutor_biz_comm_apply_total[1d]))`

申请沟通通过/拒绝数量（教师主动/学生主动）：

- 通过总计：`sum(increase(ai_tutor_biz_comm_apply_decision_total{decision="approved"}[1d]))`
- 拒绝总计：`sum(increase(ai_tutor_biz_comm_apply_decision_total{decision="rejected"}[1d]))`
- 分发起方：`sum by (initiator, decision) (increase(ai_tutor_biz_comm_apply_decision_total[1d]))`

验证码发送次数：

- `sum(increase(ai_tutor_biz_sms_code_send_total[1d]))`

每日支付信息费总和（元）：

- `sum(increase(ai_tutor_biz_payment_info_fee_amount_cents_total[1d])) / 100`

每日达成合作总次数：

- `sum(increase(ai_tutor_biz_collaboration_success_total[1d]))`

每日退款次数与退款总额（元）：

- 退款次数：`sum(increase(ai_tutor_biz_refund_total[1d]))`
- 退款总额：`sum(increase(ai_tutor_biz_refund_amount_cents_total[1d])) / 100`

### 口径与注意事项

- 低基数标签：禁止把 `userId/orderNo/phone` 等作为 label，避免 Prometheus 存储爆炸
- 幂等：打点仅发生在“状态变更成功”的路径上（例如申请审批 update 成功、支付状态首次变更成功、退款从 DISPUTE 变更成功）
- 安全：`/actuator/prometheus` 建议仅内网可访问或通过网关白名单限制

## Change Log（每次 AI 修改完成后追加）

- 2026-03-25：创建 skill「ai-platform-context」，沉淀仓库结构与本地启动信息
- 2026-03-25：补充 Nacos（配置中心/服务注册）地址、namespace 与本地配置方式
- 2026-03-25：将各服务 discovery 的 `server-addr` 在本地配置文件中写死为 `106.52.179.248:8848`
- 2026-03-25：新增 `common-used.md`（已在 `.gitignore` 中忽略）用于记录本地常用命令
- 2026-03-25：为各服务补齐 discovery namespace（dev/prod）以便在 Nacos 对应命名空间中可见
- 2026-03-25：将 discovery namespace 固定为 DEV（3066af4f-57ee-4f4d-80fe-0d2a4e791f7d），避免因 profile 不一致导致控制台看不到服务
- 2026-03-25：移除 ai-tutor-starter，改为微服务独立启动，并新增一键启动/停止脚本
- 2026-03-25：新增 Nacos 配置迁移指南与带注释模板（docs/nacos）
- 2026-03-28：补充 Prometheus + Grafana 本地启动与 Grafana 页面入口说明，并提供默认看板（AI Tutor KPI）
