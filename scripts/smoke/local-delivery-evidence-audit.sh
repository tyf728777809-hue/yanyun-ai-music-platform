#!/usr/bin/env bash
set -euo pipefail
set +x

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
STRICT_GIT_CLEAN="${STRICT_GIT_CLEAN:-false}"
MAX_TRACKED_FILE_BYTES="${MAX_TRACKED_FILE_BYTES:-20000000}"

PASS_COUNT=0
WARN_COUNT=0
FAIL_COUNT=0

log() {
  printf '[delivery-audit] %s\n' "$*"
}

pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  log "PASS $*"
}

warn() {
  WARN_COUNT=$((WARN_COUNT + 1))
  log "WARN $*"
}

fail_check() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  log "FAIL $*"
}

require_tool() {
  if ! command -v "$1" >/dev/null 2>&1; then
    fail_check "required tool missing: $1"
  fi
}

require_file() {
  local path="$1"
  if [[ -f "$path" ]]; then
    pass "file exists: $path"
  else
    fail_check "missing file: $path"
  fi
}

require_executable() {
  local path="$1"
  if [[ -x "$path" ]]; then
    pass "executable exists: $path"
  else
    fail_check "missing executable bit or file: $path"
  fi
}

require_pattern() {
  local path="$1"
  local pattern="$2"
  local label="$3"
  if rg -q "$pattern" "$path"; then
    pass "$label"
  else
    fail_check "$label"
  fi
}

check_git_status() {
  local status
  status="$(git status --short)"
  if [[ -z "$status" ]]; then
    pass "git worktree clean"
    return
  fi

  if [[ "$STRICT_GIT_CLEAN" == "true" ]]; then
    fail_check "git worktree has uncommitted changes"
  else
    warn "git worktree has uncommitted changes; set STRICT_GIT_CLEAN=true to fail this gate"
  fi
}

check_smoke_index() {
  local list_output
  local plan_output

  if ! list_output="$(scripts/smoke/real-model-controlled-smoke.sh 2>&1)"; then
    fail_check "real-model controlled smoke index list mode failed"
    return
  fi

  if [[ "$list_output" == *"DreamMaker music and DreamMaker Image 2 must remain the production-target paths."* ]]; then
    pass "real-model controlled smoke index lists DreamMaker production-target rule"
  else
    fail_check "real-model controlled smoke index missing DreamMaker production-target rule"
  fi

  if ! plan_output="$(TARGET=dreammaker-suno MODE=plan scripts/smoke/real-model-controlled-smoke.sh 2>&1)"; then
    fail_check "real-model controlled smoke index DreamMaker plan failed"
    return
  fi

  if [[ "$plan_output" == *"production-target path"* && "$plan_output" == *"ALLOW_DREAMMAKER_REAL_SMOKE=1"* ]]; then
    pass "real-model controlled smoke index prints DreamMaker execution gate"
  else
    fail_check "real-model controlled smoke index DreamMaker plan missing production/gate text"
  fi
}

check_real_model_gate_audit() {
  local output
  if output="$(scripts/smoke/real-model-safety-gates-audit.sh 2>&1)"; then
    pass "real-model safety gates audit passes"
    return
  fi

  fail_check "real-model safety gates audit failed"
  printf '%s\n' "$output" | tail -40
}

check_secret_patterns() {
  local matches
  matches="$(
    git grep -nE \
      -e 'sk-[A-Za-z0-9_-]{12,}' \
      -e 'Bearer [A-Za-z0-9._~+/=-]{16,}' \
      -e 'AccessKey[[:space:]]*[:：]' \
      -e 'SecretKey[[:space:]]*[:：]' \
      -e 'BEGIN ((RSA|EC|OPENSSH|DSA) )?PRIVATE KEY' \
      -e 'eyJ[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{20,}' \
      -- . || true
  )"

  if [[ -z "$matches" ]]; then
    pass "tracked files contain no obvious committed secret patterns"
    return
  fi

  fail_check "tracked files contain secret-like patterns at:"
  printf '%s\n' "$matches" | awk -F: '{print "  " $1 ":" $2}' | sort -u
}

check_large_tracked_files() {
  local oversized
  oversized="$(
    git ls-files -z | while IFS= read -r -d '' file; do
      [[ -f "$file" ]] || continue
      size="$(wc -c < "$file" | tr -d ' ')"
      if [[ "$size" -gt "$MAX_TRACKED_FILE_BYTES" ]]; then
        printf '%s:%s\n' "$file" "$size"
      fi
    done
  )"

  if [[ -z "$oversized" ]]; then
    pass "no tracked file exceeds MAX_TRACKED_FILE_BYTES=$MAX_TRACKED_FILE_BYTES"
    return
  fi

  fail_check "tracked files exceed MAX_TRACKED_FILE_BYTES=$MAX_TRACKED_FILE_BYTES"
  printf '%s\n' "$oversized" | sort
}

cd "$REPO_ROOT"

require_tool git
require_tool rg
require_tool awk
require_tool sort
require_tool wc

check_git_status

required_files=(
  "README.md"
  "AGENTS.md"
  "docs/project-progress.md"
  "docs/checklists/local-commercial-delivery-acceptance.md"
  "docs/checklists/local-commercial-delivery-audit-2026-06-06.md"
  "docs/handover/local-commercial-delivery-status-v0.1.md"
  "docs/handover/company-adapter-deployment-handoff-v0.1.md"
  "docs/handover/company-delivery-package-v0.1.md"
  "docs/checklists/company-adapter-replacement-readiness.md"
  "docs/adr/0003-frontend-delivery-track.md"
  "docs/api/openapi-v0.1.yaml"
  "docs/specs/real-model-controlled-smoke-index-v0.1.md"
  "docs/specs/real-model-readiness-preflight-v0.1.md"
  "docs/specs/real-model-safety-gates-audit-v0.1.md"
  "docs/specs/deepseek-real-lyrics-smoke-v0.1.md"
  "docs/specs/dreammaker-image2-real-cover-stack-smoke-v0.1.md"
  "docs/specs/local-delivery-evidence-audit-v0.1.md"
  "docs/specs/company-handoff-package-index-v0.1.md"
)

for file in "${required_files[@]}"; do
  require_file "$file"
done

required_executables=(
  "scripts/smoke/api-main-flow.sh"
  "scripts/smoke/openapi-contract.sh"
  "scripts/smoke/company-adapter-readiness-smoke.sh"
  "scripts/smoke/real-model-readiness-preflight.sh"
  "scripts/smoke/real-model-controlled-smoke.sh"
  "scripts/smoke/real-model-safety-gates-audit.sh"
  "scripts/smoke/deepseek-real-lyrics-smoke.sh"
  "scripts/smoke/dreammaker-image2-real-cover-smoke.sh"
  "scripts/smoke/dreammaker-image2-real-cover-stack-smoke.sh"
  "scripts/smoke/local-delivery-evidence-audit.sh"
  "scripts/smoke/company-handoff-package-audit.sh"
  "scripts/smoke/dreammaker-real-music-stack-smoke.sh"
  "scripts/smoke/yunwu-suno-real-music-stack-smoke.sh"
  "scripts/smoke/wellapi-image2-real-cover-stack-smoke.sh"
)

for file in "${required_executables[@]}"; do
  require_executable "$file"
done

require_pattern "AGENTS.md" "DreamMaker.*正式生产目标" "AGENTS keeps DreamMaker production-target rule"
require_pattern "README.md" "real-model-controlled-smoke\\.sh" "README references real-model controlled smoke index"
require_pattern "README.md" "real-model-safety-gates-audit\\.sh" "README references real-model safety gates audit"
require_pattern "README.md" "deepseek-real-lyrics-smoke\\.sh" "README references DeepSeek real lyrics smoke"
require_pattern "README.md" "dreammaker-image2-real-cover-stack-smoke\\.sh|ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1" "README references DreamMaker Image2 smoke"
require_pattern "README.md" "Yunwu.*公网联调" "README labels Yunwu as public-network integration"
require_pattern "README.md" "WellAPI.*公网联调" "README labels WellAPI as public-network integration"
require_pattern "README.md" "company-delivery-package-v0\\.1\\.md" "README references company handoff package"
require_pattern "docs/handover/local-commercial-delivery-status-v0.1.md" "READY_LOCAL" "status handoff includes READY_LOCAL"
require_pattern "docs/handover/local-commercial-delivery-status-v0.1.md" "PREPARED_SMOKE" "status handoff includes PREPARED_SMOKE"
require_pattern "docs/handover/local-commercial-delivery-status-v0.1.md" "PREPARED_HANDOFF" "status handoff includes PREPARED_HANDOFF"
require_pattern "docs/handover/local-commercial-delivery-status-v0.1.md" "BLOCKED_EXTERNAL" "status handoff includes BLOCKED_EXTERNAL"
require_pattern "docs/handover/local-commercial-delivery-status-v0.1.md" "DECISION_REQUIRED" "status handoff includes DECISION_REQUIRED"
require_pattern "docs/checklists/local-commercial-delivery-acceptance.md" "MODE=preflight" "acceptance checklist requires real-model preflight"
require_pattern "docs/checklists/local-commercial-delivery-acceptance.md" "ALLOW_REAL_MODEL_SMOKE=1" "acceptance checklist requires global real-smoke allow gate"

check_smoke_index
check_real_model_gate_audit
check_secret_patterns
check_large_tracked_files

if [[ "$FAIL_COUNT" -gt 0 ]]; then
  log "SUMMARY fail=$FAIL_COUNT warn=$WARN_COUNT pass=$PASS_COUNT"
  exit 1
fi

log "SUMMARY fail=0 warn=$WARN_COUNT pass=$PASS_COUNT"
