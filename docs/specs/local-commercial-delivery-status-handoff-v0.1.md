# Local Commercial Delivery Status Handoff v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The project now has many execution artifacts: local Mock end-to-end smoke, frontend real-backend smoke, render-worker MP4 smoke, OpenAPI contract smoke, real-provider guard smoke, Yunwu / WellAPI controlled smoke scripts, and company Adapter readiness smoke.

The acceptance checklist remains the full gate, while dated audit records capture evidence from individual rounds. A company developer or product owner still needs one stable handoff page that separates verified local readiness from controlled real-model preparation, external company integration, and deployment decisions.

This status handoff prevents overclaiming. In particular, local Mock success MUST NOT be described as real supplier success, a real-backend frontend smoke MUST NOT be described as real AI model success, and Yunwu / WellAPI public-network smoke paths MUST NOT replace the retained DreamMaker production target.

## Functional Requirements

- FR-1: The handoff MUST present a concise status legend that distinguishes local verified, prepared for controlled smoke, prepared for handoff, external blocker, and decision-required items.
- FR-2: The handoff MUST list the current state of local backend, frontend, MP4 rendering, OpenAPI contract, company Adapter boundary, and real-model integration.
- FR-3: The handoff MUST identify evidence files or commands for every local verified or prepared item.
- FR-4: The handoff MUST explicitly state that company account, moderation, quota, publish, and share are not implemented as real company systems in this repo.
- FR-5: The handoff MUST explicitly state that DreamMaker remains the production target, while Yunwu / WellAPI are current public-network controlled smoke paths.
- FR-6: The handoff MUST list the next recommended execution order toward user real AI music testing and company handoff.
- FR-7: The handoff MUST NOT include real credentials, bearer tokens, signed URLs, supplier raw payloads, or private company API responses.

## Non-Functional Requirements

- NFR-1: The handoff MUST be readable as a standalone Markdown document in under five minutes.
- NFR-2: The handoff MUST avoid checkboxes that imply final completion; final gates remain in the acceptance checklists.
- NFR-3: The handoff MUST use stable file links and command names rather than copying volatile command output.
- NFR-4: The handoff MUST be safe to share with company developers after internal review.

## Acceptance Criteria

- AC-1: Given a company developer reads the handoff, when they ask what can be locally verified now, then the handoff points to Mock backend, frontend, OpenAPI, MP4, and readiness smoke evidence. Covers FR-1 through FR-3.
- AC-2: Given a company developer asks whether real company systems are implemented, when they read the handoff, then the answer is clearly no and each company-owned boundary is listed. Covers FR-4.
- AC-3: Given a developer sees Yunwu / WellAPI scripts, when they read the handoff, then they understand these are public-network smoke paths and DreamMaker must remain for production. Covers FR-5.
- AC-4: Given the user wants to test real AI music next, when they read the handoff, then they see a safe recommended order that starts with single-provider controlled smoke and keeps other systems Mock unless explicitly enabled. Covers FR-6.
- AC-5: Given the handoff is scanned for secrets, when reviewing it, then no real key/token/signed URL or supplier raw payload is present. Covers FR-7 and NFR-4.

## Edge Cases

- EC-1: If a smoke script exists but has not been run with real credentials, the status MUST be "prepared", not "verified".
- EC-2: If a dated audit contains older evidence, the handoff MUST reference it as historical evidence and avoid implying it was just rerun.
- EC-3: If the frontend prototype is testable but not in `apps/web`, the handoff MUST mark frontend final ownership as a decision-required item.
- EC-4: If company share is fully owned by the company community system, the handoff MUST identify it as external/company-owned rather than requiring this platform to implement share behavior.

## API Contracts

N/A - documentation-only handoff. It references existing APIs and smoke scripts but adds no new API.

## Data Models

| Field | Type | Constraint | Description |
|---|---|---|---|
| item | string | required | Delivery or integration item being classified. |
| status | enum | required | `READY_LOCAL`, `PREPARED_SMOKE`, `PREPARED_HANDOFF`, `BLOCKED_EXTERNAL`, `DECISION_REQUIRED`, `NOT_STARTED`. |
| owner | string | required | Responsible party or boundary owner. |
| evidence | string | required when available | File, command, or checklist proving the status. |
| overclaim_guard | string | required | What must not be overstated. |

## Out of Scope

- OS-1: This handoff MUST NOT replace `docs/checklists/local-commercial-delivery-acceptance.md`.
- OS-2: This handoff MUST NOT execute smoke scripts or record volatile runtime output.
- OS-3: This handoff MUST NOT make production deployment decisions for render-worker, frontend ownership, or company Adapter replacement.
- OS-4: This handoff MUST NOT store or request real model or company credentials.
