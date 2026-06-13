package com.yanyun.music.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KnowledgeRetrievalModeTest {

  @Test
  void parsesKnownModesAndDefaultsToDisabled() {
    assertEquals(KnowledgeRetrievalMode.PGVECTOR, KnowledgeRetrievalMode.from("pgvector"));
    assertEquals(KnowledgeRetrievalMode.MOCK, KnowledgeRetrievalMode.from("mock"));
    assertEquals(KnowledgeRetrievalMode.DISABLED, KnowledgeRetrievalMode.from("disabled"));
    assertEquals(KnowledgeRetrievalMode.DISABLED, KnowledgeRetrievalMode.from("unexpected"));
    assertEquals(KnowledgeRetrievalMode.DISABLED, KnowledgeRetrievalMode.from(null));
  }
}
