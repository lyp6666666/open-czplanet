#!/usr/bin/env bash
set -euo pipefail

PRE_SHA="${1:-78830d0}"
POST_SHA="${2:-52eee45}"
ROOT="$(git rev-parse --show-toplevel)"
CMD=(./mvnw -pl ai-tutor-gateway -am -Dtest=GatewayApplicationContextTest test)

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/task1-gateway-tdd-evidence.XXXXXX")"
WT_PRE="$TMP_DIR/pre"
WT_POST="$TMP_DIR/post"

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

run_and_capture() {
  local wt="$1"
  local sha="$2"
  git -C "$ROOT" worktree add --detach "$wt" "$sha" >/dev/null

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

  if ! contains_text "$output" "Could not find the selected project in the reactor" \
    || ! contains_text "$output" "ai-tutor-gateway"; then
    echo "[FAIL] PRE commit failed, but missing expected reactor/module markers: $PRE_SHA"
    return 1
  fi

  echo "[OK] PRE commit fails as expected ($PRE_SHA): module ai-tutor-gateway missing"
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

  if ! contains_text "$output" "GatewayApplicationContextTest" \
    || ! contains_text "$output" "Failures: 0, Errors: 0"; then
    echo "[FAIL] POST commit missing robust pass indicators for GatewayApplicationContextTest: $POST_SHA"
    return 1
  fi

  echo "[OK] POST commit passes as expected ($POST_SHA): GatewayApplicationContextTest green"
}

verify_pre
verify_post

echo "[OK] Task 1 fail-first evidence replay succeeded."
