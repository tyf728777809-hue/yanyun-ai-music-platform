package com.yanyun.music.creativeagent;

import java.util.List;

public record CreativeBriefResult(
    String userIntentSummary,
    String theme,
    List<String> moodTags,
    String narrativeViewpoint,
    String musicDirection,
    List<String> yanyunReferences,
    List<String> constraints,
    List<String> riskNotes) {

  public CreativeBriefResult {
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
            ? List.of("Mock Yanyun Reference")
            : List.copyOf(yanyunReferences);
    constraints =
        constraints == null || constraints.isEmpty()
            ? List.of("keep lyrics singable", "avoid unsupported real-person claims")
            : List.copyOf(constraints);
    riskNotes = riskNotes == null ? List.of() : List.copyOf(riskNotes);
  }

  private static String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
