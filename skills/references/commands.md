# Commands

Use these commands as the default local toolbox for this repo.

## Repo Snapshot

```bash
bash skills/scripts/project-snapshot.sh
```

## Changed Area Routing

```bash
bash skills/scripts/changed-area-check.sh path/to/file1 path/to/file2
```

## Local Development

```bash
bash scripts/dev_local_up.sh
bash scripts/dev_local_down.sh
```

Defaults:

- local up: `MANAGE_INFRA=auto`
- local down: `STOP_INFRA=1`
- local default profile: `dev` -> namespace `481e4376-4576-4b18-ac19-f61e170ca3ae`
- `sh scripts/dev_all_up.sh` prefers local `127.0.0.1:8848`; if unavailable, it may auto-start a local Nacos tunnel and switch to `127.0.0.1:18848`
- direct all-in-one start also works now:
  `sh scripts/dev_all_up.sh`

Useful local variants:

```bash
STOP_INFRA=0 bash scripts/dev_local_down.sh
SPRING_PROFILES_ACTIVE=prod bash scripts/dev_local_up.sh
AUTO_NACOS_TUNNEL=never sh scripts/dev_all_up.sh
```

## Remote Development

```bash
bash scripts/dev_remote_up.sh
bash scripts/dev_remote_down.sh
```

Defaults:

- remote up: open tunnel locally, then run remote `MANAGE_INFRA=never sh scripts/dev_all_up.sh`
- remote down: run remote `STOP_INFRA=0 sh scripts/dev_all_down.sh`, then stop the local tunnel
- remote default profile: `dev` -> namespace `481e4376-4576-4b18-ac19-f61e170ca3ae`
- known prod namespace: `44cf681d-9f93-443e-aa9e-ba6ec8f721d5`
- remote default Nacos address: `127.0.0.1:8848` when Nacos runs on the same server
- remote default tunnel mode: `REMOTE_USE_TUNNEL=1`

Direct remote browser access:

```bash
REMOTE_USE_TUNNEL=0 bash scripts/dev_remote_up.sh
```

Notes:

- direct mode makes frontend dev servers bind `0.0.0.0`
- you still need security-group/firewall access for `5173`, `5174`, `18080`
- if you keep tunnel mode, frontend stays on `127.0.0.1`

Direct server-side start:

```bash
cd /opt/ai-platform
MANAGE_INFRA=never sh scripts/dev_all_up.sh
STOP_INFRA=0 sh scripts/dev_all_down.sh
```

Current shared remote payment-test start on `111.228.20.88`:

```bash
cd /opt/ai-platform
MANAGE_INFRA=never FRONTEND_HOST=0.0.0.0 NACOS_SERVER_ADDR=127.0.0.1:8848 sh scripts/dev_local_up.sh
STOP_INFRA=0 sh scripts/dev_local_down.sh
```

Explicit namespace switching on server:

```bash
SPRING_PROFILES_ACTIVE=dev NACOS_NAMESPACE=481e4376-4576-4b18-ac19-f61e170ca3ae MANAGE_INFRA=never sh scripts/dev_all_up.sh
SPRING_PROFILES_ACTIVE=prod NACOS_NAMESPACE=44cf681d-9f93-443e-aa9e-ba6ec8f721d5 MANAGE_INFRA=never sh scripts/dev_all_up.sh
```

## SSH Tunnel Only

```bash
bash scripts/ssh_tunnel.sh start
bash scripts/ssh_tunnel.sh status
bash scripts/ssh_tunnel.sh stop
bash scripts/nacos_tunnel.sh start
bash scripts/nacos_tunnel.sh status
bash scripts/nacos_tunnel.sh stop
```

Optional environment overrides:

```bash
REMOTE_USER=root REMOTE_HOST=111.228.20.88 bash scripts/ssh_tunnel.sh start
```

Current laptop-to-shared-server payment-test workflow:

```bash
bash scripts/ssh_tunnel.sh start
bash scripts/ssh_tunnel.sh status
bash scripts/ssh_tunnel.sh stop
```

Then browse locally:

- `http://localhost:5173`
- `http://localhost:5174`
- `http://localhost:18080`

## Infra Only

```bash
docker compose -f Dockerfile/docker-compose.yml up -d
docker compose -f Dockerfile/docker-compose.yml down -v
```

If the environment only has legacy Compose:

```bash
docker-compose -f Dockerfile/docker-compose.yml up -d
docker-compose -f Dockerfile/docker-compose.yml down -v
```

For server environments with always-on middleware:

```bash
MANAGE_INFRA=never sh scripts/dev_all_up.sh
STOP_INFRA=0 sh scripts/dev_all_down.sh
```

If the repo should actively manage middleware on that machine:

```bash
MANAGE_INFRA=auto sh scripts/dev_all_up.sh
```

For same-host Nacos on Docker, verify gRPC ports too:

```bash
ss -lntp | grep -E ':8848|:9848|:9849'
nc -z -w 2 127.0.0.1 8848
nc -z -w 2 127.0.0.1 9848
nc -z -w 2 127.0.0.1 9849
```

## Runtime Inspection

Logs and pid files:

```bash
ls .logs
ls .pids
tail -n 120 .logs/payment-service.log
tail -n 120 .logs/videoCall-IM-service.log
```

Which ports are actually listening:

```bash
lsof -i tcp:18080
lsof -i tcp:18081
lsof -i tcp:18082
lsof -i tcp:18083
lsof -i tcp:18084
lsof -i tcp:5173
lsof -i tcp:5174
```

Verify which Nacos configs were really loaded:

```bash
bash scripts/verify_nacos_effect.sh
```

Confirm remote helper scripts were synced:

```bash
grep -n "manage.infra" scripts/dev_all_up.sh
grep -n "REMOTE_MANAGE_INFRA" scripts/dev_remote_up.sh
```

Force-sync the current repo to the shared remote dev machine and restart services:

```bash
bash scripts/dev_remote_sync_up.sh
```

Apply all SQL migrations on the shared remote dev machine after syncing code:

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform && sh scripts/db_apply_migrations.sh"
```

Payment callback verification logs:

```bash
ssh root@111.229.64.41 "tail -f /var/log/nginx/ai-tutor-payment-domain.access.log | grep --line-buffered -E 'payment/notify/yungouos|payment/return/yungouos'"
ssh root@111.228.20.88 "cd /opt/ai-platform && tail -f .logs/payment-service.log | grep --line-buffered -E 'PAY_NOTIFY|PAY_FINALIZE|updated to SUCCESS|YunGouOS 回调|YunGouOS 回调验签|YunGouOS 回调订单不存在'"
ssh root@111.228.20.88 "cd /opt/ai-platform && tail -f .logs/videoCall-IM-service.log | grep --line-buffered -E 'payment_success_received|brokerage_payment_success|tutor_application_paid'"
```

## Backend

```bash
./mvnw test
./mvnw -pl tutor-appointment-service test
./mvnw -pl videoCall-IM-service test
./mvnw -pl payment-service test
./mvnw -pl ai-tutor-admin test
```

## User Web

```bash
cd ai-tutor-web
npm ci
npm run dev
npm run lint
npm run typecheck
npm run test
```

## Admin Web

```bash
cd ai-tutor-admin-web
npm ci
npm run dev
npm run lint
npm run typecheck
```

## Miniprogram

```bash
cd ai-tutor-miniprogram
npm ci
npm run dev:mp-weixin
npm run type-check
```

## QA Automation

```bash
cd qa/automation
./scripts/ensure_backend.sh
./scripts/run_smoke.sh
./scripts/run_regression.sh
```

## Helpful Grep Targets

```bash
rg --files tutor-appointment-service/src/main/java
rg --files videoCall-IM-service/src/main/java
rg --files payment-service/src/main/java
rg --files ai-tutor-web/src
rg -n "chat/stream|ackRead|loginOrRegister|/user/me|RoleInterceptor"
```

Config source tracing:

```bash
rg -n "spring\\.config\\.import|optional:nacos|optional:file" -g 'application*.yml'
rg -n "@ConfigurationProperties|@Value\\(" tutor-appointment-service videoCall-IM-service payment-service ai-tutor-admin ai-tutor-gateway ai-tutor-common
rg -n "NACOS_SERVER_ADDR|NACOS_NAMESPACE|MANAGE_INFRA|REMOTE_USE_TUNNEL" scripts
```

## Maintenance Rule

If you discover a command that you repeat during two or more tasks, add it here or wrap it in `skills/scripts/`.
