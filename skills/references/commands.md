# 常用命令

把下面这些命令作为本仓库的默认本地工具箱。

## 仓库快照

```bash
bash skills/scripts/project-snapshot.sh
```

## 变更区域路由

```bash
bash skills/scripts/changed-area-check.sh path/to/file1 path/to/file2
```

## 本地开发

```bash
bash scripts/dev_local_up.sh
bash scripts/dev_local_down.sh
```

默认值：

- 本地启动：`MANAGE_INFRA=auto`
- 本地停止：`STOP_INFRA=1`
- 本地默认 profile：`dev` -> namespace `481e4376-4576-4b18-ac19-f61e170ca3ae`
- 日常测试默认是在本机启动应用进程，并让 MySQL、Redis、RabbitMQ、MinIO、LiveKit、Prometheus/Grafana 等中间件走本机 Docker；不要默认把测试放到远程服务器跑
- Nacos 是常见例外：`sh scripts/dev_all_up.sh` 会优先使用本机 `127.0.0.1:8848`；如果本地 Nacos 不可用，脚本会自动拉起 Nacos SSH 隧道并切换到 `127.0.0.1:18848`，此时只有 Nacos 来自远端 dev
- 现在也支持直接一键启动：
  `sh scripts/dev_all_up.sh`

日常本地联调默认就用下面这组：

- 启动：`bash scripts/dev_local_up.sh`
- 关闭：`bash scripts/dev_local_down.sh`
- 只关应用不关本地中间件：`STOP_INFRA=0 bash scripts/dev_local_down.sh`

本地几个启动/停止脚本的区别：

- `dev_local_up.sh`：本地日常入口，设置本地开发默认值后调用 `dev_all_up.sh`；会管理本机中间件，并按需处理 Nacos 隧道
- `dev_local_down.sh`：本地日常关闭入口，默认 `STOP_INFRA=1`，会停应用、本机中间件，以及本地公网/SSH/Nacos 隧道
- `dev_all_up.sh`：底层启动引擎；本机和远程都复用它。直接跑也可以，但需要你自己理解 `MANAGE_INFRA`、`NACOS_SERVER_ADDR` 等环境变量
- `dev_all_down.sh`：底层停止引擎；默认 `STOP_INFRA=0`，直接跑时会保留中间件。通过 `dev_local_down.sh` 跑时默认会变成 `STOP_INFRA=1`

一些常用的本地变体：

```bash
STOP_INFRA=0 bash scripts/dev_local_down.sh
SPRING_PROFILES_ACTIVE=prod bash scripts/dev_local_up.sh
AUTO_NACOS_TUNNEL=never sh scripts/dev_all_up.sh
```

## 远程开发

```bash
bash scripts/dev_remote_up.sh
bash scripts/dev_remote_down.sh
```

默认值：

- 远程启动：先在本地打开隧道，再在远程执行 `MANAGE_INFRA=never sh scripts/dev_all_up.sh`
- 远程停止：先在远程执行 `STOP_INFRA=0 sh scripts/dev_all_down.sh`，再关闭本地隧道
- 远程默认 profile：`dev` -> namespace `481e4376-4576-4b18-ac19-f61e170ca3ae`
- 当前已知 prod namespace：`c3476048-10f6-4cc3-b3f1-90135d736a73`
- 远程默认 Nacos 地址：如果 Nacos 跑在同一台服务器上，使用 `127.0.0.1:8848`
- 远程默认隧道模式：`REMOTE_USE_TUNNEL=1`

直接通过远程浏览器访问：

```bash
REMOTE_USE_TUNNEL=0 bash scripts/dev_remote_up.sh
```

说明：

- 直连模式会让前端 dev server 绑定 `0.0.0.0`
- 你仍然需要放通安全组或防火墙中的 `5173`、`5174`、`18080`
- 如果保留隧道模式，前端仍然会只绑定在 `127.0.0.1`

直接在服务器上启动开发环境：

```bash
cd /opt/ai-platform
MANAGE_INFRA=never sh scripts/dev_all_up.sh
STOP_INFRA=0 sh scripts/dev_all_down.sh
```

当前生产环境使用的不是上面这个目录。生产目录与发布入口是：

```bash
ssh root@111.228.20.88 "sed -n '1,220p' /usr/local/bin/ai-platform-prod-deploy.sh"
ssh root@111.228.20.88 "cd /opt/ai-platform-prod && git branch --show-current && git rev-parse --short HEAD"
```

如果需要在服务器上手动启动当前生产应用：

```bash
cd /opt/ai-platform-prod
SPRING_PROFILES_ACTIVE=prod MANAGE_INFRA=never AUTO_BOOTSTRAP_DEV_DB=0 NACOS_SERVER_ADDR=127.0.0.1:8848 NACOS_GRPC_CHECK=warn FRONTEND_HOST=127.0.0.1 sh scripts/dev_all_up.sh
STOP_INFRA=0 sh scripts/dev_all_down.sh
```

当前 `111.228.20.88` 上手动启动开发环境的方式：

```bash
cd /opt/ai-platform
MANAGE_INFRA=never FRONTEND_HOST=0.0.0.0 NACOS_SERVER_ADDR=127.0.0.1:8848 sh scripts/dev_local_up.sh
STOP_INFRA=0 sh scripts/dev_local_down.sh
```

在服务器上显式切换 namespace：

```bash
SPRING_PROFILES_ACTIVE=dev NACOS_NAMESPACE=481e4376-4576-4b18-ac19-f61e170ca3ae MANAGE_INFRA=never sh scripts/dev_all_up.sh
SPRING_PROFILES_ACTIVE=prod NACOS_NAMESPACE=c3476048-10f6-4cc3-b3f1-90135d736a73 MANAGE_INFRA=never sh scripts/dev_all_up.sh
```

## 仅操作 SSH 隧道

```bash
bash scripts/ssh_tunnel.sh start
bash scripts/ssh_tunnel.sh status
bash scripts/ssh_tunnel.sh stop
bash scripts/nacos_tunnel.sh start
bash scripts/nacos_tunnel.sh status
bash scripts/nacos_tunnel.sh stop
```

可选环境覆盖：

```bash
REMOTE_USER=root REMOTE_HOST=111.228.20.88 bash scripts/ssh_tunnel.sh start
```

当前从笔记本访问共享服务器做支付测试的常见流程：

```bash
bash scripts/ssh_tunnel.sh start
bash scripts/ssh_tunnel.sh status
bash scripts/ssh_tunnel.sh stop
```

然后在本地浏览器访问：

- `http://localhost:5173`
- `http://localhost:5174`
- `http://localhost:18080`

## 仅操作基础设施

```bash
docker compose -f Dockerfile/docker-compose.yml up -d
docker compose -f Dockerfile/docker-compose.yml down -v
```

如果环境里只有旧版 Compose：

```bash
docker-compose -f Dockerfile/docker-compose.yml up -d
docker-compose -f Dockerfile/docker-compose.yml down -v
```

针对中间件常驻的服务器环境：

```bash
MANAGE_INFRA=never sh scripts/dev_all_up.sh
STOP_INFRA=0 sh scripts/dev_all_down.sh
```

如果希望仓库脚本主动管理该机器上的中间件：

```bash
MANAGE_INFRA=auto sh scripts/dev_all_up.sh
```

对于 Docker 中的同机 Nacos，也要检查 gRPC 端口：

```bash
ss -lntp | grep -E ':8848|:9848|:9849'
nc -z -w 2 127.0.0.1 8848
nc -z -w 2 127.0.0.1 9848
nc -z -w 2 127.0.0.1 9849
```

如果首次本地启动卡在镜像拉取，先单独补启动基础依赖，等镜像缓存好后再跑总入口：

```bash
docker compose -f Dockerfile/docker-compose.yml up -d
bash scripts/dev_local_up.sh
```

## 远程 MinIO 静态资源

共享业务服务器是 `root@111.228.20.88`。

- 开发/历史副本通常在：`/opt/ai-platform`
- 当前生产副本在：`/opt/ai-platform-prod`
- 中间件当前常驻在同一台业务机上，通过 Docker 运行

把品牌 logo 资源上传到共享远程 MinIO：

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform-prod && docker run --rm --network container:minio --entrypoint /bin/sh -v \"\$PWD/ai-tutor-web/public/brand:/data/brand:ro\" quay.io/minio/mc:latest -lc \"mc alias set local http://127.0.0.1:9000 minioadmin minioadmin; mc mb -p local/ai-tutor-assets || true; mc anonymous set download local/ai-tutor-assets || true; mc mirror --overwrite /data/brand local/ai-tutor-assets/brand; mc ls local/ai-tutor-assets/brand\""
```

通过服务器上的网关验证上传后的 logo：

```bash
ssh root@111.228.20.88 "curl -sS -I http://127.0.0.1:18080/api/v1/public/assets/brand/logo-icon.svg | sed -n '1,20p'"
ssh root@111.228.20.88 "curl -sS http://127.0.0.1:18080/api/v1/public/assets/brand/logo-icon.svg | head -c 120"
```

如果上传的是其它静态资源目录，沿用同样模式：

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform-prod && docker run --rm --network container:minio --entrypoint /bin/sh -v \"\$PWD/<local-folder>:/data/<folder>:ro\" quay.io/minio/mc:latest -lc \"mc alias set local http://127.0.0.1:9000 minioadmin minioadmin; mc mirror --overwrite /data/<folder> local/ai-tutor-assets/<object-prefix>; mc ls local/ai-tutor-assets/<object-prefix>\""
```

## 运行态排查

日志和 pid 文件：

```bash
ls .logs
ls .pids
tail -n 120 .logs/payment-service.log
tail -n 120 .logs/videoCall-IM-service.log
```

确认哪些端口真的在监听：

```bash
lsof -i tcp:18080
lsof -i tcp:18081
lsof -i tcp:18082
lsof -i tcp:18083
lsof -i tcp:18084
lsof -i tcp:5173
lsof -i tcp:5174
```

验证实际加载了哪些 Nacos 配置：

```bash
bash scripts/verify_nacos_effect.sh
```

确认远程辅助脚本已同步：

```bash
grep -n "manage.infra" scripts/dev_all_up.sh
grep -n "REMOTE_MANAGE_INFRA" scripts/dev_remote_up.sh
```

强制把当前仓库同步到共享远程开发机并重启服务：

```bash
bash scripts/dev_remote_sync_up.sh
```

代码同步后，在共享远程开发机上应用所有 SQL 迁移：

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform && sh scripts/db_apply_migrations.sh"
```

如果远程主机没有本地 `mysql` 客户端，先定位运行中的 MySQL 容器，再通过容器应用或校验 schema 变更：

```bash
ssh root@111.228.20.88 "docker ps --format '{{.Names}}' | grep -E 'mysql' | head -1"
ssh root@111.228.20.88 "docker exec -i <running-mysql-container> mysql -uroot -pAa123456 ai_tutor" < sqlDoc/migrations/<migration>.sql
ssh root@111.228.20.88 "docker exec -i <running-mysql-container> mysql -uroot -pAa123456 -N -e \"USE ai_tutor; SHOW TABLES LIKE 'invite_%';\""
```

交付 schema 时，也记得保持 bootstrap schema 同步：

```bash
git diff -- sqlDoc/huoyue.sql sqlDoc/migrations
```

支付回调验证日志：

```bash
ssh root@111.229.64.41 "tail -f /var/log/nginx/ai-tutor-payment-domain.access.log | grep --line-buffered -E 'payment/notify/yungouos|payment/return/yungouos'"
ssh root@111.228.20.88 "cd /opt/ai-platform && tail -f .logs/payment-service.log | grep --line-buffered -E 'PAY_NOTIFY|PAY_FINALIZE|updated to SUCCESS|YunGouOS 回调|YunGouOS 回调验签|YunGouOS 回调订单不存在'"
ssh root@111.228.20.88 "cd /opt/ai-platform && tail -f .logs/videoCall-IM-service.log | grep --line-buffered -E 'payment_success_received|brokerage_payment_success|tutor_application_paid'"
```

生产环境常用日志查看：

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform-prod && tail -f .logs/tutor-appointment-service.log | grep --line-buffered 'SMS SEND SUCCESS'"
ssh root@111.228.20.88 "cd /opt/ai-platform-prod && tail -f .logs/payment-service.log"
ssh root@111.228.20.88 "cd /opt/ai-platform-prod && tail -f .logs/videoCall-IM-service.log"
```

实时课堂媒体链路验证：

```bash
nc -vz -w 4 111.228.20.88 7881
ssh root@111.228.20.88 'timeout 90 tcpdump -n -i any "(udp portrange 50000-50100) or tcp port 7881" -vv -c 220'
cd ai-tutor-web
PLAYWRIGHT_BASE_URL=https://huoyue.online \
PLAYWRIGHT_API_BASE_URL=https://huoyue.online \
OPS_VERIFY_TOKEN=DevOpsVerifyTokenForE2E \
npx playwright test e2e/live-classroom.spec.ts --project=chromium
```

## 后端

```bash
./mvnw test
./mvnw -pl tutor-appointment-service test
./mvnw -pl videoCall-IM-service test
./mvnw -pl payment-service test
./mvnw -pl ai-tutor-admin test
```

## 用户端 Web

```bash
cd ai-tutor-web
npm ci
npm run dev
npm run lint
npm run typecheck
npm run test
```

## 管理端 Web

```bash
cd ai-tutor-admin-web
npm ci
npm run dev
npm run lint
npm run typecheck
```

## 小程序

```bash
cd ai-tutor-miniprogram
npm ci
npm run dev:mp-weixin
npm run type-check
```

## QA 自动化

```bash
cd qa/automation
./scripts/ensure_backend.sh
./scripts/run_smoke.sh
./scripts/run_regression.sh
```

## 常用 grep 目标

```bash
rg --files tutor-appointment-service/src/main/java
rg --files videoCall-IM-service/src/main/java
rg --files payment-service/src/main/java
rg --files ai-tutor-web/src
rg -n "chat/stream|ackRead|loginOrRegister|/user/me|RoleInterceptor"
```

配置来源追踪：

```bash
rg -n "spring\\.config\\.import|optional:nacos|optional:file" -g 'application*.yml'
rg -n "@ConfigurationProperties|@Value\\(" tutor-appointment-service videoCall-IM-service payment-service ai-tutor-admin ai-tutor-gateway ai-tutor-common
rg -n "NACOS_SERVER_ADDR|NACOS_NAMESPACE|MANAGE_INFRA|REMOTE_USE_TUNNEL" scripts
```

## 维护规则

如果你发现某个命令在两个或更多任务中反复出现，就把它补到这里，或者封装到 `skills/scripts/` 里。
