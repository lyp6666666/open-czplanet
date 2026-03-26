---
title: 移除 ai-tutor-starter，微服务化启动与 Nacos Config/Discovery 对齐
status: draft
date: 2026-03-25
---

# 目标

将当前仓库从“聚合启动（ai-tutor-starter）”过渡到“微服务单独启动”，并确保：

- `ai-tutor-starter` 模块被移除（不再作为启动入口、也不再参与构建模块列表）
- 每个后端服务都能：
  - 从 Nacos 拉取配置（Nacos Config）
  - 在 Nacos 进行服务注册（Nacos Discovery），且命名空间/分组策略一致可控
- 本地 `application.yml` 等配置文件保留作为兜底（Nacos 配置缺失时仍可启动）
- 提供一个脚本可以一键启动本地依赖与所有服务（便于本地联调）
- `ai-tutor-starter` 内的可复用代码迁移到公共模块（优先 `ai-tutor-common`），避免删除后引发编译/运行错误

# 背景与现状

## 当前模块

父工程模块列表（根 [pom.xml](file:///Users/bytedance/lyp/project/huoyue/ai_platform/pom.xml#L22-L32)）：

- `tutor-appointment-service`
- `videoCall-IM-service`
- `payment-service`
- `ai-tutor-gateway`
- `ai-tutor-admin`
- `ai-tutor-common`
- `ai-tutor-mq`
- `ai-tutor-starter`（拟移除）

## 当前 Nacos 使用方式

- `ai-tutor-starter`：通过 `spring.config.import=optional:nacos:...` 从 Nacos 拉取多个 DataId（Config），不承担服务注册（Discovery）
  - 入口配置：[ai-tutor-starter/application.yml](file:///Users/bytedance/lyp/project/huoyue/ai_platform/ai-tutor-starter/src/main/resources/application.yml)
- 网关与业务服务：具备 Discovery（注册）配置；但 Config（拉取配置）的能力需要统一补齐到“每个服务”

## ai-tutor-starter 内主要代码（需要处理）

- 启动入口：`com.ai.tutor.TutorStartApplication`（移除后不应再被引用）
- 业务日志链路（RocketMQ + 落库）：`com.ai.tutor.log.*`（存在可复用价值）
- 单体阶段本地适配器：`LocalImFacade/LocalBrokerageOrderFacade/AppointmentImSyncListener`（依赖其他服务内部包，微服务化后需重新归位）

# 设计原则

- 单个服务启动即可自洽（最小依赖），不依赖“聚合启动”隐式注入的 Bean
- Nacos Config / Discovery 配置约定一致：地址、group、namespace、DataId 命名方式、profile 组合方式
- 保持可回退：在移除模块前保证每个服务独立启动通过；移除后仍可全量构建与运行
- 避免把“跨服务内部实现依赖”迁移到 `ai-tutor-common`（否则 common 反向依赖业务服务会破坏依赖方向）

# 目标架构

## 启动方式

- 本地依赖：使用 `Dockerfile/docker-compose.yml` 启动 MySQL/Redis/MinIO/RabbitMQ
- 服务启动：每个服务独立启动（IDE 或脚本）
  - `ai-tutor-gateway`
  - `tutor-appointment-service`
  - `videoCall-IM-service`
  - `payment-service`
  - `ai-tutor-admin`

## Nacos 配置策略（推荐）

### 统一参数

- Nacos server-addr：`106.52.179.248:8848`
- group：`DEFAULT_GROUP`
- namespace：dev/prod 分离（由 `spring.profiles.active` 决定），允许通过环境变量覆盖

### DataId 命名（建议）

保留现有公共配置 DataId（兼容 `ai-tutor-starter` 时代），并补齐每服务专用配置：

- 公共（跨服务共享）
  - `ai-tutor-common.yaml`
  - `ai-tutor-common-dev.yaml`
  - `ai-tutor-common-prod.yaml`
- 业务域（按仓库既有约定）
  - `ai-tutor-home.yaml`、`ai-tutor-home-<env>.yaml`
  - `ai-tutor-sms.yaml`、`ai-tutor-sms-<env>.yaml`
  - `ai-tutor-payment.yaml`、`ai-tutor-payment-<env>.yaml`
- 服务专用（新增，避免把所有服务配置混在一起）
  - `ai-tutor-gateway.yaml`、`ai-tutor-gateway-<env>.yaml`
  - `tutor-appointment-service.yaml`、`tutor-appointment-service-<env>.yaml`
  - `videoCall-IM-service.yaml`、`videoCall-IM-service-<env>.yaml`
  - `payment-service.yaml`、`payment-service-<env>.yaml`
  - `ai-tutor-admin.yaml`、`ai-tutor-admin-<env>.yaml`

### 每个服务的加载顺序（建议）

每个服务的 `application.yml` 增加如下结构（均为 optional，保留本地兜底）：

- 先拉公共配置（common + 领域配置）
- 再拉服务专用配置（service + service-env）

并且每个服务都引入 `spring-cloud-starter-alibaba-nacos-config`（Config）与 `spring-cloud-starter-alibaba-nacos-discovery`（Discovery）。

# 代码与模块改造方案

## 1) 移除 ai-tutor-starter 模块

- 从父工程 `pom.xml` 的 `<modules>` 中移除 `ai-tutor-starter`
- 删除 `ai-tutor-starter` 模块目录（或保留但不编译；以最终一致性为准）
- 清理任何引用：
  - 文档/脚本/IDE 配置里指向 `TutorStartApplication` 的启动方式

## 2) 将“可复用代码”迁移到公共模块

### 2.1 BizLog（MQ + 落库）代码迁移

目标：保留“业务日志”能力，不依赖聚合启动才能工作。

候选方案：

- 方案 A（严格按需求）：迁移到 `ai-tutor-common`
  - 优点：满足“放到 common 模块”要求，复用简单
  - 风险：common 引入 RocketMQ/MyBatis-Plus 依赖会向所有服务传播，增大体积与启动复杂度；各服务需要明确是否启用（建议通过 `@ConditionalOnProperty` 开关控制）
- 方案 B（推荐工程化）：迁移到新模块 `ai-tutor-bizlog-starter`
  - 优点：依赖隔离、按需引入、避免 common 过重
  - 说明：若你坚持只用 common，则不采用该方案

本次改造默认采用方案 A，若你确认要严格隔离依赖则切换为方案 B。

### 2.2 单体阶段 Local Facade 处理

`LocalImFacade/LocalBrokerageOrderFacade/AppointmentImSyncListener` 直接依赖其他服务内部包（例如 `com.ai.tutor.videocallimservice.*`），不适合迁移到 `ai-tutor-common`。

处理策略：

- 微服务模式：默认使用远程调用（Feign 或 HTTP），不再需要 Local Facade
- 将这些类：
  - 要么删除（并确保引用处已切换为 remote 实现）
  - 要么移动到对应服务内部（例如 IM 内部监听/实现放回 IM 服务），避免跨模块内部依赖

## 3) 每个服务补齐 Nacos Config 能力

逐服务补齐：

- 为 `ai-tutor-gateway/tutor-appointment-service/videoCall-IM-service/payment-service/ai-tutor-admin` 引入 `spring-cloud-starter-alibaba-nacos-config`
- 在每个服务 `application.yml` 中添加 `spring.cloud.nacos.config.server-addr/group/namespace` 与 `spring.config.import=optional:nacos:...`
- 本地 `application.yml` 保留默认值（Nacos 缺失时兜底），Nacos 只覆盖差异化配置（例如第三方密钥、回调地址、开关）

## 4) 一键启动脚本

提供一个脚本（例如 `scripts/dev_all_up.sh`）实现：

- 确认 Docker daemon 可用
- `docker compose -f Dockerfile/docker-compose.yml up -d`
- 按固定端口启动服务（避免冲突），并把日志输出到 `.logs/`
- 提供对应 stop 脚本（或同脚本支持 `down` 子命令）

# 配置迁移到 Nacos 的操作指南（给开发者）

## 迁移原则

- 先迁移“必须且敏感”的：第三方 key/secret、回调地址、生产开关等
- 本地默认值继续保留（避免 Nacos 缺项导致无法启动）
- DataId 维度尽量小：公共配置与服务专用配置拆开，避免冲突与误覆盖

## 迁移步骤（dev/prod 各做一次）

1) 进入 Nacos Console：`http://106.52.179.248:8848/nacos`
2) 选择命名空间：
   - dev：`3066af4f-57ee-4f4d-80fe-0d2a4e791f7d`
   - prod：`be1662a5-65d2-4be8-92ef-31001b7fa427`
3) 配置管理 → 配置列表 → 新建配置
4) 填写：
   - Data ID：例如 `tutor-appointment-service-dev.yaml`
   - Group：`DEFAULT_GROUP`
   - 配置格式：YAML
   - 配置内容：从本地 `application.yml` 中挑选需要迁移的 key（建议先迁移少量验证闭环）
5) 发布后，重启服务验证日志出现：
   - `[Nacos Config] Listening config...` 或等价的订阅/加载日志
6) 在服务端做功能验证（关键接口可用），并在 Nacos 服务管理 → 服务列表确认注册

# 测试计划（必须执行）

## 构建与单测

- 全量：`sh ./mvnw test`
- 关键模块：`sh ./mvnw -pl ai-tutor-gateway,tutor-appointment-service,videoCall-IM-service,payment-service,ai-tutor-admin -am test`

## 运行时验证（每个服务都要单独启动）

对每个服务执行：

1) 启动服务（IDE 或命令行）
2) 观察日志确认：
   - Nacos Config：出现订阅/加载对应 DataId 的日志（至少包含 service 的 DataId）
   - Nacos Discovery：出现 `REGISTER-SERVICE` / `nacos registry ... register finished`
3) Nacos 控制台验证：
   - 对应命名空间（dev/prod）下能看到服务名（`spring.application.name`）
   - 实例健康
4) 最小 HTTP 验证：
   - 网关：`/actuator/health`（若未开 actuator，则至少访问任一 route，或只验证启动与注册）
   - 业务服务：打开 swagger 或 `v3/api-docs`（若提供）

## 本地依赖（Docker）验证

- `docker info` 正常
- `docker compose -f Dockerfile/docker-compose.yml ps` 显示 mysql/redis/minio/rabbitmq up
- 业务服务若依赖 MinIO/DB/Redis：确保不会因缺配置导致启动直接失败（例如 MinIO endpoint 为 null）

# 风险与规避

- 风险：网关/服务缺少 nacos-config 依赖导致“能注册但不能拉配置”
  - 规避：逐服务加入 nacos-config 并用日志验证 DataId 订阅
- 风险：namespace/group/profile 不一致导致控制台“看不到服务/看不到配置”
  - 规避：在每个服务中统一 `server-addr/group/namespace`，并在启动说明中明确选择命名空间
- 风险：把跨服务内部包依赖迁移进 common 造成依赖反转
  - 规避：Local Facade 删除或归位到对应服务内部；common 只保留接口与 DTO

