package com.yanyun.music.knowledge;

import java.util.List;

public record KnowledgeRetrievalResult(String kbVersion, List<KnowledgeReference> references) {

  public KnowledgeRetrievalResult {
    references = references == null ? List.of() : List.copyOf(references);
  }
}
