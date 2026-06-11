# Real Model Safety Gates Audit v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The project now has controlled smoke entries for Yunwu Suno, DreamMaker Suno/MiniMax, DeepSeek, WellAPI Image 2, DreamMaker Image 2, and the combined public-network full-experience smoke. Each real provider path has a global `ALLOW_REAL_MODEL_SMOKE=1` gate and a target-specific `ALLOW_*` gate. The requirement applies to both the unified index and any low-level direct/stack smoke script.

Manual review is no longer enough. The delivery handoff needs a non-real-call audit that proves the safety matrix still behaves as designed: list/plan modes are safe, execute mode refuses without the global gate, and even with the global gate, each target still refuses without its target-specific gate. This audit protects against future edits that accidentally bypass one of the real-cost or credential-bearing controls.

## Functional Requirements

- FR-1: The audit MUST run without starting API, worker, Docker Compose, browsers, databases, object storage, or external providers.
- FR-2: The audit MUST verify the real-model controlled smoke index list mode succeeds and labels DreamMaker music/Image 2 as production-target paths.
- FR-3: The audit MUST verify `MODE=plan` succeeds for all supported targets: `yunwu-suno`, `dreammaker-suno`, `dreammaker-minimax`, `deepseek`, `wellapi-image2`, `dreammaker-image2`, and `public-real-full-experience`.
- FR-4: The audit MUST verify `MODE=execute` without `ALLOW_REAL_MODEL_SMOKE=1` fails for every supported target before delegated script execution.
- FR-5: The audit MUST verify `MODE=execute` with `ALLOW_REAL_MODEL_SMOKE=1` but without the target-specific `ALLOW_*` gate still fails for every supported target.
- FR-6: The audit MUST statically verify every real-provider direct/stack smoke script contains both `ALLOW_REAL_MODEL_SMOKE` and its target-specific `ALLOW_*` gate before any external call path.
- FR-7: The audit MUST fail if real-provider smoke scripts print full create/detail/provider responses instead of allowlisted summaries.
- FR-8: The audit MUST provide only fake placeholder environment values for strict preflight and MUST NOT use real credentials.
- FR-9: The audit MUST NOT print fake credential values, real credential values, provider raw payloads, supplier URLs, full prompts, or JWTs.
- FR-10: The audit MUST scan command outputs for obvious secret-like patterns and fail if any are present.
- FR-11: The audit MUST print a concise PASS/FAIL summary and exit non-zero on any failed gate.

## Non-Functional Requirements

- NFR-1: The audit MUST be a shell script requiring no new project dependencies beyond `bash` and `rg`.
- NFR-2: The audit SHOULD complete quickly because it does not start services or wait for provider/network activity.
- NFR-3: The audit MUST be safe to run in normal local development and company handoff checks.

## Acceptance Criteria

- AC-1: Given a normal checkout, when `scripts/smoke/real-model-safety-gates-audit.sh` runs, then list and plan modes pass without external calls. Covers FR-1 through FR-3.
- AC-2: Given each supported target, when execute mode is run without `ALLOW_REAL_MODEL_SMOKE=1`, then it exits non-zero and mentions the global allow gate. Covers FR-4.
- AC-3: Given each supported target and fake preflight-ready variables, when execute mode is run with only `ALLOW_REAL_MODEL_SMOKE=1`, then strict preflight passes and the delegated target script exits non-zero on its own missing `ALLOW_*` gate. Covers FR-5 and FR-8.
- AC-4: Given all real-provider direct/stack smoke scripts, when the audit performs static checks, then each script contains both the global and target gate and no raw create/detail response print. Covers FR-6 and FR-7.
- AC-5: Given all audit command output, when scanned for likely real secrets, then no obvious secret-like pattern is present. Covers FR-9 and FR-10.
- AC-6: Given a future change removes a target from plan mode or bypasses a target allow gate, when the audit runs, then it exits non-zero. Covers FR-11.

## Edge Cases

- EC-1: If `rg` is missing, the audit MUST fail with an explicit tool-missing message.
- EC-2: If a delegated script reorders checks and requires secrets before its target allow gate, the audit MUST fail because that is a regression in safety ergonomics.
- EC-3: If a target is renamed in the real-model controlled smoke index, this audit MUST be updated in the same change.
- EC-4: The `public-real-full-experience` target intentionally uses fake placeholder values for three provider credentials during target-gate testing and still MUST fail on missing `ALLOW_PUBLIC_REAL_FULL_EXPERIENCE=1` before service startup.

## API Contracts

N/A - this feature is a local shell audit and does not add HTTP APIs.

## Data Models

N/A - this feature does not add or inspect database tables.

## Out of Scope

- OS-1: This audit does not prove real provider success.
- OS-2: This audit does not call DeepSeek, DreamMaker, Yunwu, WellAPI, Suno, MiniMax, Image 2, or company systems.
- OS-3: This audit does not replace provider-specific smoke scripts or runbooks.
- OS-4: This audit does not start local infrastructure or application processes.
