# 易踩坑

这里只记录仓库专属事实。

## 当前已知坑点

- 仓库里有些文档已经过时。
  例如旧文档可能会描述 `/user/me` 缺失或角色控制未完成，但当前代码其实已经实现。
- `videoCall-IM-service` 的职责比名字看起来更宽。
  不要把它当成“纯 IM 模块”；它还承载了合作流程和部分交易相关逻辑。
- 认证问题通常要同时看三层：
  网关 JWT 解析、公共身份签名、以及服务拦截器。
- 聊天问题通常要同时看后端和前端实时状态。
  不要只改 `/chat/*` 就结束，还要检查 `ai-tutor-web/src/stores/chatRealtime.ts`。
- `scripts/dev_all_up.sh` 虽然方便，但它内置了较强的环境假设。
  在把它当成接近生产环境的行为之前，先确认 Nacos 和默认密钥等前提。
- 日常测试现在默认是本地应用 + 本地 Docker 中间件。
  除非用户明确说要远程联调、公网验收或支付回调链路，否则先用 `dev_local_*`，不要默认把应用放到 `111.228.20.88` 上跑。
- Nacos 是本地测试里的常见例外。
  `dev_all_up.sh` 会先尝试 `127.0.0.1:8848`，如果本机没有 Nacos，会自动建立到远端 dev Nacos 的隧道并使用 `127.0.0.1:18848`；这不代表其它中间件也在远端。
- `scripts/dev_all_down.sh` 现在默认会保留中间件。
  只有你明确想把 compose 管理的中间件一起停掉时，才使用 `STOP_INFRA=1`。
- 优先使用封装好的 wrapper 脚本，而不是直接随手调用 `dev_all_up/down`。
  `dev_local_*` 和 `dev_remote_*` 已经编码了本地和远程环境的更安全默认值。
- SSH 隧道现在是带独立 PID 的托管后台进程。
  用 `scripts/ssh_tunnel.sh stop` 或 `scripts/dev_remote_down.sh` 关闭，不要手动到处杀 SSH 进程。
- 当远程表现和本地脚本改动不一致时，先怀疑同步漂移。
  如果远程日志里没有你新加的 echo 文案，通常说明更新后的脚本还没上传到服务器。
- 远程开发默认应该尽量不碰中间件。
  `scripts/dev_remote_up.sh` 现在默认使用 `REMOTE_MANAGE_INFRA=never`。
- 当前生产目录已经切到 `/opt/ai-platform-prod`。
  排查线上问题时，先确认自己看的到底是不是这个目录；旧目录 `/opt/ai-platform` 更适合当开发/历史副本看待。
- 当前生产发布不是手工 rsync 到服务器。
  真实链路是 `master` -> GitHub Actions -> `111.228.20.88:/usr/local/bin/ai-platform-prod-deploy.sh` -> `/opt/ai-platform-prod`。
- 生产发布后，如果现场行为看起来和预期不一致，先核对服务器上的实际 checkout。
  优先执行 `cd /opt/ai-platform-prod && git branch --show-current && git rev-parse --short HEAD`，不要只看 GitHub Actions 是否显示绿色。
- 这个仓库跨环境共用一套 Nacos 服务器。
  环境隔离靠 namespace，不靠不同主机。当前默认是 `dev=481e4376-4576-4b18-ac19-f61e170ca3ae`、`prod=c3476048-10f6-4cc3-b3f1-90135d736a73`。
- 日常测试默认使用 `dev` namespace，除非用户明确说要用 `prod`。
- 当服务和 Nacos 跑在同一台服务器上时，远程启动脚本优先用 `127.0.0.1:8848`，不要优先用公网 IP。
  这样能避免额外依赖公网入口或 hairpin 网络。
- Nacos 2.x 不是“只要 8848 就行”。
  如果 Docker 只映射了 `8848`，但没映射 `9848/9849`，Java 服务可能看起来能访问配置 HTTP，却依然无法正确获取配置或注册发现数据，因为 gRPC 端口没通。
- 像 `storage.minio.endpoint must not be null` 这样的启动错误，可能不是 MinIO 自己的问题，而是 Nacos 没加载成功。
  在这个仓库里，如果 Nacos 中缺少 `ai-tutor-common-dev.yaml`，会级联出看上去和 Nacos 无关的 Bean 创建错误。
- 如果远程测试跳过 SSH 隧道、改走浏览器直连，前端 dev server 必须绑定到 `0.0.0.0`。
  否则服务器上的 `5173/5174` 即使进程在跑，外部机器也访问不到。
- `sh scripts/dev_all_up.sh` 现在会根据环境自动选择 Nacos 行为。
  在服务器上它应该用本地 `127.0.0.1:8848`；在笔记本上则可能自动打开专用 Nacos 隧道并切换到 `127.0.0.1:18848`。
- 这个仓库里，运行时配置排查通常是四层问题。
  在断言“Nacos 没工作”之前，要先检查启动环境变量、Nacos 导入链、可选 `.private` 文件，以及代码中的默认值。
- `tutor-appointment-service` 比其它服务多一层文件回退能力。
  它还会读取 `./.private/tutor-appointment-service.yml` 或 `../.private/tutor-appointment-service.yml`，所以它的有效配置路径会比其它后端更宽。
- 判断最终生效配置最快的证据在启动日志里，不在模板文件里。
  大多数时候，`bash scripts/verify_nacos_effect.sh` 比手动猜哪个 DataId 被加载更快。
- `huoyue.online` 当前已经是线上公网入口的一部分，不再只是“支付回调专用域名”。
  现在的真实拓扑是双层 nginx：`111.229.64.41` 负责域名和 TLS 第一跳，再把主站与 API 流量转发到 `111.228.20.88:80`。
- 在 `111.229.64.41` 上，`/ops/grafana/` 与 `/ops/prometheus/` 不是转发到业务机。
  它们仍指向域名机本地 nginx 配置，排查监控入口时不要误判成业务机路由问题。
- 云购收回调里即使出现 `PAY_NOTIFY failed reason=missing_order_no`，也不代表支付一定失败。
  还要继续检查是否出现了 `updated to SUCCESS by provider query` 和 `PAY_FINALIZE success`。
- 在当前远程支付测试里，最强的成功证据是三段日志链：
  域名服务器 access log -> `payment-service` 成功/落库日志 -> IM 服务里的 `tutor_application_paid`
- 任何会改数据库 schema 的仓库变更，如果没有在同一轮同步更新 `sqlDoc/`，就算未完成。
  最终回复里也应明确说明 `sqlDoc` 是否已更新。
- 对这个仓库来说，schema 同步不仅是迁移 SQL，也包括完整 bootstrap schema。
  新增或修改表时，要同时更新 `sqlDoc/migrations/*.sql` 和 `sqlDoc/huoyue.sql`，否则新环境和旧环境会产生漂移。
- `scripts/db_bootstrap_if_missing.sh` 只会初始化空数据库。
  如果一台机器上已经存在核心表，它会直接退出，不会补跑 `sqlDoc/migrations/` 下新加的文件；代码同步后要显式执行 `sh scripts/db_apply_migrations.sh`。
- 对这个仓库里的聊天/信息费链路来说，`brokerage_order` 表结构漂移会直接伪装成“前端首次点通过报系统内部异常”。
  共享开发库即使已经补过一部分列，也可能仍缺 `refund_locked`、`refunded_amount_fen` 这类较晚加入的字段；聊天页初始化会调用 `/chat/refund/state`，所以问题不一定出在“同意申请”接口本身，排查时要一起核对整张 `brokerage_order` 表是否与 `sqlDoc/huoyue.sql` 对齐。
- `sqlDoc/migrations/` 里的文件并不都完全幂等。
  例如 `20260301_student_job_posting_admin_alter.sql` 在重复执行时可能报 `Duplicate column name 'reject_reason'`，所以在老的共享开发库上批量重放迁移后，要额外确认目标 IM/支付表状态，而不要把“日志里没全绿”直接等同于“数据库一定坏了”。
- 这个仓库的任何 schema 变更，都必须在同一轮里同步应用到共享远程开发服务器 `111.228.20.88`。
  只改本地 SQL 文件、不做远程数据库同步，视为交付不完整。
- 共享远程开发服务器当前 shell 环境里没有可直接使用的本地 `mysql` 二进制。
  在那里手动同步或校验 schema 时，应通过 `docker exec` 使用实际运行中的 MySQL 容器。
- 不要假设远程 MySQL 容器名字就叫 `mysql`。
  在 `111.228.20.88` 上它可能是类似 `759d793c134e_mysql` 的 compose 生成名，所以执行数据库命令前先用 `docker ps --format '{{.Names}}' | grep mysql | head -1` 探测。
- 当用户说测试跑在服务器上时，远程 MinIO 静态资源工作应在 `111.228.20.88` 上完成，而不是在笔记本上做。
  应使用服务器上已运行的 `minio` 容器，并搭配 `--network container:minio` 的 `mc` 辅助容器；笔记本直接访问 `111.228.20.88:9000` 或 `111.228.20.88:18080` 可能被拦截，即使服务器侧网关验证其实是健康的。
- 当前业务机上的中间件是 Docker 常驻容器，不是每次发版跟着重建。
  实际长期运行的包括 `mysql`、`redis`、`minio`、`nacos`、`livekit`、`grafana`、`prometheus`、`loki`、`promtail`、`node-exporter`、`alertmanager`；日常发版通常只停业务进程，不停中间件。
- 在 Linux 远程服务器上，不要盲目复用默认 MinIO 地址是 `host.docker.internal:9000` 的脚本。
  这个主机名主要适用于 Docker Desktop 风格的本地环境；共享远程 MinIO 应从 `--network container:minio` 容器内部访问 `http://127.0.0.1:9000`。
- 用户端 Web 新增课程页时，除了页面实现本身，还要同时检查 Vite dev proxy 与 Gateway 路由是否包含 `/courses/**`。
  这个仓库里的课程相关接口实际走 `videoCall-IM-service`；如果漏掉这层，页面可以正常编译和打开，但真实请求会在 `18080` 上返回 `404`，而不是业务鉴权应有的 `401/403`。
- 管理端如果要挂在 `/admin/` 这类子路径下，远程启动脚本也必须把 `ADMIN_WEB_BASE_PATH` 传给 `dev_all_up.sh`。
  否则 `nginx` 虽然能把 `/admin/` 代理到 `5174`，但管理端返回的 HTML 里脚本仍是根路径 `/src/main.ts`、`/@vite/client`，最终会表现为公网管理页空白。
- 这个仓库里的首页和产品文案工作，必须明确区分“当前已运营能力”和“战略愿景”。
  当前业务仍处于预上线、第一阶段模式，所以不要把学生 AI 学习代理、平台内完整授课闭环、按课时持续抽成等规划中能力写成已经落地。
- `ai-agent-service` 当前是内部 Agent 服务，不是前端直连服务。
  第一版接口通过网关 `/internal/ai/**` 暴露给内部 Java 微服务使用；如果未来需要纳入统一一键启动脚本，优先增加显式开关，不要直接默认随所有服务一起启动。
- 实时课堂的 `/livekit` WebSocket 能通，不等于真实音视频也通。
  在当前共享 DEV 拓扑里，`111.229.64.41` 只负责 `80/443` 和 `/livekit` 信令转发；浏览器真正的音视频流会直接访问 `111.228.20.88` 的 `TCP 7881` 和 `UDP 50000-50100`。如果这里没放通，页面会表现为双方都在等待对方，哪怕课堂业务层已经进入同一个房间。
- `Dockerfile/docker-compose.yml` 里原先部分 `docker.m.daocloud.io/...` 镜像在当前环境下会出现 `403 Forbidden` 或下载 `EOF`。
  如果本地 `dev_local_up.sh` 卡在镜像拉取，优先改回官方镜像地址再重试，不要先怀疑 Docker daemon 或 compose 脚本本身。
- `rabbitmq-exporter` 的 `v1.0.0-RC19` 在当前环境下不可用。
  这个仓库本地 compose 现改为 `kbudde/rabbitmq-exporter:latest`；如果以后又看到 `manifest not found`，先检查 tag 漂移而不是 RabbitMQ 配置。

## 维护规则

每当你发现下面任意一种情况，就补一条记录：

- 一个会误导后续工作的过时文档
- 一个隐藏的跨服务依赖
- 一个容易遗漏但实际必需的验证步骤
- 一个会改变行为的环境假设
