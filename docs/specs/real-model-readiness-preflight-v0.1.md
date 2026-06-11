# Real Model Readiness Preflight v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The project now has controlled real smoke scripts for Yunwu Suno, WellAPI Image 2, DreamMaker music, DeepSeek lyrics, DreamMaker Image 2, and one combined public-network full-experience smoke. Those scripts either call real providers or require a running API. Before using them, the user needs a safe command that checks local shell configuration and optional API readiness without starting services or making supplier requests.

This preflight is a guardrail, not an integration test. It helps confirm whether a selected real-model path is ready to attempt a single controlled smoke, while keeping the repo safe from accidental credential disclosure or uncontrolled external calls.

DreamMaker remains the mandatory production target for music and Image 2. Yunwu and WellAPI are current public-network smoke paths only. The preflight must preserve that distinction.

## Functional Requirements

- FR-1: The preflight MUST NOT call DreamMaker, Yunwu, WellAPI, DeepSeek, MiniMax, Suno, company systems, databases, object storage, or user-facing work APIs.
- FR-2: The preflight MUST inspect only local environment variables and, when explicitly requested, `GET /health` and `GET /internal/integration-readiness`.
- FR-3: The preflight MUST support `TARGET=all|yunwu-suno|dreammaker-suno|dreammaker-minimax|deepseek|wellapi-image2|dreammaker-image2|public-real-full-experience`.
- FR-4: The preflight MUST support `STRICT=true`; strict mode MUST fail when the selected target is not ready for its controlled smoke.
- FR-5: The preflight MUST never print credential values. It MAY print whether a secret variable is configured.
- FR-6: The preflight MUST check real music targets require `MUSIC_WORKFLOW_DISPATCH_MODE=outbox`, `WORKFLOW_OUTBOX_DISPATCH_TARGET=temporal`, and `WORKFLOW_OUTBOX_DISPATCHER_ENABLED=true`.
- FR-7: The preflight MUST check DreamMaker production-target paths retain `DREAMMAKER_REAL_CALLS_ENABLED`, `DREAMMAKER_API_BASE_URL`, `DREAMMAKER_ACCESS_KEY`, and `DREAMMAKER_SECRET_KEY`.
- FR-8: The preflight MUST distinguish disabled targets from blocked targets.
- FR-9: When optional API readiness is enabled, the preflight MUST scan readiness responses for obvious `sk-...` or long Bearer token leaks before printing summaries.
- FR-10: `TARGET=public-real-full-experience` MUST check the combined public-network configuration for DeepSeek, Yunwu Suno, WellAPI Image 2, outbox/Temporal dispatch, legacy workflow mode, album-ffmpeg rendering, and DreamMaker real calls disabled.

## Non-Functional Requirements

- NFR-1: The script MUST pass `bash -n`.
- NFR-2: The default command MUST complete without requiring real credentials.
- NFR-3: The script SHOULD complete within 10 seconds when `CHECK_API=false`.
- NFR-4: The output MUST be compact and safe to paste into a handoff note.

## Acceptance Criteria

- AC-1: Given no real-model credentials are configured, when the preflight runs with defaults, then it prints disabled or blocked summaries and exits zero. Covers FR-3, FR-8, and NFR-2.
- AC-2: Given `TARGET=yunwu-suno STRICT=true` without `YUNWU_API_KEY`, when the preflight runs, then it exits non-zero without printing secret values. Covers FR-4 and FR-5.
- AC-3: Given a real music target is selected with `STRICT=true` and dispatch mode is not `outbox/temporal` with dispatcher enabled, when the preflight runs, then it exits non-zero. Covers FR-6.
- AC-4: Given `TARGET=dreammaker-image2 STRICT=true` with DreamMaker AK/SK and image switches configured, when the preflight runs, then it can mark the target ready without requiring music outbox settings. Covers FR-6 and FR-7.
- AC-5: Given `CHECK_API=true` and API readiness contains an obvious key or Bearer token, when the preflight runs, then it fails before printing the full response. Covers FR-9.
- AC-6: Given `TARGET=public-real-full-experience STRICT=true` with DeepSeek, Yunwu, WellAPI, Temporal dispatch, legacy workflow, album-ffmpeg render, and DreamMaker disabled variables configured, when the preflight runs, then it can mark the combined target ready without printing secret values. Covers FR-4, FR-5, and FR-10.

## Edge Cases

- EC-1: Unknown `TARGET` values MUST fail with a clear message.
- EC-2: Secret variables containing only whitespace MUST count as missing.
- EC-3: `CHECK_API=true` with API down MUST fail fast and explain that no supplier call was made.
- EC-4: Multiple real providers enabled at once SHOULD print a warning, because single-provider smoke is the default safe path.
- EC-5: Multiple real providers enabled at once SHOULD NOT warn for `TARGET=public-real-full-experience`, because that target intentionally opens three public-network providers in one controlled smoke.

## API Contracts

Optional API checks use existing endpoints:

```typescript
type HealthResponse = {
  service: "music-api"
  timestamp: string
  status: "OK"
}

type IntegrationReadinessReport = {
  environment: string
  service: "music-api"
  overall_status: string
  components: Array<{
    component: string
    configured_mode: string
    implementation: string
    status: string
    blocks_company_deployment: boolean
    required_env_vars: string[]
  }>
}
```

## Data Models

| Field | Type | Constraint | Description |
|---|---|---|---|
| target | enum | required | Selected preflight target. |
| status | enum | required | `READY`, `DISABLED`, `BLOCKED`, or `WARN`. |
| missing | string[] | safe names only | Missing variable names. |
| blockers | string[] | safe text only | Unsafe switch combinations. |
| notes | string[] | safe text only | Handoff-safe explanation. |

## Out of Scope

- OS-1: The preflight MUST NOT start API, worker, frontend, Docker, Temporal, or render-worker.
- OS-2: The preflight MUST NOT create works, confirm works, retry music, generate images, or write databases.
- OS-3: The preflight MUST NOT prove real supplier success.
- OS-4: The preflight MUST NOT decide production deployment topology or company Adapter replacement.
