#!/usr/bin/env bash
set -euo pipefail
set +x

API_ROOT="${API_ROOT:-http://localhost:8080}"
WORKER_ROOT="${WORKER_ROOT:-http://localhost:8081}"
START_TIMEOUT_SECONDS="${START_TIMEOUT_SECONDS:-60}"
LOG_DIR="${LOG_DIR:-build/smoke/yunwu-suno-real-music-stack-$(date +%Y%m%d%H%M%S)}"

API_PID=""
WORKER_PID=""

fail() {
  printf '[yunwu-stack] ERROR: %s\n' "$*" >&2
  print_logs_hint >&2
  exit 1
}

log() {
  printf '[yunwu-stack] %s\n' "$*"
}

need_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

print_logs_hint() {
  if [ -n "${WORKER_LOG:-}" ] || [ -n "${API_LOG:-}" ]; then
    printf '[yunwu-stack] logs:\n'
    [ -n "${WORKER_LOG:-}" ] && printf '  worker: %s\n' "$WORKER_LOG"
    [ -n "${API_LOG:-}" ] && printf '  api:    %s\n' "$API_LOG"
  fi
}

cleanup() {
  set +e
  if [ -n "$API_PID" ] && kill -0 "$API_PID" >/dev/null 2>&1; then
    log "stopping API pid=$API_PID"
    kill "$API_PID" >/dev/null 2>&1
    wait "$API_PID" >/dev/null 2>&1
  fi
  if [ -n "$WORKER_PID" ] && kill -0 "$WORKER_PID" >/dev/null 2>&1; then
    log "stopping worker pid=$WORKER_PID"
    kill "$WORKER_PID" >/dev/null 2>&1
    wait "$WORKER_PID" >/dev/null 2>&1
  fi
  unset YUNWU_API_KEY
}

trap cleanup EXIT INT TERM

require_real_confirmation() {
  if [ "${ALLOW_YUNWU_REAL_SMOKE:-}" != "1" ]; then
    fail "refusing to run real provider smoke; set ALLOW_YUNWU_REAL_SMOKE=1"
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
    fail "port $port is already in use; stop existing service or use yunwu-suno-real-music-smoke.sh against it manually"
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

start_worker() {
  log "starting music-worker"
  SUNO_BACKEND=yunwu \
  YUNWU_REAL_CALLS_ENABLED=true \
  YUNWU_BASE_URL="${YUNWU_BASE_URL:-https://yunwu.ai}" \
  YUNWU_SUNO_MODEL="${YUNWU_SUNO_MODEL:-chirp-v5}" \
  MUSIC_PROVIDER=suno \
  TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy \
  ./gradlew :apps:music-worker:bootRun >"$WORKER_LOG" 2>&1 &
  WORKER_PID="$!"
  wait_for_url "worker" "$WORKER_ROOT/actuator/health"
}

start_api() {
  log "starting music-api"
  SUNO_BACKEND=yunwu \
  YUNWU_REAL_CALLS_ENABLED=true \
  YUNWU_BASE_URL="${YUNWU_BASE_URL:-https://yunwu.ai}" \
  YUNWU_SUNO_MODEL="${YUNWU_SUNO_MODEL:-chirp-v5}" \
  MUSIC_PROVIDER=suno \
  MUSIC_WORKFLOW_DISPATCH_MODE=outbox \
  WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true \
  WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal \
  WORKFLOW_OUTBOX_POLL_INTERVAL=1s \
  RENDER_WORKER_MODE=mock \
  ./gradlew :apps:music-api:bootRun >"$API_LOG" 2>&1 &
  API_PID="$!"
  wait_for_url "api" "$API_ROOT/health"
}

run_smoke() {
  log "running real Yunwu Suno smoke"
  ALLOW_YUNWU_REAL_SMOKE=1 \
  SUNO_BACKEND=yunwu \
  YUNWU_REAL_CALLS_ENABLED=true \
  API_ROOT="$API_ROOT" \
  scripts/smoke/yunwu-suno-real-music-smoke.sh
}

main() {
  require_real_confirmation
  need_command curl
  need_command jq
  need_command lsof
  ensure_java_home
  read_secret_if_needed YUNWU_API_KEY "YUNWU_API_KEY: "
  export SUNO_BACKEND=yunwu
  export YUNWU_BASE_URL="${YUNWU_BASE_URL:-https://yunwu.ai}"
  export YUNWU_REAL_CALLS_ENABLED=true
  export YUNWU_SUNO_MODEL="${YUNWU_SUNO_MODEL:-chirp-v5}"

  mkdir -p "$LOG_DIR"
  WORKER_LOG="$LOG_DIR/music-worker.log"
  API_LOG="$LOG_DIR/music-api.log"
  log "logs will be written under $LOG_DIR"

  port_free 8080
  port_free 8081
  start_worker
  start_api
  run_smoke
  log "PASS stack smoke provider=suno backend=yunwu"
  print_logs_hint
}

main "$@"
