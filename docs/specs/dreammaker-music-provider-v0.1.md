# DreamMaker Music Provider Integration Spec v0.1

## Metadata

- Author: Codex
- Date: 2026-06-05
- Status: Approved for local implementation
- Scope: Suno and MiniMax music provider integration through DreamMaker task APIs

## Context

The platform already has a local mock song production workflow and a shared `MusicProvider`
contract. Suno and MiniMax provider modules exist, but real submission is still disabled. Feishu
integration notes confirm both providers use DreamMaker run/status task APIs.

This stage prepares the real provider path without calling external APIs in automated tests and
without storing real provider credentials in the repository. The local product flow must continue
to expose only platform-owned media URLs, so provider audio URLs must be imported into object
storage before they appear in work details or publish packages.

## Functional Requirements

- FR-1: The system MUST keep `mock`, `suno`, and `minimax` selectable through the existing
  `MusicProvider` contract.
- FR-2: Suno and MiniMax providers MUST build DreamMaker submit params from
  `MusicGenerationRequest`.
- FR-3: Providers MUST submit a task, poll status, and return `SUCCEEDED`, `FAILED`, or timeout
  through `MusicGenerationResult`.
- FR-4: Providers MUST map DreamMaker `queued`, `running`, `success`, and `failed` statuses to
  internal music generation statuses.
- FR-5: Successful real-provider results MUST carry a provider audio source URL and MUST NOT expose
  that URL as a platform object key.
- FR-6: The song production workflow MUST import provider audio source URLs into object storage
  before writing the `AUDIO` media asset.
- FR-7: Missing AccessKey/SecretKey, HTTP failure, non-zero provider code, failed task, missing
  audio output, and polling timeout MUST produce internal failure codes suitable for existing retry
  logic.
- FR-8: Automated tests MUST NOT call real DreamMaker, Suno, MiniMax, DeepSeek, Image 2, or company
  systems.
- FR-9: Real provider credentials MUST only be read from local environment variables or production
  secret injection, never from committed files.
- FR-10: DreamMaker requests MUST generate an HS256 JWT per request using `DREAMMAKER_ACCESS_KEY`
  as JWT `iss` and `DREAMMAKER_SECRET_KEY` as the signing secret, then send it as
  `Authorization: Bearer <jwt>`.
- FR-11: When `DREAMMAKER_USER_ACCESS_TOKEN` is configured, the HTTP client MUST forward it as
  `X-Access-Token`; otherwise user identity and accounting use the DreamMaker API key binding.
- FR-12: Real DreamMaker HTTP calls MUST be blocked unless `DREAMMAKER_REAL_CALLS_ENABLED=true`.
- FR-13: API and worker MUST share the same DreamMaker HTTP client and properties so sync, outbox
  local, and Temporal worker paths use identical authentication and safety checks.

## Non-Functional Requirements

- NFR-1: Logs, docs, tests, fixtures, and commits MUST NOT contain real credentials.
- NFR-2: Remote audio import MUST reject non-HTTP(S) source URLs.
- NFR-3: Remote audio import MUST enforce a maximum downloaded byte size.
- NFR-4: Provider HTTP client errors MUST be converted to sanitized messages, not raw provider
  payload dumps.
- NFR-5: Existing `mock` provider local smoke path MUST remain the default and must not require
  external network access.
- NFR-6: JWTs and user access tokens MUST NOT be logged, committed, or included in test snapshots.
- NFR-7: Real provider integration SHOULD run through outbox + Temporal worker; sync mode is not
  recommended for real calls because provider polling can block API request threads.

## Acceptance Criteria

- AC-1: Given a fake DreamMaker client returning a task id then success output, when Suno submit is
  called, then it returns `SUCCEEDED` with provider task id and audio source URL.
- AC-2: Given a fake DreamMaker client returning a task id then success output, when MiniMax submit
  is called, then it returns `SUCCEEDED` with provider task id and audio source URL.
- AC-3: Given DreamMaker status remains queued/running past the configured poll attempts, when a
  provider submit is called, then it returns `FAILED / PROVIDER_TIMEOUT`.
- AC-4: Given DreamMaker returns failed status, when a provider submit is called, then it returns a
  sanitized failed `MusicGenerationResult`.
- AC-5: Given a `MusicGenerationResult` with an external audio source URL, when workflow media
  assets are created, then the source URL is imported into object storage and the media asset uses
  the imported object key.
- AC-6: Given a `MusicGenerationResult` with an existing audio object key, when workflow media
  assets are created, then no remote import is attempted.
- AC-7: Given no real provider credentials, when automated tests run, then all tests pass without
  external DreamMaker calls.
- AC-8: Given fake AccessKey/SecretKey and a fake local DreamMaker server, when the HTTP client
  submits and polls, then the server receives a valid three-part JWT with `alg=HS256`, `typ=JWT`,
  `iss=<access_key>`, `exp=now+1800s`, `nbf=now-5s`, and a valid HMAC signature.
- AC-9: Given `DREAMMAKER_REAL_CALLS_ENABLED=false`, when Suno or MiniMax would call DreamMaker,
  then the HTTP client fails before making an external request.
- AC-10: Given a non-2xx DreamMaker response, when the HTTP client reports an error, then the
  message is short and sanitized rather than a raw provider payload dump.

## API Contracts

```java
interface DreamMakerClient {
  DreamMakerSubmitResponse submit(DreamMakerRunRequest request);
  DreamMakerStatusResponse status(DreamMakerStatusRequest request);
}

record DreamMakerRunRequest(String appName, String subAppName, Map<String, Object> params) {}
record DreamMakerStatusRequest(String appName, String subAppName, String taskId) {}
record DreamMakerSubmitResponse(int code, String message, String taskId) {}
record DreamMakerStatusResponse(
    int code,
    String message,
    DreamMakerTaskStatus status,
    String providerStatus,
    List<DreamMakerOutputFile> outputFiles) {}
record DreamMakerOutputFile(
    String name, String url, String coverUrl, String fileType, Integer durationMs) {}
```

## Data Models

No database migration is required in this stage.

`MusicGenerationResult` is extended with:

| Field | Type | Constraints |
|---|---|---|
| `audioSourceUrl` | `String` | Optional; provider source URL before import |
| `audioContentType` | `String` | Optional; defaults to `audio/mpeg` when imported |

## Edge Cases

- EC-1: Missing or blank `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY` returns
  `MUSIC_GENERATION_FAILED`.
- EC-2: HTTP 429 maps to `RATE_LIMITED`.
- EC-3: HTTP timeout or polling exhaustion maps to `PROVIDER_TIMEOUT`.
- EC-4: Non-zero DreamMaker `code` maps conservatively until real error samples are collected.
- EC-5: Success status without an audio output maps to `MUSIC_QUALITY_FAILED`.
- EC-6: Relative provider output URLs are resolved against `DREAMMAKER_API_BASE_URL` by the HTTP
  client before reaching providers.
- EC-7: Audio download failure maps to `PACKAGE_BUILD_FAILED` in the workflow because the provider
  succeeded but the platform failed to import the asset.
- EC-8: Unknown provider task status maps to `UNKNOWN` and is treated as non-terminal by providers;
  polling continues until a terminal status or timeout.
- EC-9: Real provider failure messages are sanitized before entering `provider_calls`, work failure
  state, user-facing responses, or project documentation.

## Out of Scope

- Real authenticated Suno/MiniMax smoke calls are out of scope for automated tests; manual calls
  must follow `docs/runbook/dreammaker-controlled-real-integration.md`.
- Provider cost accounting and quota unit mapping are out of scope until pricing is confirmed.
- Automatic fallback from one real music provider to another is out of scope until the operations
  strategy is defined.
