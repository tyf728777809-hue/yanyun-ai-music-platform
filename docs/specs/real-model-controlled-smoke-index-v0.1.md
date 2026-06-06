# Real Model Controlled Smoke Index v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The project now has separate controlled smoke scripts and runbooks for Yunwu Suno, DreamMaker Suno/MiniMax, WellAPI Image 2, DeepSeek, and DreamMaker Image 2. This is safer than a single always-on integration path, but it is easy for an operator to choose the wrong script, skip preflight, or misread a public-network smoke backend as the production target.

DreamMaker remains the mandatory production target for music and Image 2. Yunwu and WellAPI are current public-network smoke paths only. A central smoke index is needed so future local testing, user handoff, and company handoff all start from the same target matrix and safety gates.

## Functional Requirements

- FR-1: The smoke index MUST provide one command entry point for listing, planning, preflight, and eligible execution of real-model controlled smokes.
- FR-2: The default mode MUST NOT call real model providers, company systems, databases, object storage, work APIs, or user-facing APIs.
- FR-3: The index MUST support these targets: `yunwu-suno`, `dreammaker-suno`, `dreammaker-minimax`, `deepseek`, `wellapi-image2`, and `dreammaker-image2`.
- FR-4: The index MUST clearly label DreamMaker music and DreamMaker Image 2 as production-target paths.
- FR-5: The index MUST clearly label Yunwu and WellAPI as public-network smoke paths, not production replacements for DreamMaker.
- FR-6: `MODE=preflight` MUST delegate to `scripts/smoke/real-model-readiness-preflight.sh` with the matching `TARGET` and `STRICT=true`.
- FR-7: `MODE=execute` MUST require a global explicit gate, `ALLOW_REAL_MODEL_SMOKE=1`, before delegating to any script that may call a real provider.
- FR-8: `MODE=execute` MUST also rely on the target script's existing provider-specific `ALLOW_*` gate, so the index cannot bypass downstream safety controls.
- FR-9: Targets without a dedicated safe execution script MAY refuse `MODE=execute` and print the relevant runbook path instead.
- FR-10: The index MUST NOT print credentials, token values, JWTs, provider raw payloads, or full supplier media URLs.
- FR-11: `MODE=execute` MUST run the strict read-only preflight for the selected target before delegating to any real smoke script.

## Non-Functional Requirements

- NFR-1: The command MUST be shell-only and require no new dependency.
- NFR-2: Default output MUST fit in a terminal handoff and be readable by a non-specialist operator.
- NFR-3: Failure modes MUST be explicit and non-zero when an unsupported target, unsupported mode, or missing allow gate is encountered.

## Acceptance Criteria

- AC-1: Given no environment variables, when the command runs, then it lists supported targets and does not call any provider. Covers FR-1 and FR-2.
- AC-2: Given `TARGET=dreammaker-suno MODE=plan`, when the command runs, then it labels DreamMaker as production-target and prints the DreamMaker runbook/script path. Covers FR-3 and FR-4.
- AC-3: Given `TARGET=yunwu-suno MODE=preflight`, when the command runs, then it delegates to the readiness preflight for `yunwu-suno` with strict checks and does not call Yunwu directly. Covers FR-5 and FR-6.
- AC-4: Given `TARGET=yunwu-suno MODE=execute` without `ALLOW_REAL_MODEL_SMOKE=1`, when the command runs, then it exits non-zero before invoking preflight or the provider smoke script. Covers FR-7.
- AC-5: Given `TARGET=deepseek MODE=execute` without `ALLOW_REAL_MODEL_SMOKE=1`, when the command runs, then it exits non-zero before invoking preflight or the DeepSeek smoke script. Covers FR-7.
- AC-6: Given any mode, when output is scanned for likely `sk-...` or long Bearer token patterns, then no such value is emitted by the index itself. Covers FR-10.
- AC-7: Given `TARGET=wellapi-image2 MODE=execute` with global allow gate but missing Image 2 readiness variables, when the command runs, then strict preflight fails before the WellAPI smoke script is invoked. Covers FR-11.
- AC-8: Given `TARGET=deepseek MODE=execute` with global allow gate but missing `ALLOW_DEEPSEEK_REAL_SMOKE=1`, when strict preflight passes, then the delegated DeepSeek script still refuses before creating a work. Covers FR-8 and FR-11.

## Edge Cases

- EC-1: Unknown target MUST fail with supported target names.
- EC-2: Unknown mode MUST fail with supported mode names.
- EC-3: `MODE=execute` for DreamMaker music MUST set `REAL_PROVIDER=suno|minimax` only for the delegated command scope.
- EC-4: `MODE=execute` for WellAPI Image 2 MUST not open music, DeepSeek, render-worker, or company Adapter real paths by itself.
- EC-5: `dreammaker-image2` MUST delegate to its dedicated stack smoke and still require `ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE=1`.

## API Contracts

N/A - this feature is a local shell orchestration entry point and does not add HTTP APIs.

## Data Models

| Field | Type | Constraint | Description |
|---|---|---|---|
| `TARGET` | string | Optional; one supported target | Smoke target to plan, preflight, or execute. |
| `MODE` | string | Optional; `list`, `plan`, `preflight`, `execute` | Operation mode. Defaults to `list` when target is omitted, otherwise `plan`. |
| `ALLOW_REAL_MODEL_SMOKE` | string | Required as `1` only for `MODE=execute` | Global explicit real-call gate. |
| `ALLOW_DEEPSEEK_REAL_SMOKE` | string | Required as `1` only for DeepSeek delegated execution | Target-specific explicit real-call gate for `scripts/smoke/deepseek-real-lyrics-smoke.sh`. |
| `ALLOW_DREAMMAKER_IMAGE2_REAL_SMOKE` | string | Required as `1` only for DreamMaker Image 2 delegated execution | Target-specific explicit real-call gate for `scripts/smoke/dreammaker-image2-real-cover-stack-smoke.sh`. |

## Out of Scope

- OS-1: This does not run all real providers as a suite.
- OS-2: This does not replace per-provider runbooks or checklists.
- OS-3: This does not run DreamMaker Image 2 unless the operator explicitly uses `MODE=execute` with both global and target allow gates.
- OS-4: This does not store credentials, read `.env`, or prompt for secrets.
- OS-5: This does not call real providers unless the operator explicitly uses `MODE=execute` with all required allow gates.
