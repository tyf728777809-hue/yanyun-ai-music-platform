#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api/v1}"
API_HEALTH_URL="${API_HEALTH_URL:-http://localhost:8080/health}"
OPENAPI_FILE="${OPENAPI_FILE:-docs/api/openapi-v0.1.yaml}"
MOCK_USER_ID="${MOCK_USER_ID:-contract_user_$(date +%s)_$RANDOM}"

fail() {
  printf '[contract] ERROR: %s\n' "$*" >&2
  if [[ -n "${RESULT_BODY:-}" && -f "$RESULT_BODY" ]]; then
    printf '[contract] last response: %s\n' "$(jq -c . "$RESULT_BODY" 2>/dev/null || cat "$RESULT_BODY")" >&2
  fi
  exit 1
}

log() {
  printf '[contract] %s\n' "$*"
}

need_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

idempotency_key() {
  printf 'contract-%s-%s-%s' "$1" "$(date +%s)" "$RANDOM"
}

TMP_DIR="$(mktemp -d)"
RESULT_BODY="${TMP_DIR}/response.json"
trap 'rm -rf "$TMP_DIR"' EXIT

need_command curl
need_command jq
need_command ruby

static_openapi_check() {
  ruby - "$OPENAPI_FILE" <<'RUBY'
require "yaml"

file = ARGV.fetch(0)
data = YAML.load_file(file)

def assert(condition, message)
  raise message unless condition
end

paths = data.fetch("paths")
schemas = data.fetch("components").fetch("schemas")

expected_operations = {
  "/api/v1/me" => {"get" => "getCurrentUser"},
  "/api/v1/works/inspiration" => {"post" => "createWorkFromInspiration"},
  "/api/v1/works/lyrics" => {"post" => "createWorkFromLyrics"},
  "/api/v1/works" => {"get" => "listWorks"},
  "/api/v1/works/{work_id}" => {"get" => "getWork"},
  "/api/v1/works/{work_id}/lyrics/polish" => {"post" => "polishLyrics"},
  "/api/v1/works/{work_id}/lyrics/continue" => {"post" => "continueLyrics"},
  "/api/v1/works/{work_id}/confirm" => {"post" => "confirmWork"},
  "/api/v1/works/{work_id}/music/retry" => {"post" => "retryMusic"},
  "/api/v1/works/{work_id}/cover/regenerate" => {"post" => "regenerateCover"},
  "/api/v1/works/{work_id}/video/rerender" => {"post" => "rerenderVideo"},
  "/api/v1/works/{work_id}/publish-package" => {"get" => "getPublishPackage"},
  "/api/v1/works/{work_id}/publish-package/mark-fetched" => {"post" => "markPublishPackageFetched"},
  "/api/v1/works/{work_id}/publish-package/refresh-url" => {"post" => "refreshPublishPackageUrl"}
}

expected_operations.each do |path, methods|
  assert(paths.key?(path), "missing OpenAPI path #{path}")
  methods.each do |method, operation_id|
    assert(paths[path].key?(method), "missing OpenAPI method #{method.upcase} #{path}")
    assert(paths[path][method]["operationId"] == operation_id, "operationId mismatch for #{method.upcase} #{path}")
  end
end

required_fields = {
  "UserProfile" => %w[user_id nickname roles],
  "CreateWorkResponse" => %w[work_id work_code status generation_stage job_id quota_hint available_actions],
  "JobAcceptedResponse" => %w[work_id status generation_stage job_id available_actions],
  "WorkListResponse" => %w[items pagination],
  "WorkSummary" => %w[work_id work_code status generation_stage package_status updated_at],
  "WorkDetail" => %w[work_id work_code creation_mode status generation_stage package_status polish_remaining_count quota_hint available_actions publish_handoff_hint],
  "LyricsDraft" => %w[lyrics_draft_id version_no lyrics_text music_prompt],
  "QuotaHint" => %w[locked commit_timing remaining_generate_count],
  "FailureInfo" => %w[failure_code failure_message retryable],
  "PublishHandoffHint" => %w[ready_for_handoff message],
  "PublishPackage" => %w[work_id package_status available_actions],
  "PublishPackageJson" => %w[work_id video cover lyrics metadata],
  "PublishAsset" => %w[url mime_type],
  "Pagination" => %w[page page_size total_items total_pages],
  "ErrorResponse" => %w[error]
}

required_fields.each do |schema, fields|
  assert(schemas.key?(schema), "missing schema #{schema}")
  actual = schemas[schema].fetch("required", [])
  missing = fields - actual
  assert(missing.empty?, "schema #{schema} missing required fields: #{missing.join(", ")}")
end

expected_enums = {
  "WorkStatus" => %w[DRAFT LYRICS_GENERATING LYRICS_READY LYRICS_FAILED GENERATING GENERATED FAILED CANCELLED],
  "GenerationStage" => %w[NONE USER_INPUT_PRECHECK LYRICS_GENERATING LYRICS_PRECHECK WAITING_CONFIRM QUOTA_LOCKING MUSIC_GENERATING COVER_GENERATING TIMELINE_BUILDING VIDEO_RENDERING PACKAGE_BUILDING PACKAGE_PRECHECK PACKAGE_READY FAILED],
  "PackageStatus" => %w[PACKAGE_NOT_READY PACKAGE_READY PACKAGE_FETCHED PACKAGE_EXPIRED PACKAGE_BLOCKED],
  "AvailableAction" => %w[POLISH_LYRICS CONTINUE_LYRICS CONFIRM_WORK RETRY_LYRICS RETRY_MUSIC RETRY_COVER RERENDER_VIDEO REFRESH_PACKAGE_URL MARK_PACKAGE_FETCHED RETURN_TO_EDIT CONTACT_SUPPORT],
  "FailureCode" => %w[USER_INPUT_BLOCKED LYRICS_GENERATION_FAILED LYRICS_PRECHECK_FAILED QUOTA_LOCK_FAILED MUSIC_GENERATION_FAILED MUSIC_QUALITY_FAILED COVER_GENERATION_FAILED VIDEO_RENDER_FAILED PACKAGE_BUILD_FAILED PACKAGE_BLOCKED PROVIDER_AUTH_FAILED PROVIDER_TIMEOUT RATE_LIMITED UNKNOWN_ERROR],
  "ErrorCode" => %w[VALIDATION_ERROR UNAUTHORIZED FORBIDDEN NOT_FOUND CONFLICT IDEMPOTENCY_CONFLICT RATE_LIMITED PROVIDER_UNAVAILABLE INTERNAL_ERROR]
}

expected_enums.each do |schema, values|
  actual = schemas.fetch(schema).fetch("enum")
  missing = values - actual
  assert(missing.empty?, "schema #{schema} missing enum values: #{missing.join(", ")}")
end
RUBY
}

request() {
  local method="$1"
  local path="$2"
  local body="${3:-__NO_BODY__}"
  local key="${4:-}"
  local status
  local args=(-sS -o "$RESULT_BODY" -w "%{http_code}" -X "$method" "${API_BASE_URL}${path}" -H "Accept: application/json" -H "X-Mock-User-Id: ${MOCK_USER_ID}")
  if [[ "$body" != "__NO_BODY__" ]]; then
    args+=(-H "Content-Type: application/json")
    if [[ -n "$key" ]]; then
      args+=(-H "Idempotency-Key: ${key}")
    fi
    args+=(-d "$body")
  fi
  status="$(curl "${args[@]}")" || fail "curl failed for ${method} ${path}"
  printf '%s\n' "$status"
}

assert_status() {
  local actual="$1"
  local expected="$2"
  [[ "$actual" == "$expected" ]] || fail "expected HTTP ${expected}, got ${actual}"
}

assert_json() {
  local expression="$1"
  local message="$2"
  jq -e "$expression" >/dev/null <"$RESULT_BODY" || fail "$message"
}

assert_error_response() {
  local expected_code="$1"
  jq -e --arg code "$expected_code" '.error.code == $code' >/dev/null <"$RESULT_BODY" \
    || fail "error.code did not match ${expected_code}"
  assert_json '.error.message | type == "string" and length > 0' "error.message missing"
  assert_json '.error.request_id | type == "string" and length > 0' "error.request_id missing"
  assert_json '.error.timestamp | type == "string" and length > 0' "error.timestamp missing"
  assert_json '.error.details | type == "array"' "error.details missing"
}

assert_create_work_response() {
  assert_json '.work_id and .work_code and .status and .generation_stage and .job_id and .quota_hint and (.available_actions | type == "array")' "CreateWorkResponse required fields missing"
  assert_json '.status == "LYRICS_READY" and .generation_stage == "WAITING_CONFIRM"' "created work status mismatch"
  assert_json '.quota_hint.commit_timing == "ON_PACKAGE_READY"' "quota_hint.commit_timing mismatch"
  assert_json '(.available_actions | index("CONFIRM_WORK")) != null' "CONFIRM_WORK action missing"
}

assert_job_response() {
  assert_json '.work_id and .status and .generation_stage and .job_id and (.available_actions | type == "array")' "JobAcceptedResponse required fields missing"
}

assert_work_detail_base() {
  assert_json '.work_id and .work_code and .creation_mode and .status and .generation_stage and .package_status and (.polish_remaining_count | type == "number") and .quota_hint and (.available_actions | type == "array") and .publish_handoff_hint' "WorkDetail required fields missing"
  assert_json '.quota_hint.commit_timing == "ON_PACKAGE_READY" and (.quota_hint.remaining_generate_count | type == "number")' "QuotaHint required fields missing"
  assert_json '.publish_handoff_hint.ready_for_handoff | type == "boolean"' "PublishHandoffHint.ready_for_handoff missing"
}

assert_lyrics_draft() {
  assert_json '.lyrics_draft.lyrics_draft_id and (.lyrics_draft.version_no | type == "number") and (.lyrics_draft.lyrics_text | length > 0) and (.lyrics_draft.music_prompt | length > 0)' "LyricsDraft required fields missing"
  assert_json '.lyrics_draft.yanyun_references | type == "array"' "LyricsDraft.yanyun_references missing"
}

assert_publish_package_ready() {
  assert_json '.work_id and .package_status and (.available_actions | type == "array")' "PublishPackage required fields missing"
  assert_json '.package_status == "PACKAGE_READY"' "PublishPackage status is not PACKAGE_READY"
  assert_json '(.package_url | type == "string" and length > 0) and (.package_url_expires_at | type == "string" and length > 0)' "PublishPackage URL fields missing"
  assert_json '.package_json.work_id and .package_json.video.url and .package_json.video.mime_type and .package_json.cover.url and .package_json.cover.mime_type and (.package_json.lyrics.text | length > 0) and .package_json.metadata' "PublishPackageJson required fields missing"
}

assert_work_summary() {
  assert_json '.items | type == "array"' "WorkListResponse.items missing"
  assert_json '.pagination.page and .pagination.page_size and (.pagination.total_items | type == "number") and (.pagination.total_pages | type == "number")' "Pagination required fields missing"
  assert_json '(.items | length) > 0' "WorkListResponse should include created works"
  assert_json '.items[0].work_id and .items[0].work_code and .items[0].status and .items[0].generation_stage and .items[0].package_status and .items[0].updated_at' "WorkSummary required fields missing"
}

health_status="$(curl -sS -o "$RESULT_BODY" -w "%{http_code}" "$API_HEALTH_URL" || true)"
assert_status "$health_status" "200"
assert_json '.status == "OK"' "API health did not return status OK"

log "checking static OpenAPI contract"
static_openapi_check

log "checking /me"
status="$(request GET "/me")"
assert_status "$status" "200"
assert_json '.user_id and .nickname and (.roles | type == "array")' "UserProfile required fields missing"

log "checking missing idempotency error"
status="$(request POST "/works/lyrics" '{"lyrics_input":"missing key"}')"
assert_status "$status" "400"
assert_error_response "VALIDATION_ERROR"

log "checking idempotency conflict"
conflict_key="$(idempotency_key conflict)"
status="$(request POST "/works/lyrics" '{"lyrics_input":"first conflict body"}' "$conflict_key")"
assert_status "$status" "202"
status="$(request POST "/works/lyrics" '{"lyrics_input":"second conflict body"}' "$conflict_key")"
assert_status "$status" "409"
assert_error_response "IDEMPOTENCY_CONFLICT"

log "checking inspiration create contract"
inspiration_body='{"story_input":"雁门关外风雪夜，少年与旧友重逢。","mood":"温柔热血","scene":"边塞雪夜","music_style":"国风摇滚"}'
status="$(request POST "/works/inspiration" "$inspiration_body" "$(idempotency_key inspiration)")"
assert_status "$status" "202"
assert_create_work_response

log "checking lyrics create and detail contract"
lyrics_body='{"song_title":"契约 Smoke 歌","lyrics_input":"风过雁门\n灯照长街","music_style":"国风民谣"}'
status="$(request POST "/works/lyrics" "$lyrics_body" "$(idempotency_key lyrics)")"
assert_status "$status" "202"
assert_create_work_response
work_id="$(jq -er '.work_id' <"$RESULT_BODY")"
log "created work_id=${work_id}"

status="$(request GET "/works/${work_id}")"
assert_status "$status" "200"
assert_work_detail_base
assert_lyrics_draft
assert_json '.status == "LYRICS_READY" and .generation_stage == "WAITING_CONFIRM" and .package_status == "PACKAGE_NOT_READY"' "initial WorkDetail state mismatch"
assert_json '(.available_actions | index("POLISH_LYRICS")) != null and (.available_actions | index("CONTINUE_LYRICS")) != null and (.available_actions | index("CONFIRM_WORK")) != null' "initial available actions missing"

log "checking polish and continue contracts"
status="$(request POST "/works/${work_id}/lyrics/polish" '{"instruction":"增强边塞画面感。"}' "$(idempotency_key polish)")"
assert_status "$status" "202"
assert_job_response
status="$(request GET "/works/${work_id}")"
assert_status "$status" "200"
assert_work_detail_base
assert_lyrics_draft
assert_json '.lyrics_draft.version_no == 2 and .polish_used_count == 1 and .polish_remaining_count == 1' "polish did not update draft counters"

status="$(request POST "/works/${work_id}/lyrics/continue" '{"instruction":"续写一段副歌。"}' "$(idempotency_key continue)")"
assert_status "$status" "202"
assert_job_response
status="$(request GET "/works/${work_id}")"
assert_status "$status" "200"
assert_json '.lyrics_draft.version_no == 3 and .polish_used_count == 2 and .polish_remaining_count == 0' "continue did not exhaust edit counters"

log "checking edit limit error"
status="$(request POST "/works/${work_id}/lyrics/polish" '{"instruction":"第三次润色应失败。"}' "$(idempotency_key polish-limit)")"
assert_status "$status" "409"
assert_error_response "CONFLICT"

log "checking confirm, generated detail, cover and video actions"
status="$(request POST "/works/${work_id}/confirm" '{"music_provider":"mock"}' "$(idempotency_key confirm)")"
assert_status "$status" "202"
assert_job_response
assert_json '.status == "GENERATED" and .generation_stage == "PACKAGE_READY"' "confirm response state mismatch"

status="$(request GET "/works/${work_id}")"
assert_status "$status" "200"
assert_work_detail_base
assert_json '.status == "GENERATED" and .generation_stage == "PACKAGE_READY" and .package_status == "PACKAGE_READY"' "generated WorkDetail state mismatch"
assert_json '.media_assets.audio_url and .media_assets.cover_url and .media_assets.video_url and (.media_assets.video_duration_ms | type == "number")' "generated media assets missing"
assert_json '(.available_actions | index("REFRESH_PACKAGE_URL")) != null and (.available_actions | index("MARK_PACKAGE_FETCHED")) != null' "generated package actions missing"

status="$(request POST "/works/${work_id}/cover/regenerate" '{}' "$(idempotency_key cover)")"
assert_status "$status" "202"
assert_job_response
status="$(request POST "/works/${work_id}/video/rerender" '{}' "$(idempotency_key video)")"
assert_status "$status" "202"
assert_job_response

log "checking list and publish package contracts"
status="$(request GET "/works?page=1&page_size=5&status=GENERATED")"
assert_status "$status" "200"
assert_work_summary

status="$(request GET "/works/${work_id}/publish-package")"
assert_status "$status" "200"
assert_publish_package_ready

status="$(request POST "/works/${work_id}/publish-package/refresh-url" '{}' "$(idempotency_key refresh)")"
assert_status "$status" "200"
assert_publish_package_ready

status="$(request POST "/works/${work_id}/publish-package/mark-fetched" '{}' "$(idempotency_key fetched)")"
assert_status "$status" "200"
assert_json '.work_id and .package_status == "PACKAGE_FETCHED" and (.available_actions | type == "array")' "mark fetched response mismatch"

status="$(request GET "/works/${work_id}")"
assert_status "$status" "200"
assert_json '.package_status == "PACKAGE_FETCHED"' "WorkDetail did not retain PACKAGE_FETCHED"

log "checking not found error contract"
missing_work_id="00000000-0000-4000-8000-000000000000"
status="$(request GET "/works/${missing_work_id}")"
assert_status "$status" "404"
assert_error_response "NOT_FOUND"

log "checking controlled music failure and retry contract"
status="$(request POST "/works/lyrics" '{"song_title":"失败契约 Smoke","lyrics_input":"先失败，再重试"}' "$(idempotency_key failure-create)")"
assert_status "$status" "202"
failure_work_id="$(jq -er '.work_id' <"$RESULT_BODY")"
status="$(request POST "/works/${failure_work_id}/confirm" '{"music_provider":"suno"}' "$(idempotency_key failure-confirm)")"
assert_status "$status" "409"
assert_error_response "CONFLICT"

status="$(request GET "/works/${failure_work_id}")"
assert_status "$status" "200"
assert_work_detail_base
assert_json '.status == "FAILED" and .generation_stage == "FAILED" and .package_status == "PACKAGE_NOT_READY"' "failed WorkDetail state mismatch"
assert_json '.failure.failure_code == "MUSIC_GENERATION_FAILED" and (.failure.failure_message | length > 0) and .failure.retryable == true and .failure.retry_count == 0 and .failure.retry_limit == 2 and .failure.remaining_retry_count == 2 and .failure.recommended_action == "RETRY_MUSIC"' "FailureInfo fields mismatch"
assert_json '(.available_actions | index("RETRY_MUSIC")) != null' "RETRY_MUSIC action missing"

status="$(request POST "/works/${failure_work_id}/music/retry" '{"music_provider":"mock"}' "$(idempotency_key failure-retry)")"
assert_status "$status" "202"
assert_job_response
assert_json '.status == "GENERATED" and .generation_stage == "PACKAGE_READY"' "retry response state mismatch"
status="$(request GET "/works/${failure_work_id}")"
assert_status "$status" "200"
assert_json '.status == "GENERATED" and .package_status == "PACKAGE_READY" and .failure == null' "retry did not recover failed work"

log "PASS openapi contract smoke work_id=${work_id} failure_work_id=${failure_work_id}"
