package com.yanyun.music.knowledge;

import java.util.Locale;

public enum KnowledgeSourceClass {
  CONFIRMED_FACTS,
  CREATIVE_MATERIALS,
  PENDING_CLUES;

  public static KnowledgeSourceClass fromFactLevel(String factLevel) {
    if (factLevel == null || factLevel.isBlank()) {
      return PENDING_CLUES;
    }
    return switch (factLevel.trim().toLowerCase(Locale.ROOT)) {
      case "official_public_seed", "in_game_confirmed" -> CONFIRMED_FACTS;
      case "accepted_story_synthesis", "creative_guidance", "creative_boundary" ->
          CREATIVE_MATERIALS;
      default -> PENDING_CLUES;
    };
  }

  public boolean promptInjectable() {
    return true;
  }
}
