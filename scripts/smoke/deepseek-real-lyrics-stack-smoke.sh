#!/usr/bin/env bash
set -euo pipefail
set +x

API_ROOT="${API_ROOT:-http://localhost:8080}"
API_PORT="${API_PORT:-8080}"
START_TIMEOUT_SECONDS="${START_TIMEOUT_SECONDS:-90}"
LOG_DIR="${LOG_DIR:-build/smoke/deepseek-real-lyrics-stack-$(date +%Y%m%d%H%M%S)}"

API_PID=""
API_LOG=""

fail() {
  printf '[deepseek-stack] ERROR: %s\n' "$*" >&2
  print_logs_hint >&2
  exit 1
}

log() {
  printf '[deepseek-stack] %s\n' "$*"
}

need_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

print_logs_hint() {
  if [ -n "${API_LOG:-}" ]; then
    printf '[deepseek-stack] logs:\n'
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
  unset DEEPSEEK_API_KEY
}

trap cleanup EXIT INT TERM

require_real_confirmation() {
  if [ "${ALLOW_DEEPSEEK_REAL_SMOKE:-}" != "1" ]; then
    fail "refusing to run real DeepSeek smoke; set ALLOW_DEEPSEEK_REAL_SMOKE=1"
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
    fail "port $port is already in use; stop existing service or use deepseek-real-lyrics-smoke.sh against it manually"
  fi
}

wait_for_api() {
  local deadline=$((SECONDS + START_TIMEOUT_SECONDS))
  while [ "$SECONDS" -lt "$deadline" ]; do
    if curl -fsS "$API_ROOT/health" >/dev/null 2>&1; then
      log "api is healthy"
      return
    fi
    sleep 2
  done
  fail "api did not become healthy within ${START_TIMEOUT_SECONDS}s: $API_ROOT/health"
}

start_api() {
  log "starting music-api"
  AGENT_REAL_CALLS_ENABLED=true \
  DEEPSEEK_REAL_CALLS_ENABLED=true \
  DEEPSEEK_BASE_URL="${DEEPSEEK_BASE_URL:-https://api.deepseek.com}" \
  DEEPSEEK_MODEL_NAME="${DEEPSEEK_MODEL_NAME:-deepseek-v4-pro}" \
  DEEPSEEK_TIMEOUT_MS="${DEEPSEEK_TIMEOUT_MS:-30000}" \
  DEEPSEEK_MAX_ATTEMPTS="${DEEPSEEK_MAX_ATTEMPTS:-1}" \
  DEEPSEEK_RESPONSE_MAX_TOKENS="${DEEPSEEK_RESPONSE_MAX_TOKENS:-1800}" \
  DEEPSEEK_TEMPERATURE="${DEEPSEEK_TEMPERATURE:-0.7}" \
  MUSIC_PROVIDER=mock \
  IMAGE_PROVIDER=mock \
  DREAMMAKER_REAL_CALLS_ENABLED=false \
  IMAGE_REAL_CALLS_ENABLED=false \
  YUNWU_REAL_CALLS_ENABLED=false \
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
  wait_for_api
}

run_smoke() {
  log "running real DeepSeek lyrics smoke"
  ALLOW_DEEPSEEK_REAL_SMOKE=1 \
  AGENT_REAL_CALLS_ENABLED=true \
  DEEPSEEK_REAL_CALLS_ENABLED=true \
  MUSIC_PROVIDER=mock \
  IMAGE_PROVIDER=mock \
  DREAMMAKER_REAL_CALLS_ENABLED=false \
  IMAGE_REAL_CALLS_ENABLED=false \
  YUNWU_REAL_CALLS_ENABLED=false \
  API_ROOT="$API_ROOT" \
    scripts/smoke/deepseek-real-lyrics-smoke.sh
}

main() {
  require_real_confirmation
  need_command curl
  need_command jq
  need_command lsof
  ensure_java_home
  read_secret_if_needed DEEPSEEK_API_KEY "DEEPSEEK_API_KEY: "

  export AGENT_REAL_CALLS_ENABLED=true
  export DEEPSEEK_REAL_CALLS_ENABLED=true
  export DEEPSEEK_BASE_URL="${DEEPSEEK_BASE_URL:-https://api.deepseek.com}"
  export DEEPSEEK_MODEL_NAME="${DEEPSEEK_MODEL_NAME:-deepseek-v4-pro}"
  export MUSIC_PROVIDER=mock
  export IMAGE_PROVIDER=mock
  export DREAMMAKER_REAL_CALLS_ENABLED=false
  export IMAGE_REAL_CALLS_ENABLED=false
  export YUNWU_REAL_CALLS_ENABLED=false

  mkdir -p "$LOG_DIR"
  API_LOG="$LOG_DIR/music-api.log"
  log "logs will be written under $LOG_DIR"

  port_free "$API_PORT"
  start_api
  run_smoke
  log "PASS stack smoke provider=deepseek"
  print_logs_hint
}

main "$@"
