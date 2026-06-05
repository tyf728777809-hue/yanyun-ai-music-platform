# Suno / MiniMax Music Provider Pre-Integration Notes

## Status

- Current stage: local mock stage.
- Real Suno calls: not implemented.
- Real MiniMax calls: not implemented.
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

Both Suno and MiniMax use DreamMaker task-style APIs:

- Submit task: `POST https://api-all.dreammaker.netease.com/api/v1/apps/{app}/run?sub_app_name={sub_app}`.
- Poll task: `GET https://api-all.dreammaker.netease.com/api/v1/apps/{app}/status?sub_app_name={sub_app}&task_id={task_id}`.
- Auth header: `Authorization: Bearer <key>`.
- Body wrapper: submit request body is `{ "params": { ... } }`.
- Submit success: `code = 0`, `data.task_id` is returned.
- Status success: `code = 0`, `data.status` is one of `success`, `failed`, `running`, `queued`.
- Output files: `data.output.output[]`, with provider-relative `url` values that must be downloaded or copied into our object storage before package build.

## Suno Integration Details

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
| Auth, signature, project config, or unsupported model error | `MUSIC_GENERATION_FAILED` initially; refine after concrete codes | Usually no, but keep conservative until samples exist |

## Required Real Provider Details

Before implementing real calls, confirm these remaining details:

1. Real API key delivery path and local secret naming.
2. Whether `Bearer <key>` differs by Suno and MiniMax or uses one shared DreamMaker key.
3. Concrete non-zero `code` values and retryability.
4. `failed` task response examples and failure-message schema.
5. Rate limit, timeout, polling interval, and maximum polling duration.
6. Whether status API `url` values are relative to the same DreamMaker domain.
7. Whether audio URLs expire.
8. Required file download API, if direct URL download is not allowed.
9. Pricing or quota unit per task.
10. Content moderation and blocked-content error codes.

## Implementation Plan When Docs Are Available

1. Add DreamMaker config properties, with no real defaults:
   - `DREAMMAKER_API_BASE_URL`.
   - `DREAMMAKER_API_KEY`.
   - `SUNO_MODEL`.
   - `MINIMAX_MODEL`.
   - submit timeout, poll interval, poll timeout.
2. Implement request builders:
   - Suno custom lyrics mode maps `lyricsText` to `prompt`, `musicPrompt` to `tags`, title to `title`, vocal preference to `metadata.vocal_gender` when possible.
   - MiniMax maps `musicPrompt` to `prompt`, `lyricsText` to `lyrics`, default `audio_format=mp3`, `sample_rate=44100`, `bitrate=256000`.
3. Implement DreamMaker submit + polling client.
4. Add provider failure-code mapping tests.
5. Add HTTP client tests with mocked DreamMaker responses.
6. Store `data.task_id` in `provider_calls.provider_trace_id`.
7. Download or import generated audio into object storage before returning success.
8. Keep automated tests on mock provider only unless explicit test credentials are provided.

## Non-Goals

- Do not call real Suno or MiniMax in local automated tests.
- Do not store real API keys in repo files.
- Do not expose provider raw error payloads directly to users.
- Do not bypass company-side account, audit, publish, or entitlement systems.
