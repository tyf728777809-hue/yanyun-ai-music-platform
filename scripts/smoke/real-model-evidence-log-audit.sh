#!/usr/bin/env bash
set -euo pipefail
set +x

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
EVIDENCE_LOG="docs/integrations/real-model-smoke-evidence-log.md"
PASS_COUNT=0
FAIL_COUNT=0

log() {
  printf '[real-model-evidence-audit] %s\n' "$*"
}

pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  log "PASS $*"
}

fail_check() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  log "FAIL $*"
}

require_tool() {
  command -v "$1" >/dev/null 2>&1 || fail_check "required tool missing: $1"
}

require_file() {
  local path="$1"
  [[ -f "$path" ]] && pass "file exists: $path" || fail_check "missing file: $path"
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

check_secret_patterns() {
  local matches
  matches="$(
    rg -n \
      -e 'sk-[A-Za-z0-9_-]{12,}' \
      -e 'Bearer [A-Za-z0-9._~+/=-]{16,}' \
      -e 'AccessKey[[:space:]]*[:：]' \
      -e 'SecretKey[[:space:]]*[:：]' \
      -e 'BEGIN ((RSA|EC|OPENSSH|DSA) )?PRIVATE KEY' \
      -e 'eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}' \
      "$EVIDENCE_LOG" \
      docs/integrations/dreammaker-open-questions-tracker.md \
      docs/integrations/deepseek-open-questions-tracker.md \
      docs/integrations/image2-open-questions-tracker.md || true
  )"

  if [[ -z "$matches" ]]; then
    pass "evidence and integration trackers contain no obvious secret-like patterns"
    return
  fi

  fail_check "evidence or tracker files contain secret-like patterns at:"
  printf '%s\n' "$matches" | awk -F: '{print "  " $1 ":" $2}' | sort -u
}

check_real_smoke_script_redaction() {
  local script_files=(
    "scripts/smoke/dreammaker-real-music-smoke.sh"
    "scripts/smoke/yunwu-suno-real-music-smoke.sh"
    "scripts/smoke/wellapi-image2-real-cover-smoke.sh"
    "scripts/smoke/dreammaker-image2-real-cover-smoke.sh"
    "scripts/smoke/public-real-full-experience-stack.sh"
    "scripts/smoke/deepseek-real-lyrics-stack-smoke.sh"
    "scripts/smoke/yunwu-suno-timestamped-lyrics-smoke.sh"
  )
  local raw_media
  local raw_package
  local raw_trace

  raw_media="$(rg -n "jq '\\{work_id, status, generation_stage, package_status, failure, media_assets\\}'" "${script_files[@]}" || true)"
  if [[ -z "$raw_media" ]]; then
    pass "real smoke scripts do not print raw media_assets blocks"
  else
    fail_check "real smoke scripts print raw media_assets at:"
    printf '%s\n' "$raw_media" | awk -F: '{print "  " $1 ":" $2}' | sort -u
  fi

  raw_package="$(rg -n "jq '\\{work_id, package_status, package_url|jq '\\{work_id, package_status, expires_at, package_json:" "${script_files[@]}" || true)"
  if [[ -z "$raw_package" ]]; then
    pass "real smoke scripts do not print raw package URL blocks"
  else
    fail_check "real smoke scripts print raw package URL blocks at:"
    printf '%s\n' "$raw_package" | awk -F: '{print "  " $1 ":" $2}' | sort -u
  fi

  raw_trace="$(rg -n "select provider, model_name, provider_trace_id" scripts/smoke/dreammaker-real-music-smoke.sh scripts/smoke/yunwu-suno-real-music-smoke.sh scripts/smoke/public-real-full-experience-stack.sh docs/checklists/dreammaker-real-music-smoke-10min.md docs/runbook/dreammaker-controlled-real-integration.md || true)"
  if [[ -z "$raw_trace" ]]; then
    pass "real smoke script/runbook provider_trace_id summaries are masked"
  else
    fail_check "provider_trace_id summaries are unmasked at:"
    printf '%s\n' "$raw_trace" | awk -F: '{print "  " $1 ":" $2}' | sort -u
  fi
}

cd "$REPO_ROOT"

require_tool rg
require_tool awk
require_tool sort

required_files=(
  "docs/specs/real-model-smoke-evidence-log-v0.1.md"
  "docs/specs/public-real-full-experience-smoke-v0.1.md"
  "docs/specs/deepseek-real-lyrics-stack-smoke-v0.1.md"
  "$EVIDENCE_LOG"
  "docs/checklists/local-commercial-delivery-acceptance.md"
  "docs/checklists/dreammaker-real-integration-acceptance.md"
  "docs/checklists/deepseek-real-integration-acceptance.md"
  "docs/checklists/image2-real-integration-acceptance.md"
  "docs/runbook/dreammaker-controlled-real-integration.md"
  "docs/runbook/deepseek-controlled-real-integration.md"
  "docs/runbook/image2-controlled-real-integration.md"
  "docs/runbook/yunwu-suno-controlled-real-integration.md"
  "docs/handover/local-commercial-delivery-status-v0.1.md"
  "docs/handover/company-delivery-package-v0.1.md"
  "docs/project-progress.md"
  "scripts/smoke/yunwu-suno-timestamped-lyrics-smoke.sh"
)

for file in "${required_files[@]}"; do
  require_file "$file"
done

for target in dreammaker-suno dreammaker-minimax yunwu-suno yunwu-suno-timestamped-lyrics deepseek wellapi-image2 dreammaker-image2 public-real-full-experience; do
  require_pattern "$EVIDENCE_LOG" "$target" "evidence log covers target $target"
done

require_pattern "$EVIDENCE_LOG" "Sanitized only|脱敏" "evidence log states sanitized-only policy"
require_pattern "$EVIDENCE_LOG" "DreamMaker.*production-target|DreamMaker.*生产目标" "evidence log keeps DreamMaker production-target rule"
require_pattern "$EVIDENCE_LOG" "Yunwu.*public-network|Yunwu.*公网" "evidence log labels Yunwu as public-network smoke"
require_pattern "$EVIDENCE_LOG" "WellAPI.*public-network|WellAPI.*公网" "evidence log labels WellAPI as public-network smoke"
require_pattern "$EVIDENCE_LOG" "timestamped lyrics|时间轴歌词" "evidence log covers timestamped lyrics evidence fields"
require_pattern "$EVIDENCE_LOG" "HTTP 403" "evidence log records sanitized DreamMaker 403 sample"
require_pattern "$EVIDENCE_LOG" "<present>|<empty>|N/A" "evidence log uses redacted trace markers"
require_pattern "docs/checklists/local-commercial-delivery-acceptance.md" "$EVIDENCE_LOG" "delivery checklist references unified evidence log"
require_pattern "docs/checklists/dreammaker-real-integration-acceptance.md" "$EVIDENCE_LOG" "DreamMaker checklist references unified evidence log"
require_pattern "docs/checklists/deepseek-real-integration-acceptance.md" "$EVIDENCE_LOG" "DeepSeek checklist references unified evidence log"
require_pattern "docs/checklists/image2-real-integration-acceptance.md" "$EVIDENCE_LOG" "Image2 checklist references unified evidence log"
require_pattern "docs/runbook/dreammaker-controlled-real-integration.md" "$EVIDENCE_LOG" "DreamMaker runbook references unified evidence log"
require_pattern "docs/runbook/deepseek-controlled-real-integration.md" "$EVIDENCE_LOG" "DeepSeek runbook references unified evidence log"
require_pattern "docs/runbook/image2-controlled-real-integration.md" "$EVIDENCE_LOG" "Image2 runbook references unified evidence log"
require_pattern "docs/runbook/yunwu-suno-controlled-real-integration.md" "$EVIDENCE_LOG" "Yunwu runbook references unified evidence log"
require_pattern "docs/handover/local-commercial-delivery-status-v0.1.md" "$EVIDENCE_LOG" "status handoff references unified evidence log"
require_pattern "docs/handover/company-delivery-package-v0.1.md" "$EVIDENCE_LOG" "company package references unified evidence log"
require_pattern "docs/checklists/local-commercial-delivery-acceptance.md" "public-real-full-experience-stack\\.sh" "delivery checklist references public full experience smoke"
require_pattern "docs/handover/local-commercial-delivery-status-v0.1.md" "public-real-full-experience-stack\\.sh" "status handoff references public full experience smoke"
require_pattern "docs/specs/public-real-full-experience-smoke-v0.1.md" "DreamMaker.*production|DreamMaker.*生产" "public full experience spec keeps DreamMaker production rule"

check_secret_patterns
check_real_smoke_script_redaction

if [[ "$FAIL_COUNT" -gt 0 ]]; then
  log "SUMMARY fail=$FAIL_COUNT pass=$PASS_COUNT"
  exit 1
fi

log "SUMMARY fail=0 pass=$PASS_COUNT"
