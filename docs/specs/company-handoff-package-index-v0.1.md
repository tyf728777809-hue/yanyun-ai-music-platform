# Company Handoff Package Index v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The project now has enough local evidence for a commercial-grade mock loop and enough controlled gates for real-model smoke preparation. Company developers still need a single starting document that tells them what to read first, what can be verified locally, what must be replaced by company systems, and which paths must not be removed when public-network smoke providers are used.

Without a handoff package index, the company side may confuse local mock readiness with production readiness, treat `mark-fetched` as community publish success, trust `X-Mock-User-Id`, or accidentally weaken the DreamMaker production-target path while using Yunwu or WellAPI for public-network testing.

## Functional Requirements

- FR-1: The handoff package MUST provide one company-facing starting document under `docs/handover`.
- FR-2: The handoff package MUST list the first-read documents and local verification commands, including the backend acceptance stack smoke.
- FR-3: The handoff package MUST distinguish local-ready, smoke-prepared, handoff-prepared, blocked-external, and decision-required items.
- FR-4: The handoff package MUST state that the platform does not implement real company account, moderation, quota, publish, share, interaction, or recommendation systems.
- FR-5: The handoff package MUST state that company production must not trust `X-Mock-User-Id`.
- FR-6: The handoff package MUST state that `mark-fetched` means delivery to the company publish flow, not community publish success.
- FR-7: The handoff package MUST state that DreamMaker music and DreamMaker Image 2 remain production-target paths.
- FR-8: The handoff package MUST state that Yunwu and WellAPI are current public-network smoke paths only.
- FR-9: The handoff package MUST point company developers to Adapter replacement evidence and readiness smoke.
- FR-10: The handoff package MUST point operators to the real-model controlled smoke index before any real model call.
- FR-11: The audit script MUST run without starting services or calling external systems.
- FR-12: The audit script MUST verify required documents, commands, and safety phrases are present.

## Non-Functional Requirements

- NFR-1: The handoff document MUST be concise enough for a company developer to read before implementation starts.
- NFR-2: The audit script MUST be shell-only and require no new project dependencies.
- NFR-3: The audit script MUST NOT print secrets or require credentials.

## Acceptance Criteria

- AC-1: Given the repository checkout, when the company handoff audit runs, then it verifies the package document, Adapter handoff, replacement checklist, local delivery status, acceptance checklist, OpenAPI, frontend ADR, local runbook, and key smoke scripts exist. Covers FR-1, FR-2, and FR-12.
- AC-2: Given the package document, when the audit scans it, then it finds local-ready, smoke-prepared, handoff-prepared, blocked-external, and decision-required status terms. Covers FR-3.
- AC-3: Given the package document, when the audit scans it, then it finds explicit company-system boundary text, `X-Mock-User-Id` warning, `mark-fetched` semantics, DreamMaker production-target rule, and Yunwu/WellAPI public-network smoke labels. Covers FR-4 through FR-8.
- AC-4: Given the package document, when the audit scans it, then it references `scripts/smoke/company-adapter-readiness-smoke.sh`, `scripts/smoke/local-commercial-backend-acceptance-stack.sh`, `scripts/smoke/local-delivery-evidence-audit.sh`, and `scripts/smoke/real-model-controlled-smoke.sh`. Covers FR-9 and FR-10.
- AC-5: Given the audit runs, when required evidence is missing, then it exits non-zero. Covers FR-11 and FR-12.

## Edge Cases

- EC-1: If the frontend prototype path changes, the package document MUST be updated with the new ADR or decision.
- EC-2: If company shares are fully owned by the existing community system, the package document MAY keep sharing as company-owned and not require a platform Share Adapter implementation.
- EC-3: If company deployment chooses an independent render worker service, the package document MUST still preserve the `VideoRenderService` boundary.
- EC-4: If production temporarily uses Yunwu or WellAPI, the package document MUST still require DreamMaker retention.

## API Contracts

N/A - this is a documentation and shell audit feature, not an HTTP API.

## Data Models

| Field | Type | Constraint | Description |
|---|---|---|---|
| `COMPANY_HANDOFF_PACKAGE` | path | Optional; defaults to `docs/handover/company-delivery-package-v0.1.md` | Handoff package document audited by the script. |

## Out of Scope

- OS-1: This package does not replace full runtime smoke tests.
- OS-2: This package does not implement company Adapter code.
- OS-3: This package does not run real model providers.
- OS-4: This package does not decide the final frontend migration strategy.
