CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge_kb_versions (
  kb_version            VARCHAR(128) PRIMARY KEY,
  title                 TEXT NOT NULL,
  content_as_of         DATE NOT NULL,
  source_policy         TEXT NOT NULL,
  notes                 TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE knowledge_entities (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  kb_version            VARCHAR(128) NOT NULL,
  entity_type           VARCHAR(64) NOT NULL,
  canonical_name        TEXT NOT NULL,
  summary               TEXT NOT NULL,
  source_type           VARCHAR(64) NOT NULL,
  source_ref            TEXT NOT NULL,
  fact_level            VARCHAR(64) NOT NULL DEFAULT 'official_public',
  spoiler_level         VARCHAR(64) NOT NULL DEFAULT 'open',
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (kb_version, entity_type, canonical_name)
);

CREATE INDEX idx_knowledge_entities_version_type
  ON knowledge_entities (kb_version, entity_type);

CREATE TABLE knowledge_entity_aliases (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  entity_id             UUID NOT NULL REFERENCES knowledge_entities(id) ON DELETE CASCADE,
  alias                 TEXT NOT NULL,
  normalized_alias      TEXT NOT NULL,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (entity_id, normalized_alias)
);

CREATE INDEX idx_knowledge_entity_aliases_normalized
  ON knowledge_entity_aliases (normalized_alias);

ALTER TABLE knowledge_documents
  ADD COLUMN IF NOT EXISTS source_type VARCHAR(64),
  ADD COLUMN IF NOT EXISTS source_ref TEXT,
  ADD COLUMN IF NOT EXISTS source_updated_at DATE,
  ADD COLUMN IF NOT EXISTS content_as_of DATE,
  ADD COLUMN IF NOT EXISTS verified_by TEXT;

ALTER TABLE knowledge_chunks
  ADD COLUMN IF NOT EXISTS entity_id UUID REFERENCES knowledge_entities(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS entity_type VARCHAR(64),
  ADD COLUMN IF NOT EXISTS entity_name TEXT,
  ADD COLUMN IF NOT EXISTS aliases_json JSONB,
  ADD COLUMN IF NOT EXISTS theme_tags_json JSONB,
  ADD COLUMN IF NOT EXISTS fact_level VARCHAR(64),
  ADD COLUMN IF NOT EXISTS spoiler_level VARCHAR(64),
  ADD COLUMN IF NOT EXISTS story_phase TEXT,
  ADD COLUMN IF NOT EXISTS summary_for_prompt TEXT,
  ADD COLUMN IF NOT EXISTS usable_imagery_json JSONB,
  ADD COLUMN IF NOT EXISTS emotional_arc TEXT,
  ADD COLUMN IF NOT EXISTS avoid_claims_json JSONB,
  ADD COLUMN IF NOT EXISTS source_ref TEXT,
  ADD COLUMN IF NOT EXISTS embedding vector(64);

CREATE INDEX idx_knowledge_chunks_entity
  ON knowledge_chunks (kb_version, entity_type, entity_name);

CREATE INDEX idx_knowledge_chunks_entity_id
  ON knowledge_chunks (entity_id);

CREATE INDEX idx_knowledge_chunks_embedding
  ON knowledge_chunks USING hnsw (embedding vector_cosine_ops);
