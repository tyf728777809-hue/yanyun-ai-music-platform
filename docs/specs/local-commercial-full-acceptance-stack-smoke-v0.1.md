# Local Commercial Full Acceptance Stack Smoke v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The local commercial baseline has separate verification entries for backend Mock flow, OpenAPI contract, company Adapter readiness, publish-package moderation block, album-ffmpeg MP4 rendering, and Claude Web v1 real-backend UI smoke. These are individually useful, but final local handoff still requires an operator to run them in the correct order with the correct Mock-only environment.

This full acceptance stack provides one high-level local gate for "can I hand this to the user/company for local testing?" It composes the existing lower-level scripts instead of duplicating their assertions.

DreamMaker music and DreamMaker Image 2 remain the production-target provider paths. This script MUST keep all real model/provider/company calls disabled. Yunwu and WellAPI are public-network controlled smoke paths only and are not part of this local full acceptance stack.

## Functional Requirements

- FR-1: The script MUST first run `scripts/smoke/local-commercial-backend-acceptance-stack.sh`.
- FR-2: The script MUST then start `music-api` in `RENDER_WORKER_MODE=album-ffmpeg` and run `api-main-flow.sh` with `EXPECT_RENDER_WORKER=album-ffmpeg` to verify MP4 and timeline output.
- FR-3: The script MUST then start `music-api` in Mock render mode and run `cd prototypes/Claude-web-v1 && npm run smoke:real-backend`.
- FR-4: The script MUST stop any API process it starts between phases and on failure or interruption.
- FR-5: The script MUST explicitly keep DreamMaker, Yunwu, DeepSeek, Image 2, Agent real calls, and company-system calls disabled.
- FR-6: The script MUST fail early with an actionable message when `apps/render-worker/node_modules`, `prototypes/Claude-web-v1/node_modules`, `ffprobe`, or required shell tools are missing.
- FR-7: The script MUST write API logs under `build/smoke/` and print log paths, not raw logs unless startup fails.
- FR-8: The script MUST leave DreamMaker production-target checks to existing readiness/audit scripts and MUST NOT weaken or bypass them.

## Non-Functional Requirements

- NFR-1: The script MUST be shell-checkable with `bash -n`.
- NFR-2: The script MUST compose existing lower-level smoke scripts and avoid reimplementing UI, MP4, or OpenAPI assertions.
- NFR-3: The script SHOULD use short Mock music duration, default `EXPECTED_DURATION_MS=1000`.
- NFR-4: The script SHOULD not require credentials.
- NFR-5: The script SHOULD leave ports 8080 and the frontend smoke port free after completion.

## Acceptance Criteria

- AC-1: Given Docker infrastructure is running and ports are free, when the script runs, then the backend acceptance stack passes before MP4 or frontend phases run. Covers FR-1.
- AC-2: Given render-worker dependencies and `ffprobe` are available, when the MP4 phase runs, then `api-main-flow.sh` verifies album-ffmpeg MP4 output through `ffprobe`. Covers FR-2 and FR-6.
- AC-3: Given Claude Web v1 dependencies and Playwright are available, when the frontend phase runs, then `npm run smoke:real-backend` passes against the API started by this script. Covers FR-3 and FR-6.
- AC-4: Given any phase fails, when the script exits, then it stops started API processes and prints log paths. Covers FR-4 and FR-7.
- AC-5: Given readiness checks inside lower-level scripts run, then real provider calls remain disabled and DreamMaker guard remains visible as the production-target guard. Covers FR-5 and FR-8.

## Edge Cases

- EC-1: Port 8080 is occupied before the script starts; the script or lower-level backend stack must refuse to run.
- EC-2: The frontend smoke port is occupied; the frontend smoke should fail clearly without the full script hiding the failure.
- EC-3: Gradle `bootRun` leaves a Java child listener; the script may terminate only a listener whose command line is `com.yanyun.music.api.MusicApiApplication`.
- EC-4: Docker infrastructure is unavailable; API startup or DB checks may fail and logs must point to the cause.
- EC-5: Render-worker dependencies are missing; the script should fail before starting the MP4 API phase.
- EC-6: Playwright browser dependencies are missing; the frontend smoke may fail with its normal message and the script must still clean up the API.

## API Contracts

N/A - this is a local shell stack smoke that composes existing commands:

```text
scripts/smoke/local-commercial-backend-acceptance-stack.sh
EXPECTED_DURATION_MS=1000 EXPECT_RENDER_WORKER=album-ffmpeg scripts/smoke/api-main-flow.sh
cd prototypes/Claude-web-v1 && npm run smoke:real-backend
```

## Data Models

| Variable | Type | Default | Description |
|---|---|---|---|
| `API_ROOT` | string | `http://localhost:8080` | Root URL for API phases started by this script. |
| `API_PORT` | integer | derived from `API_ROOT`, normally `8080` | Port that must be free before each API phase. |
| `FRONTEND_PORT` | integer | `5274` | Port passed to Claude Web v1 smoke. |
| `EXPECTED_DURATION_MS` | integer | `1000` | Expected Mock media duration for backend lower-level smokes. |
| `START_TIMEOUT_SECONDS` | integer | `90` | API startup wait timeout. |
| `STOP_TIMEOUT_SECONDS` | integer | `60` | API shutdown port-release timeout. |
| `LOG_DIR` | path | `build/smoke/local-commercial-full-acceptance-{timestamp}` | API log directory. |

## Out of Scope

- OS-1: The script MUST NOT start Docker Compose infrastructure automatically.
- OS-2: The script MUST NOT call real DreamMaker, Yunwu, WellAPI, DeepSeek, Suno, MiniMax, Image 2, or company systems.
- OS-3: The script MUST NOT run real-model smoke targets.
- OS-4: The script MUST NOT decide whether `prototypes/Claude-web-v1` is migrated into `apps/web`.
- OS-5: The script MUST NOT replace the final manual local commercial delivery checklist.
