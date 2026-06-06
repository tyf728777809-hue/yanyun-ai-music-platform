#!/usr/bin/env bash
set -euo pipefail
set +x

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
INDEX="${SCRIPT_DIR}/real-model-controlled-smoke.sh"

PASS_COUNT=0
FAIL_COUNT=0

TARGETS=(
  "yunwu-suno"
  "dreammaker-suno"
  "dreammaker-minimax"
  "deepseek"
  "wellapi-image2"
  "dreammaker-image2"
)

log() {
  printf '[real-model-gate-audit] %s\n' "$*"
}

pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  log "PASS $*"
}

fail_check() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  log "FAIL $*"
}

need_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    fail_check "required tool missing: $1"
  fi
}

run_capture() {
  local output
  set +e
  output="$("$@" 2>&1)"
  local status=$?
  set -e
  printf '%s\n' "$status"
  printf '%s' "$output"
}

secret_pattern='sk-[A-Za-z0-9_-]{12,}|Bearer [A-Za-z0-9._~+/=-]{16,}|AccessKey[[:space:]]*[:：]|SecretKey[[:space:]]*[:：]|BEGIN ((RSA|EC|OPENSSH|DSA) )?PRIVATE KEY|eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}'

check_no_secret_output() {
  local label="$1"
  local output="$2"
  if printf '%s' "$output" | rg -q "$secret_pattern"; then
    fail_check "$label emitted secret-like output"
  else
    pass "$label emitted no secret-like output"
  fi
}

assert_contains() {
  local label="$1"
  local output="$2"
  local expected="$3"
  if [[ "$output" == *"$expected"* ]]; then
    pass "$label contains '$expected'"
  else
    fail_check "$label missing '$expected'"
  fi
}

assert_success() {
  local label="$1"
  local status="$2"
  if [ "$status" -eq 0 ]; then
    pass "$label succeeded"
  else
    fail_check "$label failed with status=$status"
  fi
}

assert_failure() {
  local label="$1"
  local status="$2"
  if [ "$status" -ne 0 ]; then
    pass "$label failed as expected"
  else
    fail_check "$label unexpectedly succeeded"
  fi
}

capture_command() {
  local __status_var="$1"
  local __output_var="$2"
  shift 2
  local __captured __status __output
  __captured="$(run_capture "$@")"
  __status="$(printf '%s\n' "$__captured" | sed -n '1p')"
  __output="$(printf '%s\n' "$__captured" | sed '1d')"
  printf -v "$__status_var" '%s' "$__status"
  printf -v "$__output_var" '%s' "$__output"
}

check_list_mode() {
  local status output
  capture_command status output "$INDEX"
  assert_success "list mode" "$status"
  assert_contains "list mode" "$output" "DreamMaker music and DreamMaker Image 2 must remain the production-target paths."
  for target in "${TARGETS[@]}"; do
    assert_contains "list mode" "$output" "$target"
  done
  check_no_secret_output "list mode" "$output"
}

check_plan_mode() {
  local target status output
  for target in "${TARGETS[@]}"; do
    capture_command status output env TARGET="$target" MODE=plan "$INDEX"
    assert_success "plan target=$target" "$status"
    assert_contains "plan target=$target" "$output" "Target: $target"
    assert_contains "plan target=$target" "$output" "Preflight:"
    assert_contains "plan target=$target" "$output" "Execute:"
    check_no_secret_output "plan target=$target" "$output"
  done
}

check_global_execute_gate() {
  local target status output
  for target in "${TARGETS[@]}"; do
    capture_command status output env TARGET="$target" MODE=execute "$INDEX"
    assert_failure "global gate target=$target" "$status"
    assert_contains "global gate target=$target" "$output" "ALLOW_REAL_MODEL_SMOKE=1"
    check_no_secret_output "global gate target=$target" "$output"
  done
}

check_target_execute_gate() {
  local target status output expected
  for target in "${TARGETS[@]}"; do
    expected="$(expected_target_gate "$target")"
    capture_target_execute_without_target_gate "$target" status output
    assert_failure "target gate target=$target" "$status"
    assert_contains "target gate target=$target" "$output" "$expected"
    check_no_secret_output "target gate target=$target" "$output"
  done
}

expected_target_gate() {
  case "$1" in
    yunwu-suno) printf '%s\n' "ALLOW_YUNWU_REAL_SMOKE=1" ;;
    dreammaker-suno|dreammaker-minimax) printf '%s\n' "ALLOW_DREAMMAKER_REAL_SMOKE=1" ;;
    deepseek) printf '%s\n' "ALLOW_DEEPSEEK_REAL_SMOKE=1" ;;
    wellapi-image2) printf '%s\n' "ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1" ;;
    dreammaker-image2) printf '%s\n' "ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1" ;;
    *) printf '%s\n' "ALLOW_" ;;
  esac
}

capture_target_execute_without_target_gate() {
  local target="$1"
  local __status_var="$2"
  local __output_var="$3"

  case "$target" in
    yunwu-suno)
      capture_command "$__status_var" "$__output_var" env \
        ALLOW_REAL_MODEL_SMOKE=1 \
        TARGET=yunwu-suno \
        MODE=execute \
        SUNO_BACKEND=yunwu \
        YUNWU_REAL_CALLS_ENABLED=true \
        YUNWU_API_KEY=redacted \
        MUSIC_WORKFLOW_DISPATCH_MODE=outbox \
        WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal \
        WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true \
        "$INDEX"
      ;;
    dreammaker-suno)
      capture_command "$__status_var" "$__output_var" env \
        ALLOW_REAL_MODEL_SMOKE=1 \
        TARGET=dreammaker-suno \
        MODE=execute \
        SUNO_BACKEND=dreammaker \
        DREAMMAKER_REAL_CALLS_ENABLED=true \
        DREAMMAKER_ACCESS_KEY=redacted \
        DREAMMAKER_SECRET_KEY=redacted \
        MUSIC_WORKFLOW_DISPATCH_MODE=outbox \
        WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal \
        WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true \
        "$INDEX"
      ;;
    dreammaker-minimax)
      capture_command "$__status_var" "$__output_var" env \
        ALLOW_REAL_MODEL_SMOKE=1 \
        TARGET=dreammaker-minimax \
        MODE=execute \
        DREAMMAKER_REAL_CALLS_ENABLED=true \
        DREAMMAKER_ACCESS_KEY=redacted \
        DREAMMAKER_SECRET_KEY=redacted \
        MUSIC_WORKFLOW_DISPATCH_MODE=outbox \
        WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal \
        WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true \
        "$INDEX"
      ;;
    deepseek)
      capture_command "$__status_var" "$__output_var" env \
        ALLOW_REAL_MODEL_SMOKE=1 \
        TARGET=deepseek \
        MODE=execute \
        AGENT_REAL_CALLS_ENABLED=true \
        DEEPSEEK_REAL_CALLS_ENABLED=true \
        DEEPSEEK_API_KEY=redacted \
        "$INDEX"
      ;;
    wellapi-image2)
      capture_command "$__status_var" "$__output_var" env \
        ALLOW_REAL_MODEL_SMOKE=1 \
        TARGET=wellapi-image2 \
        MODE=execute \
        IMAGE_PROVIDER=image2 \
        IMAGE2_BACKEND=wellapi \
        IMAGE_REAL_CALLS_ENABLED=true \
        WELLAPI_API_KEY=redacted \
        "$INDEX"
      ;;
    dreammaker-image2)
      capture_command "$__status_var" "$__output_var" env \
        ALLOW_REAL_MODEL_SMOKE=1 \
        TARGET=dreammaker-image2 \
        MODE=execute \
        IMAGE_PROVIDER=image2 \
        IMAGE2_BACKEND=dreammaker \
        IMAGE_REAL_CALLS_ENABLED=true \
        DREAMMAKER_REAL_CALLS_ENABLED=true \
        DREAMMAKER_ACCESS_KEY=redacted \
        DREAMMAKER_SECRET_KEY=redacted \
        "$INDEX"
      ;;
    *)
      fail_check "unsupported target for target-gate audit: $target"
      ;;
  esac
}

cd "$REPO_ROOT"

need_command rg
need_command sed

if [ "$FAIL_COUNT" -gt 0 ]; then
  log "SUMMARY fail=$FAIL_COUNT pass=$PASS_COUNT"
  exit 1
fi

check_list_mode
check_plan_mode
check_global_execute_gate
check_target_execute_gate

if [ "$FAIL_COUNT" -gt 0 ]; then
  log "SUMMARY fail=$FAIL_COUNT pass=$PASS_COUNT"
  exit 1
fi

log "SUMMARY fail=0 pass=$PASS_COUNT"
