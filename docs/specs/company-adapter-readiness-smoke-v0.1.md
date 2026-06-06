# Company Adapter Readiness Smoke v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The project intentionally keeps company account, moderation, quota, publish, and share systems outside this platform. Local development uses Mock Adapters, while company developers will later replace those boundaries with real internal systems.

The repository already exposes `GET /internal/integration-readiness`, but there is no dedicated smoke script that turns the report into an operational gate for handoff. A company developer should be able to run one read-only command and see whether the report still shows expected local Mock boundaries, required deployment variables, and no leaked secrets.

This smoke does not prove that company adapters are implemented. It proves that the readiness report is present, structured, safe to share internally, and explicit about what blocks company deployment.

DreamMaker is a mandatory retained production target. Yunwu / WellAPI may be used for current public-network controlled smoke outside the company intranet, but the readiness smoke must keep checking the DreamMaker integration boundary so later refactors cannot remove the production switch path by accident.

## Functional Requirements

- FR-1: The smoke MUST call only `GET /health` and `GET /internal/integration-readiness`.
- FR-2: The smoke MUST verify the readiness response contains the expected company components: `company_account`, `company_moderation`, `company_quota`, `company_publish`, and `company_share`.
- FR-3: The smoke MUST verify default local company components are `MOCK_ONLY` and `blocks_company_deployment=true`.
- FR-4: The smoke MUST verify `company_share` is represented as `NotImplementedShareBoundary`, making the company-owned share boundary explicit.
- FR-5: The smoke MUST verify generation/deployment support components exist: `music_provider`, `render_worker`, `object_storage`, `workflow_dispatch`, `deepseek_guard`, `image2_guard`, `yunwu_suno_guard`, and `dreammaker_guard`.
- FR-6: The smoke MUST verify every component has non-empty `required_env_vars` and no required env var entry contains a value assignment.
- FR-7: The smoke MUST fail if the readiness response contains obvious secret-like values such as long `sk-...` keys or long Bearer tokens.
- FR-8: The smoke MUST print a compact component summary that is safe for handoff records.
- FR-9: The smoke MUST NOT call real company systems, real model providers, databases, object storage, or user-facing APIs.
- FR-10: The smoke MUST verify `dreammaker_guard` still uses `DreamMakerHttpClient` and exposes the DreamMaker production switch variable names: `DREAMMAKER_REAL_CALLS_ENABLED`, `DREAMMAKER_API_BASE_URL`, `DREAMMAKER_ACCESS_KEY`, and `DREAMMAKER_SECRET_KEY`.

## Non-Functional Requirements

- NFR-1: The script MUST pass `bash -n`.
- NFR-2: The script MUST complete within 10 seconds when API is healthy.
- NFR-3: The script MUST have no dependency on real company credentials or provider API keys.
- NFR-4: The script MUST be usable in local development and company deployment dry-run environments.

## Acceptance Criteria

- AC-1: Given API is not running, when the smoke runs, then it fails at health check without side effects. Covers FR-1 and FR-9.
- AC-2: Given default local API is running, when the smoke runs, then it confirms all five company components are `MOCK_ONLY` and block company deployment. Covers FR-2 through FR-4.
- AC-3: Given default local API is running, when the smoke runs, then it confirms all generation/deployment support components are present. Covers FR-5.
- AC-4: Given readiness contains a `required_env_vars` item with `=`, when the smoke runs, then it fails. Covers FR-6.
- AC-5: Given readiness contains an obvious API key or Bearer token, when the smoke runs, then it fails before printing the full response. Covers FR-7.
- AC-6: Given default local API is running, when the smoke runs, then it prints a compact component summary and exits zero. Covers FR-8.
- AC-7: Given default local API is running, when the smoke runs, then it fails if the DreamMaker guard, client implementation, or DreamMaker env var names are removed. Covers FR-10.

## Edge Cases

- EC-1: API returns non-JSON; the smoke MUST fail with a clear message.
- EC-2: API readiness schema changes; the smoke MUST fail rather than silently passing.
- EC-3: Company deployment sets a company adapter mode without real implementation; the readiness report may return `BLOCKED`, and this smoke should still expose the component summary for diagnosis.

## API Contracts

```typescript
type IntegrationReadinessReport = {
  environment: string
  service: "music-api"
  generated_at: string
  overall_status: "READY_FOR_LOCAL" | "BLOCKED_FOR_COMPANY_DEPLOYMENT"
  components: IntegrationComponentReadiness[]
  notes: string[]
}

type IntegrationComponentReadiness = {
  component: string
  configured_mode: string
  implementation: string
  status: "READY_FOR_LOCAL" | "MOCK_ONLY" | "BLOCKED"
  blocks_company_deployment: boolean
  required_env_vars: string[]
  handoff_note: string
}
```

## Data Models

N/A - no database reads or writes.

The smoke validates component names and fields from the existing readiness response:

| Component | Expected Default Status | Expected Deployment Block |
|---|---|---|
| `company_account` | `MOCK_ONLY` | `true` |
| `company_moderation` | `MOCK_ONLY` | `true` |
| `company_quota` | `MOCK_ONLY` | `true` |
| `company_publish` | `MOCK_ONLY` | `true` |
| `company_share` | `MOCK_ONLY` | `true` |
| `dreammaker_guard` | `MOCK_ONLY` when real calls disabled | `true` when real calls disabled |

## Out of Scope

- OS-1: The smoke MUST NOT prove real company adapter behavior.
- OS-2: The smoke MUST NOT call `/api/v1/me`, work creation, publish package, object storage, model providers, DreamMaker, Yunwu, WellAPI, or company systems.
- OS-3: The smoke MUST NOT validate frontend behavior.
- OS-4: The smoke MUST NOT decide whether `company_share` is accepted as a production exemption; that requires explicit company confirmation.
