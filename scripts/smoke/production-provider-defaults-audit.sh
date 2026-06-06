#!/usr/bin/env bash
set -euo pipefail
set +x

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PASS_COUNT=0
FAIL_COUNT=0

log() {
  printf '[production-provider-defaults-audit] %s\n' "$*"
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

require_multiline_pattern() {
  local path="$1"
  local pattern="$2"
  local label="$3"
  if rg -U -q "$pattern" "$path"; then
    pass "$label"
  else
    fail_check "$label"
  fi
}

cd "$REPO_ROOT"

require_tool rg

required_files=(
  "apps/music-api/src/main/resources/application.yml"
  "apps/music-worker/src/main/resources/application.yml"
  "apps/music-api/src/main/java/com/yanyun/music/api/config/AdapterConfiguration.java"
  "apps/music-worker/src/main/java/com/yanyun/music/worker/WorkerProductionConfiguration.java"
  "modules/production/src/main/java/com/yanyun/music/production/MediaGenerationConfiguration.java"
  "modules/config-center/src/main/java/com/yanyun/music/configcenter/CompanyIntegrationProperties.java"
  "modules/config-center/src/main/java/com/yanyun/music/configcenter/IntegrationReadinessService.java"
  "deploy/env.production.example"
  "docs/adr/0004-production-provider-targets.md"
  "docs/handover/company-adapter-deployment-handoff-v0.1.md"
  "docs/checklists/company-adapter-replacement-readiness.md"
  "docs/checklists/local-commercial-delivery-acceptance.md"
  "docs/specs/production-dreammaker-provider-defaults-v0.1.md"
)

for file in "${required_files[@]}"; do
  require_file "$file"
done

require_multiline_pattern \
  "apps/music-api/src/main/resources/application.yml" \
  'on-profile: prod \| production[\s\S]*suno-backend: \$\{SUNO_BACKEND:dreammaker\}[\s\S]*image2-backend: \$\{IMAGE2_BACKEND:dreammaker\}[\s\S]*suno:[\s\S]*backend: \$\{SUNO_BACKEND:dreammaker\}[\s\S]*image2:[\s\S]*backend: \$\{IMAGE2_BACKEND:dreammaker\}' \
  "music-api production profile defaults to DreamMaker"

require_multiline_pattern \
  "apps/music-worker/src/main/resources/application.yml" \
  'on-profile: prod \| production[\s\S]*suno:[\s\S]*backend: \$\{SUNO_BACKEND:dreammaker\}[\s\S]*image2:[\s\S]*backend: \$\{IMAGE2_BACKEND:dreammaker\}' \
  "music-worker production profile defaults to DreamMaker"

require_pattern \
  "apps/music-api/src/main/java/com/yanyun/music/api/config/AdapterConfiguration.java" \
  'yanyun\.suno\.backend:dreammaker' \
  "music-api Java fallback uses DreamMaker Suno backend"

require_pattern \
  "apps/music-worker/src/main/java/com/yanyun/music/worker/WorkerProductionConfiguration.java" \
  'yanyun\.suno\.backend:dreammaker' \
  "music-worker Java fallback uses DreamMaker Suno backend"

require_pattern \
  "modules/production/src/main/java/com/yanyun/music/production/MediaGenerationConfiguration.java" \
  'yanyun\.image2\.backend:dreammaker' \
  "media generation Java fallback uses DreamMaker Image2 backend"

require_pattern \
  "modules/config-center/src/main/java/com/yanyun/music/configcenter/CompanyIntegrationProperties.java" \
  'private String sunoBackend = "dreammaker";' \
  "readiness properties default Suno backend to DreamMaker"

require_pattern \
  "modules/config-center/src/main/java/com/yanyun/music/configcenter/CompanyIntegrationProperties.java" \
  'private String image2Backend = "dreammaker";' \
  "readiness properties default Image2 backend to DreamMaker"

require_pattern \
  "modules/config-center/src/main/java/com/yanyun/music/configcenter/IntegrationReadinessService.java" \
  'normalizeOr\(properties\.getSunoBackend\(\), "dreammaker"\)' \
  "readiness service fallback uses DreamMaker for Suno backend"

require_pattern \
  "modules/config-center/src/main/java/com/yanyun/music/configcenter/IntegrationReadinessService.java" \
  'normalizeOr\(properties\.getImage2Backend\(\), "dreammaker"\)' \
  "readiness service fallback uses DreamMaker for Image2 backend"

require_pattern "deploy/env.production.example" '^SPRING_PROFILES_ACTIVE=prod$' "production env example activates prod profile"
require_pattern "deploy/env.production.example" '^SUNO_BACKEND=dreammaker$' "production env example uses DreamMaker Suno"
require_pattern "deploy/env.production.example" '^IMAGE2_BACKEND=dreammaker$' "production env example uses DreamMaker Image2"
require_pattern "deploy/env.production.example" '^DREAMMAKER_ACCESS_KEY=$' "production env example keeps DreamMaker AccessKey empty"
require_pattern "deploy/env.production.example" '^DREAMMAKER_SECRET_KEY=$' "production env example keeps DreamMaker SecretKey empty"
require_pattern "deploy/env.production.example" 'Local `\.env\.example` keeps public-network smoke defaults' "production env example explains local smoke distinction"

require_pattern "docs/adr/0004-production-provider-targets.md" "DreamMaker.*正式生产供应商目标" "ADR keeps DreamMaker production target"
require_pattern "docs/handover/company-adapter-deployment-handoff-v0.1.md" "SPRING_PROFILES_ACTIVE=prod" "company handoff references prod profile"
require_pattern "docs/handover/company-adapter-deployment-handoff-v0.1.md" "deploy/env.production.example" "company handoff references production env example"
require_pattern "docs/checklists/company-adapter-replacement-readiness.md" "SPRING_PROFILES_ACTIVE=prod" "company readiness checklist references prod profile"
require_pattern "docs/checklists/local-commercial-delivery-acceptance.md" "production-provider-defaults-audit\\.sh" "local acceptance checklist references production defaults audit"

if rg -n 'sk-[A-Za-z0-9_-]{12,}|Bearer [A-Za-z0-9._~+/=-]{16,}|AccessKey[[:space:]]*[:：]|SecretKey[[:space:]]*[:：]|BEGIN ((RSA|EC|OPENSSH|DSA) )?PRIVATE KEY|eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}' deploy/env.production.example; then
  fail_check "production env example contains secret-like patterns"
else
  pass "production env example contains no obvious secret-like patterns"
fi

if [[ "$FAIL_COUNT" -gt 0 ]]; then
  log "SUMMARY fail=$FAIL_COUNT pass=$PASS_COUNT"
  exit 1
fi

log "SUMMARY fail=0 pass=$PASS_COUNT"
