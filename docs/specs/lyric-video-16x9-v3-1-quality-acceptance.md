# Lyric Video 16:9 v3.1 Quality Acceptance

## 1. Title And Metadata

- Title: `lyric-video-16x9-v3.1` media quality and subtitle strategy acceptance
- Author: Codex
- Date: 2026-06-11
- Status: Approved baseline for next implementation / validation batch
- Reviewers: Product owner, engineering owner, company integration owner
- Scope: 16:9 MP4 output for public real AI product experience and company community handoff

## 2. Context

`lyric-video-16x9-v2` solved the core engineering issue that caused silent videos: MP4 output must include both video and audio streams. `lyric-video-16x9-v3` defined the desired visual direction: official OST visualizer, full-screen Image2 key art, restrained motion, safe text zones, and no cheap player-template look.

The remaining risk is subtitle synchronization. A user can notice immediately when lyric subtitles do not match the sung vocal. On 2026-06-11, a public real `chirp-fenix` / v5.5 sample reached `GENERATED / PACKAGE_READY`, but Yunwu timestamped lyrics verification was blocked by provider path behavior: the default SunoAPI-compatible path returned 404 and sampled `/suno/*` paths returned HTML instead of JSON.

Therefore v3.1 must not depend on hard line-synced subtitles until a provider timestamp source is verified. The product should prefer a premium no-subtitle visualizer or weak lyric-card treatment over visibly wrong karaoke-style subtitles.

## 3. Functional Requirements

- FR-1: The v3.1 MP4 output MUST include a decodable H.264 video stream and a decodable AAC audio stream.
- FR-2: The v3.1 default subtitle mode MUST NOT use hard line-synced subtitles unless provider timestamped lyrics has a PASS evidence entry for the same provider path.
- FR-3: If provider timestamped lyrics is `blocked_provider_path`, missing, malformed, or not JSON, the video MUST use either no hard subtitles or weak lyric-card text that is clearly not synchronized to each sung line.
- FR-4: If provider timestamped lyrics later passes, the renderer MAY enable line-level or word-level subtitles only after the timeline source, language alignment, and safe-area behavior are validated.
- FR-5: The video MUST keep the official OST visualizer direction from `docs/specs/lyric-video-16x9-v3-visual-design.md`.
- FR-6: The video MUST NOT show supplier names, provider IDs, object keys, JSON, signed URLs, internal failure codes, or technical handoff wording.
- FR-7: The acceptance evidence MUST record only sanitized media facts: work id, final status, stream presence, duration, resolution, package status, and timestamped lyrics outcome.

## 4. Non-Functional Requirements

- NFR-1: The exported MP4 SHOULD be 1920x1080, 16:9, 30fps, H.264 + AAC for current local-process acceptance.
- NFR-2: The video MUST be playable in a browser video element without console errors caused by the MP4 container.
- NFR-3: Text overlays MUST stay inside the lower-third safe area and MUST NOT overflow at 390px mobile preview or 1440px desktop preview when surfaced in the web product.
- NFR-4: Real supplier keys, bearer tokens, provider raw payloads, full prompts, full lyrics, supplier media URLs, and signed platform URLs MUST NOT be stored in Git docs, test snapshots, or logs.
- NFR-5: A failed timestamped lyrics check MUST NOT block package readiness by itself; it only controls subtitle mode.

## 5. Acceptance Criteria

- AC-1: Given a generated MP4, when `ffprobe` inspects streams, then at least one `video` stream and one `audio` stream are present, satisfying FR-1 and NFR-1.
- AC-2: Given Yunwu timestamped lyrics status is `blocked_provider_path`, when v3.1 renders a video, then it does not render karaoke-style hard-synced line subtitles, satisfying FR-2 and FR-3.
- AC-3: Given a supplier later returns valid timestamped lyrics JSON, when the renderer enables subtitles, then the acceptance evidence records timestamp presence, aligned count, browser playback, and safe-area checks before the mode becomes default, satisfying FR-4 and FR-7.
- AC-4: Given a public real full experience sample reaches `GENERATED / PACKAGE_READY`, when the evidence log is updated, then it records DreamMaker production target retention and does not claim Yunwu / WellAPI are production replacements, satisfying FR-7 and NFR-4.
- AC-5: Given the MP4 is opened in browser playback, when metadata loads, then duration, video width, and video height are readable and non-zero, satisfying NFR-2.

## 6. Edge Cases

- EC-1: Provider returns HTTP 404 for timestamped lyrics. Expected result: record `blocked_provider_path`, use no hard synced subtitles.
- EC-2: Provider returns HTTP 200 with HTML instead of JSON. Expected result: treat as `blocked_provider_path`, not as a successful lyrics response.
- EC-3: Provider returns JSON without aligned words, line timestamps, or waveform. Expected result: record timestamped lyrics as failed / incomplete and avoid hard synced subtitles.
- EC-4: MP4 has video only. Expected result: fail media acceptance; do not mark video quality gate passed.
- EC-5: MP4 has audio only or unreadable video dimensions. Expected result: fail media acceptance.
- EC-6: Lyrics are very long or contain section labels. Expected result: remove section labels from visual lyrics and avoid overflow; if timing is not trustworthy, use weak lyric-card or no-subtitle mode.

## 7. API Contracts

N/A. v3.1 does not add or change user-facing OpenAPI. It uses existing work detail, media asset, timeline, and publish package contracts.

## 8. Data Models

| Entity | Field | Type | Requirement |
|---|---|---|---|
| `media_assets` | `asset_type=VIDEO` | enum | MUST exist for a ready video package. |
| `media_assets` | `metadata_json` | json | SHOULD include sanitized renderer facts such as template id, duration, resolution, and stream validation status when available. |
| `media_assets` | `asset_type=TIMELINE` | enum | MAY exist as estimated visual timing; MUST NOT be treated as provider-verified lyric timing unless timestamped lyrics evidence passes. |
| Evidence log | `Timestamped lyrics` | status | MUST be `PASS`, `FAIL`, `blocked_provider_path`, `skipped`, or `N/A`. |
| Publish package | `video` | media URL | MUST point to platform object storage, not a supplier raw URL. |

## 9. Out Of Scope

- 9:16 vertical video is out of scope for v3.1.
- AI-generated motion video, character animation, and story-scene generation are out of scope.
- Company account, audit, entitlement, publishing, and sharing systems remain external Adapter responsibilities.
- DreamMaker production validation is out of scope for Yunwu public-network smoke, but DreamMaker production target paths must remain documented and testable.
- Provider raw timestamp payload persistence is out of scope and prohibited for evidence logs.
