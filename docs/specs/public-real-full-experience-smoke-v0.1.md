# Public Real Full Experience Smoke v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The local commercial baseline can already verify the full Mock product loop, local-process MP4 rendering, and Claude Web v1 against a real backend. Separately, the repository has controlled single-provider smoke scripts for DeepSeek, Yunwu Suno, and WellAPI Image 2. The next public-network milestone is a single real-product experience that combines those public-network providers while keeping company systems mocked.

DreamMaker music and DreamMaker Image 2 remain the production-target paths. This public-network smoke exists only because the current local environment is outside the company intranet. Yunwu and WellAPI success must not be recorded as DreamMaker production validation.

## Functional Requirements

- FR-1: The smoke MUST run only when `ALLOW_REAL_MODEL_SMOKE=1` and `ALLOW_PUBLIC_REAL_FULL_EXPERIENCE=1`.
- FR-2: The smoke MUST use DeepSeek for lyrics, Yunwu Suno for music, WellAPI Image 2 for cover, local-process render-worker for MP4, and Claude Web v1 for the user-facing check.
- FR-3: The smoke MUST keep company account, moderation, quota, publish, and share adapters in Mock mode.
- FR-4: The smoke MUST keep DreamMaker real calls disabled while still preserving DreamMaker as the documented production target.
- FR-5: The smoke MUST require credentials from the current shell only and MUST NOT read or write credential files.
- FR-6: The smoke MUST refuse to start when its managed ports are already occupied.
- FR-7: The smoke MUST start and stop only the API, worker, and frontend processes it owns.
- FR-8: The smoke MUST run strict preflight for `deepseek`, `yunwu-suno`, and `wellapi-image2` before starting services.
- FR-9: The smoke MUST create one work, use real DeepSeek lyrics, confirm with `music_provider=suno`, wait for `GENERATED / PACKAGE_READY`, fetch the publish package, verify media summaries, open Claude Web v1, refresh the handoff link, and mark the package fetched.
- FR-10: The smoke output MUST be sanitized and MUST NOT print API keys, Bearer tokens, full prompts, complete lyrics, provider raw payloads, supplier media URLs, platform signed URLs, full provider task ids, or full provider trace ids.
- FR-11: The smoke MUST be available from the unified real-model controlled smoke index as `TARGET=public-real-full-experience` and MUST still require `ALLOW_PUBLIC_REAL_FULL_EXPERIENCE=1`.

## Non-Functional Requirements

- NFR-1: The script SHOULD rely on existing project dependencies and SHOULD NOT add production dependencies.
- NFR-2: The script MUST fail closed on missing credentials, failed readiness, provider failure, frontend failure, or unsafe port state.
- NFR-3: The script SHOULD write raw application logs only under `build/smoke/...`, which is not a committed evidence location.
- NFR-4: The script SHOULD complete with one real work sample unless external provider latency exceeds the configured polling window.

## Acceptance Criteria

- AC-1: Given missing global allow gates, when the script runs, then it exits before preflight or service startup. Covers FR-1.
- AC-2: Given missing one of `DEEPSEEK_API_KEY`, `YUNWU_API_KEY`, or `WELLAPI_API_KEY`, when the script runs non-interactively, then it exits before service startup and does not print secret values. Covers FR-5 and FR-10.
- AC-3: Given credentials and ports are ready, when the script runs, then API, worker, and frontend become healthy and are cleaned up on success, failure, or interruption. Covers FR-6 and FR-7.
- AC-4: Given all providers succeed, when the script finishes, then one work reaches `PACKAGE_FETCHED` after the frontend handoff action, and the output contains only sanitized summaries. Covers FR-2, FR-9, and FR-10.
- AC-5: Given any provider returns 401, 403, 429, timeout, or malformed output, when the script reaches terminal state, then it exits non-zero and prints the platform failure code without raw provider payloads. Covers NFR-2.
- AC-6: Given repository audits run after implementation, when `local-delivery-evidence-audit.sh` executes, then it verifies this spec and script exist while retaining DreamMaker production-target language. Covers FR-4.
- AC-7: Given `TARGET=public-real-full-experience MODE=plan`, when `real-model-controlled-smoke.sh` runs, then it prints the public full-experience plan and both required allow gates without calling suppliers. Covers FR-1, FR-10, and FR-11.

## Edge Cases

- EC-1: If port `8080`, `8081`, or the selected frontend port is occupied, the script MUST refuse to start instead of attaching to an unknown process.
- EC-2: If API or worker exits before health is ready, the script MUST show only local log file paths, not log tails, credentials, or provider payload fragments.
- EC-3: If Claude Web v1 cannot load the finished work, the script MUST fail even when API generated the publish package.
- EC-4: If mark-fetched succeeds but package status remains not fetched, the script MUST fail.
- EC-5: If a future change removes DreamMaker production docs or provider guards, this smoke still MUST NOT be used as replacement evidence.

## API Contracts

No public API changes. The script uses existing OpenAPI v0.1 endpoints:

| Endpoint | Purpose |
|---|---|
| `POST /api/v1/works/inspiration` | Create a real-DeepSeek lyrics work. |
| `GET /api/v1/works/{work_id}` | Poll status and media summaries. |
| `POST /api/v1/works/{work_id}/confirm` | Confirm with `music_provider=suno`. |
| `GET /api/v1/works/{work_id}/publish-package` | Verify publish package availability. |
| `POST /api/v1/works/{work_id}/publish-package/refresh-url` | Frontend refresh handoff URL. |
| `POST /api/v1/works/{work_id}/publish-package/mark-fetched` | Frontend mark handoff fetched. |

## Data Models

No schema changes. Evidence is read from existing `agent_runs`, `provider_calls`, `media_assets`, and `works` rows using sanitized projections only.

## Out of Scope

- OS-1: This smoke does not validate DreamMaker production success.
- OS-2: This smoke does not replace company account, moderation, quota, publish, or share adapters.
- OS-3: This smoke does not deploy to company servers.
- OS-4: This smoke does not migrate `prototypes/Claude-web-v1` into `apps/web`.
- OS-5: This smoke does not store secrets or real provider payloads in repository evidence.
