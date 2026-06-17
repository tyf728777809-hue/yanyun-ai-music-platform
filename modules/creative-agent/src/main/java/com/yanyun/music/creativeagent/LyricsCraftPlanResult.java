package com.yanyun.music.creativeagent;

import java.util.List;

public record LyricsCraftPlanResult(
    String planningDecision,
    String userPhraseAssessment,
    String lyricSurfaceMode,
    String deviceDecision,
    String latencyBudget,
    String confidence,
    String songThesisGuard,
    String selectedDevice,
    String selectedAngle,
    String chorusMechanism,
    String yanyunBoundaryGuard,
    List<String> rejectedAlternatives) {

  public LyricsCraftPlanResult(
      String songThesisGuard,
      String selectedDevice,
      String selectedAngle,
      String chorusMechanism,
      String yanyunBoundaryGuard,
      List<String> rejectedAlternatives) {
    this(
        "USE_PLAN",
        "WEAK_PHRASE",
        "IN_WORLD",
        "USE_SMALL_DEVICE",
        "WORTH_EXTRA_CALL",
        "HIGH",
        songThesisGuard,
        selectedDevice,
        selectedAngle,
        chorusMechanism,
        yanyunBoundaryGuard,
        rejectedAlternatives);
  }

  public LyricsCraftPlanResult {
    boolean hasCore =
        !nullToEmpty(selectedDevice).isBlank()
            || !nullToEmpty(selectedAngle).isBlank()
            || !nullToEmpty(chorusMechanism).isBlank();
    planningDecision =
        normalize(planningDecision, hasCore ? "USE_PLAN" : "SKIP_PLAN", "USE_PLAN", "SKIP_PLAN");
    userPhraseAssessment =
        normalize(userPhraseAssessment, "NONE", "STRONG_HOOK", "WEAK_PHRASE", "NONE");
    lyricSurfaceMode =
        normalize(
            lyricSurfaceMode,
            "IN_WORLD",
            "IN_WORLD",
            "PLAYER_META",
            "CHARACTER_SONG",
            "ORDINARY_STORY");
    deviceDecision =
        normalize(
            deviceDecision,
            hasCore ? "USE_SMALL_DEVICE" : "NO_DEVICE",
            "USE_USER_PHRASE",
            "USE_SMALL_DEVICE",
            "USE_DIRECT_REFRAIN",
            "NO_DEVICE");
    latencyBudget =
        normalize(
            latencyBudget,
            "USE_PLAN".equals(planningDecision) ? "WORTH_EXTRA_CALL" : "NOT_WORTH_EXTRA_CALL",
            "WORTH_EXTRA_CALL",
            "NOT_WORTH_EXTRA_CALL");
    confidence = normalize(confidence, "MEDIUM", "HIGH", "MEDIUM", "LOW");
    songThesisGuard = nullToEmpty(songThesisGuard);
    selectedDevice = nullToEmpty(selectedDevice);
    selectedAngle = nullToEmpty(selectedAngle);
    chorusMechanism = nullToEmpty(chorusMechanism);
    yanyunBoundaryGuard = nullToEmpty(yanyunBoundaryGuard);
    rejectedAlternatives =
        rejectedAlternatives == null ? List.of() : List.copyOf(rejectedAlternatives);
  }

  public boolean usable() {
    return "USE_PLAN".equals(planningDecision)
        && !"NOT_WORTH_EXTRA_CALL".equals(latencyBudget)
        && !"LOW".equals(confidence)
        && (!selectedDevice.isBlank() || !selectedAngle.isBlank() || !chorusMechanism.isBlank());
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  private static String normalize(String value, String fallback, String... allowedValues) {
    String normalized = nullToEmpty(value).toUpperCase();
    if (normalized.isBlank()) {
      return fallback;
    }
    for (String allowedValue : allowedValues) {
      if (allowedValue.equals(normalized)) {
        return normalized;
      }
    }
    return fallback;
  }
}
