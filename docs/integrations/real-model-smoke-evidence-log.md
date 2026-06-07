# Real Model Smoke Evidence Log

Sanitized only. Do not paste real API keys, AccessKey or SecretKey values, Bearer/JWT values, cookies, user tokens, raw provider payloads, full prompts, complete lyrics, generated media URLs, signed URLs, full provider task ids, screenshots with credentials, or private user text.

DreamMaker music and DreamMaker Image 2 remain the production-target paths. Yunwu Suno and WellAPI Image 2 are public-network controlled smoke paths for the current non-company-intranet environment only; their success does not replace DreamMaker production validation.

## Target Registry

| Target | Provider role | Current status | Evidence entry |
|---|---|---|---|
| `dreammaker-suno` | production-target | `blocked_external` | 2026-06-07 HTTP 403 sanitized sample below. |
| `dreammaker-minimax` | production-target | `not_started` | No real successful sample yet. |
| `yunwu-suno` | public-network-smoke | `not_started` | Public-network smoke prepared; no real successful sample yet. |
| `deepseek` | model-smoke | `not_started` | Single-sample lyrics smoke prepared; no real successful sample yet. |
| `wellapi-image2` | public-network-smoke | `not_started` | Public-network cover smoke prepared; no real successful sample yet. |
| `dreammaker-image2` | production-target | `not_started` | Production-target cover smoke prepared; no real successful sample yet. |
| `public-real-full-experience` | public-network-full-experience | `prepared` | Combined DeepSeek + Yunwu Suno + WellAPI Image 2 + local-process MP4 smoke prepared; no successful sample yet. |

## Evidence Entries

| Date | Target | Backend / Model | Environment | Work ID | Job ID | Provider Trace | Result | Failure Code | Retryable | Object Import | Package URL | Audit Evidence | Cost / Rate Notes | Next Decision |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-06-07 | `dreammaker-suno` | DreamMaker Suno | Local public network, not company intranet | `<recorded-in-local-progress>` | `<present>` | `<empty>` | Failed at provider create task with HTTP 403 | `PROVIDER_AUTH_FAILED` / provider permission | No user retry | N/A | No | `provider_calls` failed row present; no raw payload stored in this log | No confirmed billing sample | Re-test DreamMaker from company intranet or with provider-approved account permissions; keep DreamMaker as production target. |
| TBD | `dreammaker-minimax` | DreamMaker MiniMax | TBD | N/A | N/A | N/A | Not started | N/A | N/A | N/A | N/A | Run controlled smoke only through `TARGET=dreammaker-minimax` | N/A | Await company intranet / credentials window. |
| TBD | `yunwu-suno` | Yunwu Suno | Public network | N/A | N/A | N/A | Not started | N/A | N/A | N/A | N/A | Run controlled smoke only through `TARGET=yunwu-suno` | N/A | Optional public-network music smoke; does not replace DreamMaker. |
| TBD | `deepseek` | DeepSeek v4Pro | Public network | N/A | N/A | N/A | Not started | N/A | N/A | N/A | N/A | Run controlled smoke only through `TARGET=deepseek` | N/A | Run one lyrics-only sample when user provides a safe window. |
| TBD | `wellapi-image2` | WellAPI gpt-image-2 | Public network | N/A | N/A | N/A | Not started | N/A | N/A | N/A | N/A | Run controlled smoke only through `TARGET=wellapi-image2` | N/A | Optional public-network cover smoke; does not replace DreamMaker Image 2. |
| TBD | `dreammaker-image2` | DreamMaker Image 2 | TBD | N/A | N/A | N/A | Not started | N/A | N/A | N/A | N/A | Run controlled smoke only through `TARGET=dreammaker-image2` | N/A | Await company intranet / credentials window. |
| 2026-06-07 | `public-real-full-experience` | DeepSeek v4Pro + Yunwu Suno + WellAPI gpt-image-2 + local-process MP4 | Local public network | N/A | N/A | N/A | Blocked before service startup because current shell credentials were missing | N/A | N/A | N/A | N/A | Gate checks passed; strict single-target preflights reported missing env / disabled real-call switches; no provider call made | No cost | Export credentials in current shell or run script interactively, then re-run with both allow gates. |
| TBD | `public-real-full-experience` | DeepSeek v4Pro + Yunwu Suno + WellAPI gpt-image-2 + local-process MP4 | Public network | N/A | N/A | N/A | Prepared, not executed | N/A | N/A | N/A | N/A | Run only through `ALLOW_REAL_MODEL_SMOKE=1 ALLOW_PUBLIC_REAL_FULL_EXPERIENCE=1 scripts/smoke/public-real-full-experience-stack.sh` | N/A | Use only after single-target plan/preflight; does not replace DreamMaker production validation. |

## Entry Template

Use this template after each real smoke attempt. Keep evidence concise and sanitized.

| Field | Value |
|---|---|
| Date / time |  |
| Target | `dreammaker-suno` / `dreammaker-minimax` / `yunwu-suno` / `deepseek` / `wellapi-image2` / `dreammaker-image2` / `public-real-full-experience` |
| Provider role | `production-target` / `public-network-smoke` / `model-smoke` |
| Backend / model |  |
| Operator |  |
| Environment | local public network / company intranet / staging / production-like |
| `work_id` |  |
| `job_id` |  |
| Provider trace / task id | `<present>` / `<empty>` / N/A |
| Final status |  |
| Failure code |  |
| Recommended action |  |
| Retryable | yes / no / unknown |
| Duration |  |
| Platform object import | yes / no / N/A |
| Package URL available | yes / no / N/A |
| `agent_runs` / `provider_calls` evidence | sanitized summary only |
| Cost / rate-limit notes |  |
| Next decision |  |

## Forbidden Evidence

- Real API keys, AccessKey or SecretKey values, Bearer/JWT values, cookies, user tokens, private keys, or production secrets.
- Raw provider request/response bodies, raw model messages, full prompts, complete lyrics, private user text, or full generated outputs.
- Supplier media URLs, signed platform URLs, full provider task ids, full provider trace ids, and screenshots that include credentials or private data.
- Claims that Yunwu or WellAPI success means DreamMaker production validation is complete.
