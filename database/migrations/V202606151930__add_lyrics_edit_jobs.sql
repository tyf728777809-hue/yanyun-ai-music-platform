CREATE TABLE lyrics_edit_jobs (
  id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  work_id                    UUID NOT NULL REFERENCES works(id) ON DELETE CASCADE,
  user_id                    VARCHAR(128) NOT NULL,
  operation                  VARCHAR(32) NOT NULL,
  instruction                TEXT,
  source_lyrics_draft_id     UUID NOT NULL REFERENCES lyrics_drafts(id) ON DELETE CASCADE,
  source_version_no          INTEGER NOT NULL,
  status                     VARCHAR(32) NOT NULL,
  failure_code               VARCHAR(128),
  failure_message            TEXT,
  retryable                  BOOLEAN NOT NULL DEFAULT false,
  attempt_count              INTEGER NOT NULL DEFAULT 0,
  max_attempts               INTEGER NOT NULL DEFAULT 2,
  locked_at                  TIMESTAMPTZ,
  locked_by                  VARCHAR(128),
  started_at                 TIMESTAMPTZ,
  completed_at               TIMESTAMPTZ,
  created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT lyrics_edit_jobs_operation_check CHECK (
    operation IN ('POLISH', 'CONTINUE')
  ),
  CONSTRAINT lyrics_edit_jobs_status_check CHECK (
    status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')
  ),
  CONSTRAINT lyrics_edit_jobs_attempt_count_check CHECK (attempt_count >= 0),
  CONSTRAINT lyrics_edit_jobs_max_attempts_check CHECK (max_attempts > 0)
);

CREATE UNIQUE INDEX ux_lyrics_edit_jobs_active_work
  ON lyrics_edit_jobs (work_id)
  WHERE status IN ('QUEUED', 'RUNNING');

CREATE INDEX idx_lyrics_edit_jobs_due
  ON lyrics_edit_jobs (status, updated_at, created_at);

CREATE INDEX idx_lyrics_edit_jobs_work_created
  ON lyrics_edit_jobs (work_id, created_at DESC);
