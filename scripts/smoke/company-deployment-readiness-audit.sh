#!/usr/bin/env bash
set -euo pipefail
set +x

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PASS_COUNT=0
FAIL_COUNT=0

log() {
  printf '[company-deployment-audit] %s\n' "$*"
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
      "${DEPLOYMENT_AUDIT_FILES[@]}" || true
  )"

  if [[ -z "$matches" ]]; then
    pass "deployment handoff files contain no obvious secret-like patterns"
    return
  fi

  fail_check "deployment handoff files contain secret-like patterns at:"
  printf '%s\n' "$matches" | awk -F: '{print "  " $1 ":" $2}' | sort -u
}

cd "$REPO_ROOT"

require_tool rg
require_tool awk
require_tool sort

DEPLOYMENT_AUDIT_FILES=(
  "deploy/docker-compose.yml"
  "deploy/docker/music-api.Dockerfile"
  "deploy/docker/music-worker.Dockerfile"
  "deploy/docker/render-worker.Dockerfile"
  "deploy/docker/web.Dockerfile"
  "deploy/prometheus.yml"
  "deploy/env.production.example"
  "docs/handover/company-adapter-deployment-handoff-v0.1.md"
  "docs/handover/company-delivery-package-v0.1.md"
  "docs/handover/stepwise-production-implementation-task-package-v0.1.md"
  "docs/checklists/company-adapter-replacement-readiness.md"
  "docs/checklists/local-commercial-delivery-acceptance.md"
  "docs/specs/company-deployment-readiness-audit-v0.1.md"
  "scripts/smoke/stepwise-production-boundary-audit.sh"
)

for file in "${DEPLOYMENT_AUDIT_FILES[@]}"; do
  require_file "$file"
done

for service in postgres redis temporal minio opensearch prometheus grafana; do
  require_pattern "deploy/docker-compose.yml" "^[[:space:]]{2}${service}:" "docker compose includes ${service} service"
done

require_pattern "deploy/docker-compose.yml" "name: yanyun-ai-music-local" "docker compose is labeled as local infrastructure"
require_pattern "deploy/docker-compose.yml" "healthcheck:" "docker compose services include healthchecks"
require_pattern "deploy/docker-compose.yml" "volumes:" "docker compose declares persistent volumes"

require_pattern "deploy/docker/music-api.Dockerfile" "eclipse-temurin:21-jdk" "music-api Dockerfile builds with Java 21 JDK"
require_pattern "deploy/docker/music-api.Dockerfile" ":apps:music-api:bootJar" "music-api Dockerfile builds bootJar"
require_pattern "deploy/docker/music-api.Dockerfile" "USER appuser" "music-api Dockerfile runs as appuser"
require_pattern "deploy/docker/music-api.Dockerfile" "EXPOSE 8080" "music-api Dockerfile exposes 8080"
require_pattern "deploy/docker/music-api.Dockerfile" "/actuator/health" "music-api Dockerfile has actuator healthcheck"

require_pattern "deploy/docker/music-worker.Dockerfile" "eclipse-temurin:21-jdk" "music-worker Dockerfile builds with Java 21 JDK"
require_pattern "deploy/docker/music-worker.Dockerfile" ":apps:music-worker:bootJar" "music-worker Dockerfile builds bootJar"
require_pattern "deploy/docker/music-worker.Dockerfile" "USER appuser" "music-worker Dockerfile runs as appuser"
require_pattern "deploy/docker/music-worker.Dockerfile" "EXPOSE 8081" "music-worker Dockerfile exposes 8081"
require_pattern "deploy/docker/music-worker.Dockerfile" "/actuator/health" "music-worker Dockerfile has actuator healthcheck"

require_pattern "deploy/docker/render-worker.Dockerfile" "node:22" "render-worker Dockerfile uses Node 22"
require_pattern "deploy/docker/render-worker.Dockerfile" "npm ci" "render-worker Dockerfile installs locked dependencies"
require_pattern "deploy/docker/render-worker.Dockerfile" "npm run build" "render-worker Dockerfile builds TypeScript"
require_pattern "deploy/docker/render-worker.Dockerfile" "EXPOSE 3001" "render-worker Dockerfile exposes 3001"

require_pattern "deploy/docker/web.Dockerfile" "node:22" "web Dockerfile builds with Node 22"
require_pattern "deploy/docker/web.Dockerfile" "nginx:" "web Dockerfile uses nginx runtime"
require_pattern "deploy/docker/web.Dockerfile" "npm run build" "web Dockerfile builds frontend"
require_pattern "deploy/docker/web.Dockerfile" "HEALTHCHECK" "web Dockerfile has healthcheck"

require_pattern "deploy/prometheus.yml" "job_name: music-api-local" "Prometheus config scrapes music-api"
require_pattern "deploy/prometheus.yml" "job_name: music-worker-local" "Prometheus config scrapes music-worker"
require_pattern "deploy/prometheus.yml" "/actuator/prometheus" "Prometheus config uses actuator prometheus endpoint"

require_pattern "deploy/env.production.example" "^SPRING_PROFILES_ACTIVE=prod$" "production env example activates prod profile"
require_pattern "deploy/env.production.example" "^SUNO_BACKEND=dreammaker$" "production env example uses DreamMaker Suno backend"
require_pattern "deploy/env.production.example" "^IMAGE2_BACKEND=dreammaker$" "production env example uses DreamMaker Image2 backend"
require_pattern "deploy/env.production.example" "^TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy$" "production env example pins Temporal workflow mode to legacy"
require_pattern "deploy/env.production.example" "^DREAMMAKER_ACCESS_KEY=$" "production env example keeps DreamMaker AccessKey empty"
require_pattern "deploy/env.production.example" "^DREAMMAKER_SECRET_KEY=$" "production env example keeps DreamMaker SecretKey empty"

require_pattern "docs/handover/company-adapter-deployment-handoff-v0.1.md" "company-deployment-readiness-audit\\.sh" "Adapter deployment handoff references deployment audit"
require_pattern "docs/handover/company-adapter-deployment-handoff-v0.1.md" "deploy/docker/music-api\\.Dockerfile" "Adapter deployment handoff references music-api Dockerfile"
require_pattern "docs/handover/company-adapter-deployment-handoff-v0.1.md" "deploy/docker/music-worker\\.Dockerfile" "Adapter deployment handoff references music-worker Dockerfile"
require_pattern "docs/handover/company-adapter-deployment-handoff-v0.1.md" "deploy/docker/render-worker\\.Dockerfile" "Adapter deployment handoff references render-worker Dockerfile"
require_pattern "docs/handover/company-adapter-deployment-handoff-v0.1.md" "deploy/docker/web\\.Dockerfile" "Adapter deployment handoff references web Dockerfile"
require_pattern "docs/handover/company-adapter-deployment-handoff-v0.1.md" "deploy/docker-compose.yml.*本地基础设施|本地基础设施.*deploy/docker-compose.yml" "Adapter deployment handoff labels docker compose as local infrastructure"
require_pattern "docs/handover/company-adapter-deployment-handoff-v0.1.md" "DECISION_REQUIRED|生产部署形态.*确认" "Adapter deployment handoff keeps production topology decision explicit"
require_pattern "docs/handover/company-adapter-deployment-handoff-v0.1.md" "TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy" "Adapter deployment handoff pins production Temporal workflow mode to legacy"
require_pattern "docs/handover/company-adapter-deployment-handoff-v0.1.md" "stepwise-recording.*不得用于用户实测|stepwise-recording.*不得用于.*生产发布包链路" "Adapter deployment handoff forbids stepwise-recording for production handoff"
require_pattern "docs/handover/company-adapter-deployment-handoff-v0.1.md" "DreamMaker.*正式生产|正式生产.*DreamMaker" "Adapter deployment handoff keeps DreamMaker as production target"
require_pattern "docs/handover/company-adapter-deployment-handoff-v0.1.md" "Yunwu.*/.*WellAPI.*公网|Yunwu.*公网.*WellAPI" "Adapter deployment handoff labels Yunwu/WellAPI as public-network smoke"
require_pattern "docs/handover/company-delivery-package-v0.1.md" "company-deployment-readiness-audit\\.sh" "company package references deployment audit"
require_pattern "docs/handover/company-delivery-package-v0.1.md" "stepwise-production-boundary-audit\\.sh" "company package references stepwise boundary audit"
require_pattern "docs/checklists/company-adapter-replacement-readiness.md" "company-deployment-readiness-audit\\.sh" "company readiness checklist references deployment audit"
require_pattern "docs/checklists/local-commercial-delivery-acceptance.md" "company-deployment-readiness-audit\\.sh" "local acceptance checklist references deployment audit"

check_secret_patterns

if [[ "$FAIL_COUNT" -gt 0 ]]; then
  log "SUMMARY fail=$FAIL_COUNT pass=$PASS_COUNT"
  exit 1
fi

log "SUMMARY fail=0 pass=$PASS_COUNT"
