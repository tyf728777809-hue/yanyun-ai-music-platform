CREATE TABLE workflow_outbox (
  id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  aggregate_type        VARCHAR(64) NOT NULL,
  aggregate_id          UUID NOT NULL,
  event_type            VARCHAR(128) NOT NULL,
  payload_json          JSONB NOT NULL,
  status                VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  attempt_count         INTEGER NOT NULL DEFAULT 0,
  max_attempts          INTEGER NOT NULL DEFAULT 3,
  next_attempt_at       TIMESTAMPTZ,
  locked_at             TIMESTAMPTZ,
  locked_by             VARCHAR(128),
  processed_at          TIMESTAMPTZ,
  failure_code          VARCHAR(128),
  failure_message       TEXT,
  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT workflow_outbox_status_check CHECK (
    status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'SKIPPED')
  ),
  CONSTRAINT workflow_outbox_attempt_count_check CHECK (attempt_count >= 0),
  CONSTRAINT workflow_outbox_max_attempts_check CHECK (max_attempts > 0)
);

CREATE INDEX idx_workflow_outbox_due
  ON workflow_outbox (status, next_attempt_at, created_at);

CREATE INDEX idx_workflow_outbox_aggregate
  ON workflow_outbox (aggregate_type, aggregate_id, created_at DESC);
