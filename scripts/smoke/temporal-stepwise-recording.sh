#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080/api/v1}"
API_HEALTH_URL="${API_HEALTH_URL:-http://localhost:8080/health}"
MOCK_USER_ID="${MOCK_USER_ID:-stepwise_smoke_$(date +%s)_$RANDOM}"
EXPECTED_STEP_COUNT="${EXPECTED_STEP_COUNT:-13}"
CHECK_DB="${CHECK_DB:-true}"

fail() {
  printf '[stepwise-smoke] ERROR: %s\n' "$*" >&2
  exit 1
}

log() {
  printf '[stepwise-smoke] %s\n' "$*"
}

need_command() {
  command -v "$1" >/dev/null 2>&1 || fail "missing command: $1"
}

idempotency_key() {
  printf 'stepwise-smoke-%s-%s-%s' "$1" "$(date +%s)" "$RANDOM"
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

psql_query() {
  local sql="$1"
  docker exec yanyun-postgres psql -U postgres -d yanyun_music -Atc "$sql"
}

need_command curl
need_command jq
if [[ "$CHECK_DB" == "true" ]]; then
  need_command docker
fi

log "checking API health: ${API_HEALTH_URL}"
curl -sS -f "$API_HEALTH_URL" | jq -e '.status == "OK"' >/dev/null

create_body="$(
  jq -n \
    --arg title "Stepwise Smoke" \
    --arg lyrics $'Wind over Yanmen\nDrums under moon' \
    --arg reference "Yanmen pass and frontier drums" \
    '{song_title:$title, lyrics_input:$lyrics, style_tags:["smoke","stepwise"], yanyun_reference:$reference}'
)"

log "creating lyrics work"
create_response="$(post_json "/works/lyrics" "$(idempotency_key lyrics)" "$create_body")"
assert_json "$create_response" '.status == "LYRICS_READY" and .generation_stage == "WAITING_CONFIRM"' "work did not enter lyrics ready state"
work_id="$(jq -er '.work_id' <<<"$create_response")"
log "created work_id=${work_id}"

confirm_body='{"music_provider":"mock"}'
log "confirming work through outbox"
confirm_response="$(post_json "/works/${work_id}/confirm" "$(idempotency_key confirm)" "$confirm_body")"
assert_json "$confirm_response" '.status == "GENERATING" and .generation_stage == "QUOTA_LOCKING"' "confirm did not enter outbox generating state"
job_id="$(jq -er '.job_id' <<<"$confirm_response")"
log "reserved job_id=${job_id}"

if [[ "$CHECK_DB" != "true" ]]; then
  log "PASS work_id=${work_id} job_id=${job_id} without DB checks"
  exit 0
fi

outbox_status=""
step_count="0"
for _ in $(seq 1 30); do
  outbox_status="$(psql_query "select status from workflow_outbox where aggregate_id = '${work_id}'::uuid order by created_at desc limit 1;")"
  step_count="$(psql_query "select count(*) from generation_job_steps where job_id = '${job_id}'::uuid;")"
  if [[ "$outbox_status" == "SUCCEEDED" && "$step_count" == "$EXPECTED_STEP_COUNT" ]]; then
    break
  fi
  sleep 1
done

[[ "$outbox_status" == "SUCCEEDED" ]] || fail "workflow_outbox did not succeed, status=${outbox_status}"
[[ "$step_count" == "$EXPECTED_STEP_COUNT" ]] || fail "unexpected generation_job_steps count=${step_count}"

outbox_row="$(psql_query "select status, event_type, processed_at is not null, coalesce(failure_code, '') from workflow_outbox where aggregate_id = '${work_id}'::uuid order by created_at desc limit 1;")"
[[ "$outbox_row" == "SUCCEEDED|SONG_PRODUCTION_REQUESTED|t|" ]] || fail "unexpected outbox row: ${outbox_row}"

failed_steps="$(psql_query "select count(*) from generation_job_steps where job_id = '${job_id}'::uuid and status <> 'SUCCEEDED';")"
[[ "$failed_steps" == "0" ]] || fail "found failed or non-succeeded steps: ${failed_steps}"

first_last_steps="$(psql_query "select min(step_name), max(step_name) from generation_job_steps where job_id = '${job_id}'::uuid;")"
[[ -n "$first_last_steps" ]] || fail "step names missing"

work_job_row="$(psql_query "select j.status, j.stage, w.status, w.generation_stage, w.package_status from generation_jobs j join works w on w.id = j.work_id where j.id = '${job_id}'::uuid;")"
[[ "$work_job_row" == "RUNNING|QUOTA_LOCKING|GENERATING|QUOTA_LOCKING|PACKAGE_NOT_READY" ]] \
  || fail "unexpected work/job state for stepwise-recording boundary: ${work_job_row}"

log "PASS work_id=${work_id} job_id=${job_id} steps=${step_count}"
