# No Standalone Yanyun Knowledge Base v0.1

## 1. Title and Metadata

- Title: No Standalone Yanyun Knowledge Base
- Author: Codex
- Date: 2026-06-11
- Status: Approved by user decision
- Reviewers: User

## 2. Context

The project originally reserved a Markdown knowledge base, OpenSearch retrieval, knowledge chunk tables, and knowledge reference fields for lyrics generation. In practice, the current system has no real Yanyun knowledge content and no real OpenSearch-backed retrieval path. The active implementation only injects mocked references.

The user has decided to cancel the standalone Yanyun knowledge base. The product should still feel like Yanyun, but that style should come from prompts, creative brief extraction, and fixed product constraints rather than a separate RAG/knowledge-indexing system.

## 3. Functional Requirements

- FR-1: The active lyrics generation path MUST NOT require a standalone Yanyun knowledge base.
- FR-2: The active lyrics generation path MUST NOT return mocked Yanyun knowledge references as if they were real citations.
- FR-3: The system MUST preserve existing OpenAPI compatibility for `yanyun_references` and knowledge version fields until a future contract version removes them.
- FR-4: The system SHOULD return empty knowledge references and a disabled knowledge version marker when no knowledge base is used.
- FR-5: Yanyun tone SHOULD be controlled through prompt templates, CreativeBriefAgent output, and model instructions.
- FR-6: OpenSearch MUST NOT be treated as a required current product dependency for lyrics generation.

## 4. Non-Functional Requirements

- NFR-1: Removing the active knowledge dependency MUST NOT change public endpoint paths or required request fields.
- NFR-2: Automated tests MUST NOT require OpenSearch or a knowledge index.
- NFR-3: Existing local databases MUST remain usable; destructive migration cleanup is deferred.

## 5. Acceptance Criteria

- AC-1: Given the default API configuration, when lyrics are generated, then the injected knowledge service returns no references and marks the knowledge version as disabled. Covers FR-1, FR-2, FR-4.
- AC-2: Given a frontend calls `GET /works/{work_id}`, when `yanyun_references` is empty, then the response remains valid under OpenAPI v0.1. Covers FR-3.
- AC-3: Given real DeepSeek lyrics generation is enabled, when no OpenSearch or knowledge index exists, then preflight and generation do not fail because of missing knowledge infrastructure. Covers FR-1, FR-6, NFR-2.
- AC-4: Given old rows contain `knowledge_base_version`, when the application starts, then no destructive migration is required. Covers NFR-3.

## 6. Edge Cases

- EC-1: Existing works may still contain historical mock knowledge versions; API responses may expose those historical values until data cleanup.
- EC-2: Frontend prototypes that display `yanyun_references` must handle an empty array.
- EC-3: Old documents may mention knowledge base as historical design; ADR 0005 takes precedence.

## 7. API Contracts

No new public API fields.

Compatibility fields:

```ts
type LyricsDraft = {
  yanyun_references?: string[]; // default []
};

type InternalLyricsMetadata = {
  knowledge_base_version?: string | null; // default "disabled" for new no-knowledge runs
};
```

## 8. Data Models

No immediate schema changes.

| Entity | Field | Decision |
|---|---|---|
| `lyrics_drafts` | `knowledge_base_version` | Keep temporarily; new no-knowledge runs may store `disabled` |
| `knowledge_documents` | all | Legacy table, no longer active target |
| `knowledge_chunks` | all | Legacy table, no longer active target |

## 9. Out of Scope

- Removing historical migrations.
- Removing OpenSearch from Docker Compose in this same change.
- Renaming OpenAPI v0.1 fields.
- Rewriting all historical v0.1/v0.2 documents.
