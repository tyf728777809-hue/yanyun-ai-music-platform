#!/usr/bin/env bash
set -euo pipefail

API_ROOT="${API_ROOT:-http://localhost:8080}"
STRICT_LOCAL="${STRICT_LOCAL:-true}"

fail() {
  printf '[company-readiness-smoke] ERROR: %s\n' "$*" >&2
  exit 1
}

log() {
  printf '[company-readiness-smoke] %s\n' "$*"
}

need_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

component_json() {
  local component="$1"
  jq -er --arg component "$component" '.components[] | select(.component == $component)' <<<"$READINESS"
}

assert_component_exists() {
  local component="$1"
  component_json "$component" >/dev/null || fail "missing component: $component"
}

assert_company_mock_boundary() {
  local component="$1"
  local expected_implementation="$2"
  local component_report
  component_report="$(component_json "$component")"
  jq -e --arg implementation "$expected_implementation" '
    .status == "MOCK_ONLY"
    and .blocks_company_deployment == true
    and .implementation == $implementation
    and (.required_env_vars | type == "array" and length > 0)
  ' >/dev/null <<<"$component_report" || fail "unexpected local company boundary for $component: $(jq -c '{component, configured_mode, implementation, status, blocks_company_deployment, required_env_vars}' <<<"$component_report")"
}

assert_component_implementation() {
  local component="$1"
  local expected_implementation="$2"
  local component_report
  component_report="$(component_json "$component")"
  jq -e --arg implementation "$expected_implementation" '
    .implementation == $implementation
    and (.required_env_vars | type == "array" and length > 0)
  ' >/dev/null <<<"$component_report" || fail "unexpected implementation for $component: $(jq -c '{component, implementation, required_env_vars}' <<<"$component_report")"
}

assert_component_required_env_vars() {
  local component="$1"
  shift
  local component_report
  component_report="$(component_json "$component")"
  local env_var
  for env_var in "$@"; do
    jq -e --arg env_var "$env_var" '.required_env_vars | index($env_var) != null' \
      >/dev/null <<<"$component_report" \
      || fail "$component is missing required env var name: $env_var"
  done
}

assert_required_env_vars_are_names_only() {
  jq -e '
    all(.components[]; (.required_env_vars | type == "array" and length > 0))
    and all(.components[].required_env_vars[]; (contains("=") | not))
  ' >/dev/null <<<"$READINESS" || fail "required_env_vars must be non-empty variable-name lists without values"
}

need_command curl
need_command jq
need_command rg

log "checking API health: $API_ROOT/health"
curl -fsS "$API_ROOT/health" | jq -e '.status == "OK"' >/dev/null

log "fetching integration readiness"
READINESS="$(curl -fsS "$API_ROOT/internal/integration-readiness")"
echo "$READINESS" | jq -e '.components | type == "array" and length >= 13' >/dev/null \
  || fail "readiness response is missing components"

if echo "$READINESS" | rg -q 'sk-[A-Za-z0-9_-]{12,}|Bearer [A-Za-z0-9._~+/=-]{16,}'; then
  fail "readiness response contains secret-like values"
fi

assert_required_env_vars_are_names_only

for component in \
  company_account \
  company_moderation \
  company_quota \
  company_publish \
  company_share \
  music_provider \
  render_worker \
  object_storage \
  workflow_dispatch \
  deepseek_guard \
  image2_guard \
  yunwu_suno_guard \
  dreammaker_guard; do
  assert_component_exists "$component"
done

assert_component_implementation dreammaker_guard DreamMakerHttpClient
assert_component_required_env_vars \
  dreammaker_guard \
  DREAMMAKER_REAL_CALLS_ENABLED \
  DREAMMAKER_API_BASE_URL \
  DREAMMAKER_ACCESS_KEY \
  DREAMMAKER_SECRET_KEY

if [ "$STRICT_LOCAL" = "true" ]; then
  jq -e '.environment == "local" and .overall_status == "READY_FOR_LOCAL"' >/dev/null <<<"$READINESS" \
    || fail "STRICT_LOCAL expected environment=local and overall_status=READY_FOR_LOCAL"
  assert_company_mock_boundary company_account MockAccountAdapter
  assert_company_mock_boundary company_moderation MockModerationAdapter
  assert_company_mock_boundary company_quota MockQuotaAdapter
  assert_company_mock_boundary company_publish MockPublishAdapter
  assert_company_mock_boundary company_share NotImplementedShareBoundary
fi

log "safe component summary"
echo "$READINESS" |
  jq -r '
    .components
    | sort_by(.component)
    | .[]
    | [
        .component,
        .configured_mode,
        .implementation,
        .status,
        (.blocks_company_deployment | tostring),
        (.required_env_vars | join(","))
      ]
    | @tsv
  ' |
  while IFS=$'\t' read -r component mode implementation status blocks required; do
    printf '[company-readiness-smoke] component=%s mode=%s implementation=%s status=%s blocks_company_deployment=%s required_env_vars=%s\n' \
      "$component" "$mode" "$implementation" "$status" "$blocks" "$required"
  done

log "PASS integration readiness report is structured and safe"
