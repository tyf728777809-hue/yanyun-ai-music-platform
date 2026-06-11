# Public Real Full Experience Smoke v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The local commercial baseline can already verify the full Mock product loop, album-ffmpeg MP4 rendering, and Claude Web v1 against a real backend. Separately, the repository has controlled single-provider smoke scripts for DeepSeek, Yunwu Suno, and WellAPI Image 2. The next public-network milestone is a single real-product experience that combines those public-network providers while keeping company systems mocked.

DreamMaker music and DreamMaker Image 2 remain the production-target paths. This public-network smoke exists only because the current local environment is outside the company intranet. Yunwu and WellAPI success must not be recorded as DreamMaker production validation.

## Functional Requirements

- FR-1: The smoke MUST run only when `ALLOW_REAL_MODEL_SMOKE=1` and `ALLOW_PUBLIC_REAL_FULL_EXPERIENCE=1`.
- FR-2: The smoke MUST use DeepSeek for lyrics, Yunwu Suno for music, WellAPI Image 2 for cover, album-ffmpeg renderer for MP4, and Claude Web v1 for the user-facing check.
- FR-3: The smoke MUST keep company account, moderation, quota, publish, and share adapters in Mock mode.
- FR-4: The smoke MUST keep DreamMaker real calls disabled while still preserving DreamMaker as the documented production target.
- FR-5: The smoke MUST require credentials from the current shell only and MUST NOT read or write credential files.
- FR-6: The smoke MUST refuse to start when its managed ports are already occupied.
- FR-7: The smoke MUST start and stop only the API, worker, and frontend processes it owns.
- FR-8: The smoke MUST run strict preflight for `deepseek`, `yunwu-suno`, and `wellapi-image2` before starting services.
- FR-9: The smoke MUST create one work, use real DeepSeek lyrics, confirm with `music_provider=suno`, wait for `GENERATED / PACKAGE_READY`, fetch the publish package, verify media summaries, open Claude Web v1, refresh the handoff link, and mark the package fetched.
- FR-10: The smoke output MUST be sanitized and MUST NOT print API keys, Bearer tokens, full prompts, complete lyrics, provider raw payloads, supplier media URLs, platform signed URLs, full provider task ids, or full provider trace ids.
- FR-11: The smoke MUST be available from the unified real-model controlled smoke index as `TARGET=public-real-full-experience` and MUST still require `ALLOW_PUBLIC_REAL_FULL_EXPERIENCE=1`.
- FR-12: If the work fails with `MUSIC_GENERATION_FAILED`, `MUSIC_QUALITY_FAILED`, `PROVIDER_TIMEOUT`, or `RATE_LIMITED` and `available_actions` contains `RETRY_MUSIC`, the smoke MAY use the product retry endpoint with `music_provider=suno` only when `MAX_MUSIC_RETRY_ATTEMPTS` is explicitly greater than `0`; it MUST NOT retry by guessing state outside `available_actions`.
- FR-13: The publish package verification MUST require audio, cover, video, and lyrics timeline URL presence without printing the URLs.
- FR-14: After a successful Yunwu Suno music sample, the smoke SHOULD verify timestamped lyrics via the gated `yunwu-suno-timestamped-lyrics-smoke.sh` subcheck using the paired `provider_task_id + provider_audio_id` stored on the platform `AUDIO` asset.
- FR-15: Timestamped lyrics verification MUST NOT print or persist raw provider payloads, full aligned word arrays, full lyrics text, supplier media URLs, Bearer tokens, or signed URLs.

## Non-Functional Requirements

- NFR-1: The script SHOULD rely on existing project dependencies and SHOULD NOT add production dependencies.
- NFR-2: The script MUST fail closed on missing credentials, failed readiness, provider failure, frontend failure, or unsafe port state.
- NFR-3: The script SHOULD write raw application logs only under `build/smoke/...`, which is not a committed evidence location.
- NFR-4: The script SHOULD complete with one real work sample unless external provider latency exceeds the configured polling window.
- NFR-5: The script SHOULD default WellAPI Image 2 request timeout to at least `180s` so a synchronous public image generation call is not cut off by the API default `30s` timeout.
- NFR-6: The script SHOULD default Yunwu request timeout to at least `300s`, `YUNWU_MAX_POLL_ATTEMPTS` to at least `180`, and `YUNWU_POLL_INTERVAL` to `2s`, so one public music task can wait roughly 6 minutes before being judged as timeout. The script SHOULD default `MAX_MUSIC_RETRY_ATTEMPTS=0` to avoid multiplying real music cost unless an operator explicitly enables product retries.
- NFR-7: Timestamped lyrics verification SHOULD be disabled by default for public full experience while the current product path uses no-subtitle default videos; operators MAY enable it explicitly with `CHECK_YUNWU_TIMESTAMPED_LYRICS=true` when testing provider timestamp support.

## Acceptance Criteria

- AC-1: Given missing global allow gates, when the script runs, then it exits before preflight or service startup. Covers FR-1.
- AC-2: Given missing one of `DEEPSEEK_API_KEY`, `YUNWU_API_KEY`, or `WELLAPI_API_KEY`, when the script runs non-interactively, then it exits before service startup and does not print secret values. Covers FR-5 and FR-10.
- AC-3: Given credentials and ports are ready, when the script runs, then API, worker, and frontend become healthy and are cleaned up on success, failure, or interruption. Covers FR-6 and FR-7.
- AC-4: Given all providers succeed, when the script finishes, then one work reaches `PACKAGE_FETCHED` after the frontend handoff action, and the output contains only sanitized summaries. Covers FR-2, FR-9, and FR-10.
- AC-5: Given any provider returns 401, 403, 429, timeout, or malformed output, when the script reaches terminal state, then it exits non-zero and prints the platform failure code without raw provider payloads. Covers NFR-2.
- AC-6: Given repository audits run after implementation, when `local-delivery-evidence-audit.sh` executes, then it verifies this spec and script exist while retaining DreamMaker production-target language. Covers FR-4.
- AC-7: Given `TARGET=public-real-full-experience MODE=plan`, when `real-model-controlled-smoke.sh` runs, then it prints the public full-experience plan and both required allow gates without calling suppliers. Covers FR-1, FR-10, and FR-11.
- AC-8: Given a public-network successful sample, when the smoke fetches the publish package, then sanitized output reports `audio`, `cover`, `video`, and `timeline` URL presence as true, and Claude Web v1 displays the audio, cover, and video handoff fields. Covers FR-9 and FR-13.
- AC-9: Given Yunwu exposes a provider audio id, when timestamped lyrics verification runs, then the evidence file contains only `http_status`, provider code, aligned word count, waveform presence, and timestamp presence. Covers FR-14 and FR-15.
- AC-10: Given Yunwu does not expose `provider_audio_id` or returns no aligned timestamps, when timestamped lyrics verification runs, then the smoke fails with a sanitized reason and the project must treat exact subtitles as not yet verified. Covers FR-14.

## Edge Cases

- EC-1: If port `8080`, `8081`, or the selected frontend port is occupied, the script MUST refuse to start instead of attaching to an unknown process.
- EC-2: If API or worker exits before health is ready, the script MUST show only local log file paths, not log tails, credentials, or provider payload fragments.
- EC-3: If Claude Web v1 cannot load the finished work, the script MUST fail even when API generated the publish package.
- EC-4: If mark-fetched succeeds but package status remains not fetched, the script MUST fail.
- EC-5: If a future change removes DreamMaker production docs or provider guards, this smoke still MUST NOT be used as replacement evidence.
- EC-6: If timestamped lyrics is explicitly enabled and fails while the main media package is otherwise ready, record it as `blocked_provider_path`; it MUST NOT fail the no-subtitle default video acceptance.

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

No schema changes. Evidence is read from existing `agent_runs`, `provider_calls`, `media_assets`, and `works` rows using sanitized projections only. Yunwu timestamped lyrics uses `media_assets.metadata_json.provider_task_id` and `media_assets.metadata_json.provider_audio_id` on the `AUDIO` asset to avoid pairing a task id from one retry with an audio id from another retry.

## Out of Scope

- OS-1: This smoke does not validate DreamMaker production success.
- OS-2: This smoke does not replace company account, moderation, quota, publish, or share adapters.
- OS-3: This smoke does not deploy to company servers.
- OS-4: This smoke does not migrate `prototypes/Claude-web-v1` into `apps/web`.
- OS-5: This smoke does not store secrets or real provider payloads in repository evidence.
