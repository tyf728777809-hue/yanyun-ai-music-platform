#!/usr/bin/env bash
set -euo pipefail
set +x

API_ROOT="${API_ROOT:-http://localhost:8080}"
API_BASE="${API_BASE:-$API_ROOT/api/v1}"
MOCK_USER="${MOCK_USER:-mock_user_001}"
MAX_POLL_ATTEMPTS="${MAX_POLL_ATTEMPTS:-60}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-2}"
IDEMPOTENCY_PREFIX="${IDEMPOTENCY_PREFIX:-dreammaker-image2-real-cover-$(date +%s)}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-yanyun-postgres}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_DB="${POSTGRES_DB:-yanyun_music}"

fail() {
  printf '[dreammaker-image2-smoke] ERROR: %s\n' "$*" >&2
  exit 1
}

log() {
  printf '[dreammaker-image2-smoke] %s\n' "$*"
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

require_env() {
  if [ -z "${!1:-}" ]; then
    fail "missing required environment variable: $1"
  fi
}

require_true() {
  local name="$1"
  if [ "${!name:-false}" != "true" ]; then
    fail "$name must be true for this DreamMaker Image2 smoke."
  fi
}

require_false() {
  local name="$1"
  if [ "${!name:-false}" = "true" ]; then
    fail "$name must remain false for this Image2-only smoke."
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

require_command curl
require_command jq
require_command docker

if [ "${ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE:-}" != "1" ]; then
  fail "refusing to run. Set ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1 to confirm this real-provider smoke."
fi

if [ "${IMAGE_PROVIDER:-mock}" != "image2" ]; then
  fail "IMAGE_PROVIDER must be image2 for this smoke."
fi
if [ "${IMAGE2_BACKEND:-wellapi}" != "dreammaker" ]; then
  fail "IMAGE2_BACKEND must be dreammaker for this smoke."
fi
if [ "${MUSIC_PROVIDER:-mock}" != "mock" ]; then
  fail "MUSIC_PROVIDER must remain mock for this Image2-only smoke."
fi

require_true IMAGE_REAL_CALLS_ENABLED
require_true DREAMMAKER_REAL_CALLS_ENABLED
require_false DEEPSEEK_REAL_CALLS_ENABLED
require_false YUNWU_REAL_CALLS_ENABLED
require_env DREAMMAKER_ACCESS_KEY
require_env DREAMMAKER_SECRET_KEY

for mode_var in \
  COMPANY_ACCOUNT_ADAPTER_MODE \
  COMPANY_MODERATION_ADAPTER_MODE \
  COMPANY_QUOTA_ADAPTER_MODE \
  COMPANY_PUBLISH_ADAPTER_MODE \
  COMPANY_SHARE_ADAPTER_MODE; do
  if [ "${!mode_var:-mock}" != "mock" ]; then
    fail "$mode_var must remain mock for this Image2-only smoke."
  fi
done

log "running strict DreamMaker Image2 preflight"
TARGET=dreammaker-image2 STRICT=true scripts/smoke/real-model-readiness-preflight.sh >/dev/null

log "checking API health and integration readiness"
curl -fsS "$API_ROOT/health" >/dev/null
READINESS="$(curl -fsS "$API_ROOT/internal/integration-readiness")"

IMAGE2_STATUS="$(echo "$READINESS" | jq -r '.components[] | select(.component == "image2_guard") | .status')"
IMAGE2_MODE="$(echo "$READINESS" | jq -r '.components[] | select(.component == "image2_guard") | .configured_mode')"
IMAGE2_IMPL="$(echo "$READINESS" | jq -r '.components[] | select(.component == "image2_guard") | .implementation')"
DREAMMAKER_STATUS="$(echo "$READINESS" | jq -r '.components[] | select(.component == "dreammaker_guard") | .status')"
MUSIC_MODE="$(echo "$READINESS" | jq -r '.components[] | select(.component == "music_provider") | .configured_mode')"

if [ "$IMAGE2_STATUS" != "READY_FOR_LOCAL" ] \
  || [ "$IMAGE2_MODE" != "real-calls-enabled/dreammaker" ] \
  || [ "$IMAGE2_IMPL" != "DreamMakerImage2CoverGenerationService" ]; then
  echo "$READINESS" | jq '.components[] | select(.component == "image2_guard")' >&2
  fail "image2_guard is not READY_FOR_LOCAL real-calls-enabled/dreammaker DreamMakerImage2CoverGenerationService"
fi

if [ "$DREAMMAKER_STATUS" != "READY_FOR_LOCAL" ]; then
  echo "$READINESS" | jq '.components[] | select(.component == "dreammaker_guard")' >&2
  fail "dreammaker_guard is not READY_FOR_LOCAL"
fi

if [ "$MUSIC_MODE" != "mock" ]; then
  fail "music_provider must remain mock for this smoke, got: $MUSIC_MODE"
fi

log "creating one lyrics work for real DreamMaker Image2 cover smoke"
CREATE_RESPONSE="$(
  post_json "/works/lyrics" "$IDEMPOTENCY_PREFIX-create" '{
    "song_title": "DreamMaker Image2 cover smoke",
    "lyrics_input": "Clouds cross the old pass at dusk. Lanterns wake the river road. I sing to the wind of Yanyun and return with one bright chord.",
    "music_style": "cinematic folk, guzheng, flute, gentle female vocal"
  }'
)"
WORK_ID="$(echo "$CREATE_RESPONSE" | jq -r '.work_id')"
if [ -z "$WORK_ID" ] || [ "$WORK_ID" = "null" ]; then
  echo "$CREATE_RESPONSE" | jq . >&2
  fail "create work did not return work_id"
fi
log "work_id=$WORK_ID"

DETAIL_RESPONSE="$(get_json "/works/$WORK_ID")"
LYRICS_DRAFT_ID="$(echo "$DETAIL_RESPONSE" | jq -r '.lyrics_draft.lyrics_draft_id // empty')"
if [ -z "$LYRICS_DRAFT_ID" ]; then
  echo "$DETAIL_RESPONSE" | jq . >&2
  fail "lyrics draft was not ready"
fi

log "confirming work with mock music and real DreamMaker Image2 cover"
CONFIRM_RESPONSE="$(
  post_json "/works/$WORK_ID/confirm" "$IDEMPOTENCY_PREFIX-confirm" "{
    \"lyrics_draft_id\": \"$LYRICS_DRAFT_ID\",
    \"music_provider\": \"mock\"
  }"
)"
echo "$CONFIRM_RESPONSE" | jq '{work_id, status, generation_stage, package_status, job_id}'

FINAL_DETAIL=""
for _ in $(seq 1 "$MAX_POLL_ATTEMPTS"); do
  DETAIL_RESPONSE="$(get_json "/works/$WORK_ID")"
  STATUS="$(echo "$DETAIL_RESPONSE" | jq -r '.status')"
  STAGE="$(echo "$DETAIL_RESPONSE" | jq -r '.generation_stage')"
  PACKAGE_STATUS="$(echo "$DETAIL_RESPONSE" | jq -r '.package_status')"
  FAILURE_CODE="$(echo "$DETAIL_RESPONSE" | jq -r '.failure.failure_code // empty')"
  log "$(date '+%H:%M:%S') status=$STATUS stage=$STAGE package=$PACKAGE_STATUS failure=${FAILURE_CODE:-none}"
  if [ "$STATUS" = "GENERATED" ] || [ "$STATUS" = "FAILED" ] || [ "$STATUS" = "LYRICS_FAILED" ]; then
    FINAL_DETAIL="$DETAIL_RESPONSE"
    break
  fi
  sleep "$POLL_INTERVAL_SECONDS"
done

if [ -z "$FINAL_DETAIL" ]; then
  fail "real DreamMaker Image2 smoke timed out. work_id=$WORK_ID"
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

log "checking cover media asset row"
COVER_ROW="$(
  docker exec "$POSTGRES_CONTAINER" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc \
    "select provider, object_key, mime_type, width, height, coalesce(metadata_json->>'provider',''), coalesce(metadata_json->>'model',''), coalesce(metadata_json->>'object_storage_imported',''), (metadata_json ? 'source_url'), (metadata_json ? 'inline_base64') from media_assets where work_id = '$WORK_ID'::uuid and asset_type = 'COVER' limit 1;"
)"
if [ -z "$COVER_ROW" ]; then
  fail "COVER media asset row missing for work_id=$WORK_ID"
fi

IFS='|' read -r COVER_PROVIDER COVER_OBJECT_KEY COVER_MIME COVER_WIDTH COVER_HEIGHT META_PROVIDER META_MODEL IMPORTED HAS_SOURCE_URL HAS_INLINE_BASE64 <<<"$COVER_ROW"

if [ "$COVER_PROVIDER" != "dreammaker-image2" ] || [ "$META_PROVIDER" != "dreammaker-image2" ]; then
  fail "cover provider is not dreammaker-image2: provider=$COVER_PROVIDER metadata_provider=$META_PROVIDER"
fi

if [ "$COVER_WIDTH" != "1920" ] || [ "$COVER_HEIGHT" != "1080" ]; then
  fail "cover dimensions are not workflow 1920x1080: ${COVER_WIDTH}x${COVER_HEIGHT}"
fi

if [ "$IMPORTED" != "true" ]; then
  fail "cover was not marked as imported into object storage"
fi

if [ "$HAS_SOURCE_URL" != "f" ] || [ "$HAS_INLINE_BASE64" != "f" ]; then
  fail "cover metadata retained supplier raw source fields"
fi

log "cover provider=$COVER_PROVIDER object_key=$COVER_OBJECT_KEY mime=$COVER_MIME size=${COVER_WIDTH}x${COVER_HEIGHT} model=$META_MODEL imported=$IMPORTED"

if [ "$FINAL_STATUS" = "GENERATED" ] && [ "$PACKAGE_STATUS" = "PACKAGE_READY" ]; then
  log "fetching publish package"
  PACKAGE_RESPONSE="$(get_json "/works/$WORK_ID/publish-package")"
  echo "$PACKAGE_RESPONSE" | jq '{
    work_id,
    package_status,
    expires_at,
    package_url_present: (((.package_url // "") | length) > 0),
    package_json_url_present: {
      cover: (((.package_json.cover.url? // "") | length) > 0),
      video: (((.package_json.video.url? // "") | length) > 0),
      timeline: (((.package_json.lyrics.timeline_url? // "") | length) > 0)
    }
  }'
  echo "$PACKAGE_RESPONSE" | jq -e '.package_json.cover.url | length > 0' >/dev/null \
    || fail "publish package cover URL missing"
  log "PASS real DreamMaker Image2 cover smoke. work_id=$WORK_ID"
  exit 0
fi

log "real DreamMaker Image2 smoke finished without PACKAGE_READY. work_id=$WORK_ID"
exit 1
