# Gotchas

Only record repo-specific truths here.

## Current Known Pitfalls

- Some repo docs are stale.
  Example: older docs may describe missing `/user/me` or missing role gating, but current code already implements them.
- `videoCall-IM-service` is broader than its name suggests.
  Do not assume "IM-only"; it also carries collaboration and some transactional flows.
- Auth debugging often requires three layers together:
  gateway JWT parsing, common identity signing, and service interceptors.
- Chat debugging often requires both backend and frontend realtime state.
  Do not change `/chat/*` in isolation without checking `ai-tutor-web/src/stores/chatRealtime.ts`.
- `scripts/dev_all_up.sh` is convenient but contains strong environment assumptions.
  Review Nacos and default secrets before treating it as production-like behavior.
- Remote-server development often wants different infra behavior from local development.
  Prefer `MANAGE_INFRA=auto` so already-running middleware containers are reused instead of blindly re-managed.
- `scripts/dev_all_down.sh` now preserves middleware by default.
  Use `STOP_INFRA=1` only when you explicitly want the compose-managed infra stopped too.
- Use the wrapper scripts instead of calling `dev_all_up/down` blindly.
  `dev_local_*` and `dev_remote_*` encode the safer defaults for each environment.
- The SSH tunnel is now a managed background process with its own PID.
  Use `scripts/ssh_tunnel.sh stop` or `scripts/dev_remote_down.sh`; do not rely on manually hunting the SSH process.
- When remote behavior does not match local script changes, first suspect sync drift.
  If the remote log is missing newly added echo lines, the updated script likely has not been uploaded to the server yet.
- Remote development should normally leave middleware alone.
  `scripts/dev_remote_up.sh` now defaults to `REMOTE_MANAGE_INFRA=never`.
- This repo uses one shared Nacos server across environments.
  Environment separation is by namespace, not by host. Current defaults are `dev=481e4376-4576-4b18-ac19-f61e170ca3ae` and `prod=44cf681d-9f93-443e-aa9e-ba6ec8f721d5`.
- For day-to-day testing, assume `dev` namespace unless the user explicitly says otherwise.
- When services run on the same server as Nacos, prefer `127.0.0.1:8848` over the public IP in remote-start scripts.
  This avoids unnecessary dependence on public ingress or hairpin networking.
- Nacos 2.x is not "just 8848".
  If Docker only maps `8848` but not `9848/9849`, Java services may appear to reach config HTTP yet still fail to load config or discovery data because the gRPC ports are missing.
- A startup failure like `storage.minio.endpoint must not be null` can be a Nacos symptom, not a MinIO symptom.
  In this repo, missing `ai-tutor-common-dev.yaml` from Nacos can cascade into bean creation errors that look unrelated to Nacos at first glance.
- If remote testing skips the SSH tunnel and uses browser direct access, the frontend dev servers must bind `0.0.0.0`.
  Otherwise `5173/5174` may be up on the server but unreachable from another machine.
- `sh scripts/dev_all_up.sh` now has environment-sensitive Nacos behavior.
  On the server it should use local `127.0.0.1:8848`; on a laptop it may auto-open a dedicated Nacos tunnel and switch to `127.0.0.1:18848`.
- Runtime config debugging in this repo is usually a four-layer problem.
  Check startup env vars, Nacos imports, optional `.private` files, and in-code defaults before concluding that "Nacos did not work".
- `tutor-appointment-service` has extra file fallbacks that the other services do not.
  It can also read `./.private/tutor-appointment-service.yml` or `../.private/tutor-appointment-service.yml`, so its effective config path is slightly wider than the other backends.
- The quickest proof of effective config is in startup logs, not in the template files.
  `bash scripts/verify_nacos_effect.sh` is usually faster than manually guessing which DataId was loaded.
- `huoyue.online` is currently a payment callback ingress, not the main frontend domain.
  Seeing `ai-tutor payment callback proxy ok` at `/` is expected and healthy.
- The current shared payment test topology is intentionally split across two servers.
  `111.229.64.41` serves only public callback ingress, while `111.228.20.88` runs the actual app and middleware.
- A YunGouOS callback may still produce `PAY_NOTIFY failed reason=missing_order_no` in `payment-service.log`.
  Do not declare the payment failed until you also check whether the order was later `updated to SUCCESS by provider query` and `PAY_FINALIZE success`.
- For current remote payment testing, the strongest success proof is a three-log chain:
  domain-server callback access log -> payment-service success/finalize log -> IM-service `tutor_application_paid` log.
- Any repo change that alters database schema is incomplete unless `sqlDoc/` is updated in the same turn.
  The final response should explicitly say whether `sqlDoc` was updated or not.
- For this repo, schema sync means both migration SQL and the full bootstrap schema.
  When adding or altering tables, update `sqlDoc/migrations/*.sql` and `sqlDoc/huoyue.sql` together, otherwise fresh environments and existing environments will drift.
- `scripts/db_bootstrap_if_missing.sh` only initializes an empty database.
  On a machine that already has core tables, it exits early and will not apply newly added files under `sqlDoc/migrations/`; use `sh scripts/db_apply_migrations.sh` explicitly after syncing code.
- Not every file under `sqlDoc/migrations/` is fully idempotent.
  Example: `20260301_student_job_posting_admin_alter.sql` can fail on repeat runs with `Duplicate column name 'reject_reason'`, so when bulk-replaying migrations on an old shared dev DB, verify the target IM/payment tables afterward instead of assuming a clean all-green log.
- Any schema change for this repo must also be applied to the shared remote dev server `111.228.20.88` in the same turn.
  Updating local SQL files without remote DB sync is considered incomplete delivery.
- The shared remote dev server currently does not expose a host `mysql` binary in the shell.
  For manual schema sync and verification there, use `docker exec` against the actual running MySQL container.
- Do not assume the remote MySQL container is literally named `mysql`.
  On `111.228.20.88` it may be a compose-generated name like `759d793c134e_mysql`, so detect it with `docker ps --format '{{.Names}}' | grep mysql | head -1` before running DB commands.

## Maintenance Rule

Whenever you discover one of these, add a note:

- a stale doc that can mislead future work
- a hidden cross-service dependency
- a surprising required validation step
- an environment assumption that changes behavior
