# Suno / MiniMax Music Provider Pre-Integration Notes

## Status

- Current stage: DreamMaker provider skeleton remains implemented for production target; local default remains mock.
- Real Suno calls: two configurable backends now exist. `SUNO_BACKEND=yunwu` is the current public-network integration path; `SUNO_BACKEND=dreammaker` preserves the production-target DreamMaker path. Neither backend is used in automated tests.
- Real Suno via Yunwu: implemented behind `YUNWU_API_KEY`, not run in automated tests; blocked unless `YUNWU_REAL_CALLS_ENABLED=true`.
- Real Suno via DreamMaker: implemented behind `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY`, not run in automated tests; additionally blocked unless `DREAMMAKER_REAL_CALLS_ENABLED=true`.
- Real MiniMax calls: implemented behind `DREAMMAKER_ACCESS_KEY` / `DREAMMAKER_SECRET_KEY`, not run
  in automated tests; additionally blocked unless `DREAMMAKER_REAL_CALLS_ENABLED=true`.
- Feishu reference doc: fetched through `lark-cli docs +fetch` after user authorization.
- Real credentials: must not be committed, logged, or written into docs.

## Implemented Boundary

The backend already has a shared music provider contract:

- `MusicProvider.submit(MusicGenerationRequest request)`.
- `MusicProviderType`: `MOCK`, `SUNO`, `MINIMAX`.
- `MusicGenerationResult`: provider task id, status, audio object key, duration, failure code, failure message.
- Runtime selection: `MUSIC_PROVIDER=mock|suno|minimax`.
- Per-request override: `confirm` and `music/retry` accept `music_provider`.
- Provider calls are now written to `provider_calls`.
- `modules:dreammaker` defines the shared DreamMaker run/status client contract.
- `SunoMusicProvider` builds DreamMaker params, submits tasks, polls status, maps failure codes,
  and returns provider audio source URLs.
- `YunwuSunoMusicProvider` calls Yunwu `POST /suno/submit/music` and `GET /suno/fetch/{task_id}`,
  maps tolerant response shapes, and returns provider audio source URLs.
- `MiniMaxMusicProvider` builds DreamMaker params, submits tasks, polls status, maps failure codes,
  and returns provider audio source URLs.
- Workflow imports provider audio source URLs into object storage before writing `AUDIO` media
  assets, so user-facing URLs remain platform-owned.
- API and worker share the same DreamMaker HTTP client and property model; Temporal worker real
  provider calls use the same JWT and hard-switch checks as API local mode.

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

## DreamMaker API Shape

DreamMaker remains the production target and must not be deleted when using Yunwu for public-network
local testing.

Both Suno and MiniMax use DreamMaker task-style APIs:

- Submit task: `POST https://api-all.dreammaker.netease.com/api/v1/apps/{app}/run?sub_app_name={sub_app}`.
- Poll task: `GET https://api-all.dreammaker.netease.com/api/v1/apps/{app}/status?sub_app_name={sub_app}&task_id={task_id}`.
- Auth header: `Authorization: Bearer <jwt>`.
- JWT: RFC 7519 style HS256 token. Header is `{"alg":"HS256","typ":"JWT"}`. Payload includes
  `iss=<DREAMMAKER_ACCESS_KEY>`, `exp=now+1800s`, and `nbf=now-5s`. Signature uses
  `DREAMMAKER_SECRET_KEY`.
- Optional user identity header: `X-Access-Token: <DREAMMAKER_USER_ACCESS_TOKEN>`. If omitted,
  DreamMaker uses the API key binding's system account/default user group.
- Body wrapper: submit request body is `{ "params": { ... } }`.
- Submit success: `code = 0`, `data.task_id` is returned.
- Status success: `code = 0`, `data.status` is one of `success`, `failed`, `running`, `queued`.
- Output files: `data.output.output[]`, with provider-relative `url` values that must be downloaded or copied into our object storage before package build.

## Suno Integration Details

### Current public-network backend: Yunwu

- Config: `MUSIC_PROVIDER=suno`, `SUNO_BACKEND=yunwu`.
- Base URL: `YUNWU_BASE_URL=https://yunwu.ai`.
- Auth header: `Authorization: Bearer <YUNWU_API_KEY>`.
- Submit endpoint: `POST /suno/submit/music`.
- Poll endpoint: `GET /suno/fetch/{task_id}`.
- Default model: `YUNWU_SUNO_MODEL=chirp-fenix`（对应 v5.5）。
- Submit fields used by current adapter: `mv`, `make_instrumental=false`, `prompt`, `tags`, `title`.
- Submit success may return `data` as the task id directly, or `data.task_id`; adapter parses both.
- Status and audio URL shape is not fully documented; adapter tolerantly parses `status`, `data.status`,
  `clips[].status`, `audio_url`, `url`, and `data.clips[].audio_url`.

### Production-target backend: DreamMaker

- App path: `suno`.
- Submit sub app: `music-gen`.
- Submit endpoint: `/api/v1/apps/suno/run?sub_app_name=music-gen`.
- Status endpoint: `/api/v1/apps/suno/status?sub_app_name=music-gen&task_id={task_id}`.
- Default model in provider doc: `chirp-crow`.
- Supported models include `chirp-v3-5`, `chirp-v4`, `chirp-auk-turbo`, `chirp-auk`, `chirp-bluejay`, `chirp-crow`, `chirp-fenix`.

Suno submit fields:

| Field | Requirement | Notes |
|---|---|---|
| `gpt_description_prompt` | Optional | Inspiration-mode description; max 500 chars; mutually exclusive with `prompt`. |
| `prompt` | Optional | Custom lyrics; max 5000 chars; supports `[Verse]`, `[Chorus]`, `[Bridge]`, etc. |
| `tags` | Optional | Style tags; max 1000 chars. |
| `negative_tags` | Optional | Styles to avoid. |
| `mv` | Optional | Model version. |
| `title` | Optional | Song title; max 80 chars. |
| `make_instrumental` | Optional | Instrumental mode. |
| `continue_clip_id` | Optional | Source Suno ID for continuation. |
| `continue_at` | Optional | Continuation start seconds. |
| `metadata.vocal_gender` | Optional | `f` for female, `m` for male. |
| `metadata.control_sliders.style_weight` | Optional | Range 0 to 1. |
| `metadata.control_sliders.weirdness_constraint` | Optional | Range 0 to 1. |

Suno status output:

- Usually returns two audio outputs.
- Audio item fields include `name`, `url`, `cover`, `file_type`.
- `file_type` is `audio`.

## MiniMax Integration Details

- App path: `music-minimax`.
- Submit sub app: `text-to-music`.
- Submit endpoint: `/api/v1/apps/music-minimax/run?sub_app_name=text-to-music`.
- Status endpoint: `/api/v1/apps/music-minimax/status?sub_app_name=text-to-music&task_id={task_id}`.
- Current model: `minimax-music-2.6`.

MiniMax submit fields:

| Field | Requirement | Notes |
|---|---|---|
| `model` | Required | Current value: `minimax-music-2.6`. |
| `prompt` | Required | Style, mood, scene description; recommended <= 300 chars; max 2000 chars. |
| `lyrics` | Optional | Required for non-instrumental generation; length [1, 3500]; supports section tags. |
| `is_instrumental` | Optional | If true, `lyrics` may be empty. |
| `lyrics_optimizer` | Optional | Default true; can generate/optimize lyrics from prompt. |
| `audio_format` | Optional | `mp3` or `wav`; default `mp3`. |
| `sample_rate` | Optional | `16000`, `24000`, `32000`, `44100`; default `44100`. |
| `bitrate` | Optional | `32000`, `64000`, `128000`, `256000`; default `256000`. |

MiniMax status output:

- Audio item fields include `duration`, `file_type`, `name`, `url`.
- `duration` is seconds as a string.
- `file_type` is `audio`.

## Internal Failure Mapping

Keep mapping conservative until real error-code samples are available:

| Provider condition | Internal failure code | Retry |
|---|---|---|
| Network timeout or callback timeout | `PROVIDER_TIMEOUT` | Yes, while retry count remains |
| Provider rate limit or quota throttle | `RATE_LIMITED` | Yes, while retry count remains |
| Provider accepts task but generated audio fails quality gate | `MUSIC_QUALITY_FAILED` | Yes, while retry count remains |
| Provider submission fails for unknown transient reason | `MUSIC_GENERATION_FAILED` | Yes, while retry count remains |
| Submit response `code != 0` | `MUSIC_GENERATION_FAILED` initially; refine after concrete codes | Depends on provider error |
| Poll response `data.status = failed` | `MUSIC_GENERATION_FAILED` or `MUSIC_QUALITY_FAILED` based on provider failure message | Yes, while retry count remains |
| Auth, signature, project config, unsupported model, 401 / 403 | `PROVIDER_AUTH_FAILED` | No user retry; contact support / fix provider config |

## Required Real Provider Details

Before running real authenticated calls broadly, confirm these remaining details:

1. Production secret manager naming and rotation policy for AccessKey/SecretKey.
2. Concrete non-zero `code` values and retryability.
3. `failed` task response examples and failure-message schema.
4. Rate limit, timeout, polling interval, and maximum polling duration.
5. Whether status API `url` values are relative to the same DreamMaker domain.
6. Whether audio URLs expire.
7. Required file download API, if direct URL download is not allowed.
8. Pricing or quota unit per task.
9. Content moderation and blocked-content error codes.

## Implemented Plan

1. Added DreamMaker config properties, with no real secret defaults:
   - `DREAMMAKER_API_BASE_URL`.
   - `DREAMMAKER_ACCESS_KEY`.
   - `DREAMMAKER_SECRET_KEY`.
   - `DREAMMAKER_USER_ACCESS_TOKEN` optional passthrough for `X-Access-Token`.
   - `DREAMMAKER_REAL_CALLS_ENABLED` hard switch, default false.
   - `DREAMMAKER_SUNO_MODEL`.
   - `YUNWU_BASE_URL`.
   - `YUNWU_API_KEY`.
   - `YUNWU_REAL_CALLS_ENABLED`.
   - `YUNWU_SUNO_MODEL`.
   - `MINIMAX_MODEL`.
   - submit timeout, poll interval, poll timeout.
2. Implemented request builders:
   - Suno custom lyrics mode maps `lyricsText` to `prompt`, `musicPrompt` to `tags`, title to `title`, vocal preference to `metadata.vocal_gender` when possible.
   - MiniMax maps `musicPrompt` to `prompt`, `lyricsText` to `lyrics`, sets `lyrics_optimizer=false` when confirmed lyrics are present, and defaults `audio_format=mp3`, `sample_rate=44100`, `bitrate=256000`.
3. Implemented DreamMaker submit + polling client with per-request HS256 JWT authentication.
4. Added provider failure-code mapping tests.
5. Added HTTP client tests with local mocked DreamMaker responses.
6. `data.task_id` is returned as `MusicGenerationResult.providerTaskId` and recorded in `provider_calls.provider_trace_id`.
7. Provider output audio URL is imported into object storage before workflow writes the audio media asset.
8. Automated tests still do not call real providers; real calls require manual environment variables
   plus `DREAMMAKER_REAL_CALLS_ENABLED=true`.
9. Added controlled real-integration runbook, acceptance checklist, security/log handling rules,
   open-question tracker, and company handoff note for the next manual联调 window.

## Non-Goals

- Do not call real Suno or MiniMax in local automated tests.
- Do not store real API keys in repo files.
- Do not expose provider raw error payloads directly to users.
- Do not bypass company-side account, audit, publish, or entitlement systems.
