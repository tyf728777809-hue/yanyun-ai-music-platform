package com.yanyun.music.api.workflow;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WorkflowOutboxRepository {

  private final JdbcTemplate jdbcTemplate;

  public WorkflowOutboxRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public UUID insert(
      String aggregateType,
      UUID aggregateId,
      String eventType,
      String payloadJson,
      int maxAttempts) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO workflow_outbox (
          id,
          aggregate_type,
          aggregate_id,
          event_type,
          payload_json,
          status,
          max_attempts,
          next_attempt_at
        )
        VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, now())
        """,
        id,
        aggregateType,
        aggregateId,
        eventType,
        payloadJson,
        WorkflowOutboxStatus.PENDING.name(),
        Math.max(1, maxAttempts));
    return id;
  }

  public List<WorkflowOutboxEvent> claimDue(int limit, String lockedBy, Duration lockTimeout) {
    long lockTimeoutSeconds =
        Math.max(1L, safeDuration(lockTimeout, Duration.ofSeconds(60)).toSeconds());
    return jdbcTemplate.query(
        """
        WITH candidates AS (
          SELECT id
          FROM workflow_outbox
          WHERE (
              status = 'PENDING'
              AND (next_attempt_at IS NULL OR next_attempt_at <= now())
            )
            OR (
              status = 'FAILED'
              AND attempt_count < max_attempts
              AND next_attempt_at IS NOT NULL
              AND next_attempt_at <= now()
            )
            OR (
              status = 'PROCESSING'
              AND locked_at IS NOT NULL
              AND locked_at < now() - (? * interval '1 second')
            )
          ORDER BY created_at ASC
          LIMIT ?
          FOR UPDATE SKIP LOCKED
        )
        UPDATE workflow_outbox
        SET status = 'PROCESSING',
            locked_at = now(),
            locked_by = ?,
            updated_at = now()
        FROM candidates
        WHERE workflow_outbox.id = candidates.id
        RETURNING workflow_outbox.*
        """,
        this::mapEvent,
        lockTimeoutSeconds,
        Math.max(1, limit),
        lockedBy);
  }

  public void markSucceeded(UUID id) {
    jdbcTemplate.update(
        """
        UPDATE workflow_outbox
        SET status = ?,
            processed_at = now(),
            next_attempt_at = NULL,
            locked_at = NULL,
            locked_by = NULL,
            failure_code = NULL,
            failure_message = NULL,
            updated_at = now()
        WHERE id = ?
        """,
        WorkflowOutboxStatus.SUCCEEDED.name(),
        id);
  }

  public void markSkipped(UUID id, String reason) {
    jdbcTemplate.update(
        """
        UPDATE workflow_outbox
        SET status = ?,
            processed_at = now(),
            next_attempt_at = NULL,
            locked_at = NULL,
            locked_by = NULL,
            failure_code = ?,
            failure_message = ?,
            updated_at = now()
        WHERE id = ?
        """,
        WorkflowOutboxStatus.SKIPPED.name(),
        "WORKFLOW_SKIPPED",
        sanitize(reason),
        id);
  }

  public void markFailed(UUID id, String failureCode, String failureMessage, Duration retryDelay) {
    long retryDelaySeconds =
        Math.max(1L, safeDuration(retryDelay, Duration.ofSeconds(10)).toSeconds());
    jdbcTemplate.update(
        """
        UPDATE workflow_outbox
        SET status = ?,
            attempt_count = attempt_count + 1,
            next_attempt_at =
              CASE
                WHEN attempt_count + 1 < max_attempts
                THEN now() + (? * interval '1 second')
                ELSE NULL
              END,
            processed_at =
              CASE
                WHEN attempt_count + 1 >= max_attempts THEN now()
                ELSE NULL
              END,
            locked_at = NULL,
            locked_by = NULL,
            failure_code = ?,
            failure_message = ?,
            updated_at = now()
        WHERE id = ?
        """,
        WorkflowOutboxStatus.FAILED.name(),
        retryDelaySeconds,
        sanitize(failureCode),
        sanitize(failureMessage),
        id);
  }

  public Optional<WorkflowOutboxEvent> findById(UUID id) {
    try {
      return Optional.of(
          jdbcTemplate.queryForObject(
              "SELECT * FROM workflow_outbox WHERE id = ?", this::mapEvent, id));
    } catch (EmptyResultDataAccessException exception) {
      return Optional.empty();
    }
  }

  public List<WorkflowOutboxEvent> findByAggregate(UUID aggregateId) {
    return jdbcTemplate.query(
        """
        SELECT *
        FROM workflow_outbox
        WHERE aggregate_id = ?
        ORDER BY created_at ASC
        """,
        this::mapEvent,
        aggregateId);
  }

  private WorkflowOutboxEvent mapEvent(ResultSet resultSet, int rowNum) throws SQLException {
    return new WorkflowOutboxEvent(
        resultSet.getObject("id", UUID.class),
        resultSet.getString("aggregate_type"),
        resultSet.getObject("aggregate_id", UUID.class),
        resultSet.getString("event_type"),
        resultSet.getObject("payload_json").toString(),
        WorkflowOutboxStatus.valueOf(resultSet.getString("status")),
        resultSet.getInt("attempt_count"),
        resultSet.getInt("max_attempts"),
        resultSet.getObject("next_attempt_at", OffsetDateTime.class),
        resultSet.getObject("locked_at", OffsetDateTime.class),
        resultSet.getString("locked_by"),
        resultSet.getObject("processed_at", OffsetDateTime.class),
        resultSet.getString("failure_code"),
        resultSet.getString("failure_message"),
        resultSet.getObject("created_at", OffsetDateTime.class),
        resultSet.getObject("updated_at", OffsetDateTime.class));
  }

  private Duration safeDuration(Duration value, Duration fallback) {
    return value == null || value.isNegative() || value.isZero() ? fallback : value;
  }

  private String sanitize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String sanitized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
    return sanitized.length() <= 500 ? sanitized : sanitized.substring(0, 500);
  }
}
