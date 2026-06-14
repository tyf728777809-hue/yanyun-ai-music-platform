package com.yanyun.music.knowledge;

import java.util.List;

public record KnowledgeRetrievalResult(
    String kbVersion,
    List<KnowledgeReference> references,
    List<ResolvedKnowledgeEntity> resolvedEntities) {

  public KnowledgeRetrievalResult(String kbVersion, List<KnowledgeReference> references) {
    this(kbVersion, references, List.of());
  }

  public KnowledgeRetrievalResult {
    references = references == null ? List.of() : List.copyOf(references);
    resolvedEntities = resolvedEntities == null ? List.of() : List.copyOf(resolvedEntities);
  }
}
