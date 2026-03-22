#!/usr/bin/env bash
set -euo pipefail

PRE_SHA="${1:-8c44240}"
POST_SHA="${2:-bc2b0b3}"
ROOT="$(git rev-parse --show-toplevel)"
CMD=(./mvnw -pl ai-tutor-gateway -Dtest=JwtClaimsServiceTest,GatewaySignServiceTest test)

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/task2-gateway-tdd-evidence.XXXXXX")"
WT_PRE="$TMP_DIR/pre"
WT_POST="$TMP_DIR/post"

TEST_DIR="ai-tutor-gateway/src/test/java/com/ai/tutor/gateway/security"
TEST_JWT="$TEST_DIR/JwtClaimsServiceTest.java"
TEST_SIGN="$TEST_DIR/GatewaySignServiceTest.java"

cleanup() {
  git -C "$ROOT" worktree remove --force "$WT_PRE" >/dev/null 2>&1 || true
  git -C "$ROOT" worktree remove --force "$WT_POST" >/dev/null 2>&1 || true
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

contains_text() {
  local haystack="$1"
  local needle="$2"
  printf '%s\n' "$haystack" | grep -Fq "$needle"
}

contains_regex() {
  local haystack="$1"
  local pattern="$2"
  printf '%s\n' "$haystack" | grep -Eq "$pattern"
}

inject_tests_into_pre() {
  mkdir -p "$WT_PRE/$TEST_DIR"
  git -C "$ROOT" show "$POST_SHA:$TEST_JWT" > "$WT_PRE/$TEST_JWT"
  git -C "$ROOT" show "$POST_SHA:$TEST_SIGN" > "$WT_PRE/$TEST_SIGN"
}

run_and_capture() {
  local wt="$1"
  local sha="$2"
  git -C "$ROOT" worktree add --detach "$wt" "$sha" >/dev/null

  if [[ "$wt" == "$WT_PRE" ]]; then
    inject_tests_into_pre
  fi

  set +e
  local output
  output="$(cd "$wt" && "${CMD[@]}" 2>&1)"
  local code=$?
  set -e

  printf '%s\n' "$code"
  printf '%s\n' "__OUTPUT_START__"
  printf '%s\n' "$output"
  printf '%s\n' "__OUTPUT_END__"
}

verify_pre() {
  local result
  result="$(run_and_capture "$WT_PRE" "$PRE_SHA")"
  local code
  code="$(printf '%s\n' "$result" | sed -n '1p')"
  local output
  output="$(printf '%s\n' "$result" | sed -n '/^__OUTPUT_START__$/,/^__OUTPUT_END__$/p' | sed '1d;$d')"

  if [[ "$code" -eq 0 ]]; then
    echo "[FAIL] PRE commit unexpectedly passed: $PRE_SHA"
    return 1
  fi

  if contains_text "$output" "No tests were executed"; then
    echo "[FAIL] PRE commit failed due to no tests executed (not acceptable): $PRE_SHA"
    return 1
  fi

  if ! contains_text "$output" "JwtClaimsService" || ! contains_text "$output" "GatewaySignService"; then
    echo "[FAIL] PRE commit failed, but missing class references for JwtClaimsService/GatewaySignService: $PRE_SHA"
    return 1
  fi

  if ! contains_regex "$output" "cannot find symbol|找不到符号"; then
    echo "[FAIL] PRE commit failed, but missing compile-time missing symbol marker: $PRE_SHA"
    return 1
  fi

  echo "[OK] PRE commit fails as expected ($PRE_SHA): missing JwtClaimsService/GatewaySignService symbols"
}

verify_post() {
  local result
  result="$(run_and_capture "$WT_POST" "$POST_SHA")"
  local code
  code="$(printf '%s\n' "$result" | sed -n '1p')"
  local output
  output="$(printf '%s\n' "$result" | sed -n '/^__OUTPUT_START__$/,/^__OUTPUT_END__$/p' | sed '1d;$d')"

  if [[ "$code" -ne 0 ]]; then
    echo "[FAIL] POST commit unexpectedly failed: $POST_SHA"
    return 1
  fi

  if ! contains_text "$output" "BUILD SUCCESS"; then
    echo "[FAIL] POST commit has exit 0 but missing BUILD SUCCESS marker: $POST_SHA"
    return 1
  fi

  if ! contains_text "$output" "JwtClaimsServiceTest" || ! contains_text "$output" "GatewaySignServiceTest"; then
    echo "[FAIL] POST commit missing test names in output: $POST_SHA"
    return 1
  fi

  if ! contains_text "$output" "Failures: 0, Errors: 0"; then
    echo "[FAIL] POST commit missing green test summary: $POST_SHA"
    return 1
  fi

  echo "[OK] POST commit passes as expected ($POST_SHA): JwtClaimsServiceTest/GatewaySignServiceTest green"
}

verify_pre
verify_post

echo "[OK] Task 2 fail-first evidence replay succeeded."
