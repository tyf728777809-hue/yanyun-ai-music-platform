#!/usr/bin/env bash
set -euo pipefail

API_ROOT="${API_ROOT:-http://localhost:8080}"
API_BASE="${API_BASE:-$API_ROOT/api/v1}"
MOCK_USER="${MOCK_USER:-guard_user_$(date +%s)_$RANDOM}"
REAL_PROVIDER="${REAL_PROVIDER:-suno}"
CHECK_DB="${CHECK_DB:-true}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-yanyun-postgres}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_DB="${POSTGRES_DB:-yanyun_music}"

fail() {
  printf '[dreammaker-guard] ERROR: %s\n' "$*" >&2
  exit 1
}

log() {
  printf '[dreammaker-guard] %s\n' "$*"
}

need_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

idempotency_key() {
  printf 'dreammaker-guard-%s-%s-%s' "$1" "$(date +%s)" "$RANDOM"
}

post_json() {
  local path="$1"
  local key="$2"
  local body="$3"
  curl -sS -f -X POST "${API_BASE}${path}" \
    -H "Content-Type: application/json" \
    -H "X-Mock-User-Id: ${MOCK_USER}" \
    -H "Idempotency-Key: ${key}" \
    -d "$body"
}

psql_query() {
  local sql="$1"
  docker exec "$POSTGRES_CONTAINER" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "$sql"
}

need_command curl
need_command jq

case "$REAL_PROVIDER" in
  suno|minimax) ;;
  *) fail "REAL_PROVIDER must be suno or minimax, got: $REAL_PROVIDER" ;;
esac

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
CONFIRM_BODY_FILE="$TMP_DIR/confirm-response.json"

log "checking API health"
curl -sS -f "$API_ROOT/health" | jq -e '.status == "OK"' >/dev/null

log "checking integration readiness"
READINESS="$(curl -sS -f "$API_ROOT/internal/integration-readiness")"
DREAMMAKER_MODE="$(
  jq -r '.components[] | select(.component == "dreammaker_guard") | .configured_mode' <<<"$READINESS"
)"
WORKFLOW_MODE="$(
  jq -r '.components[] | select(.component == "workflow_dispatch") | .configured_mode' <<<"$READINESS"
)"

if [[ -z "$DREAMMAKER_MODE" || "$DREAMMAKER_MODE" == "real-calls-disabled" ]]; then
  fail "dreammaker_guard must show real calls enabled or blocked by missing credentials; got: ${DREAMMAKER_MODE:-missing}"
fi

if [[ "$WORKFLOW_MODE" != sync/* ]]; then
  fail "guard smoke only runs in sync workflow mode; got workflow_dispatch=${WORKFLOW_MODE:-missing}"
fi

log "creating lyrics work"
CREATE_RESPONSE="$(
  post_json "/works/lyrics" "$(idempotency_key create)" \
    '{
      "song_title": "DreamMaker Guard Smoke",
      "lyrics_input": "雁门风起过长街，灯影照见旧山河。故人踏月归来晚，一曲清歌入燕云。",
      "music_style": "国风民谣，古筝，笛子，女声，温柔叙事"
    }'
)"
WORK_ID="$(jq -er '.work_id' <<<"$CREATE_RESPONSE")"
log "created work_id=$WORK_ID"

DETAIL_RESPONSE="$(curl -sS -f "$API_BASE/works/$WORK_ID" -H "X-Mock-User-Id: $MOCK_USER")"
LYRICS_DRAFT_ID="$(jq -er '.lyrics_draft.lyrics_draft_id' <<<"$DETAIL_RESPONSE")"

log "confirming with provider=$REAL_PROVIDER; expecting runtime guard HTTP 409"
set +e
HTTP_STATUS="$(
  curl -sS -o "$CONFIRM_BODY_FILE" -w '%{http_code}' -X POST "$API_BASE/works/$WORK_ID/confirm" \
    -H "Content-Type: application/json" \
    -H "X-Mock-User-Id: $MOCK_USER" \
    -H "Idempotency-Key: $(idempotency_key confirm)" \
    -d "{
      \"lyrics_draft_id\": \"$LYRICS_DRAFT_ID\",
      \"music_provider\": \"$REAL_PROVIDER\"
    }"
)"
CURL_STATUS=$?
set -e

if [[ "$CURL_STATUS" -ne 0 ]]; then
  cat "$CONFIRM_BODY_FILE" >&2 || true
  fail "confirm request failed before receiving HTTP response"
fi

if [[ "$HTTP_STATUS" != "409" ]]; then
  cat "$CONFIRM_BODY_FILE" >&2 || true
  fail "expected HTTP 409 from runtime guard, got HTTP $HTTP_STATUS"
fi

jq -e '.error.code == "CONFLICT" and (.error.message | contains("outbox + Temporal worker"))' \
  "$CONFIRM_BODY_FILE" >/dev/null \
  || fail "runtime guard error body did not contain expected message: $(jq -c . "$CONFIRM_BODY_FILE")"

DETAIL_AFTER="$(curl -sS -f "$API_BASE/works/$WORK_ID" -H "X-Mock-User-Id: $MOCK_USER")"
jq -e '.status == "LYRICS_READY" and .generation_stage == "WAITING_CONFIRM"' >/dev/null \
  <<<"$DETAIL_AFTER" \
  || fail "work state changed unexpectedly after guard rejection: $(jq -c '{status, generation_stage, package_status}' <<<"$DETAIL_AFTER")"

if [[ "$CHECK_DB" == "true" ]]; then
  need_command docker
  provider_call_count="$(psql_query "select count(*) from provider_calls where work_id = '$WORK_ID'::uuid;")"
  [[ "$provider_call_count" == "0" ]] || fail "provider_calls were written despite guard rejection: $provider_call_count"
fi

log "PASS work_id=$WORK_ID provider=$REAL_PROVIDER"
