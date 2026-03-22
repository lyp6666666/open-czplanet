# Task 2 TDD Evidence: Gateway JWT Claims + Signing

Date: 2026-03-22  
Scope: Verifiable fail-first evidence for Task 2 (JWT claims parsing + signing core)

## Commits under verification
- Pre-task baseline (expected RED): `8c44240` (`chore(gateway): apply task1 quality fixes`)
- Task 2 implementation (expected GREEN): `bc2b0b3` (`feat(gateway): add jwt claims and signing core`)

## Repro command (automated)
Run from repository root:

```bash
bash scripts/verify-task2-gateway-tdd-evidence.sh
```

This script performs an isolated replay in temporary detached worktrees and validates:
1. At `8c44240` with only Task 2 tests injected, command fails and includes compile-time missing symbol evidence for `JwtClaimsService` and `GatewaySignService`.
2. At `bc2b0b3`, same command passes with `BUILD SUCCESS` and tests green.

## Manual reproduction commands

### RED check at pre-task commit (`8c44240`)

```bash
tmpdir=$(mktemp -d /tmp/task2-precheck.XXXXXX)
git worktree add --detach "$tmpdir/wt" 8c44240

# Inject only Task 2 tests from bc2b0b3
mkdir -p "$tmpdir/wt/ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/security"
git show bc2b0b3:ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/security/JwtClaimsServiceTest.java \
  > "$tmpdir/wt/ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/security/JwtClaimsServiceTest.java"
git show bc2b0b3:ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/security/GatewaySignServiceTest.java \
  > "$tmpdir/wt/ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/security/GatewaySignServiceTest.java"

(
  cd "$tmpdir/wt"
  ./mvnw -pl ai-tutor-gateway -Dtest=JwtClaimsServiceTest,GatewaySignServiceTest test
)
# Expected: non-zero exit and compile errors referencing missing symbols/classes
# including JwtClaimsService and GatewaySignService

git worktree remove --force "$tmpdir/wt"
rm -rf "$tmpdir"
```

### GREEN check at Task 2 commit (`bc2b0b3`)

```bash
tmpdir=$(mktemp -d /tmp/task2-postcheck.XXXXXX)
git worktree add --detach "$tmpdir/wt" bc2b0b3
(
  cd "$tmpdir/wt"
  ./mvnw -pl ai-tutor-gateway -Dtest=JwtClaimsServiceTest,GatewaySignServiceTest test
)
# Expected: zero exit with BUILD SUCCESS and test summary showing 0 failures/errors

git worktree remove --force "$tmpdir/wt"
rm -rf "$tmpdir"
```

## Observed replay summary (2026-03-22)
- `8c44240`: exit code `1`, compile errors include missing symbols for `JwtClaimsService` and `GatewaySignService`
- `bc2b0b3`: exit code `0`, includes `BUILD SUCCESS` and `Failures: 0, Errors: 0`
