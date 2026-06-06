#!/usr/bin/env bash
set -euo pipefail
set +x

API_ROOT="${API_ROOT:-http://localhost:8080}"
API_BASE="${API_BASE:-$API_ROOT/api/v1}"
MOCK_USER="${MOCK_USER:-mock_user_001}"
IDEMPOTENCY_PREFIX="${IDEMPOTENCY_PREFIX:-deepseek-real-lyrics-$(date +%s)}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-yanyun-postgres}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_DB="${POSTGRES_DB:-yanyun_music}"

fail() {
  printf '[deepseek-smoke] ERROR: %s\n' "$*" >&2
  exit 1
}

log() {
  printf '[deepseek-smoke] %s\n' "$*"
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

require_env() {
  if [ -z "${!1:-}" ]; then
    fail "missing required environment variable: $1"
  fi
}

require_flag() {
  local name="$1"
  if [ "${!name:-false}" != "true" ]; then
    fail "$name must be true for this DeepSeek real smoke."
  fi
}

require_false() {
  local name="$1"
  if [ "${!name:-false}" = "true" ]; then
    fail "$name must remain false for this DeepSeek-only smoke."
  fi
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

if [ "${ALLOW_DEEPSEEK_REAL_SMOKE:-}" != "1" ]; then
  fail "refusing to run. Set ALLOW_DEEPSEEK_REAL_SMOKE=1 to confirm this real DeepSeek smoke."
fi

require_command curl
require_command jq
require_command docker

require_flag AGENT_REAL_CALLS_ENABLED
require_flag DEEPSEEK_REAL_CALLS_ENABLED
require_env DEEPSEEK_API_KEY

if [ "${MUSIC_PROVIDER:-mock}" != "mock" ]; then
  fail "MUSIC_PROVIDER must remain mock for this DeepSeek-only smoke."
fi
if [ "${IMAGE_PROVIDER:-mock}" != "mock" ]; then
  fail "IMAGE_PROVIDER must remain mock for this DeepSeek-only smoke."
fi
require_false DREAMMAKER_REAL_CALLS_ENABLED
require_false IMAGE_REAL_CALLS_ENABLED
require_false YUNWU_REAL_CALLS_ENABLED

for mode_var in \
  COMPANY_ACCOUNT_ADAPTER_MODE \
  COMPANY_MODERATION_ADAPTER_MODE \
  COMPANY_QUOTA_ADAPTER_MODE \
  COMPANY_PUBLISH_ADAPTER_MODE \
  COMPANY_SHARE_ADAPTER_MODE; do
  if [ "${!mode_var:-mock}" != "mock" ]; then
    fail "$mode_var must remain mock for this DeepSeek-only smoke."
  fi
done

log "running strict DeepSeek preflight"
TARGET=deepseek STRICT=true scripts/smoke/real-model-readiness-preflight.sh >/dev/null

log "checking API health and integration readiness"
curl -fsS "$API_ROOT/health" >/dev/null
READINESS="$(curl -fsS "$API_ROOT/internal/integration-readiness")"

DEEPSEEK_STATUS="$(echo "$READINESS" | jq -r '.components[] | select(.component == "deepseek_guard") | .status')"
DEEPSEEK_MODE="$(echo "$READINESS" | jq -r '.components[] | select(.component == "deepseek_guard") | .configured_mode')"
DEEPSEEK_IMPL="$(echo "$READINESS" | jq -r '.components[] | select(.component == "deepseek_guard") | .implementation')"
MUSIC_MODE="$(echo "$READINESS" | jq -r '.components[] | select(.component == "music_provider") | .configured_mode')"
IMAGE2_MODE="$(echo "$READINESS" | jq -r '.components[] | select(.component == "image2_guard") | .configured_mode')"

if [ "$DEEPSEEK_STATUS" != "READY_FOR_LOCAL" ] \
  || [ "$DEEPSEEK_MODE" != "real-calls-enabled" ] \
  || [ "$DEEPSEEK_IMPL" != "RealDeepSeekLyricsClient" ]; then
  echo "$READINESS" | jq '.components[] | select(.component == "deepseek_guard")' >&2
  fail "deepseek_guard is not READY_FOR_LOCAL real-calls-enabled RealDeepSeekLyricsClient"
fi

if [ "$MUSIC_MODE" != "mock" ]; then
  fail "music_provider must remain mock, got: $MUSIC_MODE"
fi

case "$IMAGE2_MODE" in
  mock|real-calls-disabled/*) ;;
  *)
    fail "image2_guard must remain mock or disabled, got: $IMAGE2_MODE"
    ;;
esac

log "creating one inspiration work for real DeepSeek lyrics smoke"
CREATE_RESPONSE="$(
  post_json "/works/inspiration" "$IDEMPOTENCY_PREFIX-create" '{
    "story_input": "雁门关外初雪落下，少年侠客在长亭听见故人笛声，想写一首温柔但有力量的燕云主题原创歌。",
    "mood": "温柔、坚定、宿命感",
    "scene": "边塞长亭、初雪、旧友重逢",
    "music_style": "国风民谣，笛子，古筝，温暖女声"
  }'
)"

WORK_ID="$(echo "$CREATE_RESPONSE" | jq -r '.work_id // empty')"
STATUS="$(echo "$CREATE_RESPONSE" | jq -r '.status // empty')"
STAGE="$(echo "$CREATE_RESPONSE" | jq -r '.generation_stage // empty')"
JOB_ID="$(echo "$CREATE_RESPONSE" | jq -r '.job_id // empty')"
REQUEST_ID="$(echo "$CREATE_RESPONSE" | jq -r '.error.request_id // empty')"

if [ -z "$WORK_ID" ]; then
  echo "$CREATE_RESPONSE" | jq '{status, generation_stage, failure, error: {code: .error.code, request_id: .error.request_id}}' >&2
  fail "create work did not return work_id request_id=${REQUEST_ID:-none}"
fi

log "work_id=$WORK_ID status=$STATUS stage=$STAGE job_id=${JOB_ID:-none}"

DETAIL_RESPONSE="$(get_json "/works/$WORK_ID")"
DETAIL_STATUS="$(echo "$DETAIL_RESPONSE" | jq -r '.status')"
DETAIL_STAGE="$(echo "$DETAIL_RESPONSE" | jq -r '.generation_stage')"
LYRICS_DRAFT_ID="$(echo "$DETAIL_RESPONSE" | jq -r '.lyrics_draft.lyrics_draft_id // empty')"
FAILURE_CODE="$(echo "$DETAIL_RESPONSE" | jq -r '.failure.failure_code // empty')"

log "detail status=$DETAIL_STATUS stage=$DETAIL_STAGE draft=${LYRICS_DRAFT_ID:-empty} failure=${FAILURE_CODE:-none}"

if [ "$DETAIL_STATUS" != "LYRICS_READY" ] || [ "$DETAIL_STAGE" != "WAITING_CONFIRM" ]; then
  echo "$DETAIL_RESPONSE" | jq '{work_id, status, generation_stage, failure}' >&2
  fail "work did not reach LYRICS_READY / WAITING_CONFIRM"
fi

if [ -z "$LYRICS_DRAFT_ID" ]; then
  fail "lyrics draft was not ready"
fi

log "checking agent_runs summary"
AGENT_ROW="$(
  docker exec "$POSTGRES_CONTAINER" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc \
    "select agent_name, operation, model_name, status, input_hash is not null, output_hash is not null, coalesce(error_code,''), coalesce(latency_ms,0) from agent_runs where work_id = '$WORK_ID'::uuid and agent_name = 'LyricsAgent' order by started_at desc limit 1;"
)"

if [ -z "$AGENT_ROW" ]; then
  fail "LyricsAgent agent_runs row missing for work_id=$WORK_ID"
fi

IFS='|' read -r AGENT_NAME OPERATION MODEL_NAME AGENT_STATUS INPUT_HASH_PRESENT OUTPUT_HASH_PRESENT ERROR_CODE LATENCY_MS <<<"$AGENT_ROW"

if [ "$AGENT_NAME" != "LyricsAgent" ] || [ "$AGENT_STATUS" != "SUCCEEDED" ]; then
  fail "LyricsAgent row did not succeed: agent=$AGENT_NAME status=$AGENT_STATUS error=${ERROR_CODE:-none}"
fi

if [ "$MODEL_NAME" = "mock-deepseek-lyrics" ] || [ -z "$MODEL_NAME" ]; then
  fail "LyricsAgent model_name does not prove real DeepSeek: $MODEL_NAME"
fi

if [ "$INPUT_HASH_PRESENT" != "t" ] || [ "$OUTPUT_HASH_PRESENT" != "t" ]; then
  fail "LyricsAgent row missing input/output hashes"
fi

log "agent=LyricsAgent operation=$OPERATION model=$MODEL_NAME status=$AGENT_STATUS input_hash=$INPUT_HASH_PRESENT output_hash=$OUTPUT_HASH_PRESENT latency_ms=$LATENCY_MS"
log "PASS real DeepSeek lyrics smoke. work_id=$WORK_ID"
