# 运行与配置

当任务涉及启动、远程测试、Nacos、环境变量，或“这个配置到底从哪来”之类的问题时，请使用这份参考。

## 当前共享环境

- 共享业务服务器：
  `root@111.228.20.88`
- 共享域名/TLS 入口服务器：
  `root@111.229.64.41`
- 生产仓库路径：
  `/opt/ai-platform-prod`
- 开发/历史仓库路径：
  `/opt/ai-platform`
- 当前共享中间件宿主机：
  `111.228.20.88`
- 当前共享 MinIO：
  容器 `minio`，服务端 API `127.0.0.1:9000`，bucket `ai-tutor-assets`
- 当前共享 Nacos：
  `111.228.20.88:8848`
- 当前 namespaces：
  `dev=481e4376-4576-4b18-ac19-f61e170ca3ae`
  `prod=c3476048-10f6-4cc3-b3f1-90135d736a73`
- 日常工作默认使用 `dev`，除非用户明确指定 `prod`
- 当前分支约定：
  本地开发/联调优先 `dev`
  生产自动部署来源 `master`
- 当前生产自动部署：
  `.github/workflows/deploy-prod.yml`
  `push master` -> SSH `111.228.20.88` -> `/usr/local/bin/ai-platform-prod-deploy.sh`

## 项目通常如何运行

### 本地

- 推荐入口：
  `bash scripts/dev_local_up.sh`
- 本地停止：
  `bash scripts/dev_local_down.sh`
- 真正的启动核心逻辑在 `scripts/dev_all_up.sh`
- 本地默认行为：
  `MANAGE_INFRA=auto`
  `SPRING_PROFILES_ACTIVE=dev`
  应用进程在本机启动
  MySQL、Redis、RabbitMQ、MinIO、LiveKit、Prometheus/Grafana 等中间件默认使用本机 Docker
  优先使用本地 Nacos `127.0.0.1:8848`
  如果本地 Nacos 不可用，则自动打开本地 Nacos SSH 隧道并切换到 `127.0.0.1:18848`，此时只有 Nacos 使用远端 dev 配置中心

### 共享远程服务器

- 从笔记本发起的推荐远程启动方式：
  `bash scripts/dev_remote_up.sh`
- 从笔记本发起的推荐远程停止方式：
  `bash scripts/dev_remote_down.sh`
- 当前远程默认值：
  `REMOTE_MANAGE_INFRA=never`
  `REMOTE_NACOS_SERVER_ADDR=127.0.0.1:8848`
  `REMOTE_NACOS_GRPC_CHECK=warn`
  `REMOTE_USE_TUNNEL=1`
- 这意味着：服务器上的中间件应已长期运行，而应用进程则从仓库副本中启动
- 在当前支付回调拓扑下，`111.228.20.88` 仍然是真正承载应用和中间件的主机
- 除非用户明确说明不是，否则远程测试默认认为 MinIO、MySQL、Redis、RabbitMQ、Nacos、Prometheus、Grafana 都在这台共享服务器上
- 当前业务机 nginx 会把：
  `/` -> `127.0.0.1:5173`
  `/admin/` -> `127.0.0.1:5174`
  `/api/`、`/org/`、`/user/`、`/invite/`、`/chat/`、`/payment/` -> `127.0.0.1:18080`
  `/api/admin/` -> `127.0.0.1:18084`
  `/livekit` -> `127.0.0.1:7880`
  `/ops/grafana/` -> `127.0.0.1:3000`
  `/ops/prometheus/` -> `127.0.0.1:9090`

### 直接在服务器上

- 开发环境常见启动方式：
  `cd /opt/ai-platform && MANAGE_INFRA=never sh scripts/dev_all_up.sh`
- 开发环境常见停止方式：
  `cd /opt/ai-platform && STOP_INFRA=0 sh scripts/dev_all_down.sh`
- 只有当服务器确实应该由仓库脚本管理中间件时，才用 `MANAGE_INFRA=auto`
- 对于当前 `111.228.20.88` 上的共享支付测试，更明确的启动命令是：
  `cd /opt/ai-platform && MANAGE_INFRA=never FRONTEND_HOST=0.0.0.0 NACOS_SERVER_ADDR=127.0.0.1:8848 sh scripts/dev_local_up.sh`
- 当前生产环境手动启动方式是：
  `cd /opt/ai-platform-prod && SPRING_PROFILES_ACTIVE=prod MANAGE_INFRA=never AUTO_BOOTSTRAP_DEV_DB=0 NACOS_SERVER_ADDR=127.0.0.1:8848 NACOS_GRPC_CHECK=warn FRONTEND_HOST=127.0.0.1 sh scripts/dev_all_up.sh`
- 当前生产环境手动停止方式是：
  `cd /opt/ai-platform-prod && STOP_INFRA=0 sh scripts/dev_all_down.sh`

### 域名回调代理服务器

- `111.229.64.41` 只运行第一层 `nginx`
- `huoyue.online` 应解析到 `111.229.64.41`
- 主站与 API 入口会再被转发到 `111.228.20.88:80`
- `/payment/notify/*` 和 `/payment/return/*` 也属于同一条公网入口链路
- `/ops/grafana/` 与 `/ops/prometheus/` 在这台机器上仍是本地规则，不是继续代理到业务机

## `dev_all_up.sh` 会启动什么

- Gateway：
  `ai-tutor-gateway`，端口 `18080`
- Appointment / 用户服务：
  `tutor-appointment-service`，端口 `18081`
- IM / 聊天 / 合作服务：
  `videoCall-IM-service`，端口 `18082`
- Payment 服务：
  `payment-service`，端口 `18083`
- 管理后端：
  `ai-tutor-admin`，端口 `18084`
- 用户端 Web：
  `ai-tutor-web`，端口 `5173`
- 管理端 Web：
  `ai-tutor-admin-web`，端口 `5174`

运行产物：

- 日志：
  `.logs/*.log`
- pid 文件：
  `.pids/*.pid`

## 共享远程 MinIO

- MinIO 运行在 `111.228.20.88` 上，对应 Docker 容器 `minio`
- 服务端 API 端口是 `127.0.0.1:9000`；Console 是 `127.0.0.1:9001`
- 项目 bucket 是 `ai-tutor-assets`
- 通过应用网关对外暴露的公共资源路径是 `/api/v1/public/assets/{objectKey}`
- 当前品牌 logo 资源对象位于：
  `brand/logo-icon.svg`
  `brand/favicon.svg`
- 从远程服务器上验证时，优先使用：
  `curl -I http://127.0.0.1:18080/api/v1/public/assets/brand/logo-icon.svg`
- 不要依赖笔记本直接访问 `111.228.20.88:9000` 或 `111.228.20.88:18080`；即使服务器自身服务健康，这些端口也可能对笔记本不可达

## 启动决策规则

- Wrapper 脚本比直接调用 `dev_all_up/down` 更安全
- `dev_local_up.sh` 是日常测试默认入口：应用和大部分中间件本地跑，Nacos 按本机可用性自动选择本机或远端 dev 隧道
- `dev_remote_up.sh` 用于“在服务器启动、在本地浏览器访问”的场景
- `dev_all_up.sh` 是共享引擎，现在既可以在本机直接跑，也可以在服务器直接跑；直接调用时要显式确认 `MANAGE_INFRA` 和 `NACOS_SERVER_ADDR`
- 本地日常联调默认先用 `bash scripts/dev_local_up.sh` / `bash scripts/dev_local_down.sh`
- 生产发布默认不要手工执行一串命令，优先让 GitHub Actions 调用服务器上的 `/usr/local/bin/ai-platform-prod-deploy.sh`
- 远程 wrapper 会在启动前把关键启动脚本和 `common.md` 同步到服务器
- 如果远程表现和本地代码不一致，先怀疑同步漂移

## 配置来源的真实顺序

对后端服务来说，运行时配置来自四层：

1. 环境变量
2. 各服务 `application.yml` 导入的 Nacos 配置
3. 某些服务存在的本地可选回退文件
4. 代码中的 `@ConfigurationProperties` 或 `@Value` 默认值

一个重要结论：

- 排查“配置缺失”时，不要只看 Nacos
- 还要检查启动脚本传入的环境变量
- 以及代码里是否有默认值掩盖了真正的问题

## 各服务的 Nacos 导入链

### `ai-tutor-gateway`

按顺序导入：

- `ai-tutor-common.yaml`
- `ai-tutor-common-${spring.profiles.active}.yaml`
- `ai-tutor-gateway.yaml`
- `ai-tutor-gateway-${spring.profiles.active}.yaml`

### `tutor-appointment-service`

按顺序导入：

- `ai-tutor-common.yaml`
- `ai-tutor-common-${spring.profiles.active}.yaml`
- `ai-tutor-home.yaml`
- `ai-tutor-home-${spring.profiles.active}.yaml`
- `ai-tutor-sms.yaml`
- `ai-tutor-sms-${spring.profiles.active}.yaml`
- `tutor-appointment-service.yaml`
- `tutor-appointment-service-${spring.profiles.active}.yaml`
- 可选回退文件 `./.private/tutor-appointment-service.yml`
- 可选回退文件 `../.private/tutor-appointment-service.yml`

### `videoCall-IM-service`

按顺序导入：

- `ai-tutor-common.yaml`
- `ai-tutor-common-${spring.profiles.active}.yaml`
- `ai-tutor-home.yaml`
- `ai-tutor-home-${spring.profiles.active}.yaml`
- `videoCall-IM-service.yaml`
- `videoCall-IM-service-${spring.profiles.active}.yaml`

### `payment-service`

按顺序导入：

- `ai-tutor-common.yaml`
- `ai-tutor-common-${spring.profiles.active}.yaml`
- `ai-tutor-payment.yaml`
- `ai-tutor-payment-${spring.profiles.active}.yaml`
- `payment-service.yaml`
- `payment-service-${spring.profiles.active}.yaml`

### `ai-tutor-admin`

按顺序导入：

- `ai-tutor-common.yaml`
- `ai-tutor-common-${spring.profiles.active}.yaml`
- `ai-tutor-admin.yaml`
- `ai-tutor-admin-${spring.profiles.active}.yaml`

## 优先排查的高价值 DataId

在共享 `dev` namespace 中，下面这些通常是第一优先级：

- `ai-tutor-common-dev.yaml`
  共享 JWT、网关签名、MinIO、上传、开发开关
- `ai-tutor-gateway-dev.yaml`
  网关 JWT / 签名配置
- `ai-tutor-home-dev.yaml`
  首页游客内容和运营配置
- `ai-tutor-sms-dev.yaml`
  短信供应商开关和 Spug token
- `tutor-appointment-service-dev.yaml`
  appointment 服务独有开关、小程序配置
- `videoCall-IM-service-dev.yaml`
- `videoCall-IM-service-prod.yaml`
  佣金金额、管理员 token、支付开关、MQ
- `ai-tutor-payment-dev.yaml`
  支付平台 key、notify URL、return URL、支付启用开关
- `payment-service-dev.yaml`
  支付重试与服务本地支付配置
- `ai-tutor-admin-dev.yaml`
  管理端动态开关

这些模板文件位于：

- `docs/nacos/templates/*.yaml`

当用户问“这个 Nacos 该填什么”时，先从对应模板开始看，再决定是否继续读代码。

`videoCall-IM-service` 的信息费配置现在最容易混淆，建议优先看模板里的注释：

- `brokerage.info-fee.unified.*`
  新统一信息费开关；开了以后，所有新信息费订单都会直接使用统一金额
- `brokerage.amount-fen`
  旧默认金额；现在只在动态计算失败时作为兜底值

对应模板：

- `docs/nacos/templates/videoCall-IM-service-dev.yaml`
- `docs/nacos/templates/videoCall-IM-service-prod.yaml`

### 当前最值得先复查的支付配置

在 `ai-tutor-payment-dev.yaml` 中，最关键的线上字段有：

- `payment.enabled`
- `alipay.notifyUrl`
- `wechat.notifyUrl`
- `yungouos.notify-url`
- `yungouos.return-url`
- `yungouos.return-page-url`

当前共享远程支付测试预期值：

- `yungouos.notify-url: http://huoyue.online/payment/notify/yungouos`
- `yungouos.return-url: http://huoyue.online/payment/return/yungouos`

说明：

- `return-page-url` 目前仍可能指向业务主机前端
- 判断异步支付回调是否正确时，优先看日志，不要只看浏览器最终落在哪个页面

## 如何确定一个配置键是什么意思

从代码里的绑定点开始看：

- `@ConfigurationProperties(prefix = "...")`
- `@Value("${...}")`

仓库里已有的一些常见示例：

- `jwt.*`
  `tutor-appointment-service/.../config/JwtProperties.java`
- `storage.minio.*`
  `tutor-appointment-service/.../storage/MinioProperties.java`
- `storage.upload.*`
  `tutor-appointment-service/.../storage/UploadProperties.java`
- `gateway.sign.*`
  `ai-tutor-common/.../security/IdentitySignProperties.java`
  `ai-tutor-gateway/.../security/GatewaySignProperties.java`
- `gateway.jwt.*`
  `ai-tutor-gateway/.../security/GatewayJwtProperties.java`
- `payment.*`
  `payment-service/.../config/PaymentProperties.java`
- `home.guest.*`
  `tutor-appointment-service/.../config/HomeGuestProperties.java`
- `wechat.miniapp.*`
  `tutor-appointment-service/.../config/WechatProperties.java`
- `sms.*`
  `tutor-appointment-service/.../config/SmsProperties.java`
  `tutor-appointment-service/.../config/SmsSpugProperties.java`

快速 grep：

```bash
rg -n "@ConfigurationProperties|@Value\\(" tutor-appointment-service videoCall-IM-service payment-service ai-tutor-admin ai-tutor-gateway ai-tutor-common
```

## 如何读取真正生效的运行时配置

### 1. 先确认当前用了哪个 namespace 和 Nacos 地址

查看 `dev_all_up.sh` 的启动输出：

- `profile=...`
- `nacos.server-addr=...`
- `nacos.namespace=...`
- `nacos.config.namespace=...`
- `nacos.discovery.namespace=...`

这些行是判断当前启动模式最快的真相来源。

### 2. 确认哪些 DataId 实际被加载了

执行：

```bash
bash scripts/verify_nacos_effect.sh
```

或者查看日志里这些内容：

- `[Nacos Config] Load config[dataId=...]`
- `[Nacos Config] Listening config: ...`

如果预期的 DataId 没出现在日志里，通常是以下原因之一：

- namespace 错了
- Nacos 地址错了
- Nacos 里缺少对应 DataId
- 服务在进入配置导入阶段之前就已经失败退出

### 3. 看模板，再和线上 Nacos 对比

模板值位于：

- `docs/nacos/templates/*.yaml`

线上值应在 Nacos 控制台里、当前启用的 namespace 下核对，通常是 `dev`。

### 4. 在断言“配置没生效”前，先看代码默认值

有些键在代码里有直接默认值，例如：

- `brokerage.info-fee.unified.enabled:false`
- `brokerage.info-fee.unified.amount-fen:19900`
- `rocketmq.name-server:127.0.0.1:9876`
- `tutor-application.skip-payment-check:false`

当 `brokerage.info-fee.unified.enabled=false` 时，IM 服务当前会按业务规则动态计算信息费：

- 只看每周频次
- 每周 1/2/3/4/5 次分别收一周课时费的 100%/90%/80%/70%/60%
- 每周超过 5 次收 50%
- 价格区间取上下限均值，单值取原值
- 当前报价字段如果是时薪，代码里会按 2 小时/次折算成单次课时费

如果系统行为和 Nacos 不一致，要先确认是不是落回了代码默认值。

## Nacos 与远程中间件的坑点

- Nacos 2.x 需要 gRPC 端口，不只是 HTTP 端口
- 对同机 Java 服务来说，关键路径是 `127.0.0.1:8848` 加上可达的 `9848/9849`
- Docker 里如果只映射 `8848`，HTTP 看起来正常，但 Java 侧配置/注册发现仍可能失败
- 在共享服务器上，应保持中间件常驻，而应用进程使用 `MANAGE_INFRA=never` 启动

## 实战恢复检查清单

当某个服务在服务器上“启动后又挂掉”时：

1. 查看 `.logs/<service>.log`
2. 执行 `bash scripts/verify_nacos_effect.sh`
3. 确认服务器上 `127.0.0.1:8848`、`9848`、`9849` 可达
4. 确认预期的 `*-dev.yaml` 存在于 `dev` namespace
5. grep 对应配置类或 `@Value` 消费点
6. 只有做完这些之后，再去判断问题到底是 Nacos、DB、Redis、MQ、MinIO 还是业务逻辑
