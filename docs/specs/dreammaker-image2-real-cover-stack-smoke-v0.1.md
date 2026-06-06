# DreamMaker Image2 Real Cover Stack Smoke v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The project supports two Image 2 backends. `IMAGE2_BACKEND=wellapi` is the current public-network validation path, while `IMAGE2_BACKEND=dreammaker` is the mandatory production-target path.

WellAPI already has a scripted single-work smoke. DreamMaker Image 2 was still preflight/manual only, which leaves a gap in production-target handoff: company developers can see the client and runbook, but not an executable controlled smoke path. A dedicated smoke is needed to verify one real DreamMaker Image 2 cover while keeping music, DeepSeek, render worker, and company Adapters mocked.

Decision: DreamMaker Image 2 remains the production-target interface. Yunwu and WellAPI are public-network controlled smoke paths only and do not replace this DreamMaker path.

## Functional Requirements

- FR-1: The stack smoke MUST refuse to run unless `ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1`.
- FR-2: The smoke MUST only test `IMAGE_PROVIDER=image2` with `IMAGE2_BACKEND=dreammaker`.
- FR-3: The smoke MUST require `IMAGE_REAL_CALLS_ENABLED=true` and `DREAMMAKER_REAL_CALLS_ENABLED=true`.
- FR-4: The smoke MUST keep `MUSIC_PROVIDER=mock`, DeepSeek real calls disabled, Yunwu disabled, render worker mocked, and company Adapters mocked.
- FR-5: The stack smoke MUST collect `DREAMMAKER_ACCESS_KEY` and `DREAMMAKER_SECRET_KEY` using existing environment variables or interactive silent prompts.
- FR-6: The stack smoke MUST NOT print DreamMaker AK/SK, JWTs, `X-Access-Token`, full provider payloads, complete prompts, provider task ids, or supplier image URLs.
- FR-7: The stack smoke MUST refuse to start when local API port `8080` is already occupied, unless the operator uses the lower-level smoke script manually.
- FR-8: The stack smoke MUST start `music-api` with DreamMaker Image 2 real calls enabled and stop the process that it started on success, failure, or interruption.
- FR-9: The lower-level smoke MUST run strict `TARGET=dreammaker-image2` preflight before checking API readiness.
- FR-10: The lower-level smoke MUST verify `/internal/integration-readiness` reports `image2_guard=READY_FOR_LOCAL`, `configured_mode=real-calls-enabled/dreammaker`, `implementation=DreamMakerImage2CoverGenerationService`, `dreammaker_guard=READY_FOR_LOCAL`, and `music_provider=mock`.
- FR-11: The lower-level smoke MUST create one lyrics work, confirm it with mock music, poll until terminal state, and print only platform work/status evidence.
- FR-12: The lower-level smoke MUST verify the `COVER` media asset was produced by `dreammaker-image2`, imported into platform object storage, has 1920x1080 workflow dimensions, and does not retain raw supplier URL or inline base64 metadata.
- FR-13: Both scripts MUST leave real credentials out of committed files and logs.

## Non-Functional Requirements

- NFR-1: Scripts MUST pass `bash -n`.
- NFR-2: Scripts MUST avoid command-line arguments containing secret values.
- NFR-3: Stack startup SHOULD fail within 60 seconds if API health does not become ready.
- NFR-4: Automated validation MUST NOT call DreamMaker unless `ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1` is explicitly set by the operator.

## Acceptance Criteria

- AC-1: Given `ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE` is not `1`, when either script runs, then it exits before requiring credentials, starting processes, creating works, or calling DreamMaker. Covers FR-1 and NFR-4.
- AC-2: Given the allow gate is set but DreamMaker AK/SK are missing and stdin is non-interactive, when the stack script runs, then it exits without starting API. Covers FR-5 through FR-8.
- AC-3: Given local port 8080 is occupied, when the stack script runs, then it refuses to start a second API process and points the operator to the lower-level script. Covers FR-7.
- AC-4: Given credentials are available and local port 8080 is free, when the stack script runs, then it starts API, waits for health, runs the lower-level smoke, and stops API. Covers FR-8 through FR-11.
- AC-5: Given DreamMaker returns a successful image output, when the workflow completes, then the platform imports the cover into object storage and media metadata does not retain supplier raw URL or base64. Covers FR-12 and FR-13.
- AC-6: Given DreamMaker returns 401 / 403 / 429 / timeout, when the smoke reaches terminal state, then the platform failure is visible without printing secrets or raw provider payloads. Covers FR-6 and FR-11.

## Edge Cases

- EC-1: Docker infrastructure is not running; API startup may fail, and logs MUST be available for diagnosis.
- EC-2: PostgreSQL container is unavailable after generation; the script MUST fail with work id evidence rather than claiming success.
- EC-3: DreamMaker returns success without an image output; the platform MUST fail closed and the script MUST print only platform failure state.
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
| `provider` | `dreammaker-image2` |
| `width` | `1920` |
| `height` | `1080` |
| `metadata_json.provider` | `dreammaker-image2` |
| `metadata_json.object_storage_imported` | `true` |
| `metadata_json.source_url` | absent |
| `metadata_json.inline_base64` | absent |

## Out of Scope

- OS-1: The DreamMaker Image 2 smoke scripts MUST NOT test WellAPI Image 2, Suno, MiniMax, DeepSeek, render-worker local-process, or company Adapter calls.
- OS-2: The scripts MUST NOT write real credentials to `.env`, docs, logs, or Git.
- OS-3: The scripts MUST NOT start or stop Docker infrastructure automatically.
- OS-4: The scripts MUST NOT mark Yunwu or WellAPI as production replacements.
