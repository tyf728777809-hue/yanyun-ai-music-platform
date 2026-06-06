package com.yanyun.music.creativeagent;

import java.util.List;
import java.util.Map;

public record ModerationAgentResult(
    ModerationTarget target,
    ModerationAgentDecision decision,
    List<String> riskCodes,
    String message,
    String recommendedAction,
    Map<String, Object> metadata) {

  public ModerationAgentResult {
    target = target == null ? ModerationTarget.MUSIC_PROMPT : target;
    decision = decision == null ? ModerationAgentDecision.PASS : decision;
    riskCodes = riskCodes == null ? List.of() : List.copyOf(riskCodes);
    message = message == null || message.isBlank() ? null : message.trim();
    recommendedAction =
        recommendedAction == null || recommendedAction.isBlank() ? null : recommendedAction.trim();
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }

  public boolean allowed() {
    return decision == ModerationAgentDecision.PASS;
  }
}
