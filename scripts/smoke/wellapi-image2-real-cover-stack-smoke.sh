#!/usr/bin/env bash
set -euo pipefail
set +x

API_ROOT="${API_ROOT:-http://localhost:8080}"
START_TIMEOUT_SECONDS="${START_TIMEOUT_SECONDS:-60}"
LOG_DIR="${LOG_DIR:-build/smoke/wellapi-image2-real-cover-stack-$(date +%Y%m%d%H%M%S)}"

API_PID=""

fail() {
  printf '[wellapi-image2-stack] ERROR: %s\n' "$*" >&2
  print_logs_hint >&2
  exit 1
}

log() {
  printf '[wellapi-image2-stack] %s\n' "$*"
}

need_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

print_logs_hint() {
  if [ -n "${API_LOG:-}" ]; then
    printf '[wellapi-image2-stack] logs:\n'
    printf '  api: %s\n' "$API_LOG"
  fi
}

cleanup() {
  set +e
  if [ -n "$API_PID" ] && kill -0 "$API_PID" >/dev/null 2>&1; then
    log "stopping API pid=$API_PID"
    kill "$API_PID" >/dev/null 2>&1
    wait "$API_PID" >/dev/null 2>&1
  fi
  unset WELLAPI_API_KEY
}

trap cleanup EXIT INT TERM

require_real_confirmation() {
  if [ "${ALLOW_WELLAPI_IMAGE2_REAL_SMOKE:-}" != "1" ]; then
    fail "refusing to run real provider smoke; set ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1"
  fi
}

read_secret_if_needed() {
  local var_name="$1"
  local prompt="$2"
  if [ -n "${!var_name:-}" ]; then
    export "$var_name=${!var_name}"
    return
  fi
  if [ ! -t 0 ]; then
    fail "$var_name is required; run interactively or export it in this shell"
  fi
  local value
  read -r -s -p "$prompt" value
  printf '\n'
  if [ -z "$value" ]; then
    fail "$var_name is required"
  fi
  export "$var_name=$value"
}

ensure_java_home() {
  if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@21 ]; then
    export JAVA_HOME=/opt/homebrew/opt/openjdk@21
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
}

port_free() {
  local port="$1"
  if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    fail "port $port is already in use; stop existing service or use wellapi-image2-real-cover-smoke.sh against it manually"
  fi
}

wait_for_url() {
  local name="$1"
  local url="$2"
  local deadline=$((SECONDS + START_TIMEOUT_SECONDS))
  while [ "$SECONDS" -lt "$deadline" ]; do
    if curl -fsS "$url" >/dev/null 2>&1; then
      log "$name is healthy"
      return
    fi
    sleep 2
  done
  fail "$name did not become healthy within ${START_TIMEOUT_SECONDS}s: $url"
}

start_api() {
  log "starting music-api"
  IMAGE_PROVIDER=image2 \
  IMAGE2_BACKEND=wellapi \
  IMAGE_REAL_CALLS_ENABLED=true \
  WELLAPI_BASE_URL="${WELLAPI_BASE_URL:-https://wellapi.ai}" \
  IMAGE2_MODEL_NAME="${IMAGE2_MODEL_NAME:-gpt-image-2}" \
  IMAGE2_SIZE="${IMAGE2_SIZE:-2048x1152}" \
  IMAGE2_QUALITY="${IMAGE2_QUALITY:-medium}" \
  IMAGE2_OUTPUT_FORMAT="${IMAGE2_OUTPUT_FORMAT:-jpeg}" \
  WELLAPI_REQUEST_TIMEOUT="${WELLAPI_REQUEST_TIMEOUT:-180s}" \
  MUSIC_PROVIDER=mock \
  MOCK_MUSIC_DURATION_MS="${MOCK_MUSIC_DURATION_MS:-1000}" \
  AGENT_REAL_CALLS_ENABLED=false \
  DEEPSEEK_REAL_CALLS_ENABLED=false \
  MUSIC_WORKFLOW_DISPATCH_MODE=sync \
  WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false \
  RENDER_WORKER_MODE=mock \
  COMPANY_ACCOUNT_ADAPTER_MODE=mock \
  COMPANY_MODERATION_ADAPTER_MODE=mock \
  COMPANY_QUOTA_ADAPTER_MODE=mock \
  COMPANY_PUBLISH_ADAPTER_MODE=mock \
  COMPANY_SHARE_ADAPTER_MODE=mock \
  ./gradlew :apps:music-api:bootRun >"$API_LOG" 2>&1 &
  API_PID="$!"
  wait_for_url "api" "$API_ROOT/health"
}

run_smoke() {
  log "running real WellAPI Image2 cover smoke"
  ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1 \
  IMAGE_PROVIDER=image2 \
  IMAGE2_BACKEND=wellapi \
  IMAGE_REAL_CALLS_ENABLED=true \
  MUSIC_PROVIDER=mock \
  API_ROOT="$API_ROOT" \
  scripts/smoke/wellapi-image2-real-cover-smoke.sh
}

main() {
  require_real_confirmation
  need_command curl
  need_command jq
  need_command lsof
  ensure_java_home
  read_secret_if_needed WELLAPI_API_KEY "WELLAPI_API_KEY: "
  export IMAGE_PROVIDER=image2
  export IMAGE2_BACKEND=wellapi
  export IMAGE_REAL_CALLS_ENABLED=true
  export WELLAPI_BASE_URL="${WELLAPI_BASE_URL:-https://wellapi.ai}"
  export IMAGE2_MODEL_NAME="${IMAGE2_MODEL_NAME:-gpt-image-2}"
  export IMAGE2_SIZE="${IMAGE2_SIZE:-2048x1152}"
  export IMAGE2_QUALITY="${IMAGE2_QUALITY:-medium}"
  export IMAGE2_OUTPUT_FORMAT="${IMAGE2_OUTPUT_FORMAT:-jpeg}"
  export WELLAPI_REQUEST_TIMEOUT="${WELLAPI_REQUEST_TIMEOUT:-180s}"
  export MUSIC_PROVIDER=mock

  mkdir -p "$LOG_DIR"
  API_LOG="$LOG_DIR/music-api.log"
  log "logs will be written under $LOG_DIR"

  port_free 8080
  start_api
  run_smoke
  log "PASS stack smoke provider=image2 backend=wellapi"
  print_logs_hint
}

main "$@"
