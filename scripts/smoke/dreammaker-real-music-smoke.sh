#!/usr/bin/env bash
set -euo pipefail

API_ROOT="${API_ROOT:-http://localhost:8080}"
API_BASE="${API_BASE:-$API_ROOT/api/v1}"
MOCK_USER="${MOCK_USER:-mock_user_001}"
REAL_PROVIDER="${REAL_PROVIDER:-suno}"
MAX_POLL_ATTEMPTS="${MAX_POLL_ATTEMPTS:-90}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-5}"
IDEMPOTENCY_PREFIX="${IDEMPOTENCY_PREFIX:-real-music-$(date +%s)}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-yanyun-postgres}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_DB="${POSTGRES_DB:-yanyun_music}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_env() {
  if [ -z "${!1:-}" ]; then
    echo "Missing required environment variable: $1" >&2
    exit 1
  fi
}

require_command curl
require_command jq

if [ "${ALLOW_DREAMMAKER_REAL_SMOKE:-}" != "1" ]; then
  echo "Refusing to run. Set ALLOW_DREAMMAKER_REAL_SMOKE=1 to confirm this real-provider smoke." >&2
  exit 1
fi

if [ "${DREAMMAKER_REAL_CALLS_ENABLED:-false}" != "true" ]; then
  echo "DREAMMAKER_REAL_CALLS_ENABLED must be true for real DreamMaker smoke." >&2
  exit 1
fi

require_env DREAMMAKER_ACCESS_KEY
require_env DREAMMAKER_SECRET_KEY

case "$REAL_PROVIDER" in
  suno|minimax) ;;
  *)
    echo "REAL_PROVIDER must be suno or minimax, got: $REAL_PROVIDER" >&2
    exit 1
    ;;
esac

echo "Checking API health and integration readiness..."
curl -fsS "$API_ROOT/health" >/dev/null
READINESS="$(curl -fsS "$API_ROOT/internal/integration-readiness")"

DREAMMAKER_STATUS="$(
  echo "$READINESS" | jq -r '.components[] | select(.component == "dreammaker_guard") | .status'
)"
WORKFLOW_MODE="$(
  echo "$READINESS" | jq -r '.components[] | select(.component == "workflow_dispatch") | .configured_mode'
)"

if [ "$DREAMMAKER_STATUS" != "READY_FOR_LOCAL" ]; then
  echo "dreammaker_guard is not READY_FOR_LOCAL:" >&2
  echo "$READINESS" | jq '.components[] | select(.component == "dreammaker_guard")' >&2
  exit 1
fi

if [ "$WORKFLOW_MODE" != "outbox/temporal" ]; then
  echo "workflow_dispatch must be outbox/temporal for real DreamMaker smoke, got: $WORKFLOW_MODE" >&2
  exit 1
fi

echo "Creating one lyrics work for provider=$REAL_PROVIDER..."
CREATE_RESPONSE="$(
  curl -fsS -X POST "$API_BASE/works/lyrics" \
    -H "Content-Type: application/json" \
    -H "X-Mock-User-Id: $MOCK_USER" \
    -H "Idempotency-Key: $IDEMPOTENCY_PREFIX-create" \
    -d '{
      "song_title": "真实音乐联调测试",
      "lyrics_input": "雁门风起过长街，灯影照见旧山河。故人踏月归来晚，一曲清歌入燕云。",
      "music_style": "国风民谣，古筝，笛子，女声，温柔叙事"
    }'
)"
WORK_ID="$(echo "$CREATE_RESPONSE" | jq -r '.work_id')"
if [ -z "$WORK_ID" ] || [ "$WORK_ID" = "null" ]; then
  echo "$CREATE_RESPONSE" | jq . >&2
  echo "Create work did not return work_id." >&2
  exit 1
fi
echo "work_id=$WORK_ID"

LYRICS_DRAFT_ID=""
for _ in $(seq 1 30); do
  DETAIL_RESPONSE="$(curl -fsS "$API_BASE/works/$WORK_ID" -H "X-Mock-User-Id: $MOCK_USER")"
  STATUS="$(echo "$DETAIL_RESPONSE" | jq -r '.status')"
  LYRICS_DRAFT_ID="$(echo "$DETAIL_RESPONSE" | jq -r '.lyrics_draft.lyrics_draft_id // empty')"
  echo "$(date '+%H:%M:%S') lyrics status=$STATUS draft=${LYRICS_DRAFT_ID:-empty}"
  if [ -n "$LYRICS_DRAFT_ID" ]; then
    break
  fi
  if [ "$STATUS" = "FAILED" ] || [ "$STATUS" = "LYRICS_FAILED" ]; then
    echo "$DETAIL_RESPONSE" | jq . >&2
    exit 1
  fi
  sleep 2
done

if [ -z "$LYRICS_DRAFT_ID" ]; then
  echo "Lyrics draft was not ready in time." >&2
  exit 1
fi

echo "Confirming work with real provider=$REAL_PROVIDER..."
CONFIRM_RESPONSE="$(
  curl -fsS -X POST "$API_BASE/works/$WORK_ID/confirm" \
    -H "Content-Type: application/json" \
    -H "X-Mock-User-Id: $MOCK_USER" \
    -H "Idempotency-Key: $IDEMPOTENCY_PREFIX-confirm-$REAL_PROVIDER" \
    -d "{
      \"lyrics_draft_id\": \"$LYRICS_DRAFT_ID\",
      \"music_provider\": \"$REAL_PROVIDER\"
    }"
)"
echo "$CONFIRM_RESPONSE" | jq '{work_id, status, generation_stage, package_status, job_id}'

FINAL_DETAIL=""
for _ in $(seq 1 "$MAX_POLL_ATTEMPTS"); do
  DETAIL_RESPONSE="$(curl -fsS "$API_BASE/works/$WORK_ID" -H "X-Mock-User-Id: $MOCK_USER")"
  STATUS="$(echo "$DETAIL_RESPONSE" | jq -r '.status')"
  STAGE="$(echo "$DETAIL_RESPONSE" | jq -r '.generation_stage')"
  PACKAGE_STATUS="$(echo "$DETAIL_RESPONSE" | jq -r '.package_status')"
  FAILURE_CODE="$(echo "$DETAIL_RESPONSE" | jq -r '.failure.failure_code // empty')"
  echo "$(date '+%H:%M:%S') status=$STATUS stage=$STAGE package=$PACKAGE_STATUS failure=${FAILURE_CODE:-none}"
  if [ "$STATUS" = "GENERATED" ] || [ "$STATUS" = "FAILED" ] || [ "$STATUS" = "LYRICS_FAILED" ]; then
    FINAL_DETAIL="$DETAIL_RESPONSE"
    break
  fi
  sleep "$POLL_INTERVAL_SECONDS"
done

if [ -z "$FINAL_DETAIL" ]; then
  echo "Real music smoke timed out. work_id=$WORK_ID" >&2
  exit 1
fi

echo "$FINAL_DETAIL" | jq '{work_id, status, generation_stage, package_status, failure, media_assets}'

FINAL_STATUS="$(echo "$FINAL_DETAIL" | jq -r '.status')"
PACKAGE_STATUS="$(echo "$FINAL_DETAIL" | jq -r '.package_status')"

if command -v docker >/dev/null 2>&1; then
  echo "Provider call summary:"
  docker exec "$POSTGRES_CONTAINER" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc \
    "select provider, model_name, provider_trace_id, status, error_code from provider_calls where work_id = '$WORK_ID' order by created_at;" || true
fi

if [ "$FINAL_STATUS" = "GENERATED" ] && [ "$PACKAGE_STATUS" = "PACKAGE_READY" ]; then
  echo "Fetching publish package..."
  curl -fsS "$API_BASE/works/$WORK_ID/publish-package" \
    -H "X-Mock-User-Id: $MOCK_USER" | jq '{work_id, package_status, package_url, expires_at, media}'
  echo "PASS real DreamMaker music smoke. work_id=$WORK_ID provider=$REAL_PROVIDER"
  exit 0
fi

echo "Real DreamMaker music smoke finished without PACKAGE_READY. work_id=$WORK_ID provider=$REAL_PROVIDER" >&2
exit 1
