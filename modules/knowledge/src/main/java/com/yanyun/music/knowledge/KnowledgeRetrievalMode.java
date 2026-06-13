package com.yanyun.music.knowledge;

import java.util.Locale;

public enum KnowledgeRetrievalMode {
  DISABLED,
  MOCK,
  PGVECTOR;

  public static KnowledgeRetrievalMode from(String value) {
    if (value == null || value.isBlank()) {
      return DISABLED;
    }
    return switch (value.trim().toLowerCase(Locale.ROOT)) {
      case "pgvector" -> PGVECTOR;
      case "mock" -> MOCK;
      default -> DISABLED;
    };
  }
}
