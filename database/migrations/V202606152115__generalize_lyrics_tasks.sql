ALTER TABLE lyrics_edit_jobs
  DROP CONSTRAINT lyrics_edit_jobs_operation_check;

ALTER TABLE lyrics_edit_jobs
  ALTER COLUMN source_lyrics_draft_id DROP NOT NULL,
  ALTER COLUMN source_version_no DROP NOT NULL,
  ADD COLUMN request_payload_json JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE lyrics_edit_jobs
  ADD CONSTRAINT lyrics_edit_jobs_operation_check CHECK (
    operation IN ('CREATE_INSPIRATION', 'CREATE_LYRICS', 'POLISH', 'CONTINUE')
  );
