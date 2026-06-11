# Production DreamMaker Provider Defaults v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The project currently supports public-network smoke backends for environments outside the company intranet: Yunwu for Suno and WellAPI for Image 2. These paths help local real-provider testing continue when DreamMaker is not reachable or returns permission errors outside the company environment.

The production target has not changed. DreamMaker music and DreamMaker Image 2 must remain the production provider paths. A future company deployment should be able to switch to DreamMaker by configuration and should not inherit public-network smoke defaults by accident.

## Functional Requirements

- FR-1: API and worker production profiles MUST default `SUNO_BACKEND` to `dreammaker`.
- FR-2: API and worker production profiles MUST default `IMAGE2_BACKEND` to `dreammaker`.
- FR-3: Java configuration fallbacks MUST prefer DreamMaker when the backend property is absent.
- FR-4: Readiness default properties MUST prefer DreamMaker when no explicit backend is bound.
- FR-5: The repository MUST provide a production environment example that sets `SPRING_PROFILES_ACTIVE=prod`, `SUNO_BACKEND=dreammaker`, and `IMAGE2_BACKEND=dreammaker`.
- FR-6: Documentation MUST distinguish local public-network smoke defaults from production DreamMaker defaults.
- FR-7: The audit script MUST verify the production defaults without starting services or calling external providers.
- FR-8: The production environment example and company deployment handoff MUST explicitly set `TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy` until `stepwise-production` has dedicated production activities and smoke evidence.
- FR-9: The production environment example MUST NOT contain dangerous local defaults such as `localhost`, `127.0.0.1`, `default`, `song-production-local`, `=mock`, or `=local`.

## Non-Functional Requirements

- NFR-1: The audit MUST be shell-only and require no new dependencies beyond existing repository tooling.
- NFR-2: The audit MUST NOT print or require real API keys, tokens, cookies, or production secrets.
- NFR-3: Local `.env.example` MAY continue to prefer Yunwu/WellAPI for current public-network smoke convenience, as long as production profile and production example prefer DreamMaker.

## Acceptance Criteria

- AC-1: Given the repository checkout, when `scripts/smoke/production-provider-defaults-audit.sh` runs, then it confirms API and worker `prod | production` profile blocks default both music and Image 2 to DreamMaker. Covers FR-1 and FR-2.
- AC-2: Given the Java sources, when the audit scans provider configuration and readiness defaults, then it finds DreamMaker fallback defaults. Covers FR-3 and FR-4.
- AC-3: Given the production env example, when the audit scans it, then it finds `SPRING_PROFILES_ACTIVE=prod`, `SUNO_BACKEND=dreammaker`, `IMAGE2_BACKEND=dreammaker`, `TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy`, empty DreamMaker credential placeholders only, and no dangerous local/mock defaults. Covers FR-5, FR-8, FR-9, and NFR-2.
- AC-4: Given the handoff docs and acceptance checklist, when the audit scans them, then it finds the production DreamMaker default rule, public-network smoke distinction, and production Temporal workflow mode boundary. Covers FR-6 and FR-8.
- AC-5: Given the audit runs, when any required production default is missing or changed back to a public-network smoke backend, then it exits non-zero. Covers FR-7.

## Edge Cases

- EC-1: If production temporarily uses Yunwu or WellAPI, the exception MUST be documented as temporary and MUST NOT remove DreamMaker code, runbooks, or smoke targets.
- EC-2: If the final deployment profile name differs from `prod` or `production`, the production env example and audit MUST be updated before handoff.
- EC-3: If company deployment uses a secret manager instead of env files, `deploy/env.production.example` remains a variable-name reference and MUST NOT contain real values.
- EC-4: If a required production value must be intentionally blank until deployment, the production example should leave it empty instead of using localhost/default/mock placeholders.

## API Contracts

N/A - this is a configuration and audit requirement, not a user-facing HTTP API.

## Data Models

| Field | Type | Constraint | Description |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | env string | MUST include `prod` for this example | Activates production provider defaults. |
| `SUNO_BACKEND` | enum | `dreammaker` in production example | Selects DreamMaker Suno backend. |
| `IMAGE2_BACKEND` | enum | `dreammaker` in production example | Selects DreamMaker Image 2 backend. |
| `TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE` | enum | `legacy` until `stepwise-production` is implemented | Prevents production handoff from inheriting `stepwise-recording`. |
| `DREAMMAKER_*` | env names | Values empty in repo | Production credentials injected by company secret/config system. |

## Out of Scope

- OS-1: This spec does not execute real DreamMaker calls.
- OS-2: This spec does not remove Yunwu or WellAPI smoke paths.
- OS-3: This spec does not decide the final company secret manager or deployment platform.
