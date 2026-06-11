#!/usr/bin/env bash
set -euo pipefail
set +x

YUNWU_BASE_URL="${YUNWU_BASE_URL:-https://yunwu.ai}"
YUNWU_TIMESTAMPED_LYRICS_PATH="${YUNWU_TIMESTAMPED_LYRICS_PATH:-/api/v1/generate/get-timestamped-lyrics}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-yanyun-postgres}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_DB="${POSTGRES_DB:-yanyun_music}"
LOG_DIR="${LOG_DIR:-build/smoke/yunwu-timestamped-lyrics-$(date +%Y%m%d%H%M%S)}"
CURL_CONNECT_TIMEOUT_SECONDS="${CURL_CONNECT_TIMEOUT_SECONDS:-10}"
CURL_MAX_TIME_SECONDS="${CURL_MAX_TIME_SECONDS:-90}"

fail() {
  printf '[yunwu-timestamped-lyrics] ERROR: %s\n' "$*" >&2
  exit 1
}

log() {
  printf '[yunwu-timestamped-lyrics] %s\n' "$*"
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

require_env() {
  if [ -z "${!1:-}" ]; then
    fail "missing required environment variable: $1"
  fi
}

psql_query() {
  local sql="$1"
  docker exec "$POSTGRES_CONTAINER" \
    psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "$sql"
}

timestamped_url() {
  local base="${YUNWU_BASE_URL%/}"
  local path="$YUNWU_TIMESTAMPED_LYRICS_PATH"
  if [[ "$path" != /* ]]; then
    path="/$path"
  fi
  printf '%s%s' "$base" "$path"
}

require_command curl
require_command jq
require_command docker

if [ "${ALLOW_REAL_MODEL_SMOKE:-}" != "1" ]; then
  fail "refusing to call timestamped lyrics endpoint; set ALLOW_REAL_MODEL_SMOKE=1"
fi
if [ "${ALLOW_YUNWU_TIMESTAMPED_LYRICS_SMOKE:-}" != "1" ]; then
  fail "refusing to call timestamped lyrics endpoint; set ALLOW_YUNWU_TIMESTAMPED_LYRICS_SMOKE=1"
fi
if [ "${SUNO_BACKEND:-}" != "yunwu" ]; then
  fail "refusing timestamped lyrics smoke unless SUNO_BACKEND=yunwu"
fi
if [ "${YUNWU_REAL_CALLS_ENABLED:-}" != "true" ]; then
  fail "refusing timestamped lyrics smoke unless YUNWU_REAL_CALLS_ENABLED=true"
fi

require_env YUNWU_API_KEY
require_env WORK_ID

if [[ ! "$WORK_ID" =~ ^[0-9a-fA-F-]{36}$ ]]; then
  fail "WORK_ID must be a UUID"
fi

mkdir -p "$LOG_DIR"

AUDIO_PAIR="$(
  psql_query "select concat_ws(E'\\t', coalesce(metadata_json->>'provider_task_id', ''), coalesce(metadata_json->>'provider_audio_id', '')) from media_assets where work_id = '$WORK_ID'::uuid and asset_type = 'AUDIO' order by created_at desc limit 1;"
)"
IFS=$'\t' read -r TASK_ID AUDIO_ID <<<"$AUDIO_PAIR"

if [ -z "$TASK_ID" ]; then
  fail "missing provider_task_id in AUDIO media metadata; run a successful Yunwu Suno sample with current code first"
fi

if [ -z "$AUDIO_ID" ]; then
  fail "missing provider_audio_id in AUDIO media metadata; provider response may not expose audio id"
fi

REQUEST_BODY="$(
  jq -n --arg taskId "$TASK_ID" --arg audioId "$AUDIO_ID" '{taskId: $taskId, audioId: $audioId}'
)"

CURL_OUTPUT="$(
  printf '%s' "$REQUEST_BODY" | curl -sS \
    --connect-timeout "$CURL_CONNECT_TIMEOUT_SECONDS" \
    --max-time "$CURL_MAX_TIME_SECONDS" \
    -o - \
    -w $'\n__HTTP_STATUS__:%{http_code}' \
    -X POST "$(timestamped_url)" \
    -H "Accept: application/json" \
    -H "Content-Type: application/json" \
    -K <(printf 'header = "Authorization: Bearer %s"\n' "$YUNWU_API_KEY") \
    --data-binary @-
)"
HTTP_STATUS="${CURL_OUTPUT##*__HTTP_STATUS__:}"
RESPONSE_BODY="${CURL_OUTPUT%"__HTTP_STATUS__:$HTTP_STATUS"}"
RESPONSE_BODY="${RESPONSE_BODY%$'\n'}"

if [[ ! "$HTTP_STATUS" =~ ^[0-9]{3}$ ]]; then
  fail "timestamped lyrics response did not include a valid HTTP status"
fi

if ! jq -e . >/dev/null 2>&1 <<<"$RESPONSE_BODY"; then
  fail "timestamped lyrics response was not valid JSON; http_status=$HTTP_STATUS"
fi

SUMMARY="$(
  jq -c --argjson httpStatus "$HTTP_STATUS" '
    def aligned_word_arrays: [.. | objects | .alignedWords? | arrays];
    def waveform_arrays: [.. | objects | .waveformData? | arrays];
    def timestamp_objects: [.. | objects | select((has("start") or has("startTime")) and (has("end") or has("endTime")))];
    {
      http_status: $httpStatus,
      provider_code: (.code? // .status_code? // .err_code? // null),
      aligned_word_count: ((aligned_word_arrays | map(length) | max) // 0),
      waveform_present: ((waveform_arrays | length) > 0),
      first_timestamp_present: ((timestamp_objects | length) > 0)
    }
  ' <<<"$RESPONSE_BODY"
)"

EVIDENCE_FILE="$LOG_DIR/timestamped-lyrics-summary.json"
printf '%s\n' "$SUMMARY" >"$EVIDENCE_FILE"

HTTP_OK="$(echo "$SUMMARY" | jq -r '.http_status >= 200 and .http_status < 300')"
WORD_COUNT="$(echo "$SUMMARY" | jq -r '.aligned_word_count')"
TIMESTAMP_PRESENT="$(echo "$SUMMARY" | jq -r '.first_timestamp_present')"
WAVEFORM_PRESENT="$(echo "$SUMMARY" | jq -r '.waveform_present')"

log "summary=$(echo "$SUMMARY" | jq -c '{http_status, provider_code, aligned_word_count, waveform_present, first_timestamp_present}')"
log "evidence_file=$EVIDENCE_FILE"

if [ "$HTTP_OK" != "true" ]; then
  fail "timestamped lyrics endpoint returned non-2xx status"
fi

if [ "$WORD_COUNT" -le 0 ] || [ "$TIMESTAMP_PRESENT" != "true" ]; then
  fail "timestamped lyrics response did not contain aligned words with timestamps"
fi

log "PASS timestamped lyrics available. aligned_word_count=$WORD_COUNT waveform_present=$WAVEFORM_PRESENT"
