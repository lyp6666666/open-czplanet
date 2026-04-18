# Change Log

Append one short entry after validated repo changes made while using this skill.

## Template

```text
## YYYY-MM-DD
- Request:
- Areas:
- Background checked:
- Validation:
- New note added:
```

## 2026-04-12

- Request:
  Create the first durable project skill for this repo
- Areas:
  `skills/`
- Background checked:
  repo structure, backend/frontend module map, startup scripts, skill-creator guidance
- Validation:
  ran `bash skills/scripts/project-snapshot.sh`
  ran `bash skills/scripts/changed-area-check.sh ai-tutor-web/src/pages/chat/ChatRoomPage.vue payment-service/src/main/java/com/ai/tutor/payment/service/YungouosPaymentAppService.java`
- New note added:
  initial project skill split into concise `SKILL.md`, references, and routing scripts

## 2026-04-12

- Request:
  Make remote-server startup friendlier for always-on middleware and add a reusable SSH tunnel helper
- Areas:
  `scripts/`, `skills/references/`
- Background checked:
  startup scripts, remote dev workflow, skill maintenance notes
- Validation:
  ran `sh -n scripts/dev_all_up.sh`
  ran `sh -n scripts/dev_all_down.sh`
  ran `sh -n scripts/ssh_tunnel.sh`
- New note added:
  `MANAGE_INFRA=auto` and `STOP_INFRA=1` flow documented for remote environments

## 2026-04-12

- Request:
  Split local-vs-remote startup into separate scripts and make tunnel shutdown part of the remote stop flow
- Areas:
  `scripts/`, `skills/references/`
- Background checked:
  startup scripts, remote workflow expectations, tunnel lifecycle requirements
- Validation:
  ran `sh -n scripts/dev_local_up.sh`
  ran `sh -n scripts/dev_local_down.sh`
  ran `sh -n scripts/dev_remote_up.sh`
  ran `sh -n scripts/dev_remote_down.sh`
  ran `sh -n scripts/ssh_tunnel.sh`
- New note added:
  local and remote development are now intentionally separate entrypoints

## 2026-04-12

- Request:
  Fix remote startup defaults after hitting middleware container conflicts on a server with always-on infra
- Areas:
  `scripts/`, `skills/references/`
- Background checked:
  startup flow, remote env assumptions, infra container lifecycle
- Validation:
  ran `sh -n scripts/dev_all_up.sh`
  ran `sh -n scripts/dev_remote_up.sh`
- New note added:
  remote startup now defaults to `REMOTE_MANAGE_INFRA=never`, and `auto` mode reuses existing containers more safely

## 2026-04-13

- Request:
  Align repository defaults with the real shared Nacos server and current dev/prod namespace IDs
- Areas:
  `scripts/`, service `application.yml`, `common.md`, `skills/references/`
- Background checked:
  current startup defaults, service Nacos placeholders, confirmed environment namespace IDs
- Validation:
  searched all old namespace IDs and replaced repo defaults
  ran shell syntax checks on startup scripts after the change
- New note added:
  startup now defaults to the real `dev` namespace and records the current `prod` namespace for explicit switching

## 2026-04-13

- Request:
  Make remote startup use localhost Nacos by default when Nacos runs on the same server
- Areas:
  `scripts/`, `common.md`, `skills/references/`
- Background checked:
  remote startup flow, current shared Nacos deployment on the same host
- Validation:
  checked remote wrapper defaults and updated docs to match
- New note added:
  remote startup now defaults to `REMOTE_NACOS_SERVER_ADDR=127.0.0.1:8848`

## 2026-04-13

- Request:
  Support remote direct-browser testing and document the real Nacos 2.x port requirements
- Areas:
  `scripts/`, `common.md`, `skills/references/`
- Background checked:
  remote startup flow, frontend dev-server binding, confirmed Docker `nacos` port bindings from `docker inspect`
- Validation:
  ran shell syntax checks on updated startup scripts
  reviewed docs against current server-side Nacos behavior and namespace defaults
- New note added:
  remote startup now supports `REMOTE_USE_TUNNEL=0`, and the repo docs now call out that Nacos 2.x needs `9848/9849` in addition to `8848`

## 2026-04-13

- Request:
  Fix stale Nacos migration guide namespace IDs while rebuilding the shared remote Nacos setup
- Areas:
  `docs/nacos/CONFIG_MIGRATION_GUIDE.md`
- Background checked:
  current repo startup defaults, confirmed live dev/prod namespace IDs, Nacos recovery workflow
- Validation:
  searched old namespace IDs in the guide and replaced them with the current ones
- New note added:
  the migration guide now matches the active shared Nacos `dev/prod` namespace IDs

## 2026-04-13

- Request:
  Make repo startup scripts work on environments that only provide legacy `docker-compose`
- Areas:
  `scripts/`, `skills/references/`
- Background checked:
  current server Docker command behavior, startup/teardown scripts, compose usage in repo helpers
- Validation:
  ran shell syntax checks on updated scripts using both `sh -n` and `bash -n`
- New note added:
  core dev scripts now auto-detect `docker compose` vs `docker-compose`

## 2026-04-13

- Request:
  Make `sh scripts/dev_all_up.sh` work directly on both the server and local machines with the rebuilt shared Nacos
- Areas:
  `scripts/`, service `application.yml`, `common.md`, `skills/references/`
- Background checked:
  current startup flow, rebuilt Nacos deployment, new shared `dev` namespace ID, local-vs-server Nacos reachability
- Validation:
  added a dedicated Nacos tunnel script and updated startup defaults to auto-select local vs tunneled Nacos
  updated repo defaults to the current `dev` namespace ID
- New note added:
  `dev_all_up.sh` now prefers local `127.0.0.1:8848` and can auto-establish a local Nacos tunnel when running off-server

## 2026-04-14

- Request:
  Summarize how the project currently runs online, how local testing usually works, and how runtime config is loaded and retrieved, then fold that into the project skill
- Areas:
  `skills/`, startup scripts, service `application.yml`, Nacos templates, `common.md`
- Background checked:
  current startup wrappers, direct server workflow, Nacos import chains, shared namespace defaults, config templates
- Validation:
  reviewed `scripts/dev_all_up.sh`, `scripts/dev_remote_up.sh`, `scripts/dev_all_down.sh`
  reviewed backend `application.yml` import chains
  reviewed `docs/nacos/templates/*.yaml` and `common.md`
- New note added:
  the skill now includes a dedicated runtime/config reference covering remote-vs-local startup, effective config lookup, and high-value Nacos DataIds

## 2026-04-16

- Request:
  Record the now-working payment callback test topology, real verification routine, server roles, product payment flow, and Nacos payment config into the project skill
- Areas:
  `skills/`
- Background checked:
  current startup scripts, live remote server roles, verified callback logs from `111.229.64.41` and `111.228.20.88`, shared Nacos payment config
- Validation:
  reviewed `skills/SKILL.md`
  reviewed payment/business/runtime/gotchas references
  incorporated the observed successful callback sequence from the `2026-04-16` live payment test
- New note added:
  added a dedicated payment remote testing reference and routed payment/runtime work to it

## 2026-04-16

- Request:
  Rename the frontend brand to `创智星球`, improve chat realtime message notice and unread-dot behavior, and add a permanent skill rule for syncing `sqlDoc` on schema changes
- Areas:
  `ai-tutor-web/`, `skills/`
- Background checked:
  chat realtime store, top bar unread entry, current SSE/unread/ack flow, existing brand text locations, skill maintenance rules
- Validation:
  ran `cd ai-tutor-web && npm run typecheck`
  ran `cd ai-tutor-web && npm run lint` and confirmed there were no errors, only existing repo-wide Vue formatting warnings
- New note added:
  chat realtime now uses existing SSE for lightweight new-message popups plus optimistic read-state suppression, and the skill now requires same-turn `sqlDoc/` sync for any schema change

## 2026-04-18

- Request:
  Re-check the completed IM feature set, fold the supported capabilities into the project skill, and verify whether the shared remote dev server is missing synced code or DB migrations
- Areas:
  `skills/`, `scripts/`, chat/realtime feature area, shared remote dev workflow
- Background checked:
  skill references, remote startup scripts, migration helpers, recent IM-related commits
- Validation:
  re-ran focused chat frontend tests and IM backend tests
  confirmed the shared remote dev repo was behind local and inspected migration helper behavior
- New note added:
  skill docs now list the current IM capability set and explicitly call out that existing databases need manual `db_apply_migrations.sh` after remote sync
