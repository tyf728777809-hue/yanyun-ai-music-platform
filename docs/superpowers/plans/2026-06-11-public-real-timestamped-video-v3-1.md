# Public Real Timestamped Lyrics and Video v3.1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the next four-batch loop through a committed local rendering fix, public real Suno retest, timestamped lyrics verification, video v3.1 quality decision, and real-provider stability evidence.

**Architecture:** Keep deterministic workflow and Provider/Adapter boundaries. Yunwu remains the public-network Suno smoke backend while DreamMaker remains the production target. Timestamped lyrics verification is a gated smoke path that uses persisted provider trace plus audio id metadata and only records sanitized evidence.

**Tech Stack:** Java 21, Spring Boot 3, Gradle Kotlin DSL, PostgreSQL, Bash smoke scripts, jq/curl, Remotion/FFmpeg/ffprobe, Yunwu Suno, DeepSeek v4Pro, WellAPI Image2.

---

### Task 1: Persist Provider Audio Id for Yunwu Suno

**Files:**
- Modify: `modules/music-provider/src/main/java/com/yanyun/music/musicprovider/MusicGenerationResult.java`
- Modify: `modules/suno/src/main/java/com/yanyun/music/suno/YunwuSunoMusicProvider.java`
- Modify: `modules/production/src/main/java/com/yanyun/music/production/MockSongProductionWorkflow.java`
- Test: `modules/suno/src/test/java/com/yanyun/music/suno/YunwuSunoMusicProviderTest.java`
- Test: `apps/music-api/src/test/java/com/yanyun/music/production/MockSongProductionWorkflowTest.java`

- [x] **Step 1: Extend `MusicGenerationResult` with sanitized metadata**

Add a `Map<String, Object> metadata` component with constructor normalization to `Map.of()` and update all factory methods to pass empty metadata unless explicitly provided.

- [x] **Step 2: Parse Yunwu audio id from success response**

In `YunwuSunoMusicProvider`, select the same clip/object that provides the audio URL, extract `audio_id`, `audioId`, `clip_id`, `clipId`, or `id`, and return it as metadata key `provider_audio_id` without storing supplier URL.

- [x] **Step 3: Persist audio metadata into `media_assets`**

In `MockSongProductionWorkflow`, write audio asset metadata from `MusicGenerationResult.metadata()` plus safe provider labels. Do not include raw supplier URLs, API keys, full request payloads, or full response payloads.

- [x] **Step 4: Verify with unit tests**

Run:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21 ./gradlew --no-daemon :modules:suno:test :apps:music-api:test --tests 'com.yanyun.music.production.MockSongProductionWorkflowTest'
```

Expected: all selected tests pass.

---

### Task 2: Add Gated Timestamped Lyrics Smoke

**Files:**
- Create: `scripts/smoke/yunwu-suno-timestamped-lyrics-smoke.sh`
- Modify: `scripts/smoke/public-real-full-experience-stack.sh`
- Modify: `docs/runbook/yunwu-suno-controlled-real-integration.md`
- Modify: `docs/integrations/real-model-smoke-evidence-log.md`

- [x] **Step 1: Create a no-secret timestamped lyrics smoke script**

The script must require:

```bash
ALLOW_YUNWU_TIMESTAMPED_LYRICS_SMOKE=1
SUNO_BACKEND=yunwu
YUNWU_REAL_CALLS_ENABLED=true
WORK_ID=<real generated work id>
YUNWU_API_KEY=<current shell only>
```

It must read paired `media_assets.metadata_json->>'provider_task_id'` and `media_assets.metadata_json->>'provider_audio_id'` from PostgreSQL. It must fail before external HTTP if either id is missing.

- [x] **Step 2: Make endpoint configurable**

Use:

```bash
YUNWU_TIMESTAMPED_LYRICS_PATH="${YUNWU_TIMESTAMPED_LYRICS_PATH:-/api/v1/generate/get-timestamped-lyrics}"
```

This keeps the smoke compatible with Suno-compatible providers while allowing Yunwu-specific path correction after Apifox confirmation.

- [x] **Step 3: Sanitize output**

Print only:

- HTTP status
- provider `code`
- aligned word count
- waveform presence
- first timestamp presence
- PASS/FAIL

Do not print task id, audio id, full lyrics, raw response, media URL, or Authorization header.

- [x] **Step 4: Wire public full experience smoke**

After package-ready media checks and before frontend handoff, call the timestamped lyrics smoke when:

```bash
CHECK_YUNWU_TIMESTAMPED_LYRICS=true
```

Default should be `true` for the public full experience smoke so this goal cannot silently skip subtitle evidence. Operators may set it to `false` only for non-subtitle debugging.

- [x] **Step 5: Static verification**

Run:

```bash
bash -n scripts/smoke/yunwu-suno-timestamped-lyrics-smoke.sh scripts/smoke/public-real-full-experience-stack.sh
git diff --check
```

Expected: both commands pass.

---

### Task 3: Real Retest and Video v3.1 Decision

**Files:**
- Modify after real sample: `docs/integrations/real-model-smoke-evidence-log.md`
- Modify after real sample: `docs/project-progress.md`
- Modify if needed: `apps/render-worker/src/*`
- Modify if needed: `modules/production/src/main/java/com/yanyun/music/production/*`

- [x] **Step 1: Run strict preflight**

Run:

```bash
TARGET=public-real-full-experience MODE=plan scripts/smoke/real-model-controlled-smoke.sh
TARGET=public-real-full-experience MODE=preflight scripts/smoke/real-model-controlled-smoke.sh
```

Result: `MODE=plan` printed the unified controlled preflight command; `MODE=preflight` is `READY` only when current shell has all three credentials. Without credentials it reports only missing credential variables, not stack defaults that execute mode sets internally.

- [x] **Step 2: Execute one real sample**

Only after preflight is ready:

```bash
ALLOW_REAL_MODEL_SMOKE=1 ALLOW_PUBLIC_REAL_FULL_EXPERIENCE=1 TARGET=public-real-full-experience MODE=execute scripts/smoke/real-model-controlled-smoke.sh
```

Result: one `chirp-fenix` work reached `GENERATED / PACKAGE_READY` with DeepSeek v4Pro lyrics, Yunwu Suno music, WellAPI Image2 cover, and local-process MP4. The first script run ended after package-ready because its ffprobe CSV parsing misread `video,` as missing video; the MP4 itself has H.264 video and AAC audio. Frontend handoff was not rerun in this cost-saving pass after the validation false-negative.

- [x] **Step 3: Decide subtitle strategy**

If timestamped lyrics returns usable aligned words, v3.1 uses provider timeline. If missing or failed, v3.1 defaults to no subtitles or weak lyric-card treatment; do not ship estimated line-by-line subtitles as the default.

Decision: current Yunwu base URL did not expose a JSON timestamped lyrics endpoint. Default SunoAPI-compatible path returned 404; sampled `/suno/*` paths returned HTML. v3.1 must not depend on hard line-synced subtitles until the supplier confirms the correct path/base URL.

- [x] **Step 4: Verify media quality**

Run `ffprobe` on the generated MP4. Required evidence:

- H.264 video stream
- AAC audio stream
- 1920x1080
- playable in browser
- subtitle strategy explicitly recorded

Result: DB media assets contain audio, cover, video, and timeline. Local object storage MP4 was verified with H.264 video and AAC audio at 1920x1080. Browser playback remains a follow-up once frontend/manual test is rerun against this exact work.

---

### Task 4: Stabilize Real Provider Evidence

**Files:**
- Modify: `docs/integrations/real-model-smoke-evidence-log.md`
- Modify: `docs/integrations/dreammaker-open-questions-tracker.md`
- Modify: `docs/integrations/suno-minimax-preintegration-notes.md`
- Modify: `docs/project-progress.md`

- [x] **Step 1: Record DeepSeek evidence**

Record model name, status, and sanitized failure if any. Do not record full prompts, lyrics payloads, keys, or raw responses.

- [x] **Step 2: Record Yunwu evidence**

Record `yunwu:suno:chirp-fenix`, provider trace presence, audio id presence, timestamped lyrics result, and retry/failure behavior.

- [x] **Step 3: Record WellAPI evidence**

Record cover provider, model, dimensions, object-storage import status, and no raw supplier URL retention.

- [x] **Step 4: Preserve DreamMaker production target**

Confirm docs still state DreamMaker music and DreamMaker Image2 are production targets, while Yunwu/WellAPI are public-network smoke paths only.

- [ ] **Step 5: Run evidence audits**

Run:

```bash
scripts/smoke/real-model-evidence-log-audit.sh
scripts/smoke/local-delivery-evidence-audit.sh
```

Expected: both pass without external provider calls.
