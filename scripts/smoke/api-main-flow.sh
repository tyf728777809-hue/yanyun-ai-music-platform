#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api/v1}"
API_HEALTH_URL="${API_HEALTH_URL:-http://localhost:8080/health}"
MOCK_USER_ID="${MOCK_USER_ID:-smoke_user_$(date +%s)_$RANDOM}"
EXPECTED_DURATION_MS="${EXPECTED_DURATION_MS:-}"
EXPECT_RENDER_WORKER="${EXPECT_RENDER_WORKER:-}"
CHECK_DB="${CHECK_DB:-true}"
CHECK_LOCAL_FILES="${CHECK_LOCAL_FILES:-true}"
CHECK_MEDIA_URLS="${CHECK_MEDIA_URLS:-false}"
LOCAL_OBJECT_ROOTS="${LOCAL_OBJECT_ROOTS:-build/local-object-storage/yanyun-works-local:apps/music-api/build/local-object-storage/yanyun-works-local}"

fail() {
  printf '[smoke] ERROR: %s\n' "$*" >&2
  exit 1
}

log() {
  printf '[smoke] %s\n' "$*"
}

need_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

idempotency_key() {
  printf 'smoke-%s-%s-%s' "$1" "$(date +%s)" "$RANDOM"
}

assert_json() {
  local json="$1"
  local expression="$2"
  local message="$3"
  jq -e "$expression" >/dev/null <<<"$json" || fail "$message: $(jq -c . <<<"$json")"
}

post_json() {
  local path="$1"
  local key="$2"
  local body="$3"
  curl -sS -f -X POST "${API_BASE_URL}${path}" \
    -H "Content-Type: application/json" \
    -H "X-Mock-User-Id: ${MOCK_USER_ID}" \
    -H "Idempotency-Key: ${key}" \
    -d "$body"
}

get_json() {
  local path="$1"
  curl -sS -f "${API_BASE_URL}${path}" -H "X-Mock-User-Id: ${MOCK_USER_ID}"
}

psql_query() {
  local sql="$1"
  docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc "$sql"
}

find_local_object() {
  local object_key="$1"
  local old_ifs="$IFS"
  IFS=':'
  for root in $LOCAL_OBJECT_ROOTS; do
    if [[ -f "${root}/${object_key}" ]]; then
      printf '%s\n' "${root}/${object_key}"
      IFS="$old_ifs"
      return 0
    fi
  done
  IFS="$old_ifs"
  return 1
}

assert_url_readable() {
  local url="$1"
  local label="$2"
  [[ -n "$url" && "$url" != "null" ]] || fail "${label} URL missing"
  curl -sS -f -L -r 0-0 "$url" -o /dev/null || fail "${label} URL is not readable"
}

need_command curl
need_command jq

log "checking API health: ${API_HEALTH_URL}"
curl -sS -f "$API_HEALTH_URL" | jq -e '.status == "OK"' >/dev/null

create_body="$(
  jq -n \
    --arg title "Smoke Song" \
    --arg lyrics $'Wind over Yanmen\nDrums under moon' \
    --arg reference "Yanmen pass and frontier drums" \
    '{song_title:$title, lyrics_input:$lyrics, style_tags:["smoke","mock"], yanyun_reference:$reference}'
)"

log "creating lyrics work"
create_response="$(post_json "/works/lyrics" "$(idempotency_key lyrics)" "$create_body")"
assert_json "$create_response" '.status == "LYRICS_READY" and .generation_stage == "WAITING_CONFIRM"' "work did not enter lyrics ready state"
work_id="$(jq -er '.work_id' <<<"$create_response")"
log "created work_id=${work_id}"

detail_response="$(get_json "/works/${work_id}")"
assert_json "$detail_response" '.status == "LYRICS_READY" and .package_status == "PACKAGE_NOT_READY"' "work detail before confirm is invalid"
assert_json "$detail_response" '(.available_actions | index("CONFIRM_WORK")) != null' "CONFIRM_WORK action is missing"

confirm_body='{"music_provider":"mock"}'
log "confirming work"
confirm_response="$(post_json "/works/${work_id}/confirm" "$(idempotency_key confirm)" "$confirm_body")"
assert_json "$confirm_response" '.status == "GENERATED" and .generation_stage == "PACKAGE_READY"' "confirm did not finish generation"

generated_detail="$(get_json "/works/${work_id}")"
assert_json "$generated_detail" '.status == "GENERATED" and .package_status == "PACKAGE_READY"' "generated work detail is invalid"
assert_json "$generated_detail" '(.available_actions | index("MARK_PACKAGE_FETCHED")) != null' "MARK_PACKAGE_FETCHED action is missing"
if [[ -n "$EXPECTED_DURATION_MS" ]]; then
  jq -e --argjson duration "$EXPECTED_DURATION_MS" '.media_assets.video_duration_ms == $duration' >/dev/null <<<"$generated_detail" \
    || fail "video duration did not match EXPECTED_DURATION_MS=${EXPECTED_DURATION_MS}: $(jq -c '.media_assets' <<<"$generated_detail")"
fi

log "fetching publish package"
package_response="$(get_json "/works/${work_id}/publish-package")"
assert_json "$package_response" '.package_status == "PACKAGE_READY" and (.package_url | length > 0)' "publish package is not ready"
assert_json "$package_response" '(.package_json.video.url | length > 0) and (.package_json.cover.url | length > 0) and (.package_json.lyrics.text | length > 0)' "publish package json is incomplete"

if [[ "$CHECK_MEDIA_URLS" == "true" ]]; then
  log "checking publish package media URLs"
  assert_url_readable "$(jq -r '.package_url' <<<"$package_response")" "package"
  assert_url_readable "$(jq -r '.package_json.audio.url // empty' <<<"$package_response")" "audio"
  assert_url_readable "$(jq -r '.package_json.cover.url // empty' <<<"$package_response")" "cover"
  assert_url_readable "$(jq -r '.package_json.video.url // empty' <<<"$package_response")" "video"
fi

log "refreshing publish package URL"
refresh_response="$(post_json "/works/${work_id}/publish-package/refresh-url" "$(idempotency_key refresh)" '{}')"
assert_json "$refresh_response" '.package_status == "PACKAGE_READY" and (.package_url | length > 0)' "package URL refresh failed"

log "marking publish package fetched"
fetched_response="$(post_json "/works/${work_id}/publish-package/mark-fetched" "$(idempotency_key fetched)" '{}')"
assert_json "$fetched_response" '.package_status == "PACKAGE_FETCHED"' "package was not marked fetched"

final_detail="$(get_json "/works/${work_id}")"
assert_json "$final_detail" '.package_status == "PACKAGE_FETCHED"' "final work detail did not keep PACKAGE_FETCHED"

readiness_response="$(curl -sS -f "${API_BASE_URL%/api/v1}/internal/integration-readiness")"
assert_json "$readiness_response" '.overall_status == "READY_FOR_LOCAL"' "integration readiness is not local-ready"
if [[ -n "$EXPECT_RENDER_WORKER" ]]; then
  jq -e --arg mode "$EXPECT_RENDER_WORKER" '.components[] | select(.component == "render_worker") | .configured_mode == $mode' >/dev/null <<<"$readiness_response" \
    || fail "render_worker mode did not match EXPECT_RENDER_WORKER=${EXPECT_RENDER_WORKER}"
fi

package_object_key=""
video_object_key=""
if [[ "$CHECK_DB" == "true" ]]; then
  need_command docker
  log "checking database state"
  db_package="$(psql_query "select w.status, w.generation_stage, p.package_status, p.package_object_key, p.fetched_at is not null from works w join publish_packages p on p.work_id = w.id where w.id = '${work_id}'::uuid;")"
  [[ "$db_package" == GENERATED\|PACKAGE_READY\|PACKAGE_FETCHED\|*\|t ]] || fail "unexpected package DB row: ${db_package}"
  package_object_key="$(cut -d'|' -f4 <<<"$db_package")"
  provider_row="$(psql_query "select provider, operation, status from provider_calls where work_id = '${work_id}'::uuid order by created_at desc limit 1;")"
  [[ "$provider_row" == "MOCK|MUSIC_GENERATION|SUCCEEDED" ]] || fail "unexpected provider call row: ${provider_row}"
  video_object_key="$(psql_query "select object_key from media_assets where work_id = '${work_id}'::uuid and asset_type = 'VIDEO' limit 1;")"
  timeline_object_key="$(psql_query "select object_key from media_assets where work_id = '${work_id}'::uuid and asset_type = 'TIMELINE' limit 1;")"
  [[ -n "$video_object_key" && -n "$timeline_object_key" ]] || fail "video or timeline object key missing"
fi

if [[ "$CHECK_LOCAL_FILES" == "true" && -n "$package_object_key" ]]; then
  log "checking local object files"
  package_path="$(find_local_object "$package_object_key")" || fail "local package object missing: ${package_object_key}"
  jq -e --arg work_id "$work_id" '.work_id == $work_id' >/dev/null "$package_path" || fail "package JSON work_id mismatch"
  if [[ -n "$EXPECT_RENDER_WORKER" && -n "$video_object_key" ]]; then
    video_path="$(find_local_object "$video_object_key")" || fail "local video object missing: ${video_object_key}"
    need_command ffprobe
    ffprobe_json="$(ffprobe -v error -show_entries format=duration,size -show_entries stream=codec_type,codec_name,width,height,r_frame_rate -of json "$video_path")"
    jq -e '.streams[] | select(.codec_name == "h264" and .width == 1920 and .height == 1080)' >/dev/null <<<"$ffprobe_json" \
      || fail "ffprobe did not find expected H.264 1920x1080 stream"
    if [[ -n "$EXPECTED_DURATION_MS" ]]; then
      expected_seconds="$(jq -n --argjson ms "$EXPECTED_DURATION_MS" '$ms / 1000')"
      jq -e --argjson expected "$expected_seconds" '(((.format.duration | tonumber) - $expected) | if . < 0 then -. else . end) < 0.05' >/dev/null <<<"$ffprobe_json" \
        || fail "ffprobe duration did not match expected seconds ${expected_seconds}: $(jq -c '.format' <<<"$ffprobe_json")"
    fi
  fi
fi

log "PASS work_id=${work_id}"
