#!/usr/bin/env bash
set -euo pipefail
set +x

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

API_ROOT="${API_ROOT:-http://localhost:8080}"
API_ROOT="${API_ROOT%/}"
API_BASE="${API_BASE:-$API_ROOT/api/v1}"
API_PORT="${API_PORT:-}"
WORKER_ROOT="${WORKER_ROOT:-http://localhost:8081}"
WORKER_PORT="${WORKER_PORT:-8081}"
FRONTEND_HOST="${FRONTEND_HOST:-127.0.0.1}"
FRONTEND_PORT="${FRONTEND_PORT:-5274}"
FRONTEND_URL="http://${FRONTEND_HOST}:${FRONTEND_PORT}"
MOCK_USER="${MOCK_USER:-mock_user_001}"
START_TIMEOUT_SECONDS="${START_TIMEOUT_SECONDS:-120}"
STOP_TIMEOUT_SECONDS="${STOP_TIMEOUT_SECONDS:-60}"
MAX_POLL_ATTEMPTS="${MAX_POLL_ATTEMPTS:-180}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-5}"
SMOKE_TIMEOUT_MS="${SMOKE_TIMEOUT_MS:-30000}"
MAX_MUSIC_RETRY_ATTEMPTS="${MAX_MUSIC_RETRY_ATTEMPTS:-2}"
IDEMPOTENCY_PREFIX="${IDEMPOTENCY_PREFIX:-public-real-full-$(date +%s)}"
LOG_DIR="${LOG_DIR:-${REPO_ROOT}/build/smoke/public-real-full-experience-$(date +%Y%m%d%H%M%S)}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-yanyun-postgres}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_DB="${POSTGRES_DB:-yanyun_music}"

API_PID=""
WORKER_PID=""
FRONTEND_PID=""
API_LOG=""
WORKER_LOG=""
FRONTEND_LOG=""

fail() {
  printf '[public-real-full] ERROR: %s\n' "$*" >&2
  print_logs_hint >&2
  exit 1
}

log() {
  printf '[public-real-full] %s\n' "$*"
}

need_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

print_logs_hint() {
  if [ -n "${WORKER_LOG:-}" ] || [ -n "${API_LOG:-}" ] || [ -n "${FRONTEND_LOG:-}" ]; then
    printf '[public-real-full] logs:\n'
    [ -n "${WORKER_LOG:-}" ] && printf '  worker:   %s\n' "$WORKER_LOG"
    [ -n "${API_LOG:-}" ] && printf '  api:      %s\n' "$API_LOG"
    [ -n "${FRONTEND_LOG:-}" ] && printf '  frontend: %s\n' "$FRONTEND_LOG"
  fi
}

cleanup() {
  set +e
  stop_process "$FRONTEND_PID" "frontend"
  stop_process "$API_PID" "API"
  stop_process "$WORKER_PID" "worker"
  unset DEEPSEEK_API_KEY
  unset YUNWU_API_KEY
  unset WELLAPI_API_KEY
}

trap cleanup EXIT INT TERM

stop_process() {
  local pid="$1"
  local name="$2"
  if [ -n "$pid" ] && kill -0 "$pid" >/dev/null 2>&1; then
    log "stopping $name pid=$pid"
    kill "$pid" >/dev/null 2>&1 || true
    wait "$pid" >/dev/null 2>&1 || true
  fi
}

derive_api_port() {
  if [ -n "$API_PORT" ]; then
    return
  fi
  local host_part
  host_part="${API_ROOT#http://}"
  host_part="${host_part#https://}"
  host_part="${host_part%%/*}"
  if [[ "$host_part" == *:* ]]; then
    API_PORT="${host_part##*:}"
  else
    case "$API_ROOT" in
      https://*) API_PORT="443" ;;
      *) API_PORT="80" ;;
    esac
  fi
}

require_real_confirmation() {
  if [ "${ALLOW_REAL_MODEL_SMOKE:-}" != "1" ]; then
    fail "refusing to run real-model smoke; set ALLOW_REAL_MODEL_SMOKE=1"
  fi
  if [ "${ALLOW_PUBLIC_REAL_FULL_EXPERIENCE:-}" != "1" ]; then
    fail "refusing to run public full experience; set ALLOW_PUBLIC_REAL_FULL_EXPERIENCE=1"
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
    fail "$var_name is required; export it in this shell or run interactively"
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

assert_dependencies() {
  [ -d "${REPO_ROOT}/apps/render-worker/node_modules" ] \
    || fail "apps/render-worker/node_modules is missing; run cd apps/render-worker && npm install"
  [ -x "${REPO_ROOT}/apps/render-worker/node_modules/.bin/remotion" ] \
    || fail "render-worker Remotion binary is missing; run cd apps/render-worker && npm install"
  [ -d "${REPO_ROOT}/prototypes/Claude-web-v1/node_modules" ] \
    || fail "prototypes/Claude-web-v1/node_modules is missing; run cd prototypes/Claude-web-v1 && npm install"
  [ -x "${REPO_ROOT}/prototypes/Claude-web-v1/node_modules/.bin/vite" ] \
    || fail "Claude Web v1 Vite binary is missing; run cd prototypes/Claude-web-v1 && npm install"
}

assert_port_free() {
  local port="$1"
  local label="$2"
  if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    fail "$label port $port is already in use; stop existing service before running this smoke"
  fi
}

wait_for_json_health() {
  local name="$1"
  local url="$2"
  local deadline=$((SECONDS + START_TIMEOUT_SECONDS))
  while [ "$SECONDS" -lt "$deadline" ]; do
    if curl -fsS "$url" 2>/dev/null | jq -e '.status == "OK" or .status == "UP"' >/dev/null 2>&1; then
      log "$name is healthy: $url"
      return
    fi
    sleep 2
  done
  fail "$name did not become healthy within ${START_TIMEOUT_SECONDS}s: $url"
}

wait_for_http_ok() {
  local name="$1"
  local url="$2"
  local deadline=$((SECONDS + START_TIMEOUT_SECONDS))
  while [ "$SECONDS" -lt "$deadline" ]; do
    if curl -fsS "$url" >/dev/null 2>&1; then
      log "$name is reachable: $url"
      return
    fi
    sleep 1
  done
  fail "$name did not become reachable within ${START_TIMEOUT_SECONDS}s: $url"
}

post_json() {
  local path="$1"
  local key="$2"
  local body="$3"
  curl -fsS -X POST "$API_BASE$path" \
    -H "Content-Type: application/json" \
    -H "X-Mock-User-Id: $MOCK_USER" \
    -H "Idempotency-Key: $key" \
    -d "$body"
}

get_json() {
  local path="$1"
  curl -fsS "$API_BASE$path" -H "X-Mock-User-Id: $MOCK_USER"
}

run_preflights() {
  log "running strict preflight for public real full experience"
  TARGET=public-real-full-experience STRICT=true scripts/smoke/real-model-readiness-preflight.sh
  TARGET=deepseek STRICT=true scripts/smoke/real-model-readiness-preflight.sh
  TARGET=yunwu-suno STRICT=true scripts/smoke/real-model-readiness-preflight.sh
  TARGET=wellapi-image2 STRICT=true scripts/smoke/real-model-readiness-preflight.sh
}

start_worker() {
  WORKER_LOG="${LOG_DIR}/music-worker.log"
  log "starting music-worker; log=$WORKER_LOG"
  (
    cd "$REPO_ROOT"
    export MUSIC_PROVIDER=suno
    export SUNO_BACKEND=yunwu
    export YUNWU_BASE_URL="${YUNWU_BASE_URL:-https://yunwu.ai}"
    export YUNWU_REAL_CALLS_ENABLED=true
    export YUNWU_SUNO_MODEL="${YUNWU_SUNO_MODEL:-chirp-v5}"
    export YUNWU_REQUEST_TIMEOUT="${YUNWU_REQUEST_TIMEOUT:-90s}"
    export IMAGE_PROVIDER=image2
    export IMAGE2_BACKEND=wellapi
    export IMAGE_REAL_CALLS_ENABLED=true
    export WELLAPI_BASE_URL="${WELLAPI_BASE_URL:-https://wellapi.ai}"
    export IMAGE2_MODEL_NAME="${IMAGE2_MODEL_NAME:-gpt-image-2}"
    export IMAGE2_SIZE="${IMAGE2_SIZE:-2048x1152}"
    export IMAGE2_QUALITY="${IMAGE2_QUALITY:-medium}"
    export IMAGE2_OUTPUT_FORMAT="${IMAGE2_OUTPUT_FORMAT:-jpeg}"
    export WELLAPI_REQUEST_TIMEOUT="${WELLAPI_REQUEST_TIMEOUT:-180s}"
    export AGENT_REAL_CALLS_ENABLED=false
    export DEEPSEEK_REAL_CALLS_ENABLED=false
    export DREAMMAKER_REAL_CALLS_ENABLED=false
    export TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy
    export RENDER_WORKER_MODE=local-process
    export RENDER_WORKER_WORKING_DIRECTORY="${RENDER_WORKER_WORKING_DIRECTORY:-apps/render-worker}"
    export RENDER_WORKER_COMMAND="${RENDER_WORKER_COMMAND:-npm}"
    export RENDER_WORKER_ARGUMENTS="${RENDER_WORKER_ARGUMENTS:-run,render:job,--}"
    export RENDER_WORKER_TIMEOUT="${RENDER_WORKER_TIMEOUT:-900s}"
    export COMPANY_ACCOUNT_ADAPTER_MODE=mock
    export COMPANY_MODERATION_ADAPTER_MODE=mock
    export COMPANY_QUOTA_ADAPTER_MODE=mock
    export COMPANY_PUBLISH_ADAPTER_MODE=mock
    export COMPANY_SHARE_ADAPTER_MODE=mock
    if [ -n "${JAVA_HOME:-}" ]; then export JAVA_HOME; fi
    export PATH
    ./gradlew --no-daemon :apps:music-worker:bootRun
  ) >"$WORKER_LOG" 2>&1 &
  WORKER_PID="$!"
  wait_for_json_health "worker" "$WORKER_ROOT/actuator/health"
}

start_api() {
  API_LOG="${LOG_DIR}/music-api.log"
  log "starting music-api; log=$API_LOG"
  (
    cd "$REPO_ROOT"
    export MUSIC_API_PORT="$API_PORT"
    export AGENT_REAL_CALLS_ENABLED=true
    export DEEPSEEK_REAL_CALLS_ENABLED=true
    export DEEPSEEK_BASE_URL="${DEEPSEEK_BASE_URL:-https://api.deepseek.com}"
    export DEEPSEEK_MODEL_NAME="${DEEPSEEK_MODEL_NAME:-deepseek-v4-pro}"
    export DEEPSEEK_TIMEOUT_MS="${DEEPSEEK_TIMEOUT_MS:-30000}"
    export DEEPSEEK_MAX_ATTEMPTS="${DEEPSEEK_MAX_ATTEMPTS:-1}"
    export DEEPSEEK_RESPONSE_MAX_TOKENS="${DEEPSEEK_RESPONSE_MAX_TOKENS:-1800}"
    export DEEPSEEK_TEMPERATURE="${DEEPSEEK_TEMPERATURE:-0.7}"
    export MUSIC_PROVIDER=suno
    export SUNO_BACKEND=yunwu
    export YUNWU_BASE_URL="${YUNWU_BASE_URL:-https://yunwu.ai}"
    export YUNWU_REAL_CALLS_ENABLED=true
    export YUNWU_SUNO_MODEL="${YUNWU_SUNO_MODEL:-chirp-v5}"
    export YUNWU_REQUEST_TIMEOUT="${YUNWU_REQUEST_TIMEOUT:-90s}"
    export IMAGE_PROVIDER=image2
    export IMAGE2_BACKEND=wellapi
    export IMAGE_REAL_CALLS_ENABLED=true
    export WELLAPI_BASE_URL="${WELLAPI_BASE_URL:-https://wellapi.ai}"
    export IMAGE2_MODEL_NAME="${IMAGE2_MODEL_NAME:-gpt-image-2}"
    export IMAGE2_SIZE="${IMAGE2_SIZE:-2048x1152}"
    export IMAGE2_QUALITY="${IMAGE2_QUALITY:-medium}"
    export IMAGE2_OUTPUT_FORMAT="${IMAGE2_OUTPUT_FORMAT:-jpeg}"
    export WELLAPI_REQUEST_TIMEOUT="${WELLAPI_REQUEST_TIMEOUT:-180s}"
    export DREAMMAKER_REAL_CALLS_ENABLED=false
    export MUSIC_WORKFLOW_DISPATCH_MODE=outbox
    export WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true
    export WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal
    export WORKFLOW_OUTBOX_POLL_INTERVAL="${WORKFLOW_OUTBOX_POLL_INTERVAL:-1s}"
    export RENDER_WORKER_MODE=local-process
    export RENDER_WORKER_WORKING_DIRECTORY="${RENDER_WORKER_WORKING_DIRECTORY:-apps/render-worker}"
    export RENDER_WORKER_COMMAND="${RENDER_WORKER_COMMAND:-npm}"
    export RENDER_WORKER_ARGUMENTS="${RENDER_WORKER_ARGUMENTS:-run,render:job,--}"
    export RENDER_WORKER_TIMEOUT="${RENDER_WORKER_TIMEOUT:-900s}"
    export COMPANY_ACCOUNT_ADAPTER_MODE=mock
    export COMPANY_MODERATION_ADAPTER_MODE=mock
    export COMPANY_QUOTA_ADAPTER_MODE=mock
    export COMPANY_PUBLISH_ADAPTER_MODE=mock
    export COMPANY_SHARE_ADAPTER_MODE=mock
    if [ -n "${JAVA_HOME:-}" ]; then export JAVA_HOME; fi
    export PATH
    ./gradlew --no-daemon :apps:music-api:bootRun
  ) >"$API_LOG" 2>&1 &
  API_PID="$!"
  wait_for_json_health "api" "$API_ROOT/health"
}

check_api_readiness() {
  log "checking API integration readiness"
  TARGET=deepseek STRICT=true CHECK_API=true API_ROOT="$API_ROOT" scripts/smoke/real-model-readiness-preflight.sh >/dev/null
  TARGET=yunwu-suno STRICT=true CHECK_API=true API_ROOT="$API_ROOT" scripts/smoke/real-model-readiness-preflight.sh >/dev/null
  TARGET=wellapi-image2 STRICT=true CHECK_API=true API_ROOT="$API_ROOT" scripts/smoke/real-model-readiness-preflight.sh >/dev/null
}

create_real_work() {
  log "creating one inspiration work with real DeepSeek lyrics"
  CREATE_RESPONSE="$(
    post_json "/works/inspiration" "$IDEMPOTENCY_PREFIX-create" '{
      "story_input": "雁门关外风雪夜，少年与旧友重逢，想把江湖往事唱成一首温柔但有边塞鼓点的歌。",
      "mood": "温柔、坚定、宿命感",
      "scene": "边塞长亭、初雪、旧友重逢",
      "music_style": "国风民谣，笛子，古筝，温暖女声"
    }'
  )"
  WORK_ID="$(echo "$CREATE_RESPONSE" | jq -r '.work_id // empty')"
  if [ -z "$WORK_ID" ]; then
    echo "$CREATE_RESPONSE" | jq '{status, generation_stage, failure, error: {code: .error.code, request_id: .error.request_id}}' >&2
    fail "create work did not return work_id"
  fi
  log "work_id=$WORK_ID"
}

wait_for_lyrics() {
  local detail status stage failure
  LYRICS_DRAFT_ID=""
  for _ in $(seq 1 40); do
    detail="$(get_json "/works/$WORK_ID")"
    status="$(echo "$detail" | jq -r '.status')"
    stage="$(echo "$detail" | jq -r '.generation_stage')"
    failure="$(echo "$detail" | jq -r '.failure.failure_code // empty')"
    LYRICS_DRAFT_ID="$(echo "$detail" | jq -r '.lyrics_draft.lyrics_draft_id // empty')"
    log "$(date '+%H:%M:%S') lyrics status=$status stage=$stage draft=${LYRICS_DRAFT_ID:-empty} failure=${failure:-none}"
    if [ -n "$LYRICS_DRAFT_ID" ] && [ "$status" = "LYRICS_READY" ]; then
      return
    fi
    if [ "$status" = "FAILED" ] || [ "$status" = "LYRICS_FAILED" ]; then
      echo "$detail" | jq '{work_id, status, generation_stage, failure}' >&2
      fail "lyrics stage failed"
    fi
    sleep 2
  done
  fail "lyrics draft was not ready in time"
}

confirm_real_music() {
  log "confirming work with real Yunwu Suno music and real WellAPI Image2 cover"
  CONFIRM_RESPONSE="$(
    post_json "/works/$WORK_ID/confirm" "$IDEMPOTENCY_PREFIX-confirm" "{
      \"lyrics_draft_id\": \"$LYRICS_DRAFT_ID\",
      \"music_provider\": \"suno\"
    }"
  )"
  echo "$CONFIRM_RESPONSE" | jq '{work_id, status, generation_stage, package_status, job_id}'
}

can_retry_real_music() {
  local detail="$1"
  echo "$detail" \
    | jq -e '.status == "FAILED" and .failure.failure_code == "MUSIC_GENERATION_FAILED" and ((.available_actions // []) | index("RETRY_MUSIC") != null)' \
    >/dev/null
}

retry_real_music() {
  local attempt="$1" retry_response
  log "retrying real Yunwu Suno music generation via product retry action attempt=$attempt"
  retry_response="$(
    post_json "/works/$WORK_ID/music/retry" "$IDEMPOTENCY_PREFIX-music-retry-$attempt" '{
      "music_provider": "suno"
    }'
  )"
  echo "$retry_response" | jq '{work_id, status, generation_stage, package_status, job_id, available_actions}'
}

wait_for_package_ready() {
  local detail status stage package_status failure music_retry_attempts
  FINAL_DETAIL=""
  music_retry_attempts=0
  for _ in $(seq 1 "$MAX_POLL_ATTEMPTS"); do
    detail="$(get_json "/works/$WORK_ID")"
    status="$(echo "$detail" | jq -r '.status')"
    stage="$(echo "$detail" | jq -r '.generation_stage')"
    package_status="$(echo "$detail" | jq -r '.package_status')"
    failure="$(echo "$detail" | jq -r '.failure.failure_code // empty')"
    log "$(date '+%H:%M:%S') status=$status stage=$stage package=$package_status failure=${failure:-none}"
    if [ "$status" = "GENERATED" ]; then
      FINAL_DETAIL="$detail"
      break
    fi
    if [ "$status" = "FAILED" ] && can_retry_real_music "$detail" && [ "$music_retry_attempts" -lt "$MAX_MUSIC_RETRY_ATTEMPTS" ]; then
      music_retry_attempts=$((music_retry_attempts + 1))
      retry_real_music "$music_retry_attempts"
      sleep "$POLL_INTERVAL_SECONDS"
      continue
    fi
    if [ "$status" = "FAILED" ] || [ "$status" = "LYRICS_FAILED" ]; then
      FINAL_DETAIL="$detail"
      break
    fi
    sleep "$POLL_INTERVAL_SECONDS"
  done

  if [ -z "$FINAL_DETAIL" ]; then
    fail "public real full experience timed out. work_id=$WORK_ID"
  fi

  echo "$FINAL_DETAIL" | jq '{
    work_id,
    status,
    generation_stage,
    package_status,
    failure,
    media_assets_present: (.media_assets != null),
    media_url_present: {
      audio: (((.media_assets.audio_url? // "") | length) > 0),
      cover: (((.media_assets.cover_url? // "") | length) > 0),
      video: (((.media_assets.video_url? // "") | length) > 0)
    }
  }'

  FINAL_STATUS="$(echo "$FINAL_DETAIL" | jq -r '.status')"
  PACKAGE_STATUS="$(echo "$FINAL_DETAIL" | jq -r '.package_status')"
  if [ "$FINAL_STATUS" != "GENERATED" ] || [ "$PACKAGE_STATUS" != "PACKAGE_READY" ]; then
    fail "work did not reach GENERATED / PACKAGE_READY. work_id=$WORK_ID"
  fi
}

verify_sanitized_db_evidence() {
  if ! command -v docker >/dev/null 2>&1; then
    log "docker missing; skipping database evidence summaries"
    return
  fi

  log "agent_runs summary"
  docker exec "$POSTGRES_CONTAINER" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc \
    "select agent_name, operation, model_name, status, input_hash is not null, output_hash is not null, coalesce(failure_code,''), coalesce(latency_ms,0) from agent_runs where work_id = '$WORK_ID'::uuid order by created_at;" || true

  log "provider_calls summary"
  docker exec "$POSTGRES_CONTAINER" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc \
    "select provider, model_name, case when provider_trace_id is null then '<empty>' else '<present>' end, status, coalesce(error_code,'') from provider_calls where work_id = '$WORK_ID'::uuid order by created_at;" || true

  log "media_assets summary"
  docker exec "$POSTGRES_CONTAINER" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc \
    "select asset_type, coalesce(metadata_json->>'provider',''), case when object_key is null then '<empty>' else '<present>' end, mime_type, coalesce(width,0), coalesce(height,0) from media_assets where work_id = '$WORK_ID'::uuid order by asset_type;" || true
}

fetch_publish_package() {
  log "fetching publish package"
  PACKAGE_RESPONSE="$(get_json "/works/$WORK_ID/publish-package")"
  echo "$PACKAGE_RESPONSE" | jq '{
    work_id,
    package_status,
    package_url_present: (((.package_url // "") | length) > 0),
    expires_at,
    package_json_url_present: {
      audio: (((.package_json.audio.url? // "") | length) > 0),
      cover: (((.package_json.cover.url? // "") | length) > 0),
      video: (((.package_json.video.url? // "") | length) > 0),
      timeline: (((.package_json.lyrics.timeline_url? // "") | length) > 0)
    }
  }'
}

start_frontend() {
  FRONTEND_LOG="${LOG_DIR}/claude-web-v1.log"
  log "starting Claude Web v1; log=$FRONTEND_LOG"
  (
    cd "${REPO_ROOT}/prototypes/Claude-web-v1"
    VITE_API_PROXY_TARGET="$API_ROOT" \
      node_modules/.bin/vite --host "$FRONTEND_HOST" --port "$FRONTEND_PORT" --strictPort
  ) >"$FRONTEND_LOG" 2>&1 &
  FRONTEND_PID="$!"
  wait_for_http_ok "frontend" "$FRONTEND_URL"
}

run_frontend_handoff_check() {
  log "verifying Claude Web v1 finished page and handoff actions"
  (
    cd "${REPO_ROOT}/prototypes/Claude-web-v1"
    FRONTEND_URL="$FRONTEND_URL" \
    WORK_ID="$WORK_ID" \
    SMOKE_TIMEOUT_MS="$SMOKE_TIMEOUT_MS" \
    HEADLESS="${HEADLESS:-true}" \
      node <<'NODE'
import { chromium } from 'playwright';

const frontendUrl = process.env.FRONTEND_URL;
const workId = process.env.WORK_ID;
const timeout = Number(process.env.SMOKE_TIMEOUT_MS ?? 30000);
const headless = process.env.HEADLESS !== 'false';

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

async function waitForAnyText(page, texts, label) {
  const deadline = Date.now() + timeout;
  let lastError;
  while (Date.now() < deadline) {
    for (const text of texts) {
      try {
        await page.getByText(text).first().waitFor({ state: 'visible', timeout: 1000 });
        console.log(`[public-real-ui] verified ${label}: ${text}`);
        return text;
      } catch (error) {
        lastError = error;
      }
    }
  }
  throw new Error(`missing ${label}: ${lastError?.message ?? 'timeout'}`);
}

async function clickIfVisible(page, name) {
  const button = page.getByRole('button', { name });
  if ((await button.count()) === 0) return false;
  if (!(await button.first().isVisible())) return false;
  if (await button.first().isDisabled()) return false;
  await button.first().click();
  console.log(`[public-real-ui] clicked ${name}`);
  return true;
}

const browser = await chromium.launch({ headless });
try {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  const httpErrors = [];
  const consoleErrors = [];
  page.on('response', (response) => {
    if (response.status() >= 400) httpErrors.push({ status: response.status(), url: response.url() });
  });
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text());
  });

  await page.goto(`${frontendUrl}/#/work/${workId}`, { waitUntil: 'networkidle' });
  await waitForAnyText(page, ['交给社区发布', '作品已交接给社区发布流程。'], 'finished handoff page');
  await waitForAnyText(page, ['交接下载链接'], 'handoff URL');
  await waitForAnyText(page, ['音频地址'], 'audio URL');
  await waitForAnyText(page, ['封面地址'], 'cover URL');
  await waitForAnyText(page, ['视频地址'], 'video URL');

  const overflow = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    viewportWidth: window.innerWidth,
  }));
  assert(overflow.scrollWidth <= overflow.viewportWidth + 1, `mobile overflow: ${JSON.stringify(overflow)}`);

  await clickIfVisible(page, '刷新下载链接');
  await waitForAnyText(page, ['交接下载链接'], 'refreshed handoff URL');
  await clickIfVisible(page, '标记已交接');
  await waitForAnyText(page, ['作品已交接给社区发布流程。'], 'package fetched state');

  await page.setViewportSize({ width: 1440, height: 900 });
  const desktopOverflow = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    viewportWidth: window.innerWidth,
  }));
  assert(desktopOverflow.scrollWidth <= desktopOverflow.viewportWidth + 1, `desktop overflow: ${JSON.stringify(desktopOverflow)}`);

  assert(httpErrors.length === 0, `unexpected HTTP errors: ${JSON.stringify(httpErrors)}`);
  assert(consoleErrors.length === 0, `unexpected console errors: ${JSON.stringify(consoleErrors)}`);
  console.log(`[public-real-ui] PASS work_id=${workId}`);
} finally {
  await browser.close();
}
NODE
  )
}

verify_package_fetched() {
  local detail package_status
  detail="$(get_json "/works/$WORK_ID")"
  package_status="$(echo "$detail" | jq -r '.package_status')"
  if [ "$package_status" != "PACKAGE_FETCHED" ]; then
    echo "$detail" | jq '{work_id, status, generation_stage, package_status, failure}' >&2
    fail "frontend handoff did not mark package fetched"
  fi
  log "PASS public real full experience. work_id=$WORK_ID"
}

main() {
  require_real_confirmation
  need_command curl
  need_command jq
  need_command lsof
  need_command docker
  need_command ffprobe
  ensure_java_home
  derive_api_port
  assert_dependencies

  read_secret_if_needed DEEPSEEK_API_KEY "DEEPSEEK_API_KEY: "
  read_secret_if_needed YUNWU_API_KEY "YUNWU_API_KEY: "
  read_secret_if_needed WELLAPI_API_KEY "WELLAPI_API_KEY: "

  export AGENT_REAL_CALLS_ENABLED=true
  export DEEPSEEK_REAL_CALLS_ENABLED=true
  export DEEPSEEK_BASE_URL="${DEEPSEEK_BASE_URL:-https://api.deepseek.com}"
  export DEEPSEEK_MODEL_NAME="${DEEPSEEK_MODEL_NAME:-deepseek-v4-pro}"
  export SUNO_BACKEND=yunwu
  export YUNWU_BASE_URL="${YUNWU_BASE_URL:-https://yunwu.ai}"
  export YUNWU_REAL_CALLS_ENABLED=true
  export YUNWU_SUNO_MODEL="${YUNWU_SUNO_MODEL:-chirp-v5}"
  export YUNWU_REQUEST_TIMEOUT="${YUNWU_REQUEST_TIMEOUT:-90s}"
  export IMAGE_PROVIDER=image2
  export IMAGE2_BACKEND=wellapi
  export IMAGE_REAL_CALLS_ENABLED=true
  export WELLAPI_BASE_URL="${WELLAPI_BASE_URL:-https://wellapi.ai}"
  export IMAGE2_MODEL_NAME="${IMAGE2_MODEL_NAME:-gpt-image-2}"
  export MUSIC_WORKFLOW_DISPATCH_MODE=outbox
  export WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal
  export WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true
  export TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy
  export RENDER_WORKER_MODE=local-process

  mkdir -p "$LOG_DIR"
  log "logs will be written under $LOG_DIR"
  log "DreamMaker remains the production target; this smoke uses Yunwu/WellAPI only for public-network validation"

  assert_port_free "$API_PORT" "API"
  assert_port_free "$WORKER_PORT" "worker"
  assert_port_free "$FRONTEND_PORT" "frontend"

  run_preflights
  start_worker
  start_api
  check_api_readiness
  create_real_work
  wait_for_lyrics
  confirm_real_music
  wait_for_package_ready
  verify_sanitized_db_evidence
  fetch_publish_package
  start_frontend
  run_frontend_handoff_check
  verify_package_fetched
}

main "$@"
