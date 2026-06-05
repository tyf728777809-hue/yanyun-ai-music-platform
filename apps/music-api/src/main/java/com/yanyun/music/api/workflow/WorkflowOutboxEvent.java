package com.yanyun.music.api.workflow;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkflowOutboxEvent(
    UUID id,
    String aggregateType,
    UUID aggregateId,
    String eventType,
    String payloadJson,
    WorkflowOutboxStatus status,
    int attemptCount,
    int maxAttempts,
    OffsetDateTime nextAttemptAt,
    OffsetDateTime lockedAt,
    String lockedBy,
    OffsetDateTime processedAt,
    String failureCode,
    String failureMessage,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
