# Real Model Smoke Evidence Log v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The project now has controlled smoke entry points for six real-model targets: `dreammaker-suno`, `dreammaker-minimax`, `yunwu-suno`, `deepseek`, `wellapi-image2`, and `dreammaker-image2`.

DreamMaker music and DreamMaker Image 2 must remain the production-target interfaces. Yunwu Suno and WellAPI Image 2 are public-network controlled smoke paths for the current non-company-intranet environment only. A successful Yunwu or WellAPI smoke must not be recorded as a DreamMaker production success.

Real-model smoke evidence is useful for provider handoff, failure-code mapping, retry decisions, cost control, and company deployment preparation. It is also a security risk if operators paste keys, JWTs, raw provider payloads, full prompts, generated lyrics, supplier URLs, or trace ids into repository documents. A single sanitized evidence log is required so future handoff records stay consistent.

## Functional Requirements

- FR-1: The evidence log MUST exist at `docs/integrations/real-model-smoke-evidence-log.md`.
- FR-2: The evidence log MUST cover `dreammaker-suno`, `dreammaker-minimax`, `yunwu-suno`, `deepseek`, `wellapi-image2`, and `dreammaker-image2`.
- FR-3: The evidence log MUST label DreamMaker music and DreamMaker Image 2 as production-target paths.
- FR-4: The evidence log MUST label Yunwu and WellAPI as public-network smoke paths only.
- FR-5: Each real smoke attempt SHOULD record target, backend/model, date, operator, environment, work id, job id when available, status, failure code, retryable judgement, duration, platform object import result, package URL availability, audit table evidence, cost/rate-limit notes, and next decision.
- FR-6: Evidence MUST be sanitized only and MUST NOT include real API keys, AccessKey/SecretKey values, Bearer/JWT values, cookies, user tokens, raw provider payloads, full prompts, complete lyrics, generated media URLs, signed URLs, full provider task ids, screenshots with credentials, or private user text.
- FR-7: The first DreamMaker Suno HTTP 403 attempt MUST be represented as a sanitized failed sample without secrets or full provider payload.
- FR-8: Provider task ids and trace ids MAY be recorded only as `<present>`, `<empty>`, or a separately approved masked value.
- FR-9: Real smoke runbooks and acceptance checklists MUST point operators to the unified evidence log after each attempt.
- FR-10: A local audit script MUST verify the evidence log, target coverage, DreamMaker production-target rule, public-network smoke labels, and secret-like pattern absence without starting services or calling providers.

## Non-Functional Requirements

- NFR-1: The audit MUST run without starting API, worker, Docker, browsers, databases, object storage, or external providers.
- NFR-2: The audit MUST NOT print secret values when a secret-like match is found; file path and line number are sufficient.
- NFR-3: The evidence log SHOULD remain short enough for company developers to review during handoff.
- NFR-4: The evidence log MUST use stable target ids so future scripts and handoff docs can reference it.

## Acceptance Criteria

- AC-1: Given a checkout, when `scripts/smoke/real-model-evidence-log-audit.sh` runs, then it verifies the evidence log exists and names all six targets. Covers FR-1 and FR-2.
- AC-2: Given the evidence log, when the audit scans it, then it finds DreamMaker production-target language and Yunwu/WellAPI public-network smoke language. Covers FR-3 and FR-4.
- AC-3: Given the evidence log, when the audit scans it, then it finds the sanitized-only policy and rejects obvious secret-like patterns. Covers FR-6, FR-8, NFR-1, and NFR-2.
- AC-4: Given the DreamMaker 403 attempt, when the evidence log is reviewed, then it contains only status, failure class, suspected environment cause, and next decision, without raw payload or credentials. Covers FR-7.
- AC-5: Given the runbooks and acceptance checklists, when the audit scans them, then they reference `docs/integrations/real-model-smoke-evidence-log.md`. Covers FR-9 and FR-10.

## Edge Cases

- EC-1: If a provider returns a signed URL as the only output, the evidence log MUST record `platform_object_imported=false` or `true` only, not the URL.
- EC-2: If a provider returns a task id required for support, the evidence log MUST record `<present>` and keep the full id outside the repository in the approved support channel.
- EC-3: If a smoke fails before work creation, the evidence log MUST use `work_id=N/A` and record the preflight or create-stage failure.
- EC-4: If a public-network smoke succeeds, the evidence log MUST still keep DreamMaker production-target status as pending until the DreamMaker target succeeds or is formally waived.

## API Contracts

N/A - this feature is a documentation and local shell audit gate.

## Data Models

| Field | Type | Constraint | Description |
|---|---|---|---|
| `target` | string | One of the six stable target ids | Real-model smoke target. |
| `provider_role` | string | `production-target` or `public-network-smoke` | Handoff meaning of the target. |
| `status` | string | `not_started`, `failed`, `succeeded`, `blocked_external` | Sanitized attempt status. |
| `trace_id` | string | `<present>`, `<empty>`, `N/A`, or masked | Provider task or trace presence only. |
| `evidence` | string | Sanitized summary only | What was verified without raw payloads. |

## Out of Scope

- OS-1: This spec does not call real DeepSeek, DreamMaker, Yunwu, WellAPI, Suno, MiniMax, Image 2, or company systems.
- OS-2: This spec does not replace provider runbooks, acceptance checklists, or company handoff review.
- OS-3: This spec does not decide whether production may temporarily use a non-DreamMaker provider; it only preserves the current DreamMaker production-target decision.
