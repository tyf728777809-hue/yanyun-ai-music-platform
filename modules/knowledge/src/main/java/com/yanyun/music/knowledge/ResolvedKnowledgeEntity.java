package com.yanyun.music.knowledge;

import java.util.Locale;

public record ResolvedKnowledgeEntity(
    String entityId,
    String canonicalName,
    String category,
    String matchedText,
    KnowledgeEntityMatchKind matchKind,
    double confidence,
    boolean ambiguous) {

  public ResolvedKnowledgeEntity {
    matchKind = matchKind == null ? KnowledgeEntityMatchKind.ALIAS : matchKind;
    confidence = Math.max(0.0d, Math.min(1.0d, confidence));
  }

  public boolean usableForRetrieval() {
    return !ambiguous && entityId != null && !entityId.isBlank() && confidence >= 0.85d;
  }

  public String promptLabel() {
    String name =
        canonicalName == null || canonicalName.isBlank() ? "(unknown)" : canonicalName.trim();
    String matched = matchedText == null || matchedText.isBlank() ? name : matchedText.trim();
    return "%s/%s matched=%s kind=%s confidence=%.2f%s"
        .formatted(
            category == null || category.isBlank() ? "entity" : category.trim(),
            name,
            matched,
            matchKind.name().toLowerCase(Locale.ROOT),
            confidence,
            ambiguous ? " ambiguous=true" : "");
  }
}
