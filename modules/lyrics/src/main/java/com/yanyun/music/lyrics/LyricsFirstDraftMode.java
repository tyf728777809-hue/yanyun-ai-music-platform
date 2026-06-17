package com.yanyun.music.lyrics;

import java.util.Locale;

public enum LyricsFirstDraftMode {
  FULL_BRIEF,
  DIRECT,
  CONDITIONAL_BRIEF;

  public static LyricsFirstDraftMode fromProperty(String value) {
    if (value == null || value.isBlank()) {
      return CONDITIONAL_BRIEF;
    }
    return switch (value.trim().toLowerCase(Locale.ROOT).replace('_', '-')) {
      case "full", "full-brief", "creative-brief", "a" -> FULL_BRIEF;
      case "direct", "no-brief", "without-brief", "b" -> DIRECT;
      case "conditional", "conditional-brief", "brief-if-needed", "c" -> CONDITIONAL_BRIEF;
      default -> CONDITIONAL_BRIEF;
    };
  }

  public boolean alwaysUseBrief() {
    return this == FULL_BRIEF;
  }

  public boolean neverUseBrief() {
    return this == DIRECT;
  }

  public boolean conditionalBrief() {
    return this == CONDITIONAL_BRIEF;
  }
}
