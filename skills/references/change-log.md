# 变更日志

每次在使用这个 skill 的过程中完成并验证仓库变更后，都要追加一条简短记录。

## 模板

```text
## YYYY-MM-DD
- 请求：
- 涉及区域：
- 已检查背景：
- 验证：
- 新增说明：
```

## 2026-04-20

## 2026-04-23

- 请求：
  收口线上 DEV 实时课堂音视频问题，定位“双方都进入课堂但仍互相看不到/听不到”，并把排查与验证流程沉淀进项目 skill
- 涉及区域：
  `skills/`、`ai-tutor-web/e2e/`、共享远程双服务器拓扑、LiveKit 运行链路
- 已检查背景：
  双服务器 nginx 转发、LiveKit 配置、浏览器 E2E、线上抓包、共享 DEV 云端口放通情况
- 验证：
  执行了 `nc -vz -w 4 111.228.20.88 7881`
  执行了线上抓包 `tcpdump "(udp portrange 50000-50100) or tcp port 7881"`
  执行了 `cd ai-tutor-web && PLAYWRIGHT_BASE_URL=https://huoyue.online PLAYWRIGHT_API_BASE_URL=https://huoyue.online OPS_VERIFY_TOKEN=DevOpsVerifyTokenForE2E npx playwright test e2e/live-classroom.spec.ts --project=chromium`
- 新增说明：
  当前共享 DEV 实时课堂的根因和验证方法已经固定：`111.229.64.41` 只承担 `80/443` 与 `/livekit` 信令转发，真实音视频必须直达 `111.228.20.88` 的 `TCP 7881` 与 `UDP 50000-50100`

- 请求：
  新增第一版 `ai-agent-service` 内部 AI 微服务，并同步数据库 bootstrap schema、网关内部路由、Nacos 模板和服务专属 skill
- 涉及区域：
  `ai-agent-service/`、`ai-tutor-gateway/`、`sqlDoc/`、`docs/nacos/templates/`、`skills/`
- 已检查背景：
  网关路由、运行时配置、远程同步脚本、数据库同步约定
- 验证：
  执行了 `cd ai-agent-service && AI_AGENT_USE_ASYNC_WORKER=false pytest tests -q`
  执行了 `cd ai-agent-service && python -m compileall app tests -q`
- 新增说明：
  `ai-agent-service` 第一版只服务内部 Java 微服务，默认不纳入现有统一启动脚本；如果未来接入统一启动，需要显式开关以避免影响当前稳定链路

- 请求：
  为 `ai-agent-service` 增加实时课堂 AI 的 P1/P2 骨架，并接入 LangChain、LangGraph、腾讯云 ASR 配置和火山方舟 EP 配置
- 涉及区域：
  `ai-agent-service/`、`sqlDoc/`、`docs/nacos/templates/`、`skills/`
- 已检查背景：
  `ai-agent-service` 定位、实时课堂 AI PRD、后端设计、腾讯云 ASR 官方 SDK 接口、火山方舟 OpenAI 兼容接入模式
- 验证：
  本轮完成了代码骨架、表结构、Nacos 模板和说明文档同步
- 新增说明：
  实时课堂 AI 当前已经在 `ai-agent-service` 中建立了可扩展主链路，但 LiveKit 真正音频桥接和 Java 侧调用接入仍需下一步联调

- 请求：
  重做“我的课程”页，补齐长期课程状态展示、聊天跳转、线上约课弹窗、提醒与链路验证
- 涉及区域：
  `ai-tutor-web/`、`ai-tutor-gateway/`、`skills/`
- 已检查背景：
  用户端 Web 路由、聊天/合作流程、远程运行与网关配置
- 验证：
  执行了 `cd ai-tutor-web && npm run typecheck`
  执行了 `cd ai-tutor-web && npm run test -- src/pages/course/MyCoursesPage.test.ts`
  执行了 `REMOTE_SYNC_DELETE=0 bash scripts/dev_remote_sync_up.sh`
  在 `111.228.20.88` 上执行了 `cd /opt/ai-platform/ai-tutor-web && npm run typecheck`
  在 `111.228.20.88` 上执行了 `cd /opt/ai-platform/ai-tutor-web && npm run test -- src/pages/course/MyCoursesPage.test.ts`
  在 `111.228.20.88` 上执行了 `cd /opt/ai-platform && scripts/e2e/run_live_class_flow_e2e.sh`
  在 `111.228.20.88` 上验证了 `http://127.0.0.1:18080/courses/my?page=1&size=1&role=STUDENT` 返回 `401 Unauthorized`
  在 `111.228.20.88` 上验证了 `http://127.0.0.1:5173/courses/my?page=1&size=1&role=STUDENT` 返回 `401 Unauthorized`
- 新增说明：
  课程页要真正可用，除了页面实现本身，还必须补齐 `/courses/**` 的前端代理和网关转发，否则会出现“页面可打开但接口 404”的假通路

## 2026-04-20

- 请求：
  修复 `http://huoyue.online/admin/` 管理端入口空白页问题
- 涉及区域：
  `scripts/`、共享远程管理端启动链路
- 已检查背景：
  双服务器公网转发拓扑、管理端 Vite base 配置、业务机与域名机 nginx 转发
- 验证：
  执行了 `sh -n scripts/dev_remote_sync_up.sh`
  执行了 `sh -n scripts/dev_remote_up.sh`
  执行了 `REMOTE_SYNC_DELETE=0 bash scripts/dev_remote_sync_up.sh`
  验证 `http://127.0.0.1:5174/admin/` 返回的 HTML 已使用 `/admin/@vite/client` 与 `/admin/src/main.ts`
  验证 `http://127.0.0.1/admin/` 返回的 HTML 已使用 `/admin/` 前缀资源
  验证 `http://huoyue.online/admin/` 返回的 HTML 已使用 `/admin/` 前缀资源
  验证 `http://huoyue.online/admin/src/main.ts` 返回 `200 OK`
- 新增说明：
  管理端挂在 `/admin/` 子路径时，不能只配 nginx 反代，还必须让远程启动脚本把 `ADMIN_WEB_BASE_PATH=/admin/` 传给前端 dev server

## 2026-04-20

- 请求：
  修复学生主动向教师发起家教申请后，教师首次点击通过报“系统内部异常”且聊天里不主动出现信息费支付提示的问题
- 涉及区域：
  `videoCall-IM-service/`、`ai-tutor-web/`、共享远程 MySQL schema、`skills/`
- 已检查背景：
  聊天/合作/支付流程、共享远程双服务器拓扑、远程运行与数据库迁移约定
- 验证：
  执行了 `cd ai-tutor-web && npm run test -- ChatRoomPage.realtime.test.ts`
  执行了 `REMOTE_SYNC_DELETE=0 bash scripts/dev_remote_sync_up.sh`
  在 `111.228.20.88` 上通过 MySQL 容器补跑了 `sqlDoc/migrations/20260419_system_invite_promotion.sql`
  在 `111.228.20.88` 上通过 MySQL 容器补跑了 `sqlDoc/migrations/20260404_brokerage_order_refund_and_trial_fields.sql`
  验证共享远程库 `brokerage_order` 已存在 `original_amount_fen`、`discount_amount_fen`、`promotion_type`、`promotion_snapshot_json`、`refund_locked`、`refunded_amount_fen`
  执行了 `cd ai-tutor-web && node scripts/e2e-student-apply-accept-payment.mjs`
  浏览器端到端验证显示：教师首次点击“通过”后，聊天页不再出现“系统内部异常”，并即时渲染“信息费 / 去支付”卡片；对应后端日志记录 `tutor_application_decided ... status=ACCEPTED`
- 新增说明：
  这条链路的“首次通过报错”可能是两类问题叠加：一类是 `decision-message` 自调用导致事务未完整包住，另一类是共享开发库 `brokerage_order` 表结构漂移导致聊天页初始化时的 `/chat/refund/state` 查询炸掉

## 2026-04-12

- 请求：
  为本仓库创建第一版可持续维护的项目 skill
- 涉及区域：
  `skills/`
- 已检查背景：
  仓库结构、前后端模块地图、启动脚本、skill-creator 指引
- 验证：
  执行了 `bash skills/scripts/project-snapshot.sh`
  执行了 `bash skills/scripts/changed-area-check.sh ai-tutor-web/src/pages/chat/ChatRoomPage.vue payment-service/src/main/java/com/ai/tutor/payment/service/YungouosPaymentAppService.java`
- 新增说明：
  初版项目 skill 已拆分为精简 `SKILL.md`、参考文档和路由脚本

## 2026-04-12

- 请求：
  优化远程服务器启动流程以适配常驻中间件，并补一个可复用的 SSH 隧道辅助脚本
- 涉及区域：
  `scripts/`、`skills/references/`
- 已检查背景：
  启动脚本、远程开发流程、skill 维护约定
- 验证：
  执行了 `sh -n scripts/dev_all_up.sh`
  执行了 `sh -n scripts/dev_all_down.sh`
  执行了 `sh -n scripts/ssh_tunnel.sh`
- 新增说明：
  已记录远程环境中的 `MANAGE_INFRA=auto` 与 `STOP_INFRA=1` 流程

## 2026-04-12

- 请求：
  将本地与远程启动流程拆成独立脚本，并把远程停止流程纳入隧道关闭
- 涉及区域：
  `scripts/`、`skills/references/`
- 已检查背景：
  启动脚本、远程工作流预期、隧道生命周期要求
- 验证：
  执行了 `sh -n scripts/dev_local_up.sh`
  执行了 `sh -n scripts/dev_local_down.sh`
  执行了 `sh -n scripts/dev_remote_up.sh`
  执行了 `sh -n scripts/dev_remote_down.sh`
  执行了 `sh -n scripts/ssh_tunnel.sh`
- 新增说明：
  本地与远程开发现在明确使用不同入口脚本

## 2026-04-12

- 请求：
  修复远程启动默认值，解决常驻中间件服务器上的容器冲突问题
- 涉及区域：
  `scripts/`、`skills/references/`
- 已检查背景：
  启动流程、远程环境假设、中间件容器生命周期
- 验证：
  执行了 `sh -n scripts/dev_all_up.sh`
  执行了 `sh -n scripts/dev_remote_up.sh`
- 新增说明：
  远程启动默认值现为 `REMOTE_MANAGE_INFRA=never`，并且 `auto` 模式会更安全地复用已有容器

## 2026-04-13

- 请求：
  让仓库默认值与真实共享 Nacos 服务器及当前 dev/prod namespace ID 对齐
- 涉及区域：
  `scripts/`、服务 `application.yml`、`common.md`、`skills/references/`
- 已检查背景：
  当前启动默认值、服务 Nacos 占位配置、确认后的环境 namespace ID
- 验证：
  搜索并替换了所有旧 namespace ID
  变更后执行了启动脚本的 shell 语法检查
- 新增说明：
  启动默认已切到真实 `dev` namespace，并记录了当前 `prod` namespace 以便显式切换

## 2026-04-13

- 请求：
  当 Nacos 与服务运行在同一台服务器上时，让远程启动默认使用 localhost Nacos
- 涉及区域：
  `scripts/`、`common.md`、`skills/references/`
- 已检查背景：
  远程启动流程、当前共享 Nacos 的同机部署方式
- 验证：
  检查了远程 wrapper 默认值，并同步更新了文档
- 新增说明：
  远程启动默认值现为 `REMOTE_NACOS_SERVER_ADDR=127.0.0.1:8848`

## 2026-04-13

- 请求：
  支持远程浏览器直连测试，并补充真实的 Nacos 2.x 端口要求说明
- 涉及区域：
  `scripts/`、`common.md`、`skills/references/`
- 已检查背景：
  远程启动流程、前端 dev server 绑定方式、通过 `docker inspect` 确认的 Nacos 端口映射
- 验证：
  执行了更新后启动脚本的 shell 语法检查
  结合当前服务器端 Nacos 行为和 namespace 默认值复核了文档
- 新增说明：
  远程启动现支持 `REMOTE_USE_TUNNEL=0`，文档中也明确指出 Nacos 2.x 除了 `8848` 还需要 `9848/9849`

## 2026-04-13

- 请求：
  在重建共享远程 Nacos 环境时，修复过时的迁移指南 namespace ID
- 涉及区域：
  `docs/nacos/CONFIG_MIGRATION_GUIDE.md`
- 已检查背景：
  当前仓库启动默认值、已确认的线上 dev/prod namespace ID、Nacos 恢复流程
- 验证：
  搜索并替换了迁移指南中的旧 namespace ID
- 新增说明：
  迁移指南现已与实际启用的共享 Nacos `dev/prod` namespace ID 保持一致

## 2026-04-13

- 请求：
  让仓库启动脚本同时兼容只提供旧版 `docker-compose` 的环境
- 涉及区域：
  `scripts/`、`skills/references/`
- 已检查背景：
  当前服务器上的 Docker 命令行为、启动/停止脚本、仓库辅助脚本中的 compose 用法
- 验证：
  通过 `sh -n` 与 `bash -n` 检查了更新后脚本语法
- 新增说明：
  核心开发脚本现在会自动检测 `docker compose` 与 `docker-compose`

## 2026-04-13

- 请求：
  让 `sh scripts/dev_all_up.sh` 在重建后的共享 Nacos 环境下，可以直接在服务器和本地机器上运行
- 涉及区域：
  `scripts/`、服务 `application.yml`、`common.md`、`skills/references/`
- 已检查背景：
  当前启动流程、重建后的 Nacos 部署、新的共享 `dev` namespace ID、本地与服务器对 Nacos 的可达性
- 验证：
  新增了专用 Nacos 隧道脚本，并更新启动默认值以自动选择本地或隧道 Nacos
  把仓库默认值改到了当前真实 `dev` namespace ID
- 新增说明：
  `dev_all_up.sh` 现在会优先使用本地 `127.0.0.1:8848`，并能在离机环境下自动建立本地 Nacos 隧道

## 2026-04-14

- 请求：
  总结项目当前在线运行方式、本地常见测试方式，以及运行时配置加载与查询逻辑，并沉淀进项目 skill
- 涉及区域：
  `skills/`、启动脚本、服务 `application.yml`、Nacos 模板、`common.md`
- 已检查背景：
  当前 wrapper 启动流程、直接在服务器运行的方式、Nacos 导入链、共享 namespace 默认值、配置模板
- 验证：
  审阅了 `scripts/dev_all_up.sh`、`scripts/dev_remote_up.sh`、`scripts/dev_all_down.sh`
  审阅了后端 `application.yml` 导入链
  审阅了 `docs/nacos/templates/*.yaml` 与 `common.md`
- 新增说明：
  skill 现已包含独立的运行时/配置参考文档，覆盖本地与远程启动、生效配置定位和高价值 Nacos DataId

## 2026-04-16

- 请求：
  把已经跑通的支付回调测试拓扑、真实验证流程、服务器角色、产品支付流程与 Nacos 支付配置沉淀到项目 skill
- 涉及区域：
  `skills/`
- 已检查背景：
  当前启动脚本、线上远程服务器角色、来自 `111.229.64.41` 与 `111.228.20.88` 的已验证回调日志、共享 Nacos 支付配置
- 验证：
  审阅了 `skills/SKILL.md`
  审阅了支付、业务、运行时与坑点相关参考文档
  合并了 `2026-04-16` 实测支付成功链路中的真实观察结果
- 新增说明：
  新增了独立的支付远程测试参考文档，并将支付/运行时工作路由到该文档

## 2026-04-16

- 请求：
  将前端品牌名改为 `创智星球`，优化聊天实时消息提示与未读红点行为，并为 schema 变更增加永久性 `sqlDoc` 同步规则
- 涉及区域：
  `ai-tutor-web/`、`skills/`
- 已检查背景：
  聊天实时 store、顶部未读入口、当前 SSE/未读/ack 流程、现有品牌文案位置、skill 维护规则
- 验证：
  执行了 `cd ai-tutor-web && npm run typecheck`
  执行了 `cd ai-tutor-web && npm run lint`，确认没有错误，只有仓库中已有的 Vue 格式化警告
- 新增说明：
  聊天实时提示现复用已有 SSE 做轻量新消息提醒和乐观已读抑制，同时 skill 也要求任何 schema 变更必须在同一轮同步 `sqlDoc/`

## 2026-04-18

- 请求：
  重新核对当前已完成的 IM 功能集，将支持能力沉淀进项目 skill，并确认共享远程开发服务器是否存在代码或数据库迁移未同步
- 涉及区域：
  `skills/`、`scripts/`、聊天/实时功能区域、共享远程开发流程
- 已检查背景：
  skill 参考文档、远程启动脚本、迁移辅助脚本、近期 IM 相关提交
- 验证：
  重新执行了聊天前端和 IM 后端的定向测试
  确认共享远程开发仓库落后于本地，并检查了迁移辅助脚本行为
- 新增说明：
  skill 文档现在列出了当前 IM 能力集，并明确指出已有数据库在远程同步后还需要手动执行 `db_apply_migrations.sh`

## 2026-04-19

- 请求：
  将邀请功能相关表补进 `sqlDoc/huoyue.sql`，把 schema 同步到远程服务器 `111.228.20.88`，并为未来所有 schema 变更补充永久性远程同步规则
- 涉及区域：
  `sqlDoc/`、`skills/`、共享远程 MySQL 工作流
- 已检查背景：
  项目 skill 路由文档、远程运行时说明、当前共享远程 Docker/MySQL 环境
- 验证：
  确认了远程主机可达
  通过 `docker exec mysql` 将邀请迁移应用到远程 `ai_tutor` 数据库
  验证了远程 `invite_*` 表在同步后已存在
- 新增说明：
  schema 工作现在只有在 `sqlDoc` 更新完、变更应用并在 `111.228.20.88` 上验证通过后，才算交付完成

## 2026-04-19

- 请求：
  修复老用户邀请码缺失展示，优化邀请奖励页布局，并补完整个邀请分享注册闭环
- 涉及区域：
  `ai-tutor-web/`、`tutor-appointment-service/`
- 已检查背景：
  邀请概览生成流程、老用户邀请码补偿路径、认证注册页行为、当前邀请页信息架构
- 验证：
  执行了 `./mvnw -pl tutor-appointment-service -am -Dtest=InviteServiceImplTest,InviteControllerTest,UserServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  执行了 `cd ai-tutor-web && npm test -- src/api/invite.test.ts src/pages/InviteRewardPage.test.ts src/pages/AuthPage.invite.test.ts src/stores/auth.test.ts src/ui/home/HomeHeader.test.ts`
  执行了 `cd ai-tutor-web && npm run typecheck`
- 新增说明：
  邀请链接现在会自动预填认证页的邀请码，邀请概览也为历史用户未完整初始化邀请码记录的情况增加了重试保护

## 2026-04-19

- 请求：
  彻底修复远程同步开发环境中的邀请码可见性问题，并阻止被禁用用户继续登录或沿用旧 token
- 涉及区域：
  `ai-tutor-web/`、`tutor-appointment-service/`、共享远程开发数据
- 已检查背景：
  远程 `dev_remote_sync_up` 启动路径、用户端 Web 的 Vite 代理配置、线上登录日志、邀请码补齐状态、管理员禁用语义
- 验证：
  执行了 `./mvnw -pl tutor-appointment-service -am -Dtest=InviteServiceImplTest,InviteControllerTest,UserServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  执行了 `cd ai-tutor-web && npm test -- src/api/invite.test.ts src/pages/InviteRewardPage.test.ts src/pages/AuthPage.invite.test.ts src/stores/auth.test.ts src/ui/home/HomeHeader.test.ts`
  执行了 `cd ai-tutor-web && npm run typecheck`
  重新同步并重启了远程环境：`bash scripts/dev_remote_sync_up.sh`
  验证远程活跃用户都已拥有邀请码，并确认目标账号 `666892` 的邀请码是 `U0EAKS`
- 新增说明：
  用户端 Web 的开发代理现在会转发 `/invite`，老用户登录时会自动补齐邀请码，同时被禁用用户会在登录和 token 认证两个阶段都被拦截

## 2026-04-19

- 请求：
  继续完善邀请功能，补齐系统推广码流程、让邀请页风格与主站统一、验证管理端/邀请相关测试，并把 schema 重新同步到共享远程服务器
- 涉及区域：
  `ai-tutor-web/`、`ai-tutor-admin/`、`ai-tutor-admin-web/`、`tutor-appointment-service/`、`videoCall-IM-service/`、`sqlDoc/`、`skills/`
- 已检查背景：
  邀请后端流程、管理端邀请配置页面、IM 侧佣金折扣路径、远程 MySQL/容器工作流、项目 schema 同步规则
- 验证：
  执行了 `./mvnw -pl ai-tutor-admin -am -Dtest=AdminInviteControllerTest,AdminInviteServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false clean test`
  执行了 `./mvnw -pl tutor-appointment-service -am -Dtest=InviteServiceImplTest,InviteControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
  执行了 `./mvnw -pl videoCall-IM-service -am -Dtest=BrokerageOrderServiceInviteNotifyTest,AppointmentInternalClientTest -Dsurefire.failIfNoSpecifiedTests=false test`
  执行了 `cd ai-tutor-web && npm test -- --run src/api/invite.test.ts src/pages/InviteRewardPage.test.ts src/pages/AuthPage.invite.test.ts`
  执行了 `cd ai-tutor-web && npm run typecheck`
  在 `111.228.20.88` 上应用了 `sqlDoc/migrations/20260419_system_invite_promotion.sql`
  通过 Docker MySQL 验证了远程 `invite_system_config` 与 `brokerage_order` 推广相关字段
- 新增说明：
  schema 交付规则现在明确要求同时同步 `sqlDoc/migrations` 与 `sqlDoc/huoyue.sql`，再在 `111.228.20.88` 上应用并验证

## 2026-04-19

- 请求：
  隐藏用户邀请页中的系统推广码，将默认推广码从旧 Huoyue 品牌迁出，并修复远程推广文案乱码
- 涉及区域：
  `ai-tutor-web/`、`tutor-appointment-service/`、`ai-tutor-admin/`、`ai-tutor-admin-web/`、`videoCall-IM-service/`、`sqlDoc/`
- 已检查背景：
  用户邀请页展示逻辑、管理端系统推广配置默认值、邀请规则输出、SQL 迁移与远程 MySQL 字符集行为
- 验证：
  执行了 `cd ai-tutor-web && npm test -- --run src/api/invite.test.ts src/pages/InviteRewardPage.test.ts`
  执行了 `cd ai-tutor-web && npm run typecheck`
  执行了 `./mvnw -pl tutor-appointment-service -am -Dtest=InviteServiceImplTest,InviteControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
  执行了 `./mvnw -pl ai-tutor-admin -am -Dtest=AdminInviteControllerTest,AdminInviteServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  执行了 `./mvnw -pl videoCall-IM-service -am -Dtest=BrokerageOrderServiceInviteNotifyTest,AppointmentInternalClientTest -Dsurefire.failIfNoSpecifiedTests=false test`
  执行了 `cd ai-tutor-admin-web && npm test -- --run src/api/invite.test.ts`
  使用 `--default-character-set=utf8mb4` 将更新后的邀请系统迁移应用到 `111.228.20.88`
  验证远程 `invite_system_config` 现返回推广码 `CHUANGZHI` 和正常中文文案
- 新增说明：
  系统推广码属于运营发放的活动码，应在管理端保持可配置，而不应展示在个人邀请页

## 2026-04-19

- 请求：
  通过补齐缺失的网关路由，修复邀请奖励页 `/invite/**` 的 404 问题
- 涉及区域：
  `ai-tutor-gateway/`、共享远程网关运行环境
- 已检查背景：
  用户端 Web 的邀请 API 路径、Vite 代理、网关路由表、远程网关与 appointment 日志
- 验证：
  执行了 `./mvnw -pl ai-tutor-gateway -am -Dtest=GatewayRoutesSmokeTest,GatewayApplicationContextTest -Dsurefire.failIfNoSpecifiedTests=false test`
  重新同步远程环境：`bash scripts/dev_remote_sync_up.sh`
  验证远程 `http://127.0.0.1:18080/invite/overview` 已从 `404` 变成 `401`，证明网关路由生效并且请求已到达认证层
- 新增说明：
  邀请 API 现已正式通过网关路由，归入 appointment 领域

## 2026-04-19

- 请求：
  将新的项目 logo 上传到共享服务器 MinIO，并把远程 MinIO 细节记录进项目 skill
- 涉及区域：
  `ai-tutor-web/`、`scripts/`、`skills/`、共享远程 MinIO
- 已检查背景：
  远程服务器连接配置、共享运行时配置、MinIO 容器状态、现有资源上传脚本
- 验证：
  将 `brand/favicon.svg` 和 `brand/logo-icon.svg` 上传到了远程 bucket `ai-tutor-assets`
  在 `111.228.20.88` 上验证 `http://127.0.0.1:18080/api/v1/public/assets/brand/logo-icon.svg` 返回 `200 OK` 且 `Content-Type: image/svg+xml`
  观察到笔记本直连 `111.228.20.88:9000` 和 `111.228.20.88:18080` 可能被拦截，因此以服务器侧网关验证作为可靠依据
- 新增说明：
  远程静态资源上传应使用共享服务器上正在运行的 `minio` 容器，并通过 `--network container:minio` 的 `mc` 辅助容器执行

## 2026-04-19

- 请求：
  略微放大顶栏最左侧显示的品牌 logo
- 涉及区域：
  `ai-tutor-web/`、`skills/`
- 已检查背景：
  游客首页头部与登录后顶部栏的 logo 尺寸逻辑
- 验证：
  执行了 `npm --prefix ai-tutor-web run typecheck`
  执行了 `npm --prefix ai-tutor-web run build`
  把更新后的顶部栏文件和 skill 变更日志同步到了 `111.228.20.88:/opt/ai-platform`
- 新增说明：
  没有新增仓库专属坑点；这次只是视觉尺寸微调

## 2026-04-19

- 请求：
  用重新截取的源图替换顶栏 logo 资源，并重新上传到共享远程 MinIO
- 涉及区域：
  `ai-tutor-web/`、`skills/`、共享远程 MinIO
- 已检查背景：
  用户端品牌资源使用方式、顶部栏 logo 宽高比、远程 MinIO 上传与验证路径
- 验证：
  执行了 `npm --prefix ai-tutor-web run typecheck`
  执行了 `npm --prefix ai-tutor-web run build`
  把更新后的品牌资源和顶部栏文件同步到了 `111.228.20.88:/opt/ai-platform`
  将远程 `brand/logo-icon.svg` 与 `brand/favicon.svg` 重新上传到了 MinIO bucket `ai-tutor-assets`
  在 `111.228.20.88` 上验证 `http://127.0.0.1:18080/api/v1/public/assets/brand/logo-icon.svg` 返回 `200 OK`
- 新增说明：
  顶部栏 logo 使用的是更宽的横向比例，和 favicon 尺寸位不同，所以每个 UI 位的宽高应分别调节

## 2026-04-19

- 请求：
  将已澄清的产品战略、上线阶段、首页定位规则和路线图表达沉淀进项目 skill，供后续 AI 工作使用
- 涉及区域：
  `skills/`
- 已检查背景：
  现有项目 skill 路由逻辑、首页/业务流说明、用户补充的产品战略与上线优先级
- 验证：
  审阅了 `skills/SKILL.md` 和 `skills/references/business-flows.md`
  新增了 `skills/references/product-strategy.md`
  验证新参考文档已从 `skills/SKILL.md` 正确路由，并在首页流程说明中被引用
- 新增说明：
  首页和产品文案类任务现在必须先读产品战略参考，这样 AI 才能正确区分当前交付、教师优先获客和长期 AI 愿景

## 2026-04-19

- 请求：
  将本项目 `skills/` 下的全部 skill 文档、参考资料、agent 元数据和辅助脚本提示语统一转成中文
- 涉及区域：
  `skills/`
- 已检查背景：
  当前 `skills/` 目录结构、agent 元数据、所有参考文档、辅助脚本输出文案
- 验证：
  统一改写了 `skills/SKILL.md`、`skills/agents/openai.yaml`、`skills/references/*.md`
  将 `skills/scripts/changed-area-check.sh` 和 `skills/scripts/project-snapshot.sh` 的输出提示改为中文
- 新增说明：
  技能名、文件路径、脚本命令和 skill ID 保持不变，只对面向 AI 的说明文本进行了中文化
