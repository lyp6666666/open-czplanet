# Task 1 TDD Evidence: Gateway Bootstrap

Date: 2026-03-22  
Scope: Verifiable fail-first evidence for Task 1 (`ai-tutor-gateway` bootstrap)

## Commits under verification
- Pre-task baseline (expected RED): `78830d0` (`docs: add gateway+nacos+identity implementation plan`)
- Task 1 implementation (expected GREEN): `52eee45` (`feat(gateway): bootstrap gateway module and app entry`)

## Repro command (automated)
Run from repository root:

```bash
bash scripts/verify-task1-gateway-tdd-evidence.sh
```

This script performs an isolated replay in temporary detached worktrees and validates:
1. At `78830d0`, command fails and contains: `Could not find the selected project in the reactor: ai-tutor-gateway`
2. At `52eee45`, same command passes with `BUILD SUCCESS` and `Tests run: 1, Failures: 0, Errors: 0`

## Manual reproduction commands

### RED check at pre-task commit (`78830d0`)

```bash
tmpdir=$(mktemp -d /tmp/task1-precheck.XXXXXX)
git worktree add --detach "$tmpdir/wt" 78830d0
(
  cd "$tmpdir/wt"
  ./mvnw -pl ai-tutor-gateway -am -Dtest=GatewayApplicationContextTest test
)
# Expected: non-zero exit and message:
# "Could not find the selected project in the reactor: ai-tutor-gateway"
git worktree remove --force "$tmpdir/wt"
rm -rf "$tmpdir"
```

### GREEN check at Task 1 commit (`52eee45`)

```bash
tmpdir=$(mktemp -d /tmp/task1-postcheck.XXXXXX)
git worktree add --detach "$tmpdir/wt" 52eee45
(
  cd "$tmpdir/wt"
  ./mvnw -pl ai-tutor-gateway -am -Dtest=GatewayApplicationContextTest test
)
# Expected: zero exit with BUILD SUCCESS and test summary showing 0 failures/errors
git worktree remove --force "$tmpdir/wt"
rm -rf "$tmpdir"
```

## Observed replay summary (2026-03-22)
- `78830d0`: exit code `1`, includes `Could not find the selected project in the reactor: ai-tutor-gateway`
- `52eee45`: exit code `0`, includes `BUILD SUCCESS` and `Tests run: 1, Failures: 0, Errors: 0`
