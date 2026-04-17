# Payment Remote Testing

Use this reference when work touches payment callback reachability, cashier testing, chat unlock after payment, or the shared remote test topology.

## Current Topology

### Laptop

- Main working copy lives on the laptop.
- VS Code SFTP auto-sync uploads repo changes to:
  `root@111.228.20.88:/opt/ai-platform`
- Recommended local browser access during remote testing is through the managed SSH tunnel:
  `http://localhost:5173`
  `http://localhost:5174`
  `http://localhost:18080`

### Business Server

- Main app server:
  `111.228.20.88`
- Runs the actual repo checkout and all app processes:
  gateway `18080`
  appointment `18081`
  IM `18082`
  payment `18083`
  admin `18084`
  user web `5173`
  admin web `5174`
- Shared middleware is expected to stay running on this host:
  MySQL, Redis, RabbitMQ, MinIO, Prometheus, Grafana, Nacos
- Day-to-day remote startup on this host should usually leave middleware alone.

### Domain / Callback Proxy Server

- Domain server:
  `111.229.64.41`
- Domain:
  `huoyue.online`
- This machine only runs `nginx`.
- It is not the main app host.
- It exists so payment providers can call a domain-backed public URL without being blocked by the business server's cloud-provider ingress rules.
- Current responsibility:
  proxy `/payment/notify/*` and `/payment/return/*` to `111.228.20.88`
- It does not serve the full frontend site.
- `http://huoyue.online/` intentionally returns:
  `ai-tutor payment callback proxy ok`

## Why The Two-Server Split Exists

- Payment providers require a public callback URL.
- The business server `111.228.20.88` is where the app actually runs, but direct domain access there was blocked by provider-side domain interception.
- The domain server `111.229.64.41` is the public ingress for payment callback paths.
- The domain server rewrites the upstream `Host` header to `111.228.20.88` before proxying.
- Effective callback path:
  provider -> `huoyue.online` on `111.229.64.41` -> `nginx` proxy -> `111.228.20.88` gateway/payment-service

## Current Payment Test Routine

### 1. Sync Code

- Keep local code as the editing source of truth.
- Save files locally and let SFTP sync to:
  `111.228.20.88:/opt/ai-platform`
- If behavior looks stale on the server, suspect SFTP drift first and verify the remote file content directly.

### 2. Start The Business Server

On `111.228.20.88`, the standard remote start for payment testing is:

```bash
cd /opt/ai-platform
MANAGE_INFRA=never FRONTEND_HOST=0.0.0.0 NACOS_SERVER_ADDR=127.0.0.1:8848 sh scripts/dev_local_up.sh
```

Why this exact mode:

- middleware is already always-on on this server
- `MANAGE_INFRA=never` avoids touching Docker middleware
- `NACOS_SERVER_ADDR=127.0.0.1:8848` uses same-host Nacos
- `FRONTEND_HOST=0.0.0.0` allows either direct remote access or local SSH tunneling

Stop command:

```bash
cd /opt/ai-platform
STOP_INFRA=0 sh scripts/dev_local_down.sh
```

### 3. Open Local Tunnel From The Laptop

Preferred local entrypoint:

```bash
bash scripts/ssh_tunnel.sh start
```

Status / stop:

```bash
bash scripts/ssh_tunnel.sh status
bash scripts/ssh_tunnel.sh stop
```

Tunnel targets:

- `localhost:5173` -> remote `127.0.0.1:5173`
- `localhost:5174` -> remote `127.0.0.1:5174`
- `localhost:18080` -> remote `127.0.0.1:18080`

### 4. Use The Right URLs During Testing

- Frontend testing from the laptop:
  `http://localhost:5173`
- Admin frontend from the laptop:
  `http://localhost:5174`
- Gateway/API from the laptop:
  `http://localhost:18080`
- Payment callback public domain:
  `http://huoyue.online/payment/notify/yungouos`
- Payment return public domain:
  `http://huoyue.online/payment/return/yungouos`

Important distinction:

- The user does not browse the app via `huoyue.online` today.
- `huoyue.online` is only the public payment callback/return ingress.
- If `huoyue.online/` shows `ai-tutor payment callback proxy ok`, that is healthy.

## Live Callback Verification Routine

During a real payment test, keep these three log windows open.

### 1. Domain Server `nginx`

Proof that the provider reached the public callback ingress:

```bash
ssh root@111.229.64.41 "tail -f /var/log/nginx/ai-tutor-payment-domain.access.log | grep --line-buffered -E 'payment/notify/yungouos|payment/return/yungouos'"
```

Expected sign:

- `POST /payment/notify/yungouos`
- user-agent like `YunGouOS-Notify-CallBack`

### 2. Business Server Payment Service

Proof that payment callback and finalization reached the app:

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform && tail -f .logs/payment-service.log | grep --line-buffered -E 'PAY_NOTIFY|PAY_FINALIZE|updated to SUCCESS|YunGouOS 回调|YunGouOS 回调验签|YunGouOS 回调订单不存在'"
```

What matters most:

- `PAY_NOTIFY`
- `updated to SUCCESS`
- `PAY_FINALIZE success`

Observed real successful sequence on `2026-04-16`:

- provider callback hit `huoyue.online`
- `PAY_NOTIFY` logs appeared, but `orderNo` was null in repeated notify payloads
- payment status was still confirmed by provider query
- `Payment order ... updated to SUCCESS by provider query`
- `PAY_FINALIZE success`

Important repo-specific lesson:

- A YunGouOS notify request may arrive without a directly usable `orderNo` in this setup.
- Even then, the system can still reach success through provider query plus payment finalization.
- Do not assume `PAY_NOTIFY failed reason=missing_order_no` means the whole payment failed.

### 3. Business Server IM Service

Proof that payment success unlocked business/chat state:

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform && tail -f .logs/videoCall-IM-service.log | grep --line-buffered -E 'payment_success_received|brokerage_payment_success|tutor_application_paid'"
```

What matters most:

- `brokerage_payment_success`
- `tutor_application_paid`

Observed real successful sequence on `2026-04-16`:

- `brokerage_payment_success start`
- `tutor_application_paid`
- `brokerage_payment_success done`

That sequence matched the frontend result:

- payment succeeded
- page could jump back to the message area
- chat became available

## Payment Product Flow In This Project

Current intended flow:

1. Teacher initiates an application.
2. Student accepts.
3. Teacher pays the information fee.
4. Chat becomes available.
5. Teacher and student negotiate details in chat.
6. Either side can proceed to collaboration.
7. Collaboration has a one-week trial period.
8. If the trial fails, the student can initiate a refund.
9. Current default trial refund ratio is 80 percent.
10. If the match is unsuitable right after paying for contact unlock, refund can be initiated directly from chat.
11. This early-stage chat refund is intended to be 100 percent, subject to admin review.
12. If admin does not review within the configured timeout, the refund should auto-progress.

When debugging this flow, payment success is only the midpoint.
Trace all the way into IM-side collaboration and refund state if the UI still looks wrong.

## Nacos Values That Matter For Payment Testing

Shared Nacos host:

- `111.228.20.88:8848`

Namespaces:

- `dev=481e4376-4576-4b18-ac19-f61e170ca3ae`
- `prod=44cf681d-9f93-443e-aa9e-ba6ec8f721d5`

Main payment config DataId used in shared dev:

- `ai-tutor-payment-dev.yaml`

Current important live values:

- `payment.enabled: true`
- `yungouos.notify-url: "http://huoyue.online/payment/notify/yungouos"`
- `yungouos.return-url: "http://huoyue.online/payment/return/yungouos"`
- `yungouos.return-page-url: "http://111.228.20.88:5173/"`

Interpretation:

- notify/return callback must use the public domain server
- return-page currently still points to the business server frontend, not `huoyue.online`
- async callback correctness matters more than return-page behavior when judging whether payment state was really finalized

Quick lookup:

```bash
ssh root@111.228.20.88 "curl -s 'http://127.0.0.1:8848/nacos/v1/cs/configs?tenant=481e4376-4576-4b18-ac19-f61e170ca3ae&dataId=ai-tutor-payment-dev.yaml&group=DEFAULT_GROUP' | grep -nE 'notifyUrl:|notify-url:|return-url:|return-page-url:'"
```

## Fast Health Checks

Confirm domain target:

```bash
dig +short huoyue.online
```

Expected:

- `111.229.64.41`

Confirm domain server root health:

```bash
curl -i http://huoyue.online/
```

Expected body:

- `ai-tutor payment callback proxy ok`

Confirm business server ports:

```bash
ssh root@111.228.20.88 "cd /opt/ai-platform && ss -ltnp | grep -E ':18080|:18082|:18083|:5173|:5174 ' || true"
```

## Decision Rule During A Live Test

- If the domain-server `nginx` log has callback entries, the provider reached the public ingress.
- If payment-service then logs `updated to SUCCESS` and `PAY_FINALIZE success`, the app accepted payment.
- If IM-service logs `tutor_application_paid`, the business unlock succeeded.
- If frontend still looks wrong after those three are true, suspect frontend state refresh or page routing rather than callback reachability.
