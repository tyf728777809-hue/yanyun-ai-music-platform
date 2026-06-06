# Frontend Available Actions Matrix Smoke v0.1

## Title and Metadata

- Author: Codex
- Date: 2026-06-07
- Status: Draft
- Reviewers: User / company frontend owner
- Scope: `prototypes/Claude-web-v1`, `available_actions` rendering, local non-real verification

## Context

The local commercial loop already has strong evidence for the main success path: inspiration creation, lyrics edits, confirmation, generated media, publish handoff, and the `RETRY_MUSIC` failure recovery path. The remaining frontend acceptance risk is narrower: not every backend-provided `available_actions` value is proven at the view/component level.

This matters because the product contract says the frontend must not guess actions from status. The backend owns state transitions and returns the allowed actions; the UI must render and execute only those actions. Company developers will later replace account, moderation, quota, publish, and share adapters, so action rendering must stay strictly data-driven while those systems remain mocked locally.

## Functional Requirements

- FR-1: The frontend MUST render action buttons from backend-provided `available_actions` only.
- FR-2: The failed view MUST support `RETRY_MUSIC`, `RETRY_COVER`, `RERENDER_VIDEO`, `RETURN_TO_EDIT`, and `CONTACT_SUPPORT` when those actions are present.
- FR-3: The finished view MUST support generated-work actions that can appear alongside package states, including `RERENDER_VIDEO` and `CONTACT_SUPPORT`.
- FR-4: The finished view MUST hide `MARK_PACKAGE_FETCHED` when the package is fetched or blocked.
- FR-5: The finished view MUST hide `REFRESH_PACKAGE_URL` when that action is not present.
- FR-6: Action execution MUST call the corresponding existing API wrapper and use the shared loading/error handling path.
- FR-7: Verification MUST NOT call real DeepSeek, Suno, MiniMax, Image 2, DreamMaker, Yunwu, WellAPI, or company systems.

## Non-Functional Requirements

- NFR-1: Component tests SHOULD complete within the normal frontend unit test suite runtime.
- NFR-2: Tests MUST use mocked frontend service calls and MUST NOT require a running backend.
- NFR-3: User-facing copy MUST remain ordinary product copy and MUST NOT expose technical package JSON wording.
- NFR-4: The implementation MUST NOT modify `apps/web`, Java backend modules, database migrations, or production configuration.

## Acceptance Criteria

- AC-1: Given a failed work with `available_actions=["RETRY_COVER","RERENDER_VIDEO","RETURN_TO_EDIT","CONTACT_SUPPORT"]`, when the failed view renders, then it shows the four matching user-facing actions and does not show `RETRY_MUSIC`. Covers FR-1 and FR-2.
- AC-2: Given the same failed work, when the user clicks `RETRY_COVER`, then the view calls `service.regenerateCover(work_id)`. Covers FR-2 and FR-6.
- AC-3: Given the same failed work, when the user clicks `RERENDER_VIDEO`, then the view calls `service.rerenderVideo(work_id)`. Covers FR-2 and FR-6.
- AC-4: Given a package-blocked generated work with `available_actions=["RERENDER_VIDEO","CONTACT_SUPPORT"]`, when the finished view renders, then it shows blocked handoff copy, shows rerender/support actions, and hides mark-fetched/refresh buttons. Covers FR-1, FR-3, FR-4, and FR-5.
- AC-5: Given a package-blocked generated work, when the user clicks `RERENDER_VIDEO`, then the view calls `service.rerenderVideo(work_id)`. Covers FR-3 and FR-6.
- AC-6: Given frontend tests run, when the suite executes, then no real provider or company-system call is made. Covers FR-7 and NFR-2.

## Edge Cases

- EC-1: If `available_actions` is empty, no mutating action button should be shown beyond navigation links.
- EC-2: If the publish package response omits `available_actions`, the finished view may fall back to the work detail action list.
- EC-3: If a package is blocked but the backend still returns `MARK_PACKAGE_FETCHED`, the finished view must still hide the mark-fetched button because blocked packages cannot be handed off.
- EC-4: If an action call fails, existing shared toast/error handling remains responsible for user feedback.

## API Contracts

```ts
type AvailableAction =
  | "RETRY_MUSIC"
  | "RETRY_COVER"
  | "RERENDER_VIDEO"
  | "REFRESH_PACKAGE_URL"
  | "MARK_PACKAGE_FETCHED"
  | "RETURN_TO_EDIT"
  | "CONTACT_SUPPORT";

interface WorkDetail {
  work_id: string;
  status: WorkStatus;
  generation_stage: GenerationStage;
  package_status: PackageStatus;
  failure?: FailureInfo | null;
  available_actions: AvailableAction[];
}

interface PublishPackage {
  work_id: string;
  package_status: PackageStatus;
  available_actions: AvailableAction[];
  blocked_reason?: string | null;
}
```

## Data Models

| Entity | Field | Type | Constraint |
|---|---|---|---|
| WorkDetail | available_actions | AvailableAction[] | Source of truth for view actions |
| WorkDetail | package_status | PackageStatus | Drives finished handoff copy and safety hiding |
| FailureInfo | recommended_action | AvailableAction? | Display-only recommendation |
| PublishPackage | available_actions | AvailableAction[] | Preferred action source after package fetch |

## Out of Scope

- OS-1: No new backend test fixture endpoint.
- OS-2: No database migration or production state-machine change.
- OS-3: No real model, provider, company Adapter, object storage, or browser smoke execution.
- OS-4: No final decision on migrating `prototypes/Claude-web-v1` into `apps/web`.
