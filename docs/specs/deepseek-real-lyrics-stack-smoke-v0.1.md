# DeepSeek Real Lyrics Stack Smoke v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The existing `deepseek-real-lyrics-smoke.sh` verifies the real DeepSeek lyrics path against an already running API. That is useful for manual debugging, but the other public-network providers already have stack smoke scripts that start and clean up their required services. The public real full experience goal requires single-provider validation before the combined smoke, so DeepSeek needs the same operator ergonomics and safety behavior.

This stack smoke is a convenience wrapper around the existing low-level DeepSeek smoke. It starts only `music-api` with real DeepSeek enabled, keeps music, Image 2, DreamMaker, Yunwu, render-worker, and company Adapters mock or disabled, delegates to the low-level smoke, and stops only the API process it owns.

DreamMaker music and DreamMaker Image 2 remain production-target paths. This smoke validates only real DeepSeek lyrics generation and does not prove music, cover, video, package, or DreamMaker success.

## Functional Requirements

- FR-1: The stack smoke MUST require both `ALLOW_REAL_MODEL_SMOKE=1` and `ALLOW_DEEPSEEK_REAL_SMOKE=1` before reading secrets, starting API, or creating works.
- FR-2: The stack smoke MUST read `DEEPSEEK_API_KEY` only from the current shell or interactive silent prompt and MUST NOT read credential files.
- FR-3: The stack smoke MUST refuse to start if its managed API port is already occupied.
- FR-4: The stack smoke MUST start only `music-api` and MUST stop only the API process it owns on success, failure, or interruption.
- FR-5: The API MUST start with `AGENT_REAL_CALLS_ENABLED=true`, `DEEPSEEK_REAL_CALLS_ENABLED=true`, `MUSIC_PROVIDER=mock`, `IMAGE_PROVIDER=mock`, `DREAMMAKER_REAL_CALLS_ENABLED=false`, `IMAGE_REAL_CALLS_ENABLED=false`, `YUNWU_REAL_CALLS_ENABLED=false`, `RENDER_WORKER_MODE=mock`, and company Adapter modes `mock`.
- FR-6: After API health is ready, the stack smoke MUST delegate to `scripts/smoke/deepseek-real-lyrics-smoke.sh`.
- FR-7: The stack smoke output MUST be sanitized and MUST NOT print API keys, Bearer tokens, full prompts, complete lyrics, provider raw payloads, supplier URLs, or model raw responses.
- FR-8: The unified smoke index `TARGET=deepseek MODE=execute` SHOULD delegate to this stack smoke by default, while keeping the low-level smoke available for manually managed API sessions.

## Non-Functional Requirements

- NFR-1: The script MUST be shell-only and reuse existing local tools: `curl`, `jq`, `lsof`, and Gradle.
- NFR-2: The script MUST fail closed on missing credentials, failed readiness, unsafe port state, or low-level smoke failure.
- NFR-3: Raw application logs MAY be written only under `build/smoke/...`, which is not a committed evidence location.

## Acceptance Criteria

- AC-1: Given either allow gate is missing, when the stack smoke runs, then it exits before reading secrets, starting API, or creating a work. Covers FR-1.
- AC-2: Given both allow gates but missing `DEEPSEEK_API_KEY` in non-interactive execution, when the stack smoke runs, then it exits before starting API and does not print a secret value. Covers FR-2 and FR-7.
- AC-3: Given API port is occupied, when the stack smoke runs, then it exits before starting API and suggests using the low-level smoke manually. Covers FR-3.
- AC-4: Given valid credentials and healthy infrastructure, when the stack smoke runs, then it starts API, delegates to the low-level smoke, and stops the API it owns afterward. Covers FR-4 through FR-6.
- AC-5: Given `TARGET=deepseek MODE=plan`, when the unified smoke index runs, then it points operators to the DeepSeek stack smoke path and retains the low-level smoke as a manual alternative. Covers FR-8.

## Edge Cases

- EC-1: If API fails to become healthy, the script MUST print only the local log path and not tail logs into command output.
- EC-2: If the low-level smoke fails after creating a work, the stack smoke MUST still stop the API process it owns.
- EC-3: If `DEEPSEEK_API_KEY` is entered interactively, cleanup MUST unset it from the child process before exit.

## API Contracts

No new HTTP API. Delegates to the API contract defined in `docs/specs/deepseek-real-lyrics-smoke-v0.1.md`.

## Data Models

No new database tables. Delegates to the `agent_runs` evidence defined in `docs/specs/deepseek-real-lyrics-smoke-v0.1.md`.

## Out of Scope

- OS-1: This does not confirm works, generate music, generate cover, render video, build publish packages, or call company systems.
- OS-2: This does not call DreamMaker, Yunwu, WellAPI, Suno, MiniMax, or Image 2.
- OS-3: This does not replace the low-level `deepseek-real-lyrics-smoke.sh`; it wraps it for operator convenience.
- OS-4: This does not store credentials in repository files, logs, docs, screenshots, or test output.
