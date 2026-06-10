package com.yanyun.music.knowledge;

import java.util.List;

public final class NoopKnowledgeService implements KnowledgeService {

  public static final String KB_VERSION = "disabled";

  @Override
  public KnowledgeRetrievalResult retrieve(KnowledgeRetrievalRequest request) {
    return new KnowledgeRetrievalResult(KB_VERSION, List.of());
  }
}
