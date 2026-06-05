# Reliable Song Production Orchestration Spec v0.1

## Metadata

- Author: Codex
- Date: 2026-06-05
- Status: Approved for autonomous local implementation
- Scope: Song production workflow launch consistency, outbox foundation, and local async dispatch

## Context

The platform can currently run a complete local mock song production path, and Suno/MiniMax now have
DreamMaker submit/poll skeletons. The remaining architectural risk before real model calls is workflow
launch consistency: `confirmWork` and `retryMusic` still call `SongProductionWorkflow.produce(...)`
synchronously inside the API request path.

Real provider calls and MP4 rendering can take minutes and fail in ways that require retry, recovery,
or operator inspection. Before enabling real model traffic by default, the platform needs a durable
outbox record that represents "start song production for this work" inside the same database
transaction that reserves the work state. The first implementation should preserve the existing
default synchronous local path while introducing a switchable outbox mode that can be tested without
external providers.

## Functional Requirements

- FR-1: The system MUST add a durable `workflow_outbox` table for workflow launch requests.
- FR-2: Each outbox row MUST include event type, aggregate id, payload JSON, status, attempt count,
  next attempt time, lock metadata, and sanitized failure fields.
- FR-3: `confirmWork` MUST support the current synchronous dispatch mode as the default local mode.
- FR-4: `confirmWork` MUST support an outbox dispatch mode that marks the work as
  `GENERATING / QUOTA_LOCKING`, creates a `generation_jobs` row, writes an outbox event in the same
  transaction, and returns HTTP 202 without running the workflow inline.
- FR-5: `retryMusic` MUST support outbox dispatch mode after it reserves retry state with
  `works.version`, using the same generation job and outbox semantics.
- FR-6: A local dispatcher MUST be able to claim due `PENDING` or retryable `FAILED` outbox rows,
  execute `SongProductionWorkflow.produce(...)`, and mark the event `SUCCEEDED` or `FAILED`.
- FR-7: Dispatcher execution MUST be idempotent enough for local recovery: already terminal works
  MUST not be reprocessed into duplicate package-ready state by the same event.
- FR-8: Failed dispatcher attempts MUST increment attempt count and schedule bounded retry before
  marking an event terminally failed.
- FR-9: Automated tests MUST NOT call real DreamMaker, Suno, MiniMax, DeepSeek, Image 2, or company
  systems.

## Non-Functional Requirements

- NFR-1: Default local behavior MUST remain backward compatible: `MUSIC_WORKFLOW_DISPATCH_MODE=sync`
  keeps current immediate package-ready mock flow.
- NFR-2: Outbox mode MUST be enabled only by explicit local configuration.
- NFR-3: Outbox payloads MUST NOT contain secrets, raw provider credentials, or user private contact
  data.
- NFR-4: Dispatcher logs and failure fields MUST be sanitized and capped in length.
- NFR-5: Outbox polling MUST be bounded and low-frequency by default to avoid noisy local CPU usage.

## Acceptance Criteria

- AC-1: Given default config, when `confirmWork` is called, then it still runs the mock workflow
  inline and returns `GENERATED / PACKAGE_READY`.
- AC-2: Given outbox mode, when `confirmWork` is called, then it returns a job accepted response with
  work status `GENERATING`, generation stage `QUOTA_LOCKING`, and one `PENDING` outbox row.
- AC-3: Given outbox mode and a pending song-production event, when the local dispatcher runs, then
  it executes the workflow and marks the event `SUCCEEDED`.
- AC-4: Given outbox mode and a workflow exception, when the dispatcher runs, then it increments
  attempt count, stores a sanitized failure message, and schedules the next attempt.
- AC-5: Given retry limit exhaustion or non-retryable event failure, when max attempts are reached,
  then the outbox row remains `FAILED` with no immediate next attempt.
- AC-6: Given default sync mode, when the existing mock HTTP smoke path runs, then it remains green.
- AC-7: Given outbox mode, when a work is confirmed and the dispatcher drains due events, then the
  work reaches `GENERATED / PACKAGE_READY` without a second confirm request.

## Edge Cases

- EC-1: If an outbox event references a missing work or missing lyrics draft, the event fails with a
  sanitized `WORKFLOW_DISPATCH_FAILED` style message.
- EC-2: If the dispatcher crashes after claiming but before completing an event, the lock can expire
  and a later dispatcher can reclaim it.
- EC-3: If the API transaction rolls back, no outbox event should exist.
- EC-4: If the same idempotency key is replayed in outbox mode, the same accepted response should be
  replayed by existing idempotency semantics.
- EC-5: If an outbox-mode confirm is called again while the work is already `GENERATING`, it should
  be rejected by the existing state machine.

## API Contracts

No public endpoint is added in v0.1.

Configuration:

```text
MUSIC_WORKFLOW_DISPATCH_MODE=sync|outbox
WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true|false
WORKFLOW_OUTBOX_POLL_INTERVAL=5s
WORKFLOW_OUTBOX_BATCH_SIZE=5
WORKFLOW_OUTBOX_MAX_ATTEMPTS=3
WORKFLOW_OUTBOX_LOCK_TIMEOUT=60s
```

Internal event payload:

```json
{
  "workflow_type": "SONG_PRODUCTION",
  "work_id": "uuid",
  "job_id": "uuid",
  "user_id": "string",
  "lyrics_draft_id": "uuid",
  "song_title": "string",
  "song_summary": "string",
  "lyrics_text": "string",
  "music_prompt": "string",
  "vocal_preference": "AUTO",
  "music_provider": "mock|suno|minimax|null",
  "music_retry_allowed_after_failure": true
}
```

## Data Models

`workflow_outbox`

| Field | Type | Constraints |
|---|---|---|
| `id` | UUID | Primary key |
| `aggregate_type` | VARCHAR(64) | `WORK` |
| `aggregate_id` | UUID | Work id |
| `event_type` | VARCHAR(128) | `SONG_PRODUCTION_REQUESTED` |
| `payload_json` | JSONB | Required |
| `status` | VARCHAR(32) | `PENDING`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `SKIPPED` |
| `attempt_count` | INTEGER | `>= 0` |
| `max_attempts` | INTEGER | `> 0` |
| `next_attempt_at` | TIMESTAMPTZ | Nullable |
| `locked_at` | TIMESTAMPTZ | Nullable |
| `locked_by` | VARCHAR(128) | Nullable |
| `processed_at` | TIMESTAMPTZ | Nullable |
| `failure_code` | VARCHAR(128) | Nullable |
| `failure_message` | TEXT | Sanitized and capped |
| `created_at` | TIMESTAMPTZ | Required |
| `updated_at` | TIMESTAMPTZ | Required |

## Out of Scope

- Full Temporal Java workflow implementation is out of scope for this v0.1 stage.
- Distributed multi-worker production locking tuning is out of scope; local Postgres row claiming is
  sufficient here.
- Changing frontend polling behavior is out of scope; current status endpoint already exposes work
  state and generation stage.
- Real provider calls and real MP4 rendering are out of scope for automated tests.
