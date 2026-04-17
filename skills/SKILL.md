---
name: "ai-platform-context"
description: "Use when analyzing, modifying, debugging, reviewing, or extending anything in the ai-tutor-platform repository. Before making changes, route yourself to the relevant project background in skills/references based on the affected module or file area. After validated repo changes, append a short entry to skills/references/change-log.md and record any new repo-specific pitfall in skills/references/gotchas.md."
---

# AI Platform Context

This skill is the project operating guide for the `ai-tutor-platform` repository.

Use it whenever work touches this repo's code, configuration, tests, or specs.

## Core Rules

- Treat code as the source of truth. Some docs and OpenSpec files lag behind the current implementation.
- If a task changes database schema, migration SQL, or table structure, update the matching files under `sqlDoc/` in the same turn and explicitly mention that sync in the final response.
- Before changing code, first identify the affected area and read the matching sections in:
  - `references/module-map.md`
  - `references/business-flows.md`
  - `references/gotchas.md`
- If the touched files are already known, run `scripts/changed-area-check.sh <paths...>` to decide which background to read and which validation to run.
- If the affected area is still unclear, run `scripts/project-snapshot.sh` and inspect the nearest router, controller, service, or module `pom.xml`.

## Background Routing

- Authentication, identity, gateway, Feign propagation:
  Read `references/module-map.md` and `references/gotchas.md`
- User, profile, demand/job, schedule, asset upload:
  Read `references/module-map.md`, then `references/business-flows.md`
- Chat, realtime, collaboration proposal, contact unlock, refund-in-chat:
  Read `references/business-flows.md`, then `references/gotchas.md`
- Payment, cashier, refund callback, brokerage:
  Read `references/business-flows.md`, `references/payment-remote-testing.md`, `references/testing-matrix.md`, and `references/gotchas.md`
- Admin backend or admin web:
  Read `references/module-map.md` and `references/testing-matrix.md`
- Web frontend or miniprogram:
  Read `references/module-map.md`, then the relevant flow in `references/business-flows.md`
- Local startup, remote server testing, infra, Nacos, Docker, environment variables, or runtime config lookup:
  Read `references/commands.md`, then `references/runtime-config.md`, then `references/payment-remote-testing.md`, then `references/gotchas.md`
- Tests, QA automation, regression planning:
  Read `references/testing-matrix.md`

## Default Workflow

1. Classify the request by module or changed file path.
2. Read the matching reference sections before proposing or applying a change.
3. Make the smallest coherent change that fits existing repo patterns.
4. Run the narrowest meaningful validation from `references/testing-matrix.md`.
5. If you learned a new repo-specific rule, pitfall, or shortcut, update:
   - `references/gotchas.md`
   - `references/commands.md`
   - `references/business-flows.md`
   - `references/testing-matrix.md`
6. After validated repo changes, append a concise dated note to `references/change-log.md`.

## Continuous Improvement Contract

When this skill is used during real project work:

- Keep `SKILL.md` short; move evolving detail into `references/`
- Prefer adding one sharp note over rewriting large sections
- Record only repo-specific knowledge, not generic framework advice
- If an old doc is wrong but not fixed yet, note the mismatch in `references/gotchas.md`
- If a recurring command or path lookup happens twice, promote it into `references/commands.md` or a script

## References

- Repository map and ownership hints:
  `references/module-map.md`
- Product and technical flow map:
  `references/business-flows.md`
- Local commands and startup routines:
  `references/commands.md`
- Runtime environment, config import chain, and Nacos lookup:
  `references/runtime-config.md`
- Payment callback topology, shared test servers, and live verification routine:
  `references/payment-remote-testing.md`
- Validation guidance by change type:
  `references/testing-matrix.md`
- Known traps, stale docs, and cross-module cautions:
  `references/gotchas.md`
- Ongoing maintenance log for this skill:
  `references/change-log.md`

## Scripts

- `scripts/project-snapshot.sh`
  Print a compact overview of the repo's major modules and entry files
- `scripts/changed-area-check.sh <paths...>`
  Infer background docs and validation focus from touched file paths
