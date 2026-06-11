# Real Model Smoke Evidence Log

Sanitized only. Do not paste real API keys, AccessKey or SecretKey values, Bearer/JWT values, cookies, user tokens, raw provider payloads, full prompts, complete lyrics, generated media URLs, signed URLs, full provider task ids, screenshots with credentials, or private user text.

DreamMaker music and DreamMaker Image 2 remain the production-target paths. Yunwu Suno and WellAPI Image 2 are public-network controlled smoke paths for the current non-company-intranet environment only; their success does not replace DreamMaker production validation.

## Target Registry

| Target | Provider role | Current status | Evidence entry |
|---|---|---|---|
| `dreammaker-suno` | production-target | `blocked_external` | 2026-06-07 HTTP 403 sanitized sample below. |
| `dreammaker-minimax` | production-target | `not_started` | No real successful sample yet. |
| `yunwu-suno` | public-network-smoke | `succeeded_public_smoke` | 2026-06-07 public-network single music sample succeeded. |
| `yunwu-suno-timestamped-lyrics` | public-network-smoke-subcheck | `blocked_provider_path` | 2026-06-11 `chirp-fenix` sample had task/audio id, but current Yunwu base URL did not expose a JSON timestamped-lyrics endpoint. |
| `deepseek` | model-smoke | `succeeded_public_smoke` | 2026-06-07 public-network single lyrics sample succeeded. |
| `wellapi-image2` | public-network-smoke | `succeeded_public_smoke` | 2026-06-07 public-network single cover sample succeeded. |
| `dreammaker-image2` | production-target | `not_started` | Production-target cover smoke prepared; no real successful sample yet. |
| `public-real-full-experience` | public-network-full-experience | `succeeded_public_smoke` | 2026-06-12 v0.5 Agent combined sample with DeepSeek + Yunwu Suno `chirp-fenix` + WellAPI Image 2 + `album-ffmpeg` reached `GENERATED / PACKAGE_READY`, then `PACKAGE_FETCHED` after Claude Web v1 handoff smoke. |

## Evidence Entries

| Date | Target | Backend / Model | Environment | Work ID | Job ID | Provider Trace | Result | Failure Code | Retryable | Object Import | Package URL | Audit Evidence | Cost / Rate Notes | Next Decision |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-06-07 | `dreammaker-suno` | DreamMaker Suno | Local public network, not company intranet | `<recorded-in-local-progress>` | `<present>` | `<empty>` | Failed at provider create task with HTTP 403 | `PROVIDER_AUTH_FAILED` / provider permission | No user retry | N/A | No | `provider_calls` failed row present; no raw payload stored in this log | No confirmed billing sample | Re-test DreamMaker from company intranet or with provider-approved account permissions; keep DreamMaker as production target. |
| TBD | `dreammaker-minimax` | DreamMaker MiniMax | TBD | N/A | N/A | N/A | Not started | N/A | N/A | N/A | N/A | Run controlled smoke only through `TARGET=dreammaker-minimax` | N/A | Await company intranet / credentials window. |
| 2026-06-07 | `yunwu-suno` | Yunwu Suno `chirp-v5` | Local public network | `1a6bf3b8-eb8e-4236-a36a-330d11ea8a6b` | `<present>` | `<present>` | `GENERATED / PACKAGE_READY` | N/A | N/A | Audio, cover, video, timeline present in platform storage | Yes | `provider_calls`: `SUNO / yunwu:suno:chirp-v5 / SUCCEEDED`; media URL presence only | One controlled sample | Keep as public-network smoke path only; DreamMaker remains production target. |
| 2026-06-07 | `deepseek` | DeepSeek `deepseek-v4-pro` | Local public network | `850ee5e0-ddb9-4132-863e-50f4d979e76b` | `<present>` | N/A | `LYRICS_READY / WAITING_CONFIRM` | N/A | N/A | N/A | N/A | `agent_runs`: `LyricsAgent / deepseek-v4-pro / SUCCEEDED / input_hash present / output_hash present` | One controlled sample | DeepSeek real lyrics path ready for public-network testing; music/image/company systems stayed Mock. |
| 2026-06-07 | `wellapi-image2` | WellAPI `gpt-image-2` | Local public network | `ed005537-2b90-464f-814a-c8dd4a07e3df` | `<present>` | N/A | `GENERATED / PACKAGE_READY` | N/A | N/A | Cover imported to platform storage; `wellapi-image2`, `image/jpeg`, `2048x1152`; no supplier URL or inline base64 retained in metadata | Yes | `media_assets`: cover provider `wellapi-image2`; package cover/video/timeline URL presence only | One controlled sample | Keep as public-network smoke path only; DreamMaker Image 2 remains production target. |
| TBD | `dreammaker-image2` | DreamMaker Image 2 | TBD | N/A | N/A | N/A | Not started | N/A | N/A | N/A | N/A | Run controlled smoke only through `TARGET=dreammaker-image2` | N/A | Await company intranet / credentials window. |
| 2026-06-07 | `public-real-full-experience` | DeepSeek v4Pro + Yunwu Suno + WellAPI gpt-image-2 + local-process MP4 | Local public network | N/A | N/A | N/A | Blocked before service startup because current shell credentials were missing | N/A | N/A | N/A | N/A | Gate checks passed; strict preflight reported missing current-shell credentials; no provider call made | No cost | Export credentials in current shell or run script interactively, then re-run through unified target execute with both allow gates. |
| 2026-06-07 | `public-real-full-experience` | DeepSeek v4Pro + Yunwu Suno `chirp-v5` + WellAPI `gpt-image-2` + local-process MP4 + Claude Web v1 | Local public network | `0dd48e52-2477-4c89-ad7a-43d18a976657` | `1d465d0c-f907-497e-a63b-8be9a8a2a566` | `<present>` | `GENERATED / PACKAGE_READY`, then `PACKAGE_FETCHED` after frontend handoff action | N/A | N/A | Audio, cover, video, timeline present in platform storage; publish package contains audio/cover/video/timeline URL presence | Yes | `agent_runs`: LyricsAgent DeepSeek succeeded; `provider_calls`: Yunwu Suno succeeded; `media_assets`: audio/cover/video/timeline present; Playwright checked Claude Web v1 handoff actions | One controlled combined sample | Public full product experience is ready for user trial on this path; still not a DreamMaker production validation or company-system integration. |
| 2026-06-11 | `public-real-full-experience` | DeepSeek v4Pro + Yunwu Suno `chirp-fenix` + WellAPI `gpt-image-2` + local-process MP4 | Local public network | `20fa149f-d5b1-40b2-9b22-8561148158db` | `<present>` | `<present>` | `GENERATED / PACKAGE_READY`; CLI script initially failed after package-ready because ffprobe CSV parsing misread `video,` as missing video | N/A | N/A | Audio, cover, video, timeline present in platform storage; MP4 file verified locally with H.264 video and AAC audio; browser metadata smoke loaded duration/resolution and muted playback without console errors | Yes | `agent_runs`: LyricsAgent DeepSeek succeeded; `provider_calls`: Yunwu Suno `yunwu:suno:chirp-fenix` succeeded; `media_assets`: audio/cover/video/timeline present; no raw payload stored | One controlled combined sample | Keep current public full path usable; script stream parsing fixed after this sample, no need to re-spend model credits solely for this false-negative. |
| 2026-06-12 | `public-real-full-experience` | DeepSeek v4Pro Agent v0.5 + Yunwu Suno `chirp-fenix` + WellAPI `gpt-image-2` + album-ffmpeg MP4 | Local public network | `59e9560e-b81d-4319-b151-891421102262` | `<present>` | `<present>` | Failed after real Yunwu audio import; cover/video not generated because cover prompt quality gate blocked the sample | `PACKAGE_BUILD_FAILED` | yes, but recommended action was support/manual review | Audio present in platform storage; cover/video absent | No | `agent_runs`: CreativeBrief, Lyrics, MusicPrompt, and QualityEvaluation used `deepseek-v4-pro`; `provider_calls`: Yunwu Suno succeeded; no raw prompt, lyrics, payload, media URL, or provider response stored | One controlled v0.5 Agent sample; no automatic music retry after script update | Fix or tune CoverPromptAgent / cover prompt quality gate before using v0.5 Agent path for user-facing full-product tests. |
| 2026-06-12 | `public-real-full-experience` | DeepSeek v4Pro Agent v0.5 + Yunwu Suno `chirp-fenix` + WellAPI `gpt-image-2` + album-ffmpeg MP4 | Local public network, isolated Temporal task queue | `62ef3e35-2f9e-44c8-a120-1144e993c061` | `<present>` | `<present>` | `GENERATED / PACKAGE_READY`, then `PACKAGE_FETCHED` after Claude Web v1 handoff action | N/A | N/A | Audio, cover, video, timeline present in platform storage; MP4 verified H.264/AAC, 1920x1080, duration aligned to audio | Yes | `agent_runs`: CreativeBrief, Lyrics, MusicPrompt, CoverPrompt, and QualityEvaluation used `deepseek-v4-pro`; `provider_calls`: Yunwu Suno `yunwu:suno:chirp-fenix` succeeded; `media_assets`: audio/cover/video/timeline present; Claude Web v1 verified finished page, refreshed links, and mark-fetched action | One controlled sample after user replenished provider balance; no automatic music retry | Current public full product path is ready for user manual trial, with DreamMaker still the production-target path and company systems still mocked. |
| 2026-06-11 | `yunwu-suno-timestamped-lyrics` | Yunwu Suno `chirp-fenix` timestamped lyrics | Local public network | `20fa149f-d5b1-40b2-9b22-8561148158db` | N/A | `<present>` | Failed: default SunoAPI-compatible path returned 404; sampled `/suno/*` alternatives returned HTML, not JSON | N/A | No | N/A | N/A | Evidence file contains only HTTP status, provider code, aligned word count, waveform presence, timestamp presence; no raw lyric payload stored | One non-generation endpoint check | Do not rely on provider timestamped lyrics for v3.1 subtitles until Yunwu confirms the correct API path/base URL; default video should avoid hard line-synced subtitles or use a no-subtitle/weak lyric-card treatment. |

## Entry Template

Use this template after each real smoke attempt. Keep evidence concise and sanitized.

| Field | Value |
|---|---|
| Date / time |  |
| Target | `dreammaker-suno` / `dreammaker-minimax` / `yunwu-suno` / `deepseek` / `wellapi-image2` / `dreammaker-image2` / `public-real-full-experience` |
| Provider role | `production-target` / `public-network-smoke` / `model-smoke` / `public-network-full-experience` |
| Backend / model |  |
| Operator |  |
| Environment | local public network / company intranet / staging / production-like |
| `work_id` |  |
| `job_id` |  |
| Provider trace / task id | `<present>` / `<empty>` / N/A |
| Provider audio id | `<present>` / `<empty>` / N/A |
| Final status |  |
| Failure code |  |
| Recommended action |  |
| Retryable | yes / no / unknown |
| Duration |  |
| Platform object import | yes / no / N/A |
| Package URL available | yes / no / N/A |
| Timestamped lyrics | PASS / FAIL / skipped / N/A; record only aligned word count, waveform presence, and timestamp presence |
| `agent_runs` / `provider_calls` evidence | sanitized summary only |
| Cost / rate-limit notes |  |
| Next decision |  |

## Forbidden Evidence

- Real API keys, AccessKey or SecretKey values, Bearer/JWT values, cookies, user tokens, private keys, or production secrets.
- Raw provider request/response bodies, raw model messages, full prompts, complete lyrics, private user text, or full generated outputs.
- Supplier media URLs, signed platform URLs, full provider task ids, full provider trace ids, and screenshots that include credentials or private data.
- Raw timestamped lyrics payloads, full LRC/SRT/timeline text, full aligned words arrays, waveform arrays, and supplier lyric URLs.
- Claims that Yunwu or WellAPI success means DreamMaker production validation is complete.
