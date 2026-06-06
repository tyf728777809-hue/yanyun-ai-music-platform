#!/usr/bin/env bash
set -euo pipefail
set +x

API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api/v1}"
API_HEALTH_URL="${API_HEALTH_URL:-http://localhost:8080/health}"
MOCK_USER_ID="${MOCK_USER_ID:-mock_package_block_smoke}"
IDEMPOTENCY_PREFIX="${IDEMPOTENCY_PREFIX:-package-block-smoke-$(date +%s)}"

fail() {
  printf '[package-block-smoke] ERROR: %s\n' "$*" >&2
  if [[ -n "${RESULT_BODY:-}" && -f "$RESULT_BODY" ]]; then
    printf '[package-block-smoke] last response: %s\n' "$(jq -c . "$RESULT_BODY" 2>/dev/null || cat "$RESULT_BODY")" >&2
  fi
  exit 1
}

log() {
  printf '[package-block-smoke] %s\n' "$*"
}

need_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
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

need_command curl
need_command jq

TMP_DIR="$(mktemp -d)"
RESULT_BODY="${TMP_DIR}/response.json"
trap 'rm -rf "$TMP_DIR"' EXIT

log "checking API health: ${API_HEALTH_URL}"
health_status="$(curl -sS -o "$RESULT_BODY" -w "%{http_code}" "$API_HEALTH_URL" || true)"
assert_status "$health_status" "200"
assert_json '.status == "OK"' "API health did not return status OK"

create_body="$(
  jq -n \
    --arg title "Package Block Smoke" \
    --arg lyrics $'雁门风起，鼓声落入旧梦。\n长街灯火，照见故人眉峰。' \
    --arg reference "雁门、长街、旧友" \
    '{song_title:$title, lyrics_input:$lyrics, style_tags:["package-block","mock"], yanyun_reference:$reference}'
)"

log "creating lyrics work for mock user ${MOCK_USER_ID}"
status="$(request POST "/works/lyrics" "$create_body" "${IDEMPOTENCY_PREFIX}-lyrics")"
assert_status "$status" "202"
assert_json '.status == "LYRICS_READY" and .generation_stage == "WAITING_CONFIRM"' "created work did not enter lyrics ready"
work_id="$(jq -er '.work_id' "$RESULT_BODY")"
log "created work_id=${work_id}"

log "confirming work; this should be blocked before publish handoff"
confirm_body='{"music_provider":"mock"}'
status="$(request POST "/works/${work_id}/confirm" "$confirm_body" "${IDEMPOTENCY_PREFIX}-confirm")"
if [[ "$status" != "403" ]]; then
  fail "publish package was not blocked. Start API with MOCK_MODERATION_PUBLISH_PACKAGE_BLOCKED_USER_IDS=${MOCK_USER_ID} and keep MUSIC_PROVIDER=mock."
fi
assert_json '.error.code == "FORBIDDEN" or .error.code == "CONFLICT"' "confirm error should be structured"

log "checking blocked work detail"
status="$(request GET "/works/${work_id}")"
assert_status "$status" "200"
assert_json '.status == "FAILED" and .generation_stage == "FAILED"' "blocked work did not fail cleanly"
assert_json '.package_status == "PACKAGE_BLOCKED"' "blocked work did not expose PACKAGE_BLOCKED package status"
assert_json '.failure.failure_code == "PACKAGE_BLOCKED" and .failure.retryable == false' "blocked work failure info mismatch"
assert_json '(.available_actions | index("CONTACT_SUPPORT")) != null and (.available_actions | index("RETURN_TO_EDIT")) != null' "blocked work actions missing"
assert_json '(.available_actions | index("MARK_PACKAGE_FETCHED")) == null and (.available_actions | index("REFRESH_PACKAGE_URL")) == null' "blocked work should not expose handoff actions"

log "checking lightweight publish-package view"
status="$(request GET "/works/${work_id}/publish-package")"
assert_status "$status" "200"
assert_json '.package_status == "PACKAGE_BLOCKED" and .package_url == null and .package_json == null' "blocked publish package response mismatch"
assert_json '.blocked_reason == "作品暂不能交给社区发布。"' "blocked reason missing"

log "PASS package block smoke. work_id=${work_id}"
