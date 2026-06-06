CREATE TABLE generation_job_steps (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id                UUID NOT NULL REFERENCES generation_jobs(id) ON DELETE CASCADE,
  work_id               UUID NOT NULL REFERENCES works(id) ON DELETE CASCADE,
  step_name             VARCHAR(128) NOT NULL,
  idempotency_key       VARCHAR(512) NOT NULL,
  status                VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  attempt_count         INTEGER NOT NULL DEFAULT 0,
  external_trace_id     VARCHAR(512),
  failure_code          VARCHAR(128),
  failure_message       TEXT,
  started_at            TIMESTAMPTZ,
  completed_at          TIMESTAMPTZ,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT generation_job_steps_status_check CHECK (
    status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'SKIPPED')
  ),
  CONSTRAINT generation_job_steps_attempt_count_check CHECK (attempt_count >= 0),
  UNIQUE (job_id, step_name, idempotency_key)
);

CREATE INDEX idx_generation_job_steps_job
  ON generation_job_steps (job_id, created_at);

CREATE INDEX idx_generation_job_steps_work
  ON generation_job_steps (work_id, created_at);

CREATE INDEX idx_generation_job_steps_status
  ON generation_job_steps (status, updated_at);
