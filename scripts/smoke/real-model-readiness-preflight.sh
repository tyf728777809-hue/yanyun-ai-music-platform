#!/usr/bin/env bash
set -euo pipefail
set +x

TARGET="${TARGET:-all}"
STRICT="${STRICT:-false}"
CHECK_API="${CHECK_API:-false}"
API_ROOT="${API_ROOT:-http://localhost:8080}"

YUNWU_BASE_URL="${YUNWU_BASE_URL:-https://yunwu.ai}"
YUNWU_SUNO_MODEL="${YUNWU_SUNO_MODEL:-chirp-fenix}"
DREAMMAKER_API_BASE_URL="${DREAMMAKER_API_BASE_URL:-https://api-all.dreammaker.netease.com}"
DREAMMAKER_SUNO_MODEL="${DREAMMAKER_SUNO_MODEL:-chirp-crow}"
MINIMAX_MODEL="${MINIMAX_MODEL:-minimax-music-2.6}"
DEEPSEEK_BASE_URL="${DEEPSEEK_BASE_URL:-https://api.deepseek.com}"
DEEPSEEK_MODEL_NAME="${DEEPSEEK_MODEL_NAME:-deepseek-v4-pro}"
WELLAPI_BASE_URL="${WELLAPI_BASE_URL:-https://wellapi.ai}"
IMAGE2_MODEL_NAME="${IMAGE2_MODEL_NAME:-gpt-image-2}"

FAILURES=0
WARNINGS=0

fail() {
  printf '[real-model-preflight] ERROR: %s\n' "$*" >&2
  exit 1
}

log() {
  printf '[real-model-preflight] %s\n' "$*"
}

need_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

is_true() {
  local value="${1:-}"
  [ "$value" = "true" ] || [ "$value" = "1" ]
}

is_non_empty() {
  local value="${1:-}"
  [ -n "${value//[[:space:]]/}" ]
}

var_configured() {
  local name="$1"
  is_non_empty "${!name:-}"
}

secret_state() {
  local name="$1"
  if var_configured "$name"; then
    printf '%s=configured' "$name"
  else
    printf '%s=missing' "$name"
  fi
}

append_missing_var() {
  local name="$1"
  if ! var_configured "$name"; then
    if [ -n "$MISSING" ]; then
      MISSING="$MISSING,$name"
    else
      MISSING="$name"
    fi
  fi
}

append_blocker() {
  local message="$1"
  if [ -n "$BLOCKERS" ]; then
    BLOCKERS="$BLOCKERS;$message"
  else
    BLOCKERS="$message"
  fi
}

record_target() {
  local target="$1"
  local status="$2"
  local notes="$3"
  log "target=$target status=$status missing=${MISSING:-none} blockers=${BLOCKERS:-none} notes=$notes"
  if [ "$STRICT" = "true" ] && { [ "$TARGET" = "$target" ] || [ "$TARGET" = "all" ]; } && [ "$status" != "READY" ]; then
    FAILURES=$((FAILURES + 1))
  fi
}

music_dispatch_safe() {
  [ "${MUSIC_WORKFLOW_DISPATCH_MODE:-}" = "outbox" ] \
    && [ "${WORKFLOW_OUTBOX_DISPATCH_TARGET:-}" = "temporal" ] \
    && is_true "${WORKFLOW_OUTBOX_DISPATCHER_ENABLED:-}"
}

require_music_dispatch() {
  if ! music_dispatch_safe; then
    append_blocker "real music requires MUSIC_WORKFLOW_DISPATCH_MODE=outbox, WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal, WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true"
  fi
}

status_from_missing_and_blockers() {
  local enabled="$1"
  if [ "$enabled" != "true" ]; then
    printf 'DISABLED'
  elif [ -n "$MISSING" ] || [ -n "$BLOCKERS" ]; then
    printf 'BLOCKED'
  else
    printf 'READY'
  fi
}

check_yunwu_suno() {
  MISSING=""
  BLOCKERS=""
  local enabled="false"
  if is_true "${YUNWU_REAL_CALLS_ENABLED:-}" || [ "$TARGET" = "yunwu-suno" ]; then
    enabled="true"
  fi
  [ "${SUNO_BACKEND:-yunwu}" = "yunwu" ] || append_blocker "SUNO_BACKEND must be yunwu"
  append_missing_var YUNWU_BASE_URL
  append_missing_var YUNWU_API_KEY
  append_missing_var YUNWU_SUNO_MODEL
  is_true "${YUNWU_REAL_CALLS_ENABLED:-}" || append_blocker "YUNWU_REAL_CALLS_ENABLED must be true"
  require_music_dispatch
  local status
  status="$(status_from_missing_and_blockers "$enabled")"
  record_target "yunwu-suno" "$status" "$(secret_state YUNWU_API_KEY)"
}

check_dreammaker_suno() {
  MISSING=""
  BLOCKERS=""
  local enabled="false"
  if is_true "${DREAMMAKER_REAL_CALLS_ENABLED:-}" || [ "$TARGET" = "dreammaker-suno" ]; then
    enabled="true"
  fi
  [ "${SUNO_BACKEND:-yunwu}" = "dreammaker" ] || append_blocker "SUNO_BACKEND must be dreammaker"
  append_missing_var DREAMMAKER_API_BASE_URL
  append_missing_var DREAMMAKER_ACCESS_KEY
  append_missing_var DREAMMAKER_SECRET_KEY
  append_missing_var DREAMMAKER_SUNO_MODEL
  is_true "${DREAMMAKER_REAL_CALLS_ENABLED:-}" || append_blocker "DREAMMAKER_REAL_CALLS_ENABLED must be true"
  require_music_dispatch
  local status
  status="$(status_from_missing_and_blockers "$enabled")"
  record_target "dreammaker-suno" "$status" "$(secret_state DREAMMAKER_ACCESS_KEY),$(secret_state DREAMMAKER_SECRET_KEY)"
}

check_dreammaker_minimax() {
  MISSING=""
  BLOCKERS=""
  local enabled="false"
  if is_true "${DREAMMAKER_REAL_CALLS_ENABLED:-}" || [ "$TARGET" = "dreammaker-minimax" ]; then
    enabled="true"
  fi
  append_missing_var DREAMMAKER_API_BASE_URL
  append_missing_var DREAMMAKER_ACCESS_KEY
  append_missing_var DREAMMAKER_SECRET_KEY
  append_missing_var MINIMAX_MODEL
  is_true "${DREAMMAKER_REAL_CALLS_ENABLED:-}" || append_blocker "DREAMMAKER_REAL_CALLS_ENABLED must be true"
  require_music_dispatch
  local status
  status="$(status_from_missing_and_blockers "$enabled")"
  record_target "dreammaker-minimax" "$status" "$(secret_state DREAMMAKER_ACCESS_KEY),$(secret_state DREAMMAKER_SECRET_KEY)"
}

check_deepseek() {
  MISSING=""
  BLOCKERS=""
  local enabled="false"
  if is_true "${DEEPSEEK_REAL_CALLS_ENABLED:-}" || is_true "${AGENT_REAL_CALLS_ENABLED:-}" || [ "$TARGET" = "deepseek" ]; then
    enabled="true"
  fi
  append_missing_var DEEPSEEK_BASE_URL
  append_missing_var DEEPSEEK_API_KEY
  append_missing_var DEEPSEEK_MODEL_NAME
  is_true "${AGENT_REAL_CALLS_ENABLED:-}" || append_blocker "AGENT_REAL_CALLS_ENABLED must be true"
  is_true "${DEEPSEEK_REAL_CALLS_ENABLED:-}" || append_blocker "DEEPSEEK_REAL_CALLS_ENABLED must be true"
  local status
  status="$(status_from_missing_and_blockers "$enabled")"
  record_target "deepseek" "$status" "$(secret_state DEEPSEEK_API_KEY)"
}

check_wellapi_image2() {
  MISSING=""
  BLOCKERS=""
  local enabled="false"
  if is_true "${IMAGE_REAL_CALLS_ENABLED:-}" || [ "$TARGET" = "wellapi-image2" ]; then
    enabled="true"
  fi
  [ "${IMAGE_PROVIDER:-mock}" = "image2" ] || append_blocker "IMAGE_PROVIDER must be image2"
  [ "${IMAGE2_BACKEND:-wellapi}" = "wellapi" ] || append_blocker "IMAGE2_BACKEND must be wellapi"
  append_missing_var WELLAPI_BASE_URL
  append_missing_var WELLAPI_API_KEY
  append_missing_var IMAGE2_MODEL_NAME
  is_true "${IMAGE_REAL_CALLS_ENABLED:-}" || append_blocker "IMAGE_REAL_CALLS_ENABLED must be true"
  local status
  status="$(status_from_missing_and_blockers "$enabled")"
  record_target "wellapi-image2" "$status" "$(secret_state WELLAPI_API_KEY)"
}

check_dreammaker_image2() {
  MISSING=""
  BLOCKERS=""
  local enabled="false"
  if is_true "${IMAGE_REAL_CALLS_ENABLED:-}" || is_true "${DREAMMAKER_REAL_CALLS_ENABLED:-}" || [ "$TARGET" = "dreammaker-image2" ]; then
    enabled="true"
  fi
  [ "${IMAGE_PROVIDER:-mock}" = "image2" ] || append_blocker "IMAGE_PROVIDER must be image2"
  [ "${IMAGE2_BACKEND:-wellapi}" = "dreammaker" ] || append_blocker "IMAGE2_BACKEND must be dreammaker"
  append_missing_var DREAMMAKER_API_BASE_URL
  append_missing_var DREAMMAKER_ACCESS_KEY
  append_missing_var DREAMMAKER_SECRET_KEY
  append_missing_var IMAGE2_MODEL_NAME
  is_true "${IMAGE_REAL_CALLS_ENABLED:-}" || append_blocker "IMAGE_REAL_CALLS_ENABLED must be true"
  is_true "${DREAMMAKER_REAL_CALLS_ENABLED:-}" || append_blocker "DREAMMAKER_REAL_CALLS_ENABLED must be true"
  local status
  status="$(status_from_missing_and_blockers "$enabled")"
  record_target "dreammaker-image2" "$status" "$(secret_state DREAMMAKER_ACCESS_KEY),$(secret_state DREAMMAKER_SECRET_KEY)"
}

check_public_real_full_experience() {
  MISSING=""
  BLOCKERS=""
  local enabled="false"
  if [ "$TARGET" = "public-real-full-experience" ]; then
    enabled="true"
  fi

  append_missing_var DEEPSEEK_BASE_URL
  append_missing_var DEEPSEEK_API_KEY
  append_missing_var DEEPSEEK_MODEL_NAME
  is_true "${AGENT_REAL_CALLS_ENABLED:-}" || append_blocker "AGENT_REAL_CALLS_ENABLED must be true"
  is_true "${DEEPSEEK_REAL_CALLS_ENABLED:-}" || append_blocker "DEEPSEEK_REAL_CALLS_ENABLED must be true"

  [ "${SUNO_BACKEND:-yunwu}" = "yunwu" ] || append_blocker "SUNO_BACKEND must be yunwu"
  append_missing_var YUNWU_BASE_URL
  append_missing_var YUNWU_API_KEY
  append_missing_var YUNWU_SUNO_MODEL
  is_true "${YUNWU_REAL_CALLS_ENABLED:-}" || append_blocker "YUNWU_REAL_CALLS_ENABLED must be true"
  require_music_dispatch

  [ "${IMAGE_PROVIDER:-mock}" = "image2" ] || append_blocker "IMAGE_PROVIDER must be image2"
  [ "${IMAGE2_BACKEND:-wellapi}" = "wellapi" ] || append_blocker "IMAGE2_BACKEND must be wellapi"
  append_missing_var WELLAPI_BASE_URL
  append_missing_var WELLAPI_API_KEY
  append_missing_var IMAGE2_MODEL_NAME
  is_true "${IMAGE_REAL_CALLS_ENABLED:-}" || append_blocker "IMAGE_REAL_CALLS_ENABLED must be true"

  if is_true "${DREAMMAKER_REAL_CALLS_ENABLED:-}"; then
    append_blocker "DREAMMAKER_REAL_CALLS_ENABLED must be false for public full experience"
  fi
  [ "${TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE:-legacy}" = "legacy" ] || append_blocker "TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE must be legacy"
  [ "${RENDER_WORKER_MODE:-}" = "album-ffmpeg" ] || append_blocker "RENDER_WORKER_MODE must be album-ffmpeg"

  local status
  status="$(status_from_missing_and_blockers "$enabled")"
  record_target "public-real-full-experience" "$status" "$(secret_state DEEPSEEK_API_KEY),$(secret_state YUNWU_API_KEY),$(secret_state WELLAPI_API_KEY)"
}

warn_multiple_real_targets() {
  if [ "$TARGET" = "public-real-full-experience" ]; then
    return
  fi
  local count=0
  is_true "${YUNWU_REAL_CALLS_ENABLED:-}" && count=$((count + 1))
  is_true "${DEEPSEEK_REAL_CALLS_ENABLED:-}" && count=$((count + 1))
  is_true "${IMAGE_REAL_CALLS_ENABLED:-}" && count=$((count + 1))
  if is_true "${DREAMMAKER_REAL_CALLS_ENABLED:-}" && [ "${IMAGE2_BACKEND:-}" != "dreammaker" ]; then
    count=$((count + 1))
  fi
  if [ "$count" -gt 1 ]; then
    WARNINGS=$((WARNINGS + 1))
    log "warning=multiple real-call switches appear enabled; single-provider smoke is the safer default"
  fi
}

check_api_readiness() {
  need_command curl
  need_command jq
  need_command rg
  log "checking API health: $API_ROOT/health"
  curl -fsS "$API_ROOT/health" | jq -e '.status == "OK"' >/dev/null
  log "fetching integration readiness"
  local readiness
  readiness="$(curl -fsS "$API_ROOT/internal/integration-readiness")"
  if echo "$readiness" | rg -q 'sk-[A-Za-z0-9_-]{12,}|Bearer [A-Za-z0-9._~+/=-]{16,}'; then
    fail "integration readiness contains secret-like values"
  fi
  echo "$readiness" |
    jq -r '
      .components
      | map(select(.component == "music_provider"
        or .component == "workflow_dispatch"
        or .component == "dreammaker_guard"
        or .component == "yunwu_suno_guard"
        or .component == "deepseek_guard"
        or .component == "image2_guard"))
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
      log "api_component=$component mode=$mode implementation=$implementation status=$status blocks_company_deployment=$blocks required_env_vars=$required"
    done
}

run_target_checks() {
  case "$TARGET" in
    all)
      check_yunwu_suno
      check_dreammaker_suno
      check_dreammaker_minimax
      check_deepseek
      check_wellapi_image2
      check_dreammaker_image2
      ;;
    yunwu-suno) check_yunwu_suno ;;
    dreammaker-suno) check_dreammaker_suno ;;
    dreammaker-minimax) check_dreammaker_minimax ;;
    deepseek) check_deepseek ;;
    wellapi-image2) check_wellapi_image2 ;;
    dreammaker-image2) check_dreammaker_image2 ;;
    public-real-full-experience) check_public_real_full_experience ;;
    *) fail "TARGET must be all|yunwu-suno|dreammaker-suno|dreammaker-minimax|deepseek|wellapi-image2|dreammaker-image2|public-real-full-experience, got: $TARGET" ;;
  esac
}

main() {
  warn_multiple_real_targets
  run_target_checks
  if [ "$CHECK_API" = "true" ]; then
    check_api_readiness
  fi
  if [ "$FAILURES" -gt 0 ]; then
    fail "STRICT preflight failed for TARGET=$TARGET"
  fi
  log "PASS preflight completed target=$TARGET strict=$STRICT check_api=$CHECK_API warnings=$WARNINGS"
}

main "$@"
