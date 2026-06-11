package com.yanyun.music.creativeagent;

import java.util.List;

public record CreativeBriefResult(
    CreativeDomainDecision domainDecision,
    String userIntentSummary,
    String theme,
    List<String> moodTags,
    String narrativeViewpoint,
    String musicDirection,
    List<String> yanyunReferences,
    List<String> constraints,
    List<String> riskNotes,
    String userFacingMessage,
    String yanyunRewriteSuggestion,
    List<String> freeformOpportunities) {

  public CreativeBriefResult(
      String userIntentSummary,
      String theme,
      List<String> moodTags,
      String narrativeViewpoint,
      String musicDirection,
      List<String> yanyunReferences,
      List<String> constraints,
      List<String> riskNotes) {
    this(
        CreativeDomainDecision.PASS,
        userIntentSummary,
        theme,
        moodTags,
        narrativeViewpoint,
        musicDirection,
        yanyunReferences,
        constraints,
        riskNotes,
        null,
        null,
        List.of());
  }

  public CreativeBriefResult {
    domainDecision = domainDecision == null ? CreativeDomainDecision.PASS : domainDecision;
    userIntentSummary = firstNonBlank(userIntentSummary, "Yanyun creative brief.");
    theme = firstNonBlank(theme, "Yanyun memory");
    moodTags =
        moodTags == null || moodTags.isEmpty()
            ? List.of("cinematic", "warm")
            : List.copyOf(moodTags);
    narrativeViewpoint = firstNonBlank(narrativeViewpoint, "player-facing");
    musicDirection = firstNonBlank(musicDirection, "ancient chinese folk pop");
    yanyunReferences =
        yanyunReferences == null || yanyunReferences.isEmpty()
            ? List.of()
            : List.copyOf(yanyunReferences);
    constraints =
        constraints == null || constraints.isEmpty()
            ? List.of("keep lyrics singable", "avoid unsupported real-person claims")
            : List.copyOf(constraints);
    riskNotes = riskNotes == null ? List.of() : List.copyOf(riskNotes);
    userFacingMessage = blankToNull(userFacingMessage);
    yanyunRewriteSuggestion = blankToNull(yanyunRewriteSuggestion);
    freeformOpportunities =
        freeformOpportunities == null ? List.of() : List.copyOf(freeformOpportunities);
  }

  private static String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
