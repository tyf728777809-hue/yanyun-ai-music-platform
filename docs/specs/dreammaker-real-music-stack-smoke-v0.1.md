# DreamMaker Real Music Stack Smoke v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The project already has `scripts/smoke/dreammaker-real-music-smoke.sh` for a single real Suno or MiniMax work, but a manual run still requires the operator to open multiple terminals, inject DreamMaker credentials into both API and worker processes, verify ports, and then run the smoke script from another terminal. This is easy to execute incorrectly during the first real Suno attempt.

The next milestone is a controlled Suno smoke, not a broad production integration. The system MUST keep company account, moderation, quota, publish, and share boundaries mocked. The operator MUST be able to run one Provider and one work while avoiding command-history credential exposure and leaving no API/worker process behind.

## Functional Requirements

- FR-1: The stack smoke MUST refuse to run unless `ALLOW_DREAMMAKER_REAL_SMOKE=1`.
- FR-2: The stack smoke MUST support `REAL_PROVIDER=suno|minimax` and default to `suno`.
- FR-3: The stack smoke MUST collect `DREAMMAKER_ACCESS_KEY` and `DREAMMAKER_SECRET_KEY` using existing environment variables or interactive silent prompts.
- FR-4: The stack smoke MUST NOT print DreamMaker AK/SK, JWT, `X-Access-Token`, or full provider payload.
- FR-5: The stack smoke MUST refuse to start when local API or worker ports are already occupied, unless the operator uses the lower-level smoke script manually.
- FR-6: The stack smoke MUST start `music-worker` with `MUSIC_PROVIDER=mock`, `DREAMMAKER_REAL_CALLS_ENABLED=true`, and `TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy`.
- FR-7: The stack smoke MUST start `music-api` with `MUSIC_WORKFLOW_DISPATCH_MODE=outbox`, `WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal`, `WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true`, `MUSIC_PROVIDER=mock`, and `DREAMMAKER_REAL_CALLS_ENABLED=true`.
- FR-8: The stack smoke MUST wait for worker and API health endpoints before running the real Provider smoke.
- FR-9: The stack smoke MUST delegate the actual single-work Provider validation to `scripts/smoke/dreammaker-real-music-smoke.sh`.
- FR-10: The stack smoke MUST stop the API and worker processes that it started on success, failure, or interruption.
- FR-11: The stack smoke MUST write process logs under `build/smoke/` and print only log file paths.

## Non-Functional Requirements

- NFR-1: The script MUST be shell-checkable with `bash -n`.
- NFR-2: The script MUST avoid command-line arguments containing secret values.
- NFR-3: The script SHOULD fail within 60 seconds if worker or API health does not become ready.
- NFR-4: The script SHOULD leave `DREAMMAKER_REAL_CALLS_ENABLED=false` in the parent shell unaffected, because child process environment changes cannot mutate the caller.

## Acceptance Criteria

- AC-1: Given `ALLOW_DREAMMAKER_REAL_SMOKE` is not `1`, when the operator runs the script, then it exits before starting API or worker. Covers FR-1.
- AC-2: Given `REAL_PROVIDER=invalid`, when the operator runs the script, then it exits before starting API or worker. Covers FR-2.
- AC-3: Given credentials are missing and stdin is non-interactive, when the operator runs the script, then it exits without starting API or worker. Covers FR-3 and FR-4.
- AC-4: Given credentials are available and local ports are free, when the operator runs the script, then it starts worker/API, waits for health, runs the lower-level smoke, and stops both processes. Covers FR-6 through FR-10.
- AC-5: Given the lower-level smoke fails, when the script exits, then it still stops worker/API and prints log file paths. Covers FR-10 and FR-11.

## Edge Cases

- EC-1: Port 8080 is already occupied by another API process; the script MUST refuse to run.
- EC-2: Port 8081 is already occupied by another worker process; the script MUST refuse to run.
- EC-3: Docker infrastructure is not running; API or worker startup may fail, and logs MUST be available for diagnosis.
- EC-4: Worker starts but Temporal is unavailable; the script MUST time out waiting for worker/API readiness or the lower-level smoke MUST fail with readiness evidence.
- EC-5: Operator interrupts with Ctrl-C; the trap MUST stop started child processes.

## API Contracts

N/A - this script uses existing HTTP endpoints:

```typescript
GET /health
GET /actuator/health
POST /api/v1/works/lyrics
POST /api/v1/works/{work_id}/confirm
GET /api/v1/works/{work_id}
GET /api/v1/works/{work_id}/publish-package
GET /internal/integration-readiness
```

## Data Models

N/A - no new database tables or persisted business fields. The lower-level smoke writes normal `works`, `generation_jobs`, `provider_calls`, `media_assets`, and `publish_packages` rows.

## Out of Scope

- OS-1: The script MUST NOT test both Suno and MiniMax in one run.
- OS-2: The script MUST NOT write real credentials to `.env`, docs, logs, or Git.
- OS-3: The script MUST NOT replace the full DreamMaker runbook or company deployment process.
- OS-4: The script MUST NOT start or stop Docker infrastructure automatically.
