# AI Tutor 常用命令

这份文档是项目的开发启动速查手册，面向日常开发、远程联调和服务器测试。

命令以当前仓库 `scripts/` 的真实脚本为准，优先给可直接复制执行的入口；更完整的背景说明见文末参考文档。

当前环境约定：

- Nacos 地址：`111.228.20.88:8848`
- `dev` namespace：`481e4376-4576-4b18-ac19-f61e170ca3ae`
- `prod` namespace：`c3476048-10f6-4cc3-b3f1-90135d736a73`
- 日常开发和测试默认使用 `dev`
- 本地/开发分支：`dev`
- 生产分支：`master`
- 当前生产仓库：`111.228.20.88:/opt/ai-platform-prod`
- 旧开发/历史仓库：`111.228.20.88:/opt/ai-platform`
- 当前生产自动部署：`push master` -> GitHub Actions `deploy-prod.yml` -> `111.228.20.88:/usr/local/bin/ai-platform-prod-deploy.sh`

## 最常用

本地启动：

```bash
bash scripts/dev_local_up.sh
```

本地关闭：

```bash
bash scripts/dev_local_down.sh
```

远程启动：

```bash
bash scripts/dev_remote_up.sh
```

远程同步代码并重启：

```bash
bash scripts/dev_remote_sync_up.sh
```

远程关闭：

```bash
bash scripts/dev_remote_down.sh
```

查看隧道状态：

```bash
bash scripts/ssh_tunnel.sh status
bash scripts/nacos_tunnel.sh status
```

服务器手动启动开发环境：

```bash
cd /opt/ai-platform
MANAGE_INFRA=never sh scripts/dev_all_up.sh
```


本地验证码日志，一行命令：
cd /Users/luyipeng/project/ai_platform/ai-platform && tail -f .logs/tutor-appointment-service.log | grep --line-buffered "SMS SEND"

远程验证码日志，一行命令：
ssh -p 22 root@111.228.20.88 "cd /opt/ai-platform && tail -f .logs/tutor-appointment-service.log | grep --line-buffered 'SMS SEND'"


服务器手动关闭开发环境：

```bash
cd /opt/ai-platform
STOP_INFRA=0 sh scripts/dev_all_down.sh
```

服务器手动启动生产环境：

```bash
cd /opt/ai-platform-prod
SPRING_PROFILES_ACTIVE=prod MANAGE_INFRA=never AUTO_BOOTSTRAP_DEV_DB=0 NACOS_SERVER_ADDR=127.0.0.1:8848 NACOS_GRPC_CHECK=warn FRONTEND_HOST=127.0.0.1 sh scripts/dev_all_up.sh
```

服务器手动关闭生产环境：

```bash
cd /opt/ai-platform-prod
STOP_INFRA=0 sh scripts/dev_all_down.sh
```

## 本地开发

启动：

```bash
bash scripts/dev_local_up.sh
```

关闭：

```bash
bash scripts/dev_local_down.sh
```

默认行为：

- `dev_local_up.sh` 默认 `MANAGE_INFRA=auto`
- `dev_local_down.sh` 默认 `STOP_INFRA=1`
- `dev_local_up.sh` 默认 `SPRING_PROFILES_ACTIVE=dev`，对应 `dev` namespace
- `dev_all_up.sh` 会优先使用本机 `127.0.0.1:8848`；若本机没有 Nacos，会自动建立一条本地 `Nacos SSH 隧道` 并改走 `127.0.0.1:18848`
- 本地关闭时会顺手尝试关闭本地 SSH 隧道

说明：

- `MANAGE_INFRA=auto` 表示本地开发时会尽量复用已存在的中间件；缺失时再补启动
- `STOP_INFRA=1` 表示本地关闭时默认也会停掉本地中间件
- 现在可以直接执行 `sh scripts/dev_all_up.sh`
- `dev_all_up.sh` 启动服务时会跳过 Maven 测试与测试编译，避免被未同步修复的测试代码阻塞开发启动
- 在服务器上执行时，脚本会直连同机 Nacos `127.0.0.1:8848`
- 在本地执行时，如果本机没有 Nacos，脚本会自动拉起到服务器的 Nacos 隧道

如果你只想停应用，不停本地中间件：

```bash
STOP_INFRA=0 bash scripts/dev_local_down.sh
```

## 远程开发

启动：

```bash
bash scripts/dev_remote_up.sh
```

同步代码并重启：

```bash
bash scripts/dev_remote_sync_up.sh
```

关闭：

```bash
bash scripts/dev_remote_down.sh
```

默认行为：

- `dev_remote_up.sh` 会先在本地启动 SSH 隧道，再 SSH 到远程执行启动脚本
- `dev_remote_up.sh` 和 `dev_remote_down.sh` 会先同步关键脚本与 `common.md` 到远程
- `dev_remote_sync_up.sh` 会先用 `rsync over SSH` 增量同步仓库代码，再重启远程应用
- `dev_remote_up.sh` 默认 `REMOTE_MANAGE_INFRA=never`
- `dev_remote_up.sh` 默认 `REMOTE_NACOS_GRPC_CHECK=warn`
- `dev_remote_up.sh` 默认 `REMOTE_NACOS_SERVER_ADDR=127.0.0.1:8848`
- `dev_remote_up.sh` 默认 `REMOTE_USE_TUNNEL=1`
- `dev_remote_down.sh` 默认 `REMOTE_STOP_INFRA=0`
- `dev_remote_sync_up.sh` 默认 `REMOTE_SYNC_DELETE=1`，会让远程代码目录尽量对齐本地
- `dev_remote_up.sh` 默认 `SPRING_PROFILES_ACTIVE=dev`，对应 `dev` namespace
- 远程关闭时会顺手停止本地 SSH 隧道

说明：

- 远程默认不管理常驻中间件，适合服务器上 MySQL/Redis/MinIO/Nacos 等已长期运行的情况
- 远程默认对 Nacos gRPC 端口只告警不阻塞，方便先做页面和接口测试
- 远程默认通过 `127.0.0.1:8848` 访问同机 Nacos，不依赖公网回环访问
- 远程默认通过 SSH 隧道把页面映射回本地；这种模式下前端默认绑定 `127.0.0.1`
- `dev_remote_up.sh` 只同步脚本和文档，不同步业务代码
- `dev_remote_sync_up.sh` 不依赖 VS Code SFTP 插件；它通过 `rsync + SSH` 直接把本地仓库同步到远程
- `dev_remote_sync_up.sh` 会排除 `.git`、`.vscode`、`node_modules`、`target`、`.logs`、`.pids` 和 Docker 持久化数据目录
- 远程启动成功后，本地浏览器访问：
  - `http://localhost:5173`
  - `http://localhost:5174`
  - `http://localhost:18080`

只管理隧道：

```bash
bash scripts/ssh_tunnel.sh start
bash scripts/ssh_tunnel.sh status
bash scripts/ssh_tunnel.sh stop
bash scripts/nacos_tunnel.sh start
bash scripts/nacos_tunnel.sh status
bash scripts/nacos_tunnel.sh stop
```

如果要指定远程机器或路径：

```bash
REMOTE_USER=root REMOTE_HOST=111.228.20.88 REMOTE_PATH=/opt/ai-platform bash scripts/dev_remote_up.sh
```

如果你改了前端或后端代码，想把当前仓库同步到远程并立即重启：

```bash
REMOTE_USER=root REMOTE_HOST=111.228.20.88 REMOTE_PATH=/opt/ai-platform bash scripts/dev_remote_sync_up.sh
```

注意：

- `dev_remote_*` 默认是给开发副本 `/opt/ai-platform` 用的，不是给生产目录 `/opt/ai-platform-prod` 用的
- 生产环境发布不建议靠 `dev_remote_sync_up.sh`，而是走 GitHub Actions 的 `master` 自动部署

如果你不想删除远程多出来的文件：

```bash
REMOTE_SYNC_DELETE=0 bash scripts/dev_remote_sync_up.sh
```

如果你打算直接在浏览器打开远程地址，不走本地隧道：

```bash
REMOTE_USE_TUNNEL=0 bash scripts/dev_remote_up.sh
```

这时脚本会让前端绑定 `0.0.0.0`，并输出远程访问地址：

- `http://111.228.20.88:5173`
- `http://111.228.20.88:5174`
- `http://111.228.20.88:18080`

前提：

- 云服务器安全组或防火墙已放通 `5173`、`5174`、`18080`
- 你接受这些开发端口可被外部访问

如果远程要连接另一台 Nacos：

```bash
REMOTE_NACOS_SERVER_ADDR=111.228.20.88:8848 bash scripts/dev_remote_up.sh
```

如果你要强制要求 Nacos gRPC 可达再启动：

```bash
REMOTE_NACOS_GRPC_CHECK=fail bash scripts/dev_remote_up.sh
```

如果你要切到生产 namespace：

```bash
SPRING_PROFILES_ACTIVE=prod bash scripts/dev_remote_up.sh
```

## Nacos 端口修复

你当前远程 `docker inspect nacos` 已确认只映射了 `8848`，没有映射 `9848/9849`。对 Nacos `2.x` 来说，这会导致：

- 配置中心 HTTP 看起来能连
- Java 客户端实际因为缺少 gRPC 端口而拿不到完整配置或服务发现能力
- 典型表现是服务启动时报 `127.0.0.1:9848 connection refused`，随后出现配置缺失，例如 `MinIO endpoint must not be null`

按你现在这个场景，更稳妥的修法是：

- 保留现有 `8848` 映射方式，避免影响你现在的访问习惯
- 先把 `9848/9849` 补到宿主机本地 `127.0.0.1`
- 这样服务器上的 Java 服务可以通过 `127.0.0.1:8848` 正常连 Nacos，但不会把新的 gRPC 端口额外暴露到公网

重建示例：

```bash
docker rm -f nacos
docker run -d \
  --name nacos \
  -e MODE=standalone \
  -e PREFER_HOST_MODE=ip \
  -e JVM_XMS=1g \
  -e JVM_XMX=1g \
  -e JVM_XMN=512m \
  -p 8848:8848 \
  -p 127.0.0.1:9848:9848 \
  -p 127.0.0.1:9849:9849 \
  --restart unless-stopped \
  nacos/nacos-server:v2.2.3
```

如果你后面确认连 `8848` 也不需要公网访问，再把上面的 `-p 8848:8848` 改成 `-p 127.0.0.1:8848:8848` 即可。

修复后在服务器验证：

```bash
ss -lntp | grep -E ':8848|:9848|:9849'
nc -z -w 2 127.0.0.1 8848
nc -z -w 2 127.0.0.1 9848
nc -z -w 2 127.0.0.1 9849
```

如果你还需要让“本地电脑直接运行的 Java 服务”连接这台远程 Nacos，那么 `9848/9849` 也必须对本地机器可达。两种做法：

- 放开公网/安全组中的 `9848/9849`
- 或者后续给本地开发再补一组 Nacos SSH 隧道

当前你先做服务器测试的话，优先把远程服务器上的 `127.0.0.1:9848/9849` 修好即可。

## 服务器上直接操作

登录后进入项目目录：

```bash
cd /opt/ai-platform
```

手动启动：

```bash
MANAGE_INFRA=never sh scripts/dev_all_up.sh
```

手动关闭：

```bash
STOP_INFRA=0 sh scripts/dev_all_down.sh
```

何时使用 `MANAGE_INFRA=never`：

- 服务器中间件已经长期运行
- 你只想启动项目进程，不想让脚本碰 Docker 中间件
- 你遇到过中间件容器名冲突，或确认服务器本身已经有独立维护的中间件

如果服务器上确实需要让脚本自动补中间件，再按需用：

```bash
MANAGE_INFRA=auto sh scripts/dev_all_up.sh
```

如果要手动切换 namespace：

```bash
SPRING_PROFILES_ACTIVE=dev NACOS_NAMESPACE=481e4376-4576-4b18-ac19-f61e170ca3ae MANAGE_INFRA=never sh scripts/dev_all_up.sh
SPRING_PROFILES_ACTIVE=prod NACOS_NAMESPACE=c3476048-10f6-4cc3-b3f1-90135d736a73 MANAGE_INFRA=never sh scripts/dev_all_up.sh
```

## 服务器测试前检查

确认当前目录：

```bash
pwd
```

确认关键脚本已同步到远程：

```bash
cd /opt/ai-platform
grep -n "manage.infra" scripts/dev_all_up.sh
grep -n "REMOTE_MANAGE_INFRA" scripts/dev_remote_up.sh
```

确认隧道是否已建立：

```bash
bash scripts/ssh_tunnel.sh status
```

确认远程进程端口：

```bash
lsof -i tcp:18080
lsof -i tcp:5173
lsof -i tcp:5174
```

确认 Nacos gRPC 端口：

```bash
ss -lntp | grep -E ':8848|:9848|:9849'
nc -z -w 2 127.0.0.1 9848
nc -z -w 2 127.0.0.1 9849
```

## 常用测试

后端全部测试：

```bash
./mvnw test
```

按模块跑 Maven 测试：

```bash
./mvnw -pl tutor-appointment-service test
./mvnw -pl videoCall-IM-service test
./mvnw -pl payment-service test
./mvnw -pl ai-tutor-admin test
```

用户端 Web：

```bash
cd ai-tutor-web
npm run typecheck
npm run lint
npm run test
```

管理端 Web：

```bash
cd ai-tutor-admin-web
npm run typecheck
npm run lint
```

QA 自动化：

```bash
cd qa/automation
./scripts/ensure_backend.sh
./scripts/run_smoke.sh
./scripts/run_regression.sh
```

## 支付与验证码排查

查看支付回调关键日志：

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform && tail -f .logs/payment-service.log | grep --line-buffered -E 'PAY_NOTIFY|updated to SUCCESS|YunGouOS 下单失败|Close stale pending payment order'"
```

查看网关是否收到支付回调/回跳请求：

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform && tail -f .logs/ai-tutor-gateway.log | grep --line-buffered -E '/payment/notify/yungouos|/payment/return/yungouos|401|403'"
```

验证公网是否能打到支付回调入口：

```bash
curl -i -m 8 -X POST http://111.228.20.88:18080/payment/notify/yungouos
curl -i -m 8 http://111.228.20.88:18080/payment/return/yungouos
```

查看短信验证码发送日志：

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform && tail -f .logs/tutor-appointment-service.log | grep --line-buffered 'SMS SEND SUCCESS'"
```

按手机号搜索短信验证码：

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform && grep 'SMS SEND SUCCESS' .logs/tutor-appointment-service.log | grep '13800138000' | tail -n 20"
```

## 排错速查

远程页面打不开：

- 先看本地隧道是否存在：`bash scripts/ssh_tunnel.sh status`
- 再看远程服务端口是否真的监听
- 再确认浏览器访问的是 `localhost`，不是远程 IP

远程脚本行为和本地不一致：

- 先怀疑脚本没有同步到远程
- 到服务器上直接 `grep` 新增的关键字确认版本
- 必要时执行一次 `SFTP: Sync Local -> Remote`

远程启动时中间件冲突：

- 不要直接 `docker compose up -d`
- 优先用 `MANAGE_INFRA=never`
- 确认服务器上常驻的容器是否已经存在并运行

保存文件后远程没更新：

- 先看 VS Code 的 `Output -> SFTP`
- 必要时手动执行 `SFTP: Upload Active File` 或 `SFTP: Sync Local -> Remote`

本地端口被占用，隧道起不来：

- 先执行 `bash scripts/ssh_tunnel.sh stop`
- 再重新 `start`
- 必要时用 `lsof -i tcp:5173`、`5174`、`18080` 查占用

登录/注册时报 `系统内部异常`，且后端日志出现 `UPDATE command denied to user 'root'@'172.18.0.1' for table 'user'`：

- 说明应用实际使用的是 `root@'%'` 连接 MySQL，但这个账号对 `ai_tutor` 缺少写权限
- 先补授权：

```bash
docker exec -i 759d793c134e_mysql mysql -uroot -pAa123456 -e "GRANT ALL PRIVILEGES ON ai_tutor.* TO 'root'@'%'; FLUSH PRIVILEGES;"
```

- 再重启服务，让连接池拿到新权限：

```bash
cd /opt/ai-platform
STOP_INFRA=0 sh scripts/dev_all_down.sh
MANAGE_INFRA=never sh scripts/dev_all_up.sh
```

## 参考文档

- 后端和中间件更完整说明：
  [README-backend.md](/Users/luyipeng/project/ai_platform/ai-platform/README-backend.md)
- 项目命令与技能沉淀：
  [skills/references/commands.md](/Users/luyipeng/project/ai_platform/ai-platform/skills/references/commands.md)

## 维护约定

- 后续新增高频命令或启动方式变化，优先更新这份 `common.md`
- 如果是 AI 使用时也需要长期记住的项目知识，再同步更新 `skills/references/commands.md` 或 `skills/references/gotchas.md`
