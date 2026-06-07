# Local Delivery Evidence Audit v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The local commercial delivery baseline now spans backend smoke scripts, frontend smoke evidence, render-worker MP4 evidence, real-model controlled smoke gates, company Adapter handoff documents, and project progress records. The manual checklist remains the final delivery gate, but a non-service audit is needed before handoff and before long goal continuation so obvious evidence drift is caught quickly.

This audit is intentionally narrower than the full smoke suite. It proves that the repository still contains the expected delivery evidence, guard scripts, and safety language. It does not prove runtime API behavior, frontend visual quality, real model success, or company Adapter replacement.

## Functional Requirements

- FR-1: The audit MUST run without starting services, Docker, browsers, workers, API processes, databases, object storage, or external providers.
- FR-2: The audit MUST verify key delivery documents exist.
- FR-3: The audit MUST verify key smoke scripts exist and are executable, including the backend acceptance stack smoke and full acceptance stack smoke.
- FR-4: The audit MUST verify the project still states DreamMaker music and DreamMaker Image 2 are retained production-target paths.
- FR-5: The audit MUST verify Yunwu and WellAPI are described as public-network controlled smoke paths, not replacements for DreamMaker.
- FR-6: The audit MUST verify the current status handoff document still distinguishes local-ready, smoke-prepared, handoff-prepared, external-blocked, and decision-required states.
- FR-7: The audit MUST verify the real-model controlled smoke index can print a target matrix and a DreamMaker plan without requiring credentials.
- FR-8: The audit MUST scan tracked files for obvious committed secret patterns, including `sk-...`, long Bearer tokens, Chinese `AccessKey` labels, Chinese `SecretKey` labels, private key headers, and long JWT-like values.
- FR-9: The audit MUST scan tracked files for unexpectedly large committed artifacts.
- FR-10: The audit MUST support strict git cleanliness through `STRICT_GIT_CLEAN=true`, while default mode MAY warn instead of failing on local modifications.
- FR-11: The audit MUST print a concise PASS/FAIL summary and exit non-zero when required checks fail.
- FR-12: The audit MUST include the production provider defaults audit, proving production profile and fallback defaults still point to DreamMaker.
- FR-13: The audit MUST include the company deployment readiness audit, proving deployment assets and handoff references remain present.
- FR-14: The audit MUST include the real-model evidence log audit, proving sanitized evidence logging exists and keeps DreamMaker as the production target while Yunwu and WellAPI remain public-network smoke paths.
- FR-15: The audit MUST include the stepwise production boundary audit, proving `stepwise-recording` is not treated as a production/user-test path and `stepwise-production` is not overstated before implementation.
- FR-16: The audit MUST verify the current long-goal completion audit exists and preserves the DreamMaker production-target rule.
- FR-17: The audit MUST verify the public real full experience smoke spec and executable exist, require the public full experience allow gate, and preserve the DreamMaker production-target rule while using Yunwu / WellAPI only for public-network validation.

## Non-Functional Requirements

- NFR-1: The audit MUST be a shell script with no new dependencies beyond common local tools already used by the project: `bash`, `git`, `rg`, `wc`, `awk`, `sort`, and `tail`.
- NFR-2: The audit MUST NOT print secret values when reporting a secret-like match; file path and line number are sufficient.
- NFR-3: The audit SHOULD complete in under 5 seconds on a normal local checkout.

## Acceptance Criteria

- AC-1: Given a normal checkout, when `scripts/smoke/local-delivery-evidence-audit.sh` runs, then it checks documents, executable scripts, backend/full acceptance stack evidence, production provider defaults evidence, real-model evidence log evidence, deployment readiness evidence, stepwise production boundary evidence, current goal completion evidence, public full experience smoke evidence, DreamMaker retention text, status labels, smoke index output, secret patterns, and large tracked files without starting services. Covers FR-1 through FR-9 and FR-12 through FR-17.
- AC-2: Given local uncommitted changes and default mode, when the audit runs, then it warns about git status but can pass remaining checks. Covers FR-10.
- AC-3: Given local uncommitted changes and `STRICT_GIT_CLEAN=true`, when the audit runs, then it exits non-zero. Covers FR-10 and FR-11.
- AC-4: Given the real-model controlled smoke index is removed or loses DreamMaker plan output, when the audit runs, then it exits non-zero. Covers FR-7 and FR-11.
- AC-5: Given a tracked file contains a likely real API key pattern, when the audit runs, then it exits non-zero without printing the secret value. Covers FR-8 and NFR-2.

## Edge Cases

- EC-1: If `rg` is unavailable, the audit MUST fail with an explicit tool-missing error.
- EC-2: If a tracked file is deleted in the worktree, the audit MUST report it through git status and skip size scanning for that missing path.
- EC-3: If the worktree contains untracked files, default mode MUST warn; strict mode MUST fail.
- EC-4: Large tracked files MAY be allowed only by increasing `MAX_TRACKED_FILE_BYTES` explicitly for the command invocation.

## API Contracts

N/A - this feature is a local shell audit and does not add HTTP APIs.

## Data Models

| Field | Type | Constraint | Description |
|---|---|---|---|
| `STRICT_GIT_CLEAN` | string | Optional; `true` to fail on dirty worktree | Controls git status strictness. |
| `MAX_TRACKED_FILE_BYTES` | integer | Optional; default `20000000` | Maximum allowed tracked file size. |

## Out of Scope

- OS-1: This audit does not run Gradle, Node, Playwright, Docker, API smoke, frontend smoke, render-worker smoke, public full experience smoke, or real model smoke.
- OS-2: This audit does not prove true Suno/MiniMax/DeepSeek/Image 2 success.
- OS-3: This audit does not prove company Adapter replacement.
- OS-4: This audit does not replace the manual local commercial delivery checklist.
- OS-5: This audit does not validate real provider task ids or raw provider payloads; those are intentionally excluded from repository evidence.
- OS-6: This audit does not implement `stepwise-production`; it only prevents overclaiming before the dedicated production step activities and smoke exist.
