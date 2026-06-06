CREATE TABLE agent_runs (
  id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  work_id                  UUID,
  job_id                   UUID,
  agent_name               VARCHAR(128) NOT NULL,
  agent_version            VARCHAR(64) NOT NULL,
  operation                VARCHAR(128) NOT NULL,
  model_name               VARCHAR(128) NOT NULL,
  prompt_template_key      VARCHAR(128),
  prompt_template_version  INTEGER,
  input_hash               VARCHAR(256),
  output_hash              VARCHAR(256),
  status                   VARCHAR(32) NOT NULL,
  latency_ms               INTEGER,
  input_tokens             INTEGER,
  output_tokens            INTEGER,
  cost_units               NUMERIC(18, 6),
  failure_code             VARCHAR(128),
  failure_message          TEXT,
  created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT agent_runs_status_check CHECK (
    status IN ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'SKIPPED')
  ),
  CONSTRAINT agent_runs_prompt_template_version_check CHECK (
    prompt_template_version IS NULL OR prompt_template_version >= 0
  ),
  CONSTRAINT agent_runs_latency_ms_check CHECK (
    latency_ms IS NULL OR latency_ms >= 0
  ),
  CONSTRAINT agent_runs_input_tokens_check CHECK (
    input_tokens IS NULL OR input_tokens >= 0
  ),
  CONSTRAINT agent_runs_output_tokens_check CHECK (
    output_tokens IS NULL OR output_tokens >= 0
  ),
  CONSTRAINT agent_runs_cost_units_check CHECK (
    cost_units IS NULL OR cost_units >= 0
  )
);

CREATE INDEX idx_agent_runs_work_created ON agent_runs (work_id, created_at DESC);
CREATE INDEX idx_agent_runs_agent_operation ON agent_runs (agent_name, operation, created_at DESC);
