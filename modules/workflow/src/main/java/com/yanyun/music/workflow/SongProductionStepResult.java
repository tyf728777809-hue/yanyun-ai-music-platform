package com.yanyun.music.workflow;

import java.util.Map;

public record SongProductionStepResult(
    SongProductionStepName stepName,
    String idempotencyKey,
    boolean success,
    String status,
    String failureCode,
    String failureMessage,
    Map<String, String> references) {

  public SongProductionStepResult {
    if (stepName == null) {
      throw new IllegalArgumentException("stepName is required");
    }
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new IllegalArgumentException("idempotencyKey is required");
    }
    status = status == null || status.isBlank() ? (success ? "SUCCEEDED" : "FAILED") : status;
    references = references == null ? Map.of() : Map.copyOf(references);
  }

  public static SongProductionStepResult succeeded(
      SongProductionActivityContext context,
      SongProductionStepName stepName,
      Map<String, String> references) {
    return new SongProductionStepResult(
        stepName, context.idempotencyKey(stepName), true, "SUCCEEDED", null, null, references);
  }

  public static SongProductionStepResult failed(
      SongProductionActivityContext context,
      SongProductionStepName stepName,
      String failureCode,
      String failureMessage,
      Map<String, String> references) {
    return new SongProductionStepResult(
        stepName,
        context.idempotencyKey(stepName),
        false,
        "FAILED",
        failureCode,
        failureMessage,
        references);
  }
}
