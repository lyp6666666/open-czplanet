# Runtime And Config

Use this reference when work touches startup, remote testing, Nacos, environment variables, or "where does this config come from?" questions.

## Current Shared Environment

- Shared remote dev/test server:
  `root@111.228.20.88`
- Remote repo path:
  `/opt/ai-platform`
- Current shared Nacos host:
  `111.228.20.88:8848`
- Current namespaces:
  `dev=481e4376-4576-4b18-ac19-f61e170ca3ae`
  `prod=44cf681d-9f93-443e-aa9e-ba6ec8f721d5`
- Day-to-day work should assume `dev` unless the user explicitly says `prod`

## How The Project Usually Runs

### Local

- Preferred entry:
  `bash scripts/dev_local_up.sh`
- Local stop:
  `bash scripts/dev_local_down.sh`
- Real work happens inside `scripts/dev_all_up.sh`
- Default local behavior:
  `MANAGE_INFRA=auto`
  `SPRING_PROFILES_ACTIVE=dev`
  prefer local Nacos `127.0.0.1:8848`
  if local Nacos is unavailable, auto-open a local Nacos SSH tunnel and switch to `127.0.0.1:18848`

### Shared Remote Server

- Preferred remote wrapper from laptop:
  `bash scripts/dev_remote_up.sh`
- Preferred remote stop from laptop:
  `bash scripts/dev_remote_down.sh`
- Current remote defaults:
  `REMOTE_MANAGE_INFRA=never`
  `REMOTE_NACOS_SERVER_ADDR=127.0.0.1:8848`
  `REMOTE_NACOS_GRPC_CHECK=warn`
  `REMOTE_USE_TUNNEL=1`
- This means the server is expected to have always-on middleware already running, while app processes are started from the repo checkout

### Directly On The Server

- Common start:
  `cd /opt/ai-platform && MANAGE_INFRA=never sh scripts/dev_all_up.sh`
- Common stop:
  `cd /opt/ai-platform && STOP_INFRA=0 sh scripts/dev_all_down.sh`
- Use `MANAGE_INFRA=auto` only when the server really should let repo scripts manage middleware containers too

## What `dev_all_up.sh` Starts

- Gateway:
  `ai-tutor-gateway` on `18080`
- Appointment/user service:
  `tutor-appointment-service` on `18081`
- IM/chat/collaboration service:
  `videoCall-IM-service` on `18082`
- Payment service:
  `payment-service` on `18083`
- Admin backend:
  `ai-tutor-admin` on `18084`
- User web:
  `ai-tutor-web` on `5173`
- Admin web:
  `ai-tutor-admin-web` on `5174`

Runtime artifacts:

- logs:
  `.logs/*.log`
- pid files:
  `.pids/*.pid`

## Startup Decision Rules That Matter

- The wrapper scripts are safer than calling `dev_all_up/down` blindly
- `dev_local_up.sh` is for laptop-style development
- `dev_remote_up.sh` is for "start on server, browse locally" development
- `dev_all_up.sh` is the shared engine and can now be run directly on either local machine or server
- Remote wrapper syncs key startup scripts and `common.md` to the server before starting
- If remote behavior differs from local code, suspect sync drift first

## Config Source Of Truth

For backend services, runtime config comes from four layers:

1. Environment variables
2. Nacos imported by each service `application.yml`
3. Optional local fallback files where present
4. In-code defaults inside `@ConfigurationProperties` or `@Value`

Important consequence:

- When debugging a missing config, do not only inspect Nacos
- also inspect env vars passed by startup scripts
- and check whether code has a fallback default that hides the real issue

## Nacos Import Chain By Service

### `ai-tutor-gateway`

Imports in order:

- `ai-tutor-common.yaml`
- `ai-tutor-common-${spring.profiles.active}.yaml`
- `ai-tutor-gateway.yaml`
- `ai-tutor-gateway-${spring.profiles.active}.yaml`

### `tutor-appointment-service`

Imports in order:

- `ai-tutor-common.yaml`
- `ai-tutor-common-${spring.profiles.active}.yaml`
- `ai-tutor-home.yaml`
- `ai-tutor-home-${spring.profiles.active}.yaml`
- `ai-tutor-sms.yaml`
- `ai-tutor-sms-${spring.profiles.active}.yaml`
- `tutor-appointment-service.yaml`
- `tutor-appointment-service-${spring.profiles.active}.yaml`
- optional fallback file `./.private/tutor-appointment-service.yml`
- optional fallback file `../.private/tutor-appointment-service.yml`

### `videoCall-IM-service`

Imports in order:

- `ai-tutor-common.yaml`
- `ai-tutor-common-${spring.profiles.active}.yaml`
- `ai-tutor-home.yaml`
- `ai-tutor-home-${spring.profiles.active}.yaml`
- `videoCall-IM-service.yaml`
- `videoCall-IM-service-${spring.profiles.active}.yaml`

### `payment-service`

Imports in order:

- `ai-tutor-common.yaml`
- `ai-tutor-common-${spring.profiles.active}.yaml`
- `ai-tutor-payment.yaml`
- `ai-tutor-payment-${spring.profiles.active}.yaml`
- `payment-service.yaml`
- `payment-service-${spring.profiles.active}.yaml`

### `ai-tutor-admin`

Imports in order:

- `ai-tutor-common.yaml`
- `ai-tutor-common-${spring.profiles.active}.yaml`
- `ai-tutor-admin.yaml`
- `ai-tutor-admin-${spring.profiles.active}.yaml`

## High-Value DataIds To Check First

In the shared `dev` namespace, these are the usual first stops:

- `ai-tutor-common-dev.yaml`
  shared JWT, gateway sign, MinIO, upload, dev toggles
- `ai-tutor-gateway-dev.yaml`
  gateway jwt/sign config
- `ai-tutor-home-dev.yaml`
  homepage guest content and operating config
- `ai-tutor-sms-dev.yaml`
  SMS provider switches and Spug token
- `tutor-appointment-service-dev.yaml`
  appointment-only switches, miniapp config
- `videoCall-IM-service-dev.yaml`
  brokerage amount, admin token, payment gating, MQ
- `ai-tutor-payment-dev.yaml`
  payment provider keys, notify URL, return URL, payment enable switch
- `payment-service-dev.yaml`
  payment retry and service-local payment settings
- `ai-tutor-admin-dev.yaml`
  admin-only dynamic flags

Template source for these lives in:

- `docs/nacos/templates/*.yaml`

When the user asks "what should I fill into Nacos?", start from the matching template before reading code.

## How To Find What A Config Key Means

Start from the binding point in code:

- `@ConfigurationProperties(prefix = "...")`
- `@Value("${...}")`

Useful examples already in repo:

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

Fast grep:

```bash
rg -n "@ConfigurationProperties|@Value\\(" tutor-appointment-service videoCall-IM-service payment-service ai-tutor-admin ai-tutor-gateway ai-tutor-common
```

## How To Read The Effective Runtime Config

### 1. Confirm Which Namespace And Nacos Address Are In Use

Check startup output from `dev_all_up.sh`:

- `profile=...`
- `nacos.server-addr=...`
- `nacos.namespace=...`
- `nacos.config.namespace=...`
- `nacos.discovery.namespace=...`

These lines are the quickest truth source for current startup mode.

### 2. Confirm Which DataIds Were Actually Loaded

Use:

```bash
bash scripts/verify_nacos_effect.sh
```

or inspect logs for:

- `[Nacos Config] Load config[dataId=...]`
- `[Nacos Config] Listening config: ...`

If the expected DataId is missing from logs, the problem is usually one of:

- wrong namespace
- wrong Nacos address
- missing DataId in Nacos
- service never reached the config import stage because it failed earlier

### 3. Inspect The Template And Compare With Live Nacos

Template values live in:

- `docs/nacos/templates/*.yaml`

The live value should be checked in the Nacos console under the active namespace, usually `dev`.

### 4. Inspect In-Code Defaults Before Assuming A Config Was Applied

Some keys have direct defaults in code, for example:

- `brokerage.info-fee.unified.enabled:false`
- `brokerage.info-fee.unified.amount-fen:19900`
- `rocketmq.name-server:127.0.0.1:9876`
- `tutor-application.skip-payment-check:false`

If behavior does not match Nacos, confirm whether code is falling back to a default.

## Nacos And Remote Middleware Pitfalls

- Nacos 2.x needs gRPC ports as well as HTTP
- for same-host Java services, `127.0.0.1:8848` plus reachable `9848/9849` is the important path
- mapping only `8848` in Docker can make HTTP look fine while Java config/discovery still fails
- on the shared server, keep middleware long-running and start app processes with `MANAGE_INFRA=never`

## Practical Recovery Checklist

When a service "starts then dies" on the server:

1. read `.logs/<service>.log`
2. run `bash scripts/verify_nacos_effect.sh`
3. confirm `127.0.0.1:8848`, `9848`, `9849` reachability on the server
4. confirm the expected `*-dev.yaml` exists in the `dev` namespace
5. grep the matching config class or `@Value` consumer in code
6. only after that decide whether the issue is Nacos, DB, Redis, MQ, MinIO, or business logic
