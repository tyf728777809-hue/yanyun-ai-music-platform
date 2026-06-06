package com.yanyun.music.creativeagent;

import java.util.List;
import java.util.Map;

public record QualityEvaluationResult(
    QualityGate gate,
    QualityDecision decision,
    int score,
    List<String> reasons,
    String recommendedAction,
    boolean retryable,
    Map<String, Object> metadata) {

  public QualityEvaluationResult {
    gate = gate == null ? QualityGate.PUBLISH_PACKAGE : gate;
    decision = decision == null ? QualityDecision.PASS : decision;
    score = Math.max(0, Math.min(100, score));
    reasons = reasons == null ? List.of() : List.copyOf(reasons);
    recommendedAction =
        recommendedAction == null || recommendedAction.isBlank() ? null : recommendedAction.trim();
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }
}
