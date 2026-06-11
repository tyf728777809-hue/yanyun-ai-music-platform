# DeepSeek Real Lyrics Smoke v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The project has a real DeepSeek OpenAI-compatible client, a DeepSeek runbook, and readiness reporting. Before this spec, DeepSeek real calls had no dedicated single-sample smoke script. Operators had to manually assemble curl calls, idempotency headers, readiness checks, and `agent_runs` database inspection.

The first DeepSeek smoke should verify only the lyrics path. Music, cover, video, DreamMaker, Image 2, and company Adapter systems must remain mock or disabled. The smoke must not print prompts, lyrics, complete model responses, or credentials.

This scope does not change the provider strategy: DreamMaker music and DreamMaker Image 2 remain the production-target interfaces. Yunwu and WellAPI are only public-network controlled smoke paths for the current non-company-intranet environment.

## Functional Requirements

- FR-1: The smoke MUST require both `ALLOW_REAL_MODEL_SMOKE=1` and `ALLOW_DEEPSEEK_REAL_SMOKE=1` before creating any work.
- FR-2: The smoke MUST require `AGENT_REAL_CALLS_ENABLED=true` and `DEEPSEEK_REAL_CALLS_ENABLED=true` in the operator shell.
- FR-3: The smoke MUST require `DEEPSEEK_API_KEY` to be present but MUST NOT print its value.
- FR-4: The smoke MUST require music, image, DreamMaker, and company Adapter real paths to remain mock or disabled.
- FR-5: The smoke MUST call API health and `/internal/integration-readiness` before creating work.
- FR-6: The smoke MUST require `deepseek_guard` to report `READY_FOR_LOCAL`, `real-calls-enabled`, and `RealDeepSeekLyricsClient`.
- FR-7: The smoke MUST create exactly one inspiration work using `POST /api/v1/works/inspiration`.
- FR-8: The smoke MUST verify the created work reaches `LYRICS_READY / WAITING_CONFIRM` and has a lyrics draft.
- FR-9: The smoke MUST query `agent_runs` for the created `work_id` and require a `LyricsAgent` row whose `model_name` is not `mock-deepseek-lyrics`, with status `SUCCEEDED`, non-null input hash, and non-null output hash.
- FR-10: The smoke MUST print only safe summaries: `work_id`, status, operation, model name, boolean hash presence, latency, and failure code.
- FR-11: The smoke MUST NOT confirm the work, generate music, generate cover, render video, build publish packages, call company systems, or call DreamMaker/Yunwu/WellAPI.

## Non-Functional Requirements

- NFR-1: The smoke MUST be shell-only and reuse existing local tools: `curl`, `jq`, `docker`, and `psql` inside the PostgreSQL container.
- NFR-2: The smoke MUST NOT print user prompt, generated lyrics, full DeepSeek request/response, authorization headers, API keys, or Bearer tokens.
- NFR-3: The smoke SHOULD complete after one API request plus database inspection, subject to external DeepSeek latency.

## Acceptance Criteria

- AC-1: Given either allow gate is missing, when the smoke runs, then it exits before calling API work creation. Covers FR-1.
- AC-2: Given allow gate but missing DeepSeek switches or API key, when the smoke runs, then it exits before work creation. Covers FR-2 and FR-3.
- AC-3: Given API readiness does not report `RealDeepSeekLyricsClient`, when the smoke runs, then it exits before work creation. Covers FR-5 and FR-6.
- AC-4: Given DeepSeek real call succeeds, when the smoke runs, then it creates one inspiration work and verifies `LYRICS_READY / WAITING_CONFIRM`. Covers FR-7 and FR-8.
- AC-5: Given DeepSeek real call succeeds, when the smoke inspects `agent_runs`, then it verifies `LyricsAgent` used a non-mock model and recorded hashes. Covers FR-9 and FR-10.
- AC-6: Given smoke output is scanned, when it contains secrets, full prompts, or generated lyrics, then the smoke is non-compliant. Covers FR-10, FR-11, and NFR-2.

## Edge Cases

- EC-1: If PostgreSQL container is unavailable, the smoke MUST fail after work creation rather than claiming success.
- EC-2: If DeepSeek returns 401, 403, or 429, the smoke MUST fail and rely on application-side sanitized errors.
- EC-3: If the API returns `LYRICS_FAILED`, the smoke MUST print only failure code and request id if available.
- EC-4: If `agent_runs` contains no matching `LyricsAgent`, the smoke MUST fail because real-model evidence is missing.
- EC-5: If `MUSIC_PROVIDER` is not mock or Image 2 real calls are enabled, the smoke MUST fail before work creation.

## API Contracts

```ts
POST /api/v1/works/inspiration
headers:
  X-Mock-User-Id: string
  Idempotency-Key: string
body:
  story_input: string
  mood?: string
  scene?: string
  music_style?: string
response:
  work_id: string
  status: "LYRICS_READY" | "LYRICS_FAILED" | string
  generation_stage: "WAITING_CONFIRM" | string
```

## Data Models

| Table | Fields Used | Requirement |
|---|---|---|
| `agent_runs` | `agent_name`, `operation`, `model_name`, `status`, `input_hash`, `output_hash`, `latency_ms`, `failure_code` | Must prove real `LyricsAgent` call without storing prompt/output text. |

## Out of Scope

- OS-1: This does not start `music-api`; the API must already be running with DeepSeek env vars.
- OS-2: This does not test lyrics polish or continue.
- OS-3: This does not confirm work, generate music, generate cover, render video, or build a publish package.
- OS-4: This does not call DreamMaker, Yunwu, WellAPI, Suno, MiniMax, Image 2, or company systems.
- OS-5: This does not replace or weaken the DreamMaker production-target interfaces.
