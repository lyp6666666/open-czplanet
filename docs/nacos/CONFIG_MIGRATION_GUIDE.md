# 配置迁移到 Nacos 指南（含详细注释模板）

本仓库已完成：各服务通过 `spring.config.import=optional:nacos:...` 拉取配置，并注册到 Nacos。你接下来要做的是把“真正应该中心化管理的配置”从本地 `application.yml` 迁移到 Nacos 的 DataId 中，并给这些配置补齐可维护的注释。

## 0. 迁移目标与原则

### 目标

- 开发/测试/生产环境的配置统一由 Nacos 管理（按 namespace 隔离环境）
- 代码仓库不提交任何敏感信息（key、token、私钥、支付证书等）
- 本地 `application.yml` 只保留：
  - Nacos 连接信息（server-addr、namespace、group）
  - `spring.config.import` 列表（optional）
  - 最小兜底（让缺失 Nacos 配置时也能启动/能自测）

### 原则（强烈推荐）

- 先迁“变化频繁/需统一”的配置：第三方回调地址、开关、限流阈值、灰度参数
- 最后迁“强依赖基础设施”的配置：数据库、Redis（迁之前先保证每个环境都有稳定可用的实例）
- 迁移顺序遵循：公共配置 → 服务专用配置 → 敏感配置
- 每次迁移只动一小块，迁完立即验证日志里“监听了哪个 DataId”

## 1. Nacos 里的配置组织方式（DataId / Group / Namespace）

### Namespace（环境隔离）

- DEV：`481e4376-4576-4b18-ac19-f61e170ca3ae`
- PROD：`44cf681d-9f93-443e-aa9e-ba6ec8f721d5`

建议：namespace 只用来隔离环境，不要在同 namespace 再用复杂 group 做隔离（除非有明确需要）。

### Group（逻辑分组）

当前工程默认：`DEFAULT_GROUP`。建议先不要改。

### DataId（推荐命名）

工程已在各服务 `application.yml` 里约定 import：

- 公共：
  - `ai-tutor-common.yaml`
  - `ai-tutor-common-${spring.profiles.active}.yaml`
- 服务专用：
  - `${spring.application.name}.yaml`
  - `${spring.application.name}-${spring.profiles.active}.yaml`

推荐你先落地这些 DataId（DEV/PROD 各一份），不要额外发明新的命名规则。

## 2. 迁移步骤（按一次迁移一个 DataId 来做）

下面以 DEV 环境为例（PROD 同理，只是 namespace 换成 PROD）。

### Step 1：先把“公共配置”放到 `ai-tutor-common-dev.yaml`

公共配置适合放：

- 网关签名校验参数（各服务一致）
- JWT issuer、token 过期时间（各服务一致）
- 通用的对象存储（MinIO）参数（各服务一致或大多数服务一致）
- 业务通用开关（例如 dev 打开 mock / 暴露验证码）

不建议放公共的：

- 某个服务独有的第三方 key（放到该服务 DataId）
- 支付相关（放到 `ai-tutor-payment[-dev].yaml` 或 payment-service 的 DataId）

操作：

1) 打开 Nacos Console
2) 选择命名空间 DEV
3) 配置管理 → 配置列表 → 新建配置
4) 填：
   - DataId：`ai-tutor-common-dev.yaml`
   - Group：`DEFAULT_GROUP`
   - 类型：YAML
5) 粘贴模板（见 `docs/nacos/templates/ai-tutor-common-dev.yaml`），把 `<TODO>` 逐个改成真实值
6) 发布

### Step 2：再把“服务专用配置”放到 `${serviceName}-dev.yaml`

例如：

- `ai-tutor-gateway-dev.yaml`：JWT secret / 签名 secret / 白名单路径等
- `tutor-appointment-service-dev.yaml`：微信小程序、上传限制、服务专用开关
- `videoCall-IM-service-dev.yaml`：brokerage/admin token、skip-payment-check 等
- `payment-service-dev.yaml`：支付开关、回调地址、三方 key 等
- `ai-tutor-admin-dev.yaml`：管理端专属开关、白名单等（如需要）

操作同 Step 1，只是 DataId 换为对应服务的 `*-dev.yaml`。

### Step 3：迁移“基础设施配置”（谨慎）

如果你希望 DB/Redis 也由 Nacos 管理：

- 先确认 DEV/PROD 各自的 MySQL/Redis 地址、账号、密码都稳定
- 再把 `spring.datasource.*` 与 `spring.data.redis.*` 放到 `ai-tutor-common-${env}.yaml`
- 最后再把本地 `application.yml` 里的 db/redis 移除（或保留本地默认值作为兜底）

强烈建议：对 PROD 的 DB/Redis 迁移，先在灰度/预发 namespace 验证一轮。

## 3. 如何验证迁移是否生效（必做）

### 3.1 看日志：服务启动时会打印监听的 DataId

关键日志长这样（示例）：

- `Listening config: dataId=ai-tutor-common-dev.yaml, group=DEFAULT_GROUP`
- `Listening config: dataId=tutor-appointment-service-dev.yaml, group=DEFAULT_GROUP`

如果你看不到这些日志：

- 检查服务的 `application.yml` 是否包含对应的 `spring.config.import=optional:nacos:...`
- 检查 `spring.cloud.nacos.config.server-addr/namespace` 是否正确（namespace 最容易错）

### 3.2 做一次“可观测的改动”验证动态刷新

例如把 `dev.exposeSmsCode` 从 `false` 改成 `true`，发布配置后：

- 如果代码用 `@RefreshScope` 或者配置绑定支持刷新，相关行为会变化
- 如果不支持动态刷新，也至少会在下次重启后生效

本仓库目前的 import 都加了 `refreshEnabled=true`，但是否实时刷新取决于代码是否绑定了动态刷新机制。

### 3.3 快速回滚

Nacos 每次发布都会保留历史版本。出问题直接回滚到上一版本，然后重启服务（或等待刷新）。

## 4. 安全与敏感信息（强制）

- 不要把以下内容写进 Git：
  - 支付私钥/证书、API key、JWT secret、签名 secret、短信/微信 secret、管理员 token
- 这些值应该只存在于：
  - Nacos（按 namespace 区分环境）
  - 或者 CI/CD 的安全变量注入（如果你们有这套）
- 本仓库提供的模板文件里会使用 `<TODO>` 占位，避免误提交真实密钥

## 5. 推荐的落地清单（从最小可运行到完善）

### 最小可运行（DEV）

- `ai-tutor-common-dev.yaml`
- `ai-tutor-gateway-dev.yaml`
- `tutor-appointment-service-dev.yaml`
- `videoCall-IM-service-dev.yaml`
- `payment-service-dev.yaml`
- `ai-tutor-admin-dev.yaml`

### 生产完善（PROD）

把上述每个 `*-dev.yaml` 在 PROD namespace 对应创建：

- `ai-tutor-common-prod.yaml`
- `ai-tutor-gateway-prod.yaml`
- ...

并逐项替换为生产值（域名、回调地址、第三方 key、数据库、redis 等）。
