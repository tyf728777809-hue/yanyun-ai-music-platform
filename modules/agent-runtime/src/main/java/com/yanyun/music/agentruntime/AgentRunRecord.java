package com.yanyun.music.agentruntime;

import java.math.BigDecimal;

public record AgentRunRecord(
    String workId,
    String jobId,
    String agentName,
    String agentVersion,
    String operation,
    String modelName,
    String promptTemplateKey,
    Integer promptTemplateVersion,
    String inputHash,
    String outputHash,
    AgentRunStatus status,
    Integer latencyMs,
    Integer inputTokens,
    Integer outputTokens,
    BigDecimal costUnits,
    String failureCode,
    String failureMessage) {

  public AgentRunRecord {
    agentName = required(agentName, "agentName");
    agentVersion = required(agentVersion, "agentVersion");
    operation = required(operation, "operation");
    modelName = required(modelName, "modelName");
    status = status == null ? AgentRunStatus.SUCCEEDED : status;
    failureMessage = AgentRunSanitizer.sanitizeFailureMessage(failureMessage);
  }

  private static String required(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }
}
