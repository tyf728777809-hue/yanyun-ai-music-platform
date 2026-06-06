#!/usr/bin/env bash
set -euo pipefail
set +x

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PASS_COUNT=0
FAIL_COUNT=0

log() {
  printf '[stepwise-boundary-audit] %s\n' "$*"
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

require_absent_pattern() {
  local path="$1"
  local pattern="$2"
  local label="$3"
  if rg -q "$pattern" "$path"; then
    fail_check "$label"
    rg -n "$pattern" "$path" | sed 's/^/  /'
  else
    pass "$label"
  fi
}

check_worker_mode_boundary() {
  local properties_file="apps/music-worker/src/main/java/com/yanyun/music/worker/TemporalWorkerProperties.java"

  require_pattern "$properties_file" "WORKFLOW_MODE_LEGACY" "worker still supports legacy mode"
  require_pattern "$properties_file" "WORKFLOW_MODE_STEPWISE_RECORDING" "worker still supports stepwise-recording mode"

  if rg -q "WORKFLOW_MODE_STEPWISE_PRODUCTION|stepwise-production" "$properties_file"; then
    pass "worker includes stepwise-production mode"
    require_file "modules/production/src/main/java/com/yanyun/music/production/ProductionSongProductionStepActivities.java"
    require_executable "scripts/smoke/temporal-stepwise-production.sh"
    require_pattern "docs/handover/local-commercial-delivery-status-v0.1.md" "Stepwise production.*READY_LOCAL|stepwise-production.*READY_LOCAL" "implemented stepwise-production must be statused as READY_LOCAL only with smoke evidence"
  else
    pass "worker does not expose stepwise-production before production activities exist"
    require_absent_pattern \
      "docs/handover/local-commercial-delivery-status-v0.1.md" \
      '\| Stepwise production[^|]*\|[[:space:]]*`READY_LOCAL`|\| .*stepwise-production[^|]*\|[[:space:]]*`READY_LOCAL`' \
      "status page must not mark missing stepwise-production as READY_LOCAL"
  fi
}

cd "$REPO_ROOT"

require_tool rg
require_tool sed

required_files=(
  "docs/specs/stepwise-temporal-production-state-advancement-v0.1.md"
  "docs/handover/stepwise-production-implementation-task-package-v0.1.md"
  "docs/handover/local-commercial-delivery-status-v0.1.md"
  "docs/handover/company-delivery-package-v0.1.md"
  "docs/handover/company-adapter-deployment-handoff-v0.1.md"
  "docs/checklists/local-commercial-delivery-acceptance.md"
  "docs/adr/0004-production-provider-targets.md"
  "docs/api/openapi-v0.1.yaml"
  "deploy/env.production.example"
  "README.md"
  "docs/project-progress.md"
  "apps/music-worker/src/main/java/com/yanyun/music/worker/TemporalWorkerProperties.java"
  "modules/production/src/main/java/com/yanyun/music/production/RecordingSongProductionStepActivities.java"
)

for file in "${required_files[@]}"; do
  require_file "$file"
done

require_pattern \
  "docs/specs/stepwise-temporal-production-state-advancement-v0.1.md" \
  "stepwise-recording.*只作为录步|stepwise-recording.*录步模式" \
  "stepwise spec keeps recording mode scoped to audit"
require_pattern \
  "docs/specs/stepwise-temporal-production-state-advancement-v0.1.md" \
  "stepwise-recording.*不得把作品推进到.*GENERATED.*/.*PACKAGE_READY|只写入完整 step audit" \
  "stepwise spec says recording must not produce package-ready state"
require_pattern \
  "docs/specs/stepwise-temporal-production-state-advancement-v0.1.md" \
  "stepwise-production.*分步生产态推进" \
  "stepwise spec defines production mode separately"

require_pattern \
  "docs/handover/stepwise-production-implementation-task-package-v0.1.md" \
  "stepwise-recording.*不得作为生产链路" \
  "task package forbids using recording as production chain"
require_pattern \
  "docs/handover/stepwise-production-implementation-task-package-v0.1.md" \
  "stepwise-production.*not implemented|stepwise-production.*未实现" \
  "task package marks stepwise-production as not implemented"
require_pattern \
  "docs/handover/stepwise-production-implementation-task-package-v0.1.md" \
  "ProductionSongProductionStepActivities" \
  "task package names production step activity requirement"
require_pattern \
  "docs/handover/stepwise-production-implementation-task-package-v0.1.md" \
  "temporal-stepwise-production\\.sh" \
  "task package names dedicated stepwise-production smoke"
require_pattern \
  "docs/handover/stepwise-production-implementation-task-package-v0.1.md" \
  "DreamMaker.*正式生产目标" \
  "task package preserves DreamMaker production-target rule"
require_pattern \
  "docs/handover/stepwise-production-implementation-task-package-v0.1.md" \
  "Yunwu.*公网受控 smoke|WellAPI.*公网受控 smoke" \
  "task package labels Yunwu/WellAPI as public-network smoke"

require_pattern \
  "docs/handover/local-commercial-delivery-status-v0.1.md" \
  "stepwise-recording" \
  "status handoff mentions stepwise-recording boundary"
require_pattern \
  "docs/handover/local-commercial-delivery-status-v0.1.md" \
  "stepwise-production" \
  "status handoff mentions stepwise-production state"
require_pattern \
  "docs/handover/local-commercial-delivery-status-v0.1.md" \
  "Stepwise production.*PREPARED_HANDOFF|PREPARED_HANDOFF.*stepwise-production|stepwise-production.*NOT_STARTED|stepwise-production.*PREPARED_HANDOFF" \
  "status handoff does not overstate stepwise-production"

require_pattern \
  "docs/handover/company-delivery-package-v0.1.md" \
  "stepwise-production-implementation-task-package-v0\\.1\\.md" \
  "company handoff references stepwise production task package"
require_pattern \
  "docs/handover/company-delivery-package-v0.1.md" \
  "stepwise-production-boundary-audit\\.sh" \
  "company handoff references stepwise boundary audit"
require_pattern \
  "docs/handover/company-delivery-package-v0.1.md" \
  "TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy" \
  "company handoff pins production Temporal workflow mode to legacy"
require_pattern \
  "docs/checklists/local-commercial-delivery-acceptance.md" \
  "stepwise-production-boundary-audit\\.sh" \
  "acceptance checklist references stepwise boundary audit"
require_pattern \
  "README.md" \
  "stepwise-production-boundary-audit\\.sh" \
  "README references stepwise boundary audit"
require_pattern \
  "README.md" \
  "TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy" \
  "README pins production/user-test Temporal mode to legacy"
require_pattern \
  "deploy/env.production.example" \
  "^TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy$" \
  "production env example pins Temporal mode to legacy"
require_pattern \
  "docs/handover/company-adapter-deployment-handoff-v0.1.md" \
  "TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy" \
  "deployment handoff pins Temporal mode to legacy"
require_pattern \
  "docs/handover/company-adapter-deployment-handoff-v0.1.md" \
  "stepwise-recording.*不得用于用户实测|stepwise-recording.*不得用于.*生产发布包链路" \
  "deployment handoff forbids stepwise-recording production use"
require_pattern \
  "docs/api/openapi-v0.1.yaml" \
  "legacy.*stepwise-production.*stepwise-recording.*不会生成发布包|stepwise-recording.*不会生成发布包" \
  "OpenAPI outbox text distinguishes legacy/stepwise-production from recording"
require_pattern \
  "docs/project-progress.md" \
  "stepwise-production" \
  "project progress mentions stepwise-production boundary"

require_pattern \
  "docs/adr/0004-production-provider-targets.md" \
  "DreamMaker.*正式生产" \
  "provider ADR keeps DreamMaker production target"
require_pattern \
  "docs/adr/0004-production-provider-targets.md" \
  "Yunwu.*公网受控" \
  "provider ADR keeps Yunwu public-network boundary"
require_pattern \
  "docs/adr/0004-production-provider-targets.md" \
  "WellAPI.*公网受控" \
  "provider ADR keeps WellAPI public-network boundary"

require_absent_pattern \
  "docs/handover/company-delivery-package-v0.1.md" \
  "stepwise-recording.*(可作为|可用于).*(用户实测主路径|可交付生产链路|生产发布包链路)" \
  "company handoff does not claim recording is production-ready"

check_worker_mode_boundary

if [[ "$FAIL_COUNT" -gt 0 ]]; then
  log "SUMMARY fail=$FAIL_COUNT pass=$PASS_COUNT"
  exit 1
fi

log "SUMMARY fail=0 pass=$PASS_COUNT"
