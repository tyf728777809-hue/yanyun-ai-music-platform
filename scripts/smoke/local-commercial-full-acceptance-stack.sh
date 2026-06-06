#!/usr/bin/env bash
set -euo pipefail
set +x

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

API_ROOT="${API_ROOT:-http://localhost:8080}"
API_ROOT="${API_ROOT%/}"
API_PORT="${API_PORT:-}"
FRONTEND_PORT="${FRONTEND_PORT:-5274}"
START_TIMEOUT_SECONDS="${START_TIMEOUT_SECONDS:-90}"
STOP_TIMEOUT_SECONDS="${STOP_TIMEOUT_SECONDS:-60}"
EXPECTED_DURATION_MS="${EXPECTED_DURATION_MS:-1000}"
LOG_DIR="${LOG_DIR:-${REPO_ROOT}/build/smoke/local-commercial-full-acceptance-$(date +%Y%m%d%H%M%S)}"

API_PID=""
API_LOG=""

fail() {
  printf '[full-acceptance] ERROR: %s\n' "$*" >&2
  print_logs_hint >&2
  exit 1
}

log() {
  printf '[full-acceptance] %s\n' "$*"
}

need_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

derive_api_port() {
  if [ -n "$API_PORT" ]; then
    return
  fi

  local without_scheme
  without_scheme="${API_ROOT#http://}"
  without_scheme="${without_scheme#https://}"
  without_scheme="${without_scheme%%/*}"
  if [[ "$without_scheme" == *:* ]]; then
    API_PORT="${without_scheme##*:}"
  else
    case "$API_ROOT" in
      https://*) API_PORT="443" ;;
      *) API_PORT="80" ;;
    esac
  fi
}

print_logs_hint() {
  if [ -n "${API_LOG:-}" ]; then
    printf '[full-acceptance] logs:\n'
    printf '  api: %s\n' "$API_LOG"
  fi
}

cleanup() {
  set +e
  stop_api
}

stop_api() {
  if [ -n "${API_PID:-}" ] && kill -0 "$API_PID" >/dev/null 2>&1; then
    log "stopping API pid=$API_PID"
    kill "$API_PID" >/dev/null 2>&1
    wait "$API_PID" >/dev/null 2>&1 || true
  fi
  API_PID=""
  if ! wait_for_port_free 10; then
    stop_managed_api_listener
  fi
  if ! wait_for_port_free "$STOP_TIMEOUT_SECONDS"; then
    log "port $API_PORT is still occupied after stopping API"
  fi
}

trap cleanup EXIT INT TERM

ensure_java_home() {
  if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@21 ]; then
    export JAVA_HOME=/opt/homebrew/opt/openjdk@21
    export PATH="$JAVA_HOME/bin:$PATH"
  fi
}

assert_port_free() {
  if lsof -nP -iTCP:"$API_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    fail "port $API_PORT is already in use; stop the existing API before running this stack smoke"
  fi
}

wait_for_port_free() {
  local timeout_seconds="$1"
  local deadline
  deadline=$((SECONDS + timeout_seconds))
  while [ "$SECONDS" -lt "$deadline" ]; do
    if ! lsof -nP -iTCP:"$API_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  return 1
}

stop_managed_api_listener() {
  local pids
  local pid
  local command_line
  pids="$(lsof -tiTCP:"$API_PORT" -sTCP:LISTEN 2>/dev/null || true)"
  for pid in $pids; do
    command_line="$(ps -o command= -p "$pid" 2>/dev/null || true)"
    if [[ "$command_line" == *"com.yanyun.music.api.MusicApiApplication"* ]]; then
      log "stopping API listener pid=$pid"
      kill "$pid" >/dev/null 2>&1 || true
    fi
  done
}

wait_for_api() {
  local deadline
  local health
  deadline=$((SECONDS + START_TIMEOUT_SECONDS))
  while [ "$SECONDS" -lt "$deadline" ]; do
    if ! kill -0 "$API_PID" >/dev/null 2>&1; then
      tail -80 "$API_LOG" >&2 || true
      fail "API process exited before health became ready"
    fi
    if health="$(curl -fsS "${API_ROOT}/health" 2>/dev/null)" \
      && jq -e '.status == "OK"' >/dev/null 2>&1 <<<"$health"; then
      log "API is healthy: ${API_ROOT}/health"
      return
    fi
    sleep 2
  done

  tail -80 "$API_LOG" >&2 || true
  fail "API did not become healthy within ${START_TIMEOUT_SECONDS}s"
}

assert_dependencies() {
  [[ -d "${REPO_ROOT}/apps/render-worker/node_modules" ]] \
    || fail "apps/render-worker/node_modules is missing; run cd apps/render-worker && npm install"
  [[ -x "${REPO_ROOT}/apps/render-worker/node_modules/.bin/remotion" ]] \
    || fail "render-worker Remotion binary is missing; run cd apps/render-worker && npm install"
  [[ -d "${REPO_ROOT}/prototypes/Claude-web-v1/node_modules" ]] \
    || fail "prototypes/Claude-web-v1/node_modules is missing; run cd prototypes/Claude-web-v1 && npm install"
  [[ -x "${REPO_ROOT}/prototypes/Claude-web-v1/node_modules/.bin/vite" ]] \
    || fail "Claude Web v1 Vite binary is missing; run cd prototypes/Claude-web-v1 && npm install"
}

start_api() {
  local phase="$1"
  local render_mode="$2"
  API_LOG="${LOG_DIR}/${phase}-music-api.log"
  mkdir -p "$LOG_DIR"
  assert_port_free
  log "starting API phase=${phase} render_mode=${render_mode}; log=${API_LOG}"

  (
    cd "$REPO_ROOT"
    if [ -n "${JAVA_HOME:-}" ]; then
      export JAVA_HOME
    fi
    export PATH
    export MUSIC_API_PORT="$API_PORT"
    export MOCK_MUSIC_DURATION_MS="$EXPECTED_DURATION_MS"
    export MUSIC_PROVIDER=mock
    export MUSIC_WORKFLOW_DISPATCH_MODE=sync
    export WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false
    export RENDER_WORKER_MODE="$render_mode"
    export RENDER_WORKER_WORKING_DIRECTORY="${RENDER_WORKER_WORKING_DIRECTORY:-apps/render-worker}"
    export RENDER_WORKER_COMMAND="${RENDER_WORKER_COMMAND:-npm}"
    export RENDER_WORKER_ARGUMENTS="${RENDER_WORKER_ARGUMENTS:-run,render:job,--}"
    export RENDER_WORKER_TIMEOUT="${RENDER_WORKER_TIMEOUT:-120s}"
    export DREAMMAKER_REAL_CALLS_ENABLED=false
    export YUNWU_REAL_CALLS_ENABLED=false
    export AGENT_REAL_CALLS_ENABLED=false
    export DEEPSEEK_REAL_CALLS_ENABLED=false
    export IMAGE_REAL_CALLS_ENABLED=false
    export MOCK_MODERATION_PUBLISH_PACKAGE_BLOCKED_USER_IDS=""
    export MOCK_MODERATION_PUBLISH_PACKAGE_BLOCKED_WORK_IDS=""
    ./gradlew --no-daemon :apps:music-api:bootRun
  ) >"$API_LOG" 2>&1 &
  API_PID="$!"
  wait_for_api
}

run_backend_stack() {
  log "phase 1/3: backend acceptance stack"
  EXPECTED_DURATION_MS="$EXPECTED_DURATION_MS" \
  API_ROOT="$API_ROOT" \
    scripts/smoke/local-commercial-backend-acceptance-stack.sh
}

run_mp4_stack() {
  log "phase 2/3: local-process MP4 backend smoke"
  start_api "local-process-mp4" "local-process"
  API_BASE_URL="${API_ROOT}/api/v1" \
  API_HEALTH_URL="${API_ROOT}/health" \
  EXPECTED_DURATION_MS="$EXPECTED_DURATION_MS" \
  EXPECT_RENDER_WORKER=local-process \
    scripts/smoke/api-main-flow.sh
  stop_api
}

run_frontend_stack() {
  log "phase 3/3: Claude Web v1 real-backend UI smoke"
  start_api "claude-web-real-backend" "mock"
  (
    cd "${REPO_ROOT}/prototypes/Claude-web-v1"
    API_ORIGIN="$API_ROOT" \
    FRONTEND_PORT="$FRONTEND_PORT" \
    HEADLESS="${HEADLESS:-true}" \
      npm run smoke:real-backend
  )
  stop_api
}

main() {
  need_command curl
  need_command jq
  need_command lsof
  need_command ps
  need_command tail
  need_command ffprobe
  derive_api_port
  ensure_java_home
  assert_dependencies

  log "full acceptance smoke is Mock-only; real provider and company-system calls stay disabled"
  log "logs will be written under ${LOG_DIR}"

  run_backend_stack
  run_mp4_stack
  run_frontend_stack

  assert_port_free
  log "PASS local commercial full acceptance stack"
  print_logs_hint
}

main "$@"
