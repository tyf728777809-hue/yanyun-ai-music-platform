# WellAPI Image2 Real Cover Stack Smoke v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The project now supports two Image 2 backends. `IMAGE2_BACKEND=wellapi` is the current public-network validation path, while `IMAGE2_BACKEND=dreammaker` remains the mandatory production-target path.

The Image 2 runbook explains manual verification, but there is no scripted single-work smoke entry. The missing script increases the chance that the operator opens multiple real providers at once, forgets to verify readiness, or records sensitive supplier output. A controlled smoke should run one work with mock music and real WellAPI Image 2 only.

Decision: WellAPI is a temporary public-network validation backend only. DreamMaker Image 2 MUST remain in code, configuration, runbooks, and handoff materials as the production-target interface.

## Functional Requirements

- FR-1: The stack smoke MUST refuse to run unless `ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1`.
- FR-2: The smoke MUST only test `IMAGE_PROVIDER=image2` with `IMAGE2_BACKEND=wellapi`.
- FR-3: The smoke MUST keep `MUSIC_PROVIDER=mock`, DeepSeek real calls disabled, render worker mocked, and company adapters mocked.
- FR-4: The stack smoke MUST collect `WELLAPI_API_KEY` using an existing environment variable or an interactive silent prompt.
- FR-5: The stack smoke MUST NOT print `WELLAPI_API_KEY`, Bearer tokens, full provider payloads, complete prompts, supplier image URLs, or platform signed package/media URLs.
- FR-6: The stack smoke MUST refuse to start when the local API port is already occupied, unless the operator uses the lower-level smoke script manually.
- FR-7: The stack smoke MUST start `music-api` with image real calls enabled for WellAPI and stop the process that it started on success, failure, or interruption.
- FR-8: The lower-level smoke MUST verify `/internal/integration-readiness` reports `image2_guard=READY_FOR_LOCAL`, `configured_mode=real-calls-enabled/wellapi`, and `music_provider=mock` before confirming the work.
- FR-9: The lower-level smoke MUST create one lyrics work, confirm it with mock music, poll until terminal state, and print only platform work/status evidence.
- FR-10: The lower-level smoke MUST verify the `COVER` media asset was produced by `wellapi-image2`, imported into platform object storage, has 1920x1080 workflow dimensions, and does not retain raw supplier URL or inline base64 metadata.
- FR-11: Both scripts MUST leave real credentials out of committed files and logs.

## Non-Functional Requirements

- NFR-1: Scripts MUST pass `bash -n`.
- NFR-2: Scripts MUST avoid command-line arguments containing secret values.
- NFR-3: Stack startup SHOULD fail within 60 seconds if API health does not become ready.
- NFR-4: Automated validation MUST NOT call WellAPI unless `ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1` is explicitly set by the operator.

## Acceptance Criteria

- AC-1: Given `ALLOW_WELLAPI_IMAGE2_REAL_SMOKE` is not `1`, when either script runs, then it exits before requiring credentials or starting processes. Covers FR-1 and NFR-4.
- AC-2: Given `WELLAPI_API_KEY` is missing and stdin is non-interactive, when the stack script runs with `ALLOW_WELLAPI_IMAGE2_REAL_SMOKE=1`, then it exits without starting API. Covers FR-4 through FR-7.
- AC-3: Given local port 8080 is occupied, when the stack script runs, then it refuses to start a second API process and points the operator to the lower-level script. Covers FR-6.
- AC-4: Given credentials are available and local port 8080 is free, when the stack script runs, then it starts API, waits for health, runs the lower-level smoke, and stops API. Covers FR-7 through FR-9.
- AC-5: Given WellAPI returns an image URL or `b64_json`, when the workflow completes, then the platform imports the cover into object storage and media metadata does not retain supplier raw URL or base64. Covers FR-10 and FR-11.
- AC-6: Given WellAPI returns 401 / 403 / 429 / timeout, when the smoke reaches terminal state, then the platform failure is visible without printing secrets. Covers FR-5 and FR-9.

## Edge Cases

- EC-1: Docker infrastructure is not running; API startup may fail, and logs MUST be available for diagnosis.
- EC-2: PostgreSQL container is unavailable after generation; the script MUST fail with work id evidence rather than claiming success.
- EC-3: WellAPI returns a success response without an image URL or `b64_json`; the platform MUST fail closed and the script MUST print only platform failure state.
- EC-4: Operator interrupts with Ctrl-C; the trap MUST stop the API process started by the stack script.

## API Contracts

N/A - scripts use existing HTTP endpoints:

```typescript
GET /health
GET /internal/integration-readiness
POST /api/v1/works/lyrics
GET /api/v1/works/{work_id}
POST /api/v1/works/{work_id}/confirm
GET /api/v1/works/{work_id}/publish-package
```

## Data Models

N/A - no new database schema. The smoke writes normal `works`, `generation_jobs`, `provider_calls`, `media_assets`, and `publish_packages` rows.

The script verifies the existing `media_assets` `COVER` row:

| Field | Expected |
|---|---|
| `asset_type` | `COVER` |
| `provider` | `wellapi-image2` |
| `width` | `1920` |
| `height` | `1080` |
| `metadata_json.provider` | `wellapi-image2` |
| `metadata_json.object_storage_imported` | `true` |
| `metadata_json.source_url` | absent |
| `metadata_json.inline_base64` | absent |

## Out of Scope

- OS-1: The WellAPI smoke scripts MUST NOT test DreamMaker Image 2; this scope limit does not permit removing or weakening the separate DreamMaker production-target path.
- OS-2: The scripts MUST NOT write real credentials to `.env`, docs, logs, or Git.
- OS-3: The scripts MUST NOT start or stop Docker infrastructure automatically.
- OS-4: The scripts MUST NOT open real DeepSeek, Suno, MiniMax, render-worker local-process, or company Adapter calls.
