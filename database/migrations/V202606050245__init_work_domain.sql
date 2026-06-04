CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE works (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  work_code             VARCHAR(64) NOT NULL UNIQUE,
  user_id               VARCHAR(128) NOT NULL,
  user_name_snapshot    VARCHAR(128),
  creation_mode         VARCHAR(64) NOT NULL,
  status                VARCHAR(32) NOT NULL,
  generation_stage      VARCHAR(64) NOT NULL DEFAULT 'NONE',
  package_status        VARCHAR(64) NOT NULL DEFAULT 'PACKAGE_NOT_READY',
  song_scope            VARCHAR(32) NOT NULL DEFAULT 'STANDARD',
  song_title            VARCHAR(256),
  song_summary          TEXT,
  polish_used_count     INTEGER NOT NULL DEFAULT 0,
  cover_regen_count     INTEGER NOT NULL DEFAULT 0,
  parent_work_id        UUID REFERENCES works(id),
  failure_code          VARCHAR(128),
  failure_message       TEXT,
  retryable             BOOLEAN,
  failed_at             TIMESTAMPTZ,
  quota_locked          BOOLEAN NOT NULL DEFAULT false,
  quota_committed       BOOLEAN NOT NULL DEFAULT false,
  version               INTEGER NOT NULL DEFAULT 0,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  generated_at          TIMESTAMPTZ,
  CONSTRAINT works_creation_mode_check CHECK (
    creation_mode IN ('INSPIRATION', 'LYRICS')
  ),
  CONSTRAINT works_status_check CHECK (
    status IN (
      'DRAFT',
      'LYRICS_GENERATING',
      'LYRICS_READY',
      'LYRICS_FAILED',
      'GENERATING',
      'GENERATED',
      'FAILED',
      'CANCELLED'
    )
  ),
  CONSTRAINT works_generation_stage_check CHECK (
    generation_stage IN (
      'NONE',
      'USER_INPUT_PRECHECK',
      'LYRICS_GENERATING',
      'LYRICS_PRECHECK',
      'WAITING_CONFIRM',
      'QUOTA_LOCKING',
      'MUSIC_GENERATING',
      'COVER_GENERATING',
      'TIMELINE_BUILDING',
      'VIDEO_RENDERING',
      'PACKAGE_BUILDING',
      'PACKAGE_PRECHECK',
      'PACKAGE_READY',
      'FAILED'
    )
  ),
  CONSTRAINT works_package_status_check CHECK (
    package_status IN (
      'PACKAGE_NOT_READY',
      'PACKAGE_READY',
      'PACKAGE_FETCHED',
      'PACKAGE_EXPIRED',
      'PACKAGE_BLOCKED'
    )
  ),
  CONSTRAINT works_polish_used_count_check CHECK (polish_used_count >= 0),
  CONSTRAINT works_cover_regen_count_check CHECK (cover_regen_count >= 0),
  CONSTRAINT works_version_check CHECK (version >= 0)
);

CREATE INDEX idx_works_user_updated ON works (user_id, updated_at DESC);
CREATE INDEX idx_works_status ON works (status);
CREATE INDEX idx_works_package_status ON works (package_status);

CREATE TABLE work_inputs (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  work_id               UUID NOT NULL REFERENCES works(id) ON DELETE CASCADE,
  story_input           TEXT,
  lyrics_input          TEXT,
  mood                  VARCHAR(128),
  scene                 VARCHAR(256),
  relationship          VARCHAR(256),
  music_style           TEXT,
  vocal_preference      VARCHAR(64),
  input_snapshot_json   JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_work_inputs_work_id ON work_inputs (work_id);

CREATE TABLE lyrics_drafts (
  id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  work_id                    UUID NOT NULL REFERENCES works(id) ON DELETE CASCADE,
  version_no                 INTEGER NOT NULL,
  song_title                 VARCHAR(256) NOT NULL,
  song_summary               TEXT,
  lyrics_text                TEXT NOT NULL,
  structured_lyrics_json     JSONB NOT NULL DEFAULT '{}'::jsonb,
  sections_json              JSONB,
  music_prompt               TEXT NOT NULL,
  cover_prompt_seed          TEXT,
  quality_score              NUMERIC(5, 4),
  risk_notes_json            JSONB,
  yanyun_references_json     JSONB,
  knowledge_base_version     VARCHAR(128),
  prompt_template_versions   JSONB,
  created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT lyrics_drafts_version_no_check CHECK (version_no >= 1),
  UNIQUE (work_id, version_no)
);

CREATE INDEX idx_lyrics_drafts_work_created ON lyrics_drafts (work_id, created_at DESC);

CREATE TABLE generation_jobs (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  work_id               UUID NOT NULL REFERENCES works(id) ON DELETE CASCADE,
  workflow_id           VARCHAR(256),
  workflow_run_id       VARCHAR(256),
  job_type              VARCHAR(64) NOT NULL,
  status                VARCHAR(32) NOT NULL,
  stage                 VARCHAR(64),
  retry_count           INTEGER NOT NULL DEFAULT 0,
  started_at            TIMESTAMPTZ,
  completed_at          TIMESTAMPTZ,
  failure_code          VARCHAR(128),
  failure_message       TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT generation_jobs_status_check CHECK (
    status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')
  ),
  CONSTRAINT generation_jobs_retry_count_check CHECK (retry_count >= 0)
);

CREATE INDEX idx_generation_jobs_work_created ON generation_jobs (work_id, created_at DESC);
CREATE INDEX idx_generation_jobs_status ON generation_jobs (status);

CREATE TABLE media_assets (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  work_id               UUID NOT NULL REFERENCES works(id) ON DELETE CASCADE,
  asset_type            VARCHAR(64) NOT NULL,
  object_key            TEXT NOT NULL,
  mime_type             VARCHAR(128),
  file_size_bytes       BIGINT,
  checksum              VARCHAR(256),
  width                 INTEGER,
  height                INTEGER,
  duration_ms           INTEGER,
  sample_rate           INTEGER,
  bitrate               INTEGER,
  metadata_json         JSONB,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT media_assets_asset_type_check CHECK (
    asset_type IN ('AUDIO', 'COVER', 'VIDEO', 'TIMELINE', 'PACKAGE')
  ),
  CONSTRAINT media_assets_file_size_bytes_check CHECK (
    file_size_bytes IS NULL OR file_size_bytes >= 0
  ),
  UNIQUE (work_id, asset_type)
);

CREATE INDEX idx_media_assets_work_type ON media_assets (work_id, asset_type);

CREATE TABLE publish_packages (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  work_id               UUID NOT NULL UNIQUE REFERENCES works(id) ON DELETE CASCADE,
  package_status        VARCHAR(64) NOT NULL,
  package_json          JSONB NOT NULL DEFAULT '{}'::jsonb,
  package_object_key    TEXT,
  package_url           TEXT,
  package_url_expires_at TIMESTAMPTZ,
  fetched_at            TIMESTAMPTZ,
  last_url_refreshed_at TIMESTAMPTZ,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT publish_packages_status_check CHECK (
    package_status IN (
      'PACKAGE_NOT_READY',
      'PACKAGE_READY',
      'PACKAGE_FETCHED',
      'PACKAGE_EXPIRED',
      'PACKAGE_BLOCKED'
    )
  )
);

CREATE INDEX idx_publish_packages_status ON publish_packages (package_status);

CREATE TABLE provider_calls (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  work_id               UUID REFERENCES works(id) ON DELETE SET NULL,
  job_id                UUID REFERENCES generation_jobs(id) ON DELETE SET NULL,
  provider              VARCHAR(64) NOT NULL,
  operation             VARCHAR(64) NOT NULL,
  model_name            VARCHAR(128),
  request_hash          VARCHAR(256),
  prompt_hash           VARCHAR(256),
  provider_trace_id     VARCHAR(256),
  status                VARCHAR(32) NOT NULL,
  latency_ms            INTEGER,
  cost_units            NUMERIC(18, 6),
  error_code            VARCHAR(128),
  error_message         TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT provider_calls_status_check CHECK (
    status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'SKIPPED')
  ),
  CONSTRAINT provider_calls_latency_ms_check CHECK (
    latency_ms IS NULL OR latency_ms >= 0
  )
);

CREATE INDEX idx_provider_calls_work_created ON provider_calls (work_id, created_at DESC);
CREATE INDEX idx_provider_calls_provider_operation ON provider_calls (provider, operation);

CREATE TABLE quota_transactions (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  work_id               UUID NOT NULL REFERENCES works(id) ON DELETE CASCADE,
  user_id               VARCHAR(128) NOT NULL,
  external_lock_id      VARCHAR(256),
  action                VARCHAR(64) NOT NULL,
  status                VARCHAR(32) NOT NULL,
  amount                INTEGER NOT NULL DEFAULT 1,
  reason                TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT quota_transactions_status_check CHECK (
    status IN ('LOCKED', 'COMMITTED', 'RELEASED', 'FAILED')
  ),
  CONSTRAINT quota_transactions_amount_check CHECK (amount > 0)
);

CREATE INDEX idx_quota_transactions_user_created ON quota_transactions (user_id, created_at DESC);
CREATE INDEX idx_quota_transactions_work ON quota_transactions (work_id);

CREATE TABLE knowledge_documents (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  kb_version            VARCHAR(128) NOT NULL,
  file_path             TEXT NOT NULL,
  title                 TEXT,
  content_hash          VARCHAR(256) NOT NULL,
  metadata_json         JSONB,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_knowledge_documents_version ON knowledge_documents (kb_version);

CREATE TABLE knowledge_chunks (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  document_id           UUID NOT NULL REFERENCES knowledge_documents(id) ON DELETE CASCADE,
  kb_version            VARCHAR(128) NOT NULL,
  chunk_index           INTEGER NOT NULL,
  heading_path          TEXT,
  content               TEXT NOT NULL,
  tags_json             JSONB,
  token_count           INTEGER,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT knowledge_chunks_chunk_index_check CHECK (chunk_index >= 0),
  CONSTRAINT knowledge_chunks_token_count_check CHECK (
    token_count IS NULL OR token_count >= 0
  ),
  UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_knowledge_chunks_version ON knowledge_chunks (kb_version);

CREATE TABLE prompt_templates (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  template_key          VARCHAR(128) NOT NULL,
  version               INTEGER NOT NULL,
  content               TEXT NOT NULL,
  enabled               BOOLEAN NOT NULL DEFAULT true,
  metadata_json         JSONB,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT prompt_templates_version_check CHECK (version >= 1),
  UNIQUE (template_key, version)
);

CREATE INDEX idx_prompt_templates_enabled ON prompt_templates (template_key, enabled);

CREATE TABLE system_configs (
  config_key            VARCHAR(128) PRIMARY KEY,
  config_value          JSONB NOT NULL,
  updated_by            VARCHAR(128),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE idempotency_keys (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id               VARCHAR(128) NOT NULL,
  idempotency_key       VARCHAR(256) NOT NULL,
  operation             VARCHAR(128) NOT NULL,
  request_hash          VARCHAR(256),
  response_json         JSONB,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at            TIMESTAMPTZ,
  UNIQUE (user_id, idempotency_key, operation)
);

CREATE INDEX idx_idempotency_keys_expires_at ON idempotency_keys (expires_at);
