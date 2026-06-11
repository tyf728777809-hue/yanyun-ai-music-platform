# Yunwu Suno Real Music Stack Smoke v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

DreamMaker remains the production-target provider, but the first real DreamMaker Suno smoke reached the provider submission step and returned HTTP 403. The user judged that the current machine is not in the company intranet, and DreamMaker may only be available from that environment.

The immediate need is a public-network controlled Suno smoke through Yunwu, without deleting or weakening DreamMaker. The smoke must keep company account, moderation, quota, publish, and share systems mocked, run exactly one Suno work, use outbox + Temporal worker, avoid command-history credential exposure, and clean up any API/worker process it starts.

Decision: Yunwu is a temporary public-network validation backend only. DreamMaker music and DreamMaker Image 2 remain mandatory production-target interfaces and MUST stay in code, configuration, runbooks, and handoff materials.

## Functional Requirements

- FR-1: The stack smoke MUST refuse to run unless both `ALLOW_REAL_MODEL_SMOKE=1` and `ALLOW_YUNWU_REAL_SMOKE=1` are set.
- FR-2: The stack smoke MUST only test `music_provider=suno` with `SUNO_BACKEND=yunwu`.
- FR-3: The stack smoke MUST collect `YUNWU_API_KEY` using an existing environment variable or an interactive silent prompt.
- FR-4: The stack smoke MUST NOT print `YUNWU_API_KEY`, Bearer tokens, full provider payloads, supplier audio URLs, full provider task ids, or platform signed package/media URLs.
- FR-5: The stack smoke MUST refuse to start when local API or worker ports are already occupied, unless the operator uses the lower-level smoke script manually.
- FR-6: The stack smoke MUST start `music-worker` with `MUSIC_PROVIDER=suno`, `SUNO_BACKEND=yunwu`, `YUNWU_REAL_CALLS_ENABLED=true`, and `TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy`.
- FR-7: The stack smoke MUST start `music-api` with `MUSIC_WORKFLOW_DISPATCH_MODE=outbox`, `WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal`, `WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true`, `MUSIC_PROVIDER=suno`, `SUNO_BACKEND=yunwu`, and `YUNWU_REAL_CALLS_ENABLED=true`.
- FR-8: The stack smoke MUST wait for worker and API health endpoints before running the single-work smoke.
- FR-9: The lower-level smoke MUST verify `/internal/integration-readiness` reports `yunwu_suno_guard=READY_FOR_LOCAL` and `workflow_dispatch=outbox/temporal` before confirming the work.
- FR-10: The lower-level smoke MUST create one lyrics work, confirm it with `music_provider=suno`, poll until terminal state, and print only platform work/status evidence.
- FR-11: The stack smoke MUST stop API and worker processes that it started on success, failure, or interruption.
- FR-12: Both scripts MUST leave real credentials out of committed files and logs.
- FR-13: The stack smoke SHOULD set `YUNWU_REQUEST_TIMEOUT` explicitly so request timeout does not depend on application defaults.

## Non-Functional Requirements

- NFR-1: Scripts MUST pass `bash -n`.
- NFR-2: Scripts MUST avoid command-line arguments containing secret values.
- NFR-3: Stack startup SHOULD fail within 60 seconds if worker or API health does not become ready.
- NFR-4: Automated validation MUST NOT call Yunwu unless both `ALLOW_REAL_MODEL_SMOKE=1` and `ALLOW_YUNWU_REAL_SMOKE=1` are explicitly set by the operator.
- NFR-5: Real audio source import SHOULD rely on the platform remote object importer, which follows redirects and retries transient CDN download failures.

## Acceptance Criteria

- AC-1: Given either allow gate is missing, when either script runs, then it exits before requiring credentials or starting processes. Covers FR-1 and NFR-4.
- AC-2: Given `YUNWU_API_KEY` is missing and stdin is non-interactive, when the stack script runs with both allow gates, then it exits without starting API or worker. Covers FR-3 through FR-5.
- AC-3: Given local port 8080 or 8081 is occupied, when the stack script runs, then it refuses to start a second process and points the operator to the lower-level script. Covers FR-5.
- AC-4: Given credentials are available and local ports are free, when the stack script runs, then it starts worker/API, waits for health, runs the lower-level smoke, and stops both processes. Covers FR-6 through FR-11.
- AC-5: Given the provider returns 401 / 403, when the smoke reaches terminal state, then the platform failure code is `PROVIDER_AUTH_FAILED` and the script exits non-zero without printing secrets. Covers FR-4, FR-10, and FR-12.

## Edge Cases

- EC-1: Docker infrastructure is not running; API or worker startup may fail, and logs MUST be available for diagnosis.
- EC-2: Temporal is unavailable; worker/API health may pass but workflow may not complete, and the lower-level smoke MUST time out with work id evidence.
- EC-3: Yunwu returns a task id but later returns failure; the script MUST show platform failure state and provider call summary without raw payload.
- EC-4: Operator interrupts with Ctrl-C; the trap MUST stop started child processes.

## API Contracts

N/A - scripts use existing HTTP endpoints:

```typescript
GET /health
GET /actuator/health
GET /internal/integration-readiness
POST /api/v1/works/lyrics
GET /api/v1/works/{work_id}
POST /api/v1/works/{work_id}/confirm
GET /api/v1/works/{work_id}/publish-package
```

## Data Models

N/A - no new database schema. The smoke writes normal `works`, `generation_jobs`, `provider_calls`, `media_assets`, and `publish_packages` rows.

## Out of Scope

- OS-1: The Yunwu smoke scripts MUST NOT test DreamMaker or MiniMax; this scope limit does not permit removing or weakening the separate DreamMaker production-target path.
- OS-2: The scripts MUST NOT write real credentials to `.env`, docs, logs, or Git.
- OS-3: The scripts MUST NOT start or stop Docker infrastructure automatically.
- OS-4: The scripts MUST NOT bypass company Adapter / Mock boundaries.
