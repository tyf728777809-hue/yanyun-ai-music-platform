package com.yanyun.music.knowledge;

import java.util.List;

public final class MockKnowledgeService implements KnowledgeService {

  public static final String KB_VERSION = "mock-yanyun-kb-v0";

  @Override
  public KnowledgeRetrievalResult retrieve(KnowledgeRetrievalRequest request) {
    int limit = Math.max(1, request.limit());
    List<KnowledgeReference> references =
        List.of(
            new KnowledgeReference(
                "mock-yanyun-001",
                "Yanyun frontier imagery",
                "mock/yanyun-frontier.md",
                "world/imagery",
                "Wind, border drums, moonlit streets, and old vows."),
            new KnowledgeReference(
                "mock-yanyun-002",
                "Yanyun ensemble motifs",
                "mock/yanyun-ensemble.md",
                "music/motifs",
                "Plucked strings, warm folk vocals, cinematic percussion, and chorus response."));
    return new KnowledgeRetrievalResult(KB_VERSION, references.stream().limit(limit).toList());
  }
}
