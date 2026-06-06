# Company Deployment Readiness Audit v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The project is intended to run locally first and then be handed to company developers for real account, moderation, quota, publish, share, and server deployment integration. The repository already includes local infrastructure, application Dockerfiles, production provider defaults, readiness reports, and handoff documents.

Company deployment is still a decision-required area because the final server topology, render-worker deployment shape, secret manager, object storage, and company Adapter implementations must be chosen by the company side. Before that happens, the repository should still provide a static audit that proves the deployment assets and handoff documents are present and internally consistent.

This audit is not a production deployment. It is a non-service, non-network check that catches missing Dockerfiles, missing production environment samples, missing monitoring config, missing handoff references, and accidental secret-like content before the company handoff.

## Functional Requirements

- FR-1: The audit MUST run without starting Docker, Gradle, Node, API, worker, browsers, databases, object storage, or external providers.
- FR-2: The audit MUST verify local infrastructure compose assets include PostgreSQL, Redis, Temporal, MinIO, OpenSearch, Prometheus, and Grafana.
- FR-3: The audit MUST verify application Dockerfiles exist for `music-api`, `music-worker`, `render-worker`, and `web`.
- FR-4: The audit MUST verify API and worker Dockerfiles use Java 21, build boot jars, expose the correct ports, run as a non-root app user, and define health checks.
- FR-5: The audit MUST verify render-worker and web Dockerfiles include Node build steps, and web has an nginx runtime health check.
- FR-6: The audit MUST verify Prometheus has scrape jobs for `music-api` and `music-worker`.
- FR-7: The audit MUST verify `deploy/env.production.example` exists, keeps DreamMaker production defaults without real secrets, and pins `TEMPORAL_SONG_PRODUCTION_WORKFLOW_MODE=legacy` until `stepwise-production` is implemented.
- FR-8: The audit MUST verify company handoff docs reference the deployment audit, production env example, local infrastructure boundary, app Dockerfiles, stepwise production boundary audit, and the fact that final company deployment shape remains a decision-required item.
- FR-9: The audit MUST scan the deployment files and company handoff docs for obvious secret-like patterns.
- FR-10: The audit MUST verify company deployment handoff keeps DreamMaker as the production target while Yunwu/WellAPI remain public-network smoke paths.

## Non-Functional Requirements

- NFR-1: The audit MUST be shell-only and depend only on tools already used by the repository, mainly `bash`, `rg`, and `sort`.
- NFR-2: The audit MUST NOT print secret values; if a secret-like pattern is found, it may print only file path and line number.
- NFR-3: The audit SHOULD complete in under 5 seconds on a normal checkout.

## Acceptance Criteria

- AC-1: Given a clean repository checkout, when `scripts/smoke/company-deployment-readiness-audit.sh` runs, then it validates infrastructure compose services, Dockerfiles, Prometheus jobs, production env defaults, Temporal workflow-mode boundary, DreamMaker/Yunwu/WellAPI provider boundary, and handoff references without starting services. Covers FR-1 through FR-8 and FR-10.
- AC-2: Given any required deployment asset is missing, when the audit runs, then it exits non-zero. Covers FR-2 through FR-7.
- AC-3: Given the handoff docs stop referencing the deployment audit or production env sample, when the audit runs, then it exits non-zero. Covers FR-8.
- AC-4: Given a scanned deployment or handoff file contains an obvious secret-like pattern, when the audit runs, then it exits non-zero without printing the secret value. Covers FR-9 and NFR-2.

## Edge Cases

- EC-1: `deploy/docker-compose.yml` is currently a local infrastructure compose file, not a production app deployment compose; the audit MUST verify this distinction is documented instead of requiring app services in compose.
- EC-2: If company deployment chooses Kubernetes, VM systemd units, or a platform-specific release flow, the audit MAY remain as repository handoff evidence but the company deployment checklist must be extended.
- EC-3: If render-worker is deployed as an independent service instead of local process mode, the handoff docs MUST still preserve the `VideoRenderService` boundary.
- EC-4: If `stepwise-production` is implemented later, the production env example and handoff docs MAY change workflow mode only after the dedicated production activity and smoke evidence are added.

## API Contracts

N/A - this feature is a local shell audit and does not add HTTP APIs.

## Data Models

| Field | Type | Constraint | Description |
|---|---|---|---|
| `DEPLOYMENT_AUDIT_FILES` | shell array | Internal script list | Files scanned for required patterns and secret-like content. |

## Out of Scope

- OS-1: This audit does not build Docker images.
- OS-2: This audit does not start local infrastructure or application services.
- OS-3: This audit does not verify a real company server deployment.
- OS-4: This audit does not replace company security review, secret-manager configuration, rollback planning, or load testing.
