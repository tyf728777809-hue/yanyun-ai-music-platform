package com.yanyun.music.lyrics;

import java.util.Locale;

public enum LyricsQualityGateMode {
  STRICT("strict"),
  SELF_SCORE_ONLY("self-score-only");

  private final String propertyValue;

  LyricsQualityGateMode(String propertyValue) {
    this.propertyValue = propertyValue;
  }

  public boolean strict() {
    return this == STRICT;
  }

  public boolean selfScoreOnly() {
    return this == SELF_SCORE_ONLY;
  }

  public String propertyValue() {
    return propertyValue;
  }

  public static LyricsQualityGateMode fromProperty(String value) {
    if (value == null || value.isBlank()) {
      return STRICT;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    for (LyricsQualityGateMode mode : values()) {
      if (mode.propertyValue.equals(normalized)) {
        return mode;
      }
    }
    throw new IllegalArgumentException(
        "Unsupported lyrics quality gate mode: "
            + value
            + ". Supported values: strict, self-score-only");
  }
}
