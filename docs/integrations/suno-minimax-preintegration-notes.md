# Suno / MiniMax Music Provider Pre-Integration Notes

## Status

- Current stage: local mock stage.
- Real Suno calls: not implemented.
- Real MiniMax calls: not implemented.
- Feishu reference doc: provided by product owner, but current agent environment cannot read it because the page requires Feishu login.
- Real credentials: must not be committed, logged, or written into docs.

## Implemented Boundary

The backend already has a shared music provider contract:

- `MusicProvider.submit(MusicGenerationRequest request)`.
- `MusicProviderType`: `MOCK`, `SUNO`, `MINIMAX`.
- `MusicGenerationResult`: provider task id, status, audio object key, duration, failure code, failure message.
- Runtime selection: `MUSIC_PROVIDER=mock|suno|minimax`.
- Per-request override: `confirm` and `music/retry` accept `music_provider`.
- Provider calls are now written to `provider_calls`.

Each provider call record captures:

- `work_id`.
- `job_id`.
- `provider`.
- `operation = MUSIC_GENERATION`.
- `model_name`.
- `request_hash`.
- `prompt_hash`.
- `provider_trace_id`.
- `status`.
- `latency_ms`.
- `error_code`.
- `error_message`.

## Internal Failure Mapping

Until the real provider documents are readable, keep mapping conservative:

| Provider condition | Internal failure code | Retry |
|---|---|---|
| Network timeout or callback timeout | `PROVIDER_TIMEOUT` | Yes, while retry count remains |
| Provider rate limit or quota throttle | `RATE_LIMITED` | Yes, while retry count remains |
| Provider accepts task but generated audio fails quality gate | `MUSIC_QUALITY_FAILED` | Yes, while retry count remains |
| Provider submission fails for unknown transient reason | `MUSIC_GENERATION_FAILED` | Yes, while retry count remains |
| Auth, signature, project config, or unsupported model error | `MUSIC_GENERATION_FAILED` initially; refine after provider docs | Depends on provider error |

## Required Real Provider Details

Before implementing real calls, confirm these for both Suno and MiniMax:

1. Authentication scheme.
2. Submit endpoint and request schema.
3. Whether generation is synchronous, polling-based, or callback-based.
4. Callback signature verification, if callbacks exist.
5. Provider task id field.
6. Audio URL or object retrieval mechanism.
7. Lyrics input format limits.
8. Prompt/style/vocal fields and supported enum values.
9. Duration limits and pricing units.
10. Rate limit, retry, and backoff policy.
11. Provider failure codes and their retryability.
12. Content moderation behavior and blocked-content error codes.
13. Generated audio file format and conversion requirements.
14. Whether provider audio URLs expire and how to download into object storage.

## Implementation Plan When Docs Are Available

1. Add provider-specific config properties, with no real defaults:
   - `SUNO_API_BASE_URL`.
   - `SUNO_API_KEY`.
   - `MINIMAX_API_BASE_URL`.
   - `MINIMAX_API_KEY`.
   - timeout and polling settings.
2. Implement request builders in `modules/suno` and `modules/minimax`.
3. Add provider failure-code mapping tests.
4. Add HTTP client tests with mocked provider responses.
5. Store provider task ids in `provider_calls.provider_trace_id`.
6. Download generated audio to object storage before returning success.
7. Keep automated tests on mock provider only unless explicit test credentials are provided.

## Non-Goals

- Do not call real Suno or MiniMax in local automated tests.
- Do not store real API keys in repo files.
- Do not expose provider raw error payloads directly to users.
- Do not bypass company-side account, audit, publish, or entitlement systems.
