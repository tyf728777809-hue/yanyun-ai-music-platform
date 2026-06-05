# Temporal Song Production Orchestration Spec v0.1

## Metadata

- Author: Codex
- Date: 2026-06-06
- Status: Approved for local implementation
- Reviewers: Project owner, Codex
- Scope: 第 4 批 Temporal 真实编排基础

## Context

The platform already has a local synchronous song production path and an Outbox v0.1 dispatcher.
`WorkService` can reserve a work, insert a `SONG_PRODUCTION` job, and enqueue a
`SONG_PRODUCTION_REQUESTED` outbox event. The dispatcher currently executes the Spring
`SongProductionWorkflow` bean directly inside `music-api`, which verifies the state transition
model but does not yet prove that long-running generation work can be picked up by an independent
worker process.

The project target is commercial-grade local completion before company integration. Before real
model calls and MP4 rendering are widened, the system needs a reliable handoff from API transaction
to Temporal worker execution. This stage should establish that handoff without changing user-facing
API contracts and without calling real DeepSeek, DreamMaker, Image 2, or company systems in
automated tests.

This stage intentionally reuses the existing `MockSongProductionWorkflow` business delegate as the
activity implementation. It avoids rewriting the whole production chain at once. Later batches can
split music generation, cover generation, video rendering, package build, moderation, and quota
steps into separate Temporal activities after the basic worker/client path is proven.

## Functional Requirements

- FR-1: The system MUST keep the existing `sync` dispatch mode for local compatibility.
- FR-2: The system MUST add a Temporal dispatch mode that starts a Temporal workflow instead of
  executing song production inside `music-api`.
- FR-3: The Temporal workflow MUST accept the existing `SongProductionWorkflowInput` payload.
- FR-4: The Temporal workflow MUST return `SongProductionWorkflowResult`.
- FR-5: The Temporal worker MUST register a workflow implementation and an activity implementation
  on `TEMPORAL_TASK_QUEUE`.
- FR-6: The Temporal activity MUST delegate to the existing Spring `SongProductionWorkflow` business
  implementation for this stage.
- FR-7: The API outbox dispatcher MUST mark the outbox event `SUCCEEDED` after a Temporal workflow
  is started successfully, not after the full production chain completes.
- FR-8: The Temporal workflow id MUST be deterministic by work id and job id so duplicate outbox
  retries do not create duplicate production executions.
- FR-9: The API MUST NOT require a running Temporal service in default `sync` mode.
- FR-10: The API MUST fail the outbox event with a sanitized `WORKFLOW_DISPATCH_FAILED` style
  failure when Temporal start fails.
- FR-11: Automated tests MUST NOT call real external model providers or company systems.
- FR-12: The implementation MUST preserve current OpenAPI request and response shapes.

## Non-Functional Requirements

- NFR-1: Temporal client and worker configuration MUST be environment-driven through
  `TEMPORAL_TARGET`, `TEMPORAL_NAMESPACE`, and `TEMPORAL_TASK_QUEUE`.
- NFR-2: Temporal workflow/activity timeouts MUST be explicit and locally conservative.
- NFR-3: Logs MUST NOT include provider secrets, JWTs, user tokens, lyrics payload dumps, or raw
  external response payloads.
- NFR-4: The default local development path MUST still pass without Temporal worker startup.
- NFR-5: Temporal smoke verification SHOULD prove API and worker can run as separate local
  processes against local Temporal.
- NFR-6: Worker shutdown SHOULD close Temporal resources cleanly.

## Acceptance Criteria

- AC-1: Given default `MUSIC_WORKFLOW_DISPATCH_MODE=sync`, when confirm work is called, then the API
  still executes the existing Spring song production bean inline and does not require Temporal.
  References FR-1 and FR-9.
- AC-2: Given `MUSIC_WORKFLOW_DISPATCH_MODE=outbox` and
  `WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal`, when an outbox event is drained, then the dispatcher
  starts a Temporal workflow with `SongProductionWorkflowInput` and marks the event `SUCCEEDED`.
  References FR-2, FR-3, FR-7, and FR-8.
- AC-3: Given the same outbox event is retried after a successful Temporal start, when the dispatcher
  drains it again, then it uses the same workflow id and does not create a second independent
  production execution. References FR-8.
- AC-4: Given the Temporal service is unavailable, when the dispatcher tries to start a Temporal
  workflow, then the outbox event is marked failed with a sanitized failure code/message and remains
  eligible for configured retry while attempts remain. References FR-10 and NFR-3.
- AC-5: Given `music-worker` starts with local Temporal available, when it initializes, then it
  registers the song production workflow and activity on `TEMPORAL_TASK_QUEUE`. References FR-5 and
  NFR-1.
- AC-6: Given a Temporal workflow is executed by `music-worker`, when the activity runs, then it
  delegates to the existing Spring `SongProductionWorkflow` implementation and returns
  `SongProductionWorkflowResult`. References FR-4 and FR-6.
- AC-7: Given automated tests run, when all test suites execute, then no real DeepSeek, DreamMaker,
  Image 2, or company API call is made. References FR-11.

## Edge Cases

- EC-1: Duplicate outbox claim after process crash MUST reuse the same Temporal workflow id.
- EC-2: Terminal work states (`GENERATED`, `PACKAGE_READY`, `PACKAGE_FETCHED`) MUST still be skipped
  before Temporal start.
- EC-3: Invalid outbox JSON MUST fail as `WORKFLOW_PAYLOAD_INVALID` and MUST NOT start Temporal.
- EC-4: Temporal unavailable, namespace unavailable, or task queue misconfiguration MUST fail the
  outbox event without leaking raw connection details beyond target/namespace/task queue.
- EC-5: Activity failure MUST let Temporal record the failure. In v0.1 the activity retry policy is
  intentionally fixed to one attempt because the delegate writes quota, provider calls, media
  assets, publish packages, and job state; automatic activity retry must wait until those writes are
  audited as idempotent. The existing business delegate is still responsible for marking work/job
  failure when it catches provider or package errors.
- EC-6: Worker startup without Temporal available SHOULD fail fast in local verification mode, as the
  existing connection verifier already does.

## API Contracts

No public HTTP API change is required in this stage.

Internal contracts:

```java
interface TemporalSongProductionStarter {
  String start(SongProductionWorkflowInput input);
}

interface TemporalSongProductionWorkflow {
  SongProductionWorkflowResult produce(SongProductionWorkflowInput input);
}

interface SongProductionActivities {
  SongProductionWorkflowResult produce(SongProductionWorkflowInput input);
}
```

Configuration:

```text
MUSIC_WORKFLOW_DISPATCH_MODE=sync|outbox
WORKFLOW_OUTBOX_DISPATCH_TARGET=local|temporal
TEMPORAL_TARGET=localhost:7233
TEMPORAL_NAMESPACE=default
TEMPORAL_TASK_QUEUE=song-production-local
```

Workflow v0.1 uses a 30 minute activity start-to-close timeout and `maximumAttempts=1` in code.
Timeout and retry tuning can become environment-driven after the activity delegate is split into
idempotent steps.

## Data Models

No database migration is required in this stage.

Existing `workflow_outbox` remains the reliability boundary:

| Field | Type | Requirement |
|---|---|---|
| `id` | UUID | Outbox event id; unchanged |
| `aggregate_id` | UUID | Work id; used as part of deterministic Temporal workflow id |
| `payload_json` | JSONB | Serialized `SongProductionWorkflowInput` |
| `status` | text | `SUCCEEDED` after Temporal start succeeds |
| `attempt_count` | integer | Incremented only when Temporal start fails |
| `failure_code` | text | Sanitized dispatch failure code |
| `failure_message` | text | Sanitized dispatch failure message |

Temporal workflow id:

| Component | Source | Notes |
|---|---|---|
| Prefix | constant `song-production` | Keeps workflow ids easy to find |
| Work id | `input.workId()` | Ensures one production chain per work |
| Job id | `input.jobId()` | Distinguishes retry attempts that intentionally create new jobs |

## Out of Scope

- OS-1: Splitting music generation, cover generation, video rendering, moderation, and package build
  into separate activities is out of scope for this stage; this stage uses one delegate activity.
- OS-2: Real DeepSeek, DreamMaker, Image 2, and company API calls are out of scope for automated
  tests.
- OS-3: Changing OpenAPI v0.1 response shapes is out of scope.
- OS-4: Production Kubernetes deployment and secret manager integration are out of scope.
- OS-5: Provider cost accounting and cross-provider fallback are out of scope.
