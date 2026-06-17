#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RUN_DIR="${ROOT_DIR}/build/run/public-real-test"
LOG_DIR="${ROOT_DIR}/build/logs/public-real-test"
API_PID_FILE="${RUN_DIR}/music-api.pid"
WORKER_PID_FILE="${RUN_DIR}/music-worker.pid"
API_LOG="${LOG_DIR}/music-api.log"
WORKER_LOG="${LOG_DIR}/music-worker.log"
API_PORT="${MUSIC_API_PORT:-8080}"
WORKER_PORT="${MUSIC_WORKER_PORT:-8081}"
API_HEALTH_URL="http://127.0.0.1:${API_PORT}/health"
WORKER_HEALTH_URL="http://127.0.0.1:${WORKER_PORT}/actuator/health"
READINESS_URL="http://127.0.0.1:${API_PORT}/internal/integration-readiness"

usage() {
  cat <<'EOF'
Usage: scripts/smoke/public-real-test-stack.sh start|stop|restart|status

Starts the local backend stack used for small-scope public real testing:
- music-api on 8080
- music-worker on 8081
- DeepSeek/Yunwu/WellAPI real providers
- S3/MinIO public media URLs
- album-ffmpeg video rendering

Required:
- .env.real.local with provider credentials, or equivalent environment variables
- S3_PUBLIC_ENDPOINT set to a public media endpoint, such as a trusted tunnel URL

Optional:
- BUILD=false to reuse existing bootJar artifacts
- ALLOW_KILL_EXISTING=true to stop existing Yanyun API/worker processes on the same ports
- USE_SCREEN=true|false, defaults to true when screen is installed
EOF
}

log() {
  printf '[public-real-test-stack] %s\n' "$*"
}

fail() {
  printf '[public-real-test-stack] ERROR: %s\n' "$*" >&2
  exit 1
}

pid_alive() {
  local pid="${1:-}"
  [ -n "$pid" ] && [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" >/dev/null 2>&1
}

screen_alive() {
  local session="${1:-}"
  local sessions
  [ -n "$session" ] || return 1
  sessions="$(screen -ls 2>/dev/null || true)"
  grep -q "[.]${session}[[:space:]]" <<<"$sessions"
}

ref_alive() {
  local ref="${1:-}"
  if [[ "$ref" =~ ^[0-9]+$ ]]; then
    pid_alive "$ref"
  else
    screen_alive "$ref"
  fi
}

pid_from_file() {
  local file="$1"
  [ -f "$file" ] && tr -d '[:space:]' <"$file" || true
}

port_pid() {
  local port="$1"
  lsof -nP -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null | head -1 || true
}

process_cmd() {
  local pid="$1"
  if [[ "$pid" =~ ^[0-9]+$ ]]; then
    ps -p "$pid" -o command= 2>/dev/null || true
  fi
}

kill_known_process_on_port() {
  local port="$1"
  local label="$2"
  local pid cmd
  pid="$(port_pid "$port")"
  [ -n "$pid" ] || return 0
  cmd="$(process_cmd "$pid")"
  case "$cmd" in
    *com.yanyun.music.api.MusicApiApplication*|*apps/music-api*|*music-api*.jar*|*com.yanyun.music.worker.MusicWorkerApplication*|*apps/music-worker*|*music-worker*.jar*)
      if [ "${ALLOW_KILL_EXISTING:-false}" = "true" ]; then
        log "stopping existing ${label} process on port ${port}. pid=${pid}"
        kill "$pid" >/dev/null 2>&1 || true
        wait_for_port_free "$port"
      else
        fail "port ${port} is occupied by a Yanyun process. Set ALLOW_KILL_EXISTING=true for restart, or stop it manually."
      fi
      ;;
    *)
      fail "port ${port} is occupied by another process. pid=${pid}"
      ;;
  esac
}

wait_for_port_free() {
  local port="$1"
  local i
  for i in $(seq 1 30); do
    [ -z "$(port_pid "$port")" ] && return 0
    sleep 1
  done
  fail "port ${port} did not become free"
}

wait_for_http() {
  local url="$1"
  local label="$2"
  local i
  for i in $(seq 1 90); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      log "${label} is healthy"
      return 0
    fi
    sleep 1
  done
  fail "${label} did not become healthy. url=${url}"
}

load_env() {
  cd "$ROOT_DIR"
  if [ -f ".env.real.local" ]; then
    set -a
    # shellcheck disable=SC1091
    source ".env.real.local"
    set +a
  fi

  export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
  export PATH="$JAVA_HOME/bin:$PATH"

  require_env DEEPSEEK_API_KEY
  require_env YUNWU_API_KEY
  require_env WELLAPI_API_KEY
  require_env S3_PUBLIC_ENDPOINT

  export AGENT_REAL_CALLS_ENABLED=true
  export DEEPSEEK_REAL_CALLS_ENABLED=true
  export DEEPSEEK_MODEL_NAME="${DEEPSEEK_MODEL_NAME:-deepseek-v4-pro}"
  export DEEPSEEK_TIMEOUT_MS="${DEEPSEEK_TIMEOUT_MS:-30000}"
  export DEEPSEEK_MAX_ATTEMPTS="${DEEPSEEK_MAX_ATTEMPTS:-1}"
  export DEEPSEEK_RESPONSE_MAX_TOKENS="${DEEPSEEK_RESPONSE_MAX_TOKENS:-4096}"
  export DEEPSEEK_TEMPERATURE="${DEEPSEEK_TEMPERATURE:-0.7}"
  export KNOWLEDGE_RETRIEVAL_MODE="${KNOWLEDGE_RETRIEVAL_MODE:-pgvector}"
  export KNOWLEDGE_KB_VERSION="${KNOWLEDGE_KB_VERSION:-yanyun-commercial-kb-2026-06-13-v1}"

  export MUSIC_PROVIDER=suno
  export SUNO_BACKEND=yunwu
  export YUNWU_REAL_CALLS_ENABLED=true
  export YUNWU_SUNO_MODEL="${YUNWU_SUNO_MODEL:-chirp-fenix}"
  export YUNWU_REQUEST_TIMEOUT="${YUNWU_REQUEST_TIMEOUT:-300s}"
  export YUNWU_MAX_POLL_ATTEMPTS="${YUNWU_MAX_POLL_ATTEMPTS:-180}"
  export YUNWU_POLL_INTERVAL="${YUNWU_POLL_INTERVAL:-2s}"

  export IMAGE_PROVIDER=image2
  export IMAGE2_BACKEND=wellapi
  export IMAGE_REAL_CALLS_ENABLED=true
  export IMAGE2_MODEL_NAME="${IMAGE2_MODEL_NAME:-gpt-image-2}"
  export IMAGE2_SIZE="${IMAGE2_SIZE:-2048x1152}"
  export IMAGE2_QUALITY="${IMAGE2_QUALITY:-medium}"
  export IMAGE2_OUTPUT_FORMAT="${IMAGE2_OUTPUT_FORMAT:-jpeg}"
  export WELLAPI_REQUEST_TIMEOUT="${WELLAPI_REQUEST_TIMEOUT:-300s}"
  export DREAMMAKER_REAL_CALLS_ENABLED=false

  export MUSIC_WORKFLOW_DISPATCH_MODE=outbox
  export WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true
  export WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal
  export WORKFLOW_OUTBOX_POLL_INTERVAL="${WORKFLOW_OUTBOX_POLL_INTERVAL:-1s}"
  export TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy
  export SONG_PRODUCTION_PARALLEL_MEDIA_ENABLED=true
  export SONG_PRODUCTION_MEDIA_PARALLELISM="${SONG_PRODUCTION_MEDIA_PARALLELISM:-2}"

  export RENDER_WORKER_MODE=album-ffmpeg
  export OBJECT_STORAGE_PROVIDER=s3
  export S3_ENDPOINT="${S3_ENDPOINT:-http://localhost:9000}"
  export S3_REGION="${S3_REGION:-us-east-1}"
  export S3_ACCESS_KEY="${S3_ACCESS_KEY:-minioadmin}"
  export S3_SECRET_KEY="${S3_SECRET_KEY:-minioadmin}"
  export S3_BUCKET_YANYUN_WORKS="${S3_BUCKET_YANYUN_WORKS:-yanyun-works-local}"
  export S3_PATH_STYLE_ENABLED="${S3_PATH_STYLE_ENABLED:-true}"
  export S3_AUTO_CREATE_BUCKET="${S3_AUTO_CREATE_BUCKET:-true}"
  export OBJECT_STORAGE_URL_TTL="${OBJECT_STORAGE_URL_TTL:-24h}"
  export RENDER_WORKER_WORKING_DIRECTORY="${RENDER_WORKER_WORKING_DIRECTORY:-apps/render-worker}"
  export RENDER_WORKER_COMMAND="${RENDER_WORKER_COMMAND:-npm}"
  export RENDER_WORKER_ARGUMENTS="${RENDER_WORKER_ARGUMENTS:-run,render:job,--}"
  export RENDER_WORKER_TIMEOUT="${RENDER_WORKER_TIMEOUT:-900s}"
  export LYRICS_EDIT_POLL_INTERVAL_MS="${LYRICS_EDIT_POLL_INTERVAL_MS:-2000}"
  export LYRICS_EDIT_BATCH_SIZE="${LYRICS_EDIT_BATCH_SIZE:-2}"
  export LYRICS_EDIT_LOCK_TIMEOUT="${LYRICS_EDIT_LOCK_TIMEOUT:-3m}"

  export COMPANY_ACCOUNT_ADAPTER_MODE=mock
  export COMPANY_MODERATION_ADAPTER_MODE=mock
  export COMPANY_QUOTA_ADAPTER_MODE=mock
  export COMPANY_PUBLISH_ADAPTER_MODE=mock
  export COMPANY_SHARE_ADAPTER_MODE=mock
}

require_env() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    fail "${name} is required"
  fi
}

build_jars() {
  if [ "${BUILD:-true}" = "false" ]; then
    return 0
  fi
  log "building music-api and music-worker boot jars"
  (cd "$ROOT_DIR" && ./gradlew --no-daemon :apps:music-api:bootJar :apps:music-worker:bootJar >/dev/null)
}

jar_path() {
  local app="$1"
  local jar
  jar="$(find "$ROOT_DIR/apps/${app}/build/libs" -maxdepth 1 -name '*.jar' ! -name '*plain.jar' | sort | tail -1)"
  [ -n "$jar" ] || fail "boot jar not found for ${app}; run with BUILD=true"
  printf '%s\n' "$jar"
}

start_service() {
  local label="$1"
  local pid_file="$2"
  local log_file="$3"
  local jar="$4"
  shift 4

  local ref
  ref="$(pid_from_file "$pid_file")"
  if ref_alive "$ref"; then
    fail "${label} is already running. ref=${ref}"
  fi

  mkdir -p "$RUN_DIR" "$LOG_DIR"
  if should_use_screen; then
    local session run_cmd quoted_log
    session="yanyun-public-real-${label}"
    printf -v run_cmd 'cd %q && exec %q -jar %q' "$ROOT_DIR" "$JAVA_HOME/bin/java" "$jar"
    printf -v quoted_log '%q' "$log_file"
    log "starting ${label} in screen session ${session}. log=${log_file}"
    "$@" screen -dmS "$session" bash -lc "${run_cmd} >${quoted_log} 2>&1"
    printf '%s\n' "$session" >"$pid_file"
  else
    log "starting ${label}. log=${log_file}"
    pushd "$ROOT_DIR" >/dev/null
    nohup "$@" "$JAVA_HOME/bin/java" -jar "$jar" >"$log_file" 2>&1 </dev/null &
    printf '%s\n' "$!" >"$pid_file"
    popd >/dev/null
  fi
}

should_use_screen() {
  if [ "${USE_SCREEN:-auto}" = "false" ]; then
    return 1
  fi
  if [ "${USE_SCREEN:-auto}" = "true" ]; then
    command -v screen >/dev/null 2>&1 || fail "USE_SCREEN=true but screen is not installed"
    return 0
  fi
  command -v screen >/dev/null 2>&1
}

start_stack() {
  load_env
  mkdir -p "$RUN_DIR" "$LOG_DIR"
  kill_known_process_on_port "$API_PORT" "music-api"
  kill_known_process_on_port "$WORKER_PORT" "music-worker"
  build_jars

  local api_jar worker_jar
  api_jar="$(jar_path music-api)"
  worker_jar="$(jar_path music-worker)"

  start_service "music-api" "$API_PID_FILE" "$API_LOG" "$api_jar" \
    env MUSIC_API_PORT="$API_PORT" LYRICS_EDIT_DISPATCHER_ENABLED=false
  start_service "music-worker" "$WORKER_PID_FILE" "$WORKER_LOG" "$worker_jar" \
    env MUSIC_WORKER_PORT="$WORKER_PORT" LYRICS_EDIT_DISPATCHER_ENABLED=true WORKFLOW_OUTBOX_DISPATCHER_ENABLED=false

  wait_for_http "$API_HEALTH_URL" "music-api"
  wait_for_http "$WORKER_HEALTH_URL" "music-worker"
  verify_readiness
  log "public real test backend stack is ready"
}

stop_service() {
  local label="$1"
  local pid_file="$2"
  local ref
  ref="$(pid_from_file "$pid_file")"
  if [ -n "$ref" ] && ! [[ "$ref" =~ ^[0-9]+$ ]]; then
    if screen_alive "$ref"; then
      log "stopping ${label} screen session ${ref}"
      screen -S "$ref" -X quit >/dev/null 2>&1 || true
    fi
  elif pid_alive "$ref"; then
    log "stopping ${label}. pid=${ref}"
    kill "$ref" >/dev/null 2>&1 || true
    local i
    for i in $(seq 1 30); do
      pid_alive "$ref" || break
      sleep 1
    done
    if pid_alive "$ref"; then
      log "force stopping ${label}. pid=${ref}"
      kill -9 "$ref" >/dev/null 2>&1 || true
    fi
  fi
  rm -f "$pid_file"
}

stop_stack() {
  stop_service "music-api" "$API_PID_FILE"
  stop_service "music-worker" "$WORKER_PID_FILE"
}

status_stack() {
  local api_ref worker_ref
  local api_alive api_health worker_alive worker_health
  api_ref="$(pid_from_file "$API_PID_FILE")"
  worker_ref="$(pid_from_file "$WORKER_PID_FILE")"
  api_alive="$(ref_alive "$api_ref" && printf yes || printf no)"
  api_health="$(curl -fsS "$API_HEALTH_URL" >/dev/null 2>&1 && printf yes || printf no)"
  worker_alive="$(ref_alive "$worker_ref" && printf yes || printf no)"
  worker_health="$(curl -fsS "$WORKER_HEALTH_URL" >/dev/null 2>&1 && printf yes || printf no)"
  printf 'music-api ref=%s alive=%s health=%s\n' "${api_ref:-none}" "$api_alive" "$api_health"
  printf 'music-worker ref=%s alive=%s health=%s\n' "${worker_ref:-none}" "$worker_alive" "$worker_health"
  if curl -fsS "$READINESS_URL" >/dev/null 2>&1; then
    local readiness
    readiness="$(curl -fsS "$READINESS_URL")"
    READINESS_JSON="$readiness" python3 - <<'PY'
import json, os
data = json.loads(os.environ["READINESS_JSON"])
components = {item["component"]: item for item in data.get("components", [])}
for name in [
    "lyrics_dispatcher",
    "media_parallelism",
    "music_provider",
    "image2_guard",
    "object_storage",
    "render_worker",
    "workflow_dispatch",
]:
    item = components.get(name, {})
    print(f'{name}: {item.get("configured_mode", "")} / {item.get("implementation", "")} / {item.get("status", "")}')
PY
  fi
  if [ "$api_alive" != "yes" ] || [ "$api_health" != "yes" ] || [ "$worker_alive" != "yes" ] || [ "$worker_health" != "yes" ]; then
    fail "public real test stack is not fully healthy"
  fi
}

verify_readiness() {
  local readiness
  readiness="$(curl -fsS "$READINESS_URL")"
  READINESS_JSON="$readiness" python3 - <<'PY'
import json, os
data = json.loads(os.environ["READINESS_JSON"])
components = {item["component"]: item for item in data.get("components", [])}

def require(component, expected_mode=None, expected_impl=None, expected_status="READY_FOR_LOCAL"):
    item = components.get(component)
    if not item:
        raise SystemExit(f"missing readiness component: {component}")
    mode = item.get("configured_mode", "")
    impl = item.get("implementation", "")
    status = item.get("status", "")
    if expected_mode and expected_mode not in mode:
        raise SystemExit(f"{component} configured_mode={mode!r}, expected contains {expected_mode!r}")
    if expected_impl and expected_impl not in impl:
        raise SystemExit(f"{component} implementation={impl!r}, expected contains {expected_impl!r}")
    if expected_status and status != expected_status:
        raise SystemExit(f"{component} status={status!r}, expected {expected_status!r}")

require("lyrics_dispatcher", "api-dispatcher-disabled-worker-expected", "LyricsEditJobDispatcher(worker)", None)
require("media_parallelism", "music-cover-parallel")
require("music_provider", "suno/yunwu", "YunwuSunoMusicProvider")
require("image2_guard", "real-calls-enabled/wellapi")
require("object_storage", "s3", "S3ObjectStorageClient")
require("render_worker", "album-ffmpeg")
require("workflow_dispatch", "outbox/temporal")
print("readiness verified")
PY
}

case "${1:-}" in
  start)
    start_stack
    ;;
  stop)
    stop_stack
    ;;
  restart)
    stop_stack
    start_stack
    ;;
  status)
    status_stack
    ;;
  -h|--help|help|"")
    usage
    ;;
  *)
    usage
    exit 2
    ;;
esac
