# Mock Publish Package Block Smoke v0.1

Author: Codex
Date: 2026-06-07
Status: Approved
Reviewers: User

## Context

The platform already calls `ModerationAdapter.preCheckPublishPackage` before making a publish handoff package available. The frontend also has component-level evidence for `PACKAGE_BLOCKED` and support-oriented actions. The remaining local-loop gap is that the default `MockModerationAdapter` always allows publish packages, so local backend smoke cannot naturally prove the publish-package moderation block path.

This feature adds a Mock-only, default-off trigger for local and CI smoke. It must not introduce a production backdoor, must not call any real model provider or company system, and must preserve the company moderation Adapter boundary.

## Functional Requirements

- FR-1: The Mock moderation adapter MUST continue to allow publish-package pre-checks by default.
- FR-2: The Mock moderation adapter MUST support an explicit local configuration list of blocked user ids for publish-package pre-check smoke.
- FR-3: The Mock moderation adapter MUST support an explicit local configuration list of blocked work ids for focused publish-package pre-check smoke.
- FR-4: A blocked publish-package pre-check MUST return a denied `ModerationDecision` with stable code `MOCK_PACKAGE_BLOCKED` and a user-readable Chinese message.
- FR-5: When publish-package pre-check is blocked in the song production workflow, the work MUST end as `status=FAILED`, `generation_stage=FAILED`, `package_status=PACKAGE_BLOCKED`, `failure.failure_code=PACKAGE_BLOCKED`, and `available_actions` MUST include `CONTACT_SUPPORT` and `RETURN_TO_EDIT`.
- FR-6: The blocked path MUST release locked generation quota and MUST NOT write a ready publish package, mark the package fetched, or commit quota.
- FR-7: A scriptable smoke entry MUST exist for an already-running local API and MUST fail with an actionable message if the API was not started with the Mock block trigger.

## Non-Functional Requirements

- NFR-1: Default local, automated, and production-like configuration MUST preserve the existing successful Mock main flow.
- NFR-2: The trigger MUST be scoped to `MockModerationAdapter`; real company moderation implementations MUST NOT inherit this behavior.
- NFR-3: The smoke script MUST NOT call DreamMaker, Yunwu, WellAPI, DeepSeek, Image 2, or company systems.
- NFR-4: The smoke script MUST NOT print real credentials, Bearer tokens, provider raw payloads, or signed media URLs.

## Acceptance Criteria

- AC-1: Given no Mock publish-package block configuration, When `preCheckPublishPackage` is called, Then it allows the request. Covers FR-1.
- AC-2: Given `mock-publish-block-user` is configured as a blocked user id, When that user reaches publish-package pre-check, Then `ModerationDecision.allowed=false`, `code=MOCK_PACKAGE_BLOCKED`, and the message is user-readable. Covers FR-2 and FR-4.
- AC-3: Given a work id is configured as blocked, When that work reaches publish-package pre-check, Then the request is blocked even if the user id is not blocked. Covers FR-3 and FR-4.
- AC-4: Given the workflow receives `MOCK_PACKAGE_BLOCKED`, When it fails the job, Then it marks work failure with `PACKAGE_BLOCKED`, sets package status to `PACKAGE_BLOCKED`, releases quota, and does not mark package ready. Covers FR-5 and FR-6.
- AC-5: Given API is running with `MOCK_MODERATION_PUBLISH_PACKAGE_BLOCKED_USER_IDS=mock_package_block_smoke`, When `scripts/smoke/api-package-blocked-flow.sh` creates and confirms one lyrics work for that user, Then the confirm response is HTTP 403 and the final work detail exposes `PACKAGE_BLOCKED`, `CONTACT_SUPPORT`, and `RETURN_TO_EDIT`. Covers FR-5 and FR-7.

## Edge Cases

- EC-1: Blank or whitespace-only configured ids are ignored.
- EC-2: Matching is case-sensitive because user ids and work ids are platform identifiers.
- EC-3: If the API was not started with the Mock block trigger, the smoke script fails and tells the operator which environment variable to set.
- EC-4: If the block occurs before a publish package row exists, `GET /publish-package` may return a lightweight `PACKAGE_BLOCKED` response without `package_json` or `package_url`.

## API Contracts

No OpenAPI schema change is required. Existing fields are used:

```ts
type WorkDetail = {
  status: "FAILED";
  generation_stage: "FAILED";
  package_status: "PACKAGE_BLOCKED";
  failure: {
    failure_code: "PACKAGE_BLOCKED";
    failure_message: string;
    retryable: false;
    recommended_action?: "CONTACT_SUPPORT" | "RETURN_TO_EDIT";
  };
  available_actions: ("CONTACT_SUPPORT" | "RETURN_TO_EDIT")[];
};
```

## Data Models

No database migration is required.

| Entity | Field | Behavior |
|---|---|---|
| `works` | `package_status` | Set to `PACKAGE_BLOCKED` only when failure code is `PACKAGE_BLOCKED`; other failures keep `PACKAGE_NOT_READY`. |
| `publish_packages` | N/A | No ready package row is required for the blocked path. |

## Out of Scope

- Real company moderation implementation.
- Real model or provider calls.
- Changing frontend routing or visual design.
- Changing OpenAPI enum names.
- Treating `PACKAGE_BLOCKED` as community publish success or handoff completion.
