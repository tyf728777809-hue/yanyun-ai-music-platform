package com.yanyun.music.production;

import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcAgentRunRecorder implements AgentRunRecorder {

  private final JdbcTemplate jdbcTemplate;

  public JdbcAgentRunRecorder(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void record(AgentRunRecord record) {
    jdbcTemplate.update(
        """
        INSERT INTO agent_runs (
          work_id,
          job_id,
          agent_name,
          agent_version,
          operation,
          model_name,
          prompt_template_key,
          prompt_template_version,
          input_hash,
          output_hash,
          status,
          latency_ms,
          input_tokens,
          output_tokens,
          cost_units,
          failure_code,
          failure_message
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        parseUuidOrNull(record.workId()),
        parseUuidOrNull(record.jobId()),
        record.agentName(),
        record.agentVersion(),
        record.operation(),
        record.modelName(),
        record.promptTemplateKey(),
        record.promptTemplateVersion(),
        record.inputHash(),
        record.outputHash(),
        record.status().name(),
        record.latencyMs(),
        record.inputTokens(),
        record.outputTokens(),
        record.costUnits(),
        record.failureCode(),
        record.failureMessage());
  }

  private UUID parseUuidOrNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }
}
