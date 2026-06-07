#!/usr/bin/env bash
set -euo pipefail
set +x

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PACKAGE="${COMPANY_HANDOFF_PACKAGE:-docs/handover/company-delivery-package-v0.1.md}"
PASS_COUNT=0
FAIL_COUNT=0

log() {
  printf '[company-handoff-audit] %s\n' "$*"
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

require_executable() {
  local path="$1"
  [[ -x "$path" ]] && pass "executable exists: $path" || fail_check "missing executable: $path"
}

require_package_pattern() {
  local pattern="$1"
  local label="$2"
  if rg -q "$pattern" "$PACKAGE"; then
    pass "$label"
  else
    fail_check "$label"
  fi
}

cd "$REPO_ROOT"

require_tool rg
require_tool git

required_files=(
  "$PACKAGE"
  "docs/handover/local-commercial-delivery-status-v0.1.md"
  "docs/checklists/local-commercial-delivery-acceptance.md"
  "docs/handover/company-adapter-deployment-handoff-v0.1.md"
  "docs/checklists/company-adapter-replacement-readiness.md"
  "docs/api/openapi-v0.1.yaml"
  "docs/runbook/local-development.md"
  "docs/adr/0003-frontend-delivery-track.md"
  "docs/adr/0004-production-provider-targets.md"
  "docs/specs/local-commercial-backend-acceptance-stack-smoke-v0.1.md"
  "docs/specs/local-commercial-full-acceptance-stack-smoke-v0.1.md"
  "docs/specs/company-handoff-package-index-v0.1.md"
  "docs/specs/production-dreammaker-provider-defaults-v0.1.md"
  "docs/specs/real-model-smoke-evidence-log-v0.1.md"
  "docs/specs/deepseek-real-lyrics-stack-smoke-v0.1.md"
  "docs/integrations/real-model-smoke-evidence-log.md"
  "docs/specs/company-deployment-readiness-audit-v0.1.md"
  "docs/handover/stepwise-production-implementation-task-package-v0.1.md"
  "docs/specs/stepwise-temporal-production-state-advancement-v0.1.md"
  "deploy/env.production.example"
)

for file in "${required_files[@]}"; do
  require_file "$file"
done

required_executables=(
  "scripts/smoke/local-delivery-evidence-audit.sh"
  "scripts/smoke/production-provider-defaults-audit.sh"
  "scripts/smoke/company-deployment-readiness-audit.sh"
  "scripts/smoke/company-adapter-readiness-smoke.sh"
  "scripts/smoke/openapi-contract.sh"
  "scripts/smoke/api-main-flow.sh"
  "scripts/smoke/api-package-blocked-flow.sh"
  "scripts/smoke/local-commercial-backend-acceptance-stack.sh"
  "scripts/smoke/local-commercial-full-acceptance-stack.sh"
  "scripts/smoke/real-model-controlled-smoke.sh"
  "scripts/smoke/real-model-safety-gates-audit.sh"
  "scripts/smoke/real-model-evidence-log-audit.sh"
  "scripts/smoke/deepseek-real-lyrics-smoke.sh"
  "scripts/smoke/deepseek-real-lyrics-stack-smoke.sh"
  "scripts/smoke/dreammaker-image2-real-cover-stack-smoke.sh"
  "scripts/smoke/stepwise-production-boundary-audit.sh"
)

for file in "${required_executables[@]}"; do
  require_executable "$file"
done

require_package_pattern 'READY_LOCAL' 'package explains READY_LOCAL status'
require_package_pattern 'PREPARED_SMOKE' 'package explains PREPARED_SMOKE status'
require_package_pattern 'PREPARED_HANDOFF' 'package explains PREPARED_HANDOFF status'
require_package_pattern 'BLOCKED_EXTERNAL' 'package explains BLOCKED_EXTERNAL status'
require_package_pattern 'DECISION_REQUIRED' 'package explains DECISION_REQUIRED status'
require_package_pattern 'X-Mock-User-Id' 'package warns about X-Mock-User-Id'
require_package_pattern 'mark-fetched' 'package explains mark-fetched semantics'
require_package_pattern 'DreamMaker.*正式生产目标|DreamMaker.*production-target' 'package keeps DreamMaker production-target rule'
require_package_pattern 'Yunwu.*公网|Yunwu.*public-network' 'package labels Yunwu as public-network smoke path'
require_package_pattern 'WellAPI.*公网|WellAPI.*public-network' 'package labels WellAPI as public-network smoke path'
require_package_pattern 'company-adapter-readiness-smoke\.sh' 'package references company readiness smoke'
require_package_pattern 'local-delivery-evidence-audit\.sh' 'package references local delivery evidence audit'
require_package_pattern 'local-commercial-backend-acceptance-stack\.sh' 'package references backend acceptance stack smoke'
require_package_pattern 'local-commercial-full-acceptance-stack\.sh' 'package references full acceptance stack smoke'
require_package_pattern 'production-provider-defaults-audit\.sh' 'package references production provider defaults audit'
require_package_pattern 'real-model-evidence-log-audit\.sh' 'package references real-model evidence log audit'
require_package_pattern 'real-model-smoke-evidence-log\.md' 'package references real-model evidence log'
require_package_pattern 'company-deployment-readiness-audit\.sh' 'package references company deployment readiness audit'
require_package_pattern 'stepwise-production-boundary-audit\.sh' 'package references stepwise production boundary audit'
require_package_pattern 'stepwise-production-implementation-task-package-v0\.1\.md' 'package references stepwise production task package'
require_package_pattern 'deploy/env\.production\.example' 'package references production env example'
require_package_pattern 'SPRING_PROFILES_ACTIVE=prod' 'package references production Spring profile'
require_package_pattern 'api-package-blocked-flow\.sh' 'package references package block smoke'
require_package_pattern 'real-model-controlled-smoke\.sh' 'package references real-model controlled smoke index'
require_package_pattern 'real-model-safety-gates-audit\.sh' 'package references real-model safety gates audit'
require_package_pattern 'deepseek-real-lyrics-stack-smoke\.sh|TARGET=deepseek MODE=execute' 'package references DeepSeek stack smoke'
require_package_pattern 'deepseek-real-lyrics-smoke\.sh' 'package references DeepSeek low-level smoke'
require_package_pattern 'dreammaker-minimax' 'package references DreamMaker MiniMax production target'
require_package_pattern 'dreammaker-image2-real-cover-stack-smoke\.sh|TARGET=dreammaker-image2 MODE=execute' 'package references DreamMaker Image2 single-work smoke'
require_package_pattern '0004-production-provider-targets' 'package references production provider ADR'
require_package_pattern '公司系统负责|公司社区系统' 'package states company-system responsibility'
require_package_pattern '不调用真实模型|不访问真实公司系统|不调用真实供应商' 'package includes no-real-call safety language'
require_package_pattern 'prototypes/Claude-web-v1' 'package references current frontend acceptance target'
require_package_pattern 'apps/web.*scaffold' 'package states apps/web remains scaffold'
require_package_pattern 'ALLOW_REAL_MODEL_SMOKE=1' 'package requires global real-model smoke gate'
require_package_pattern 'stepwise-recording' 'package explains stepwise-recording boundary'
require_package_pattern 'stepwise-production' 'package explains stepwise-production boundary'
require_package_pattern 'TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy' 'package pins production Temporal workflow mode to legacy'

if rg -n 'sk-[A-Za-z0-9_-]{12,}|Bearer [A-Za-z0-9._~+/=-]{16,}|AccessKey[[:space:]]*[:：]|SecretKey[[:space:]]*[:：]|BEGIN ((RSA|EC|OPENSSH|DSA) )?PRIVATE KEY|eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}' "$PACKAGE"; then
  fail_check "package contains secret-like patterns"
else
  pass "package contains no obvious secret-like patterns"
fi

if [[ "$FAIL_COUNT" -gt 0 ]]; then
  log "SUMMARY fail=$FAIL_COUNT pass=$PASS_COUNT"
  exit 1
fi

log "SUMMARY fail=0 pass=$PASS_COUNT"
