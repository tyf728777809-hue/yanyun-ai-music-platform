# Local Commercial Backend Acceptance Stack Smoke v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The backend now has several separate local smoke scripts: main Mock production flow, OpenAPI runtime contract, company Adapter readiness, and Mock publish-package moderation block. Before local handoff or user testing, the operator still has to start the API manually, remember the right Mock-only environment, run multiple scripts, then restart the API with package-block configuration.

This stack smoke provides one backend-only acceptance entry for the local commercial baseline. It intentionally does not cover the Claude frontend prototype, album-ffmpeg MP4 rendering, real DeepSeek, real Suno/MiniMax, real Image 2, or real company systems.

DreamMaker music and DreamMaker Image 2 remain the production-target provider paths. This script must keep all real provider calls disabled; Yunwu and WellAPI are not used here and do not replace DreamMaker.

## Functional Requirements

- FR-1: The script MUST refuse to start if the configured API port is already occupied.
- FR-2: The script MUST start `music-api` in synchronous Mock mode with shortened Mock music duration.
- FR-3: The normal run MUST execute `api-main-flow.sh`, `openapi-contract.sh`, and `company-adapter-readiness-smoke.sh` against the API it started.
- FR-4: The script MUST stop the normal API process before starting the package-block run.
- FR-5: The package-block run MUST start `music-api` with `MOCK_MODERATION_PUBLISH_PACKAGE_BLOCKED_USER_IDS=mock_package_block_smoke`.
- FR-6: The package-block run MUST execute `api-package-blocked-flow.sh`.
- FR-7: The script MUST stop any API process it started on success, failure, or interruption, including a `bootRun` Java listener process left behind by the Gradle parent.
- FR-8: The script MUST write API logs under `build/smoke/` and print only log file paths, not full logs unless startup fails.
- FR-9: The script MUST explicitly keep real external calls disabled: DreamMaker, Yunwu, DeepSeek, Image 2, and generic Agent real-call switches must be false.
- FR-10: The script MUST NOT call real DreamMaker, Yunwu, WellAPI, DeepSeek, Suno, MiniMax, Image 2, or company systems.
- FR-11: The script MUST leave DreamMaker guard checks intact through `company-adapter-readiness-smoke.sh`.

## Non-Functional Requirements

- NFR-1: The script MUST be shell-checkable with `bash -n`.
- NFR-2: The script MUST use existing lower-level smoke scripts rather than duplicating their business assertions.
- NFR-3: The script SHOULD fail within 90 seconds if API health does not become ready.
- NFR-4: The script SHOULD not require credentials.
- NFR-5: The script SHOULD be safe to run repeatedly on the same local database, because lower-level scripts create unique users and idempotency keys.

## Acceptance Criteria

- AC-1: Given port 8080 is occupied, when the script starts, then it exits before running Gradle. Covers FR-1.
- AC-2: Given Docker infrastructure is running and port 8080 is free, when the script runs, then the normal Mock API becomes healthy and all three normal lower-level smoke scripts pass. Covers FR-2 and FR-3.
- AC-3: Given the normal run has completed, when the package-block phase starts, then it uses a fresh API process with Mock moderation blocking configured and `api-package-blocked-flow.sh` passes. Covers FR-4 through FR-6.
- AC-4: Given any lower-level smoke fails, when the script exits, then it stops the API process it started and prints log file paths. Covers FR-7 and FR-8.
- AC-5: Given the script environment is inspected through readiness, then real provider calls remain disabled and `dreammaker_guard` remains present as the production-target guard. Covers FR-9 through FR-11.

## Edge Cases

- EC-1: Docker Compose infrastructure is not running; API startup or lower-level DB checks may fail, and the script must point to API logs.
- EC-2: JDK 21 is installed at Homebrew's default path but `JAVA_HOME` is unset; the script should set it for the child API process.
- EC-3: A previous API process remains on port 8080; the script must fail early instead of accidentally running against the wrong service.
- EC-4: API starts but readiness never returns OK; the script must fail with a startup timeout and log location.
- EC-5: Operator interrupts with Ctrl-C; the trap must stop the started API process.
- EC-6: Gradle `bootRun` exits while its Java child still listens on 8080; the script may terminate only a listener whose command line is `com.yanyun.music.api.MusicApiApplication`.

## API Contracts

N/A - this is a local shell stack smoke that composes existing HTTP smoke scripts:

```text
scripts/smoke/api-main-flow.sh
scripts/smoke/openapi-contract.sh
scripts/smoke/company-adapter-readiness-smoke.sh
scripts/smoke/api-package-blocked-flow.sh
```

## Data Models

| Variable | Type | Default | Description |
|---|---|---|---|
| `API_ROOT` | string | `http://localhost:8080` | Root URL for the API process started by this script. |
| `API_PORT` | integer | derived from `API_ROOT`, normally `8080` | Port that must be free before each API start. |
| `START_TIMEOUT_SECONDS` | integer | `90` | API startup health wait timeout. |
| `STOP_TIMEOUT_SECONDS` | integer | `60` | API shutdown port-release wait timeout between phases and during cleanup. |
| `LOG_DIR` | path | `build/smoke/local-commercial-backend-acceptance-{timestamp}` | Directory for API logs. |
| `EXPECTED_DURATION_MS` | integer | `1000` | Expected Mock audio/video duration for lower-level smoke. |

## Out of Scope

- OS-1: The script MUST NOT start Docker Compose infrastructure automatically.
- OS-2: The script MUST NOT run the frontend smoke.
- OS-3: The script MUST NOT run `RENDER_WORKER_MODE=album-ffmpeg` or verify MP4 with `ffprobe`.
- OS-4: The script MUST NOT execute real-model smoke targets.
- OS-5: The script MUST NOT replace the final manual local commercial delivery checklist.
