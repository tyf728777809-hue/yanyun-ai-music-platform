package com.yanyun.music.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NoopKnowledgeServiceTest {

  @Test
  void returnsDisabledVersionAndNoReferences() {
    NoopKnowledgeService service = new NoopKnowledgeService();

    KnowledgeRetrievalResult result =
        service.retrieve(new KnowledgeRetrievalRequest("雁门雪夜", List.of("yanyun"), 3));

    assertEquals("disabled", result.kbVersion());
    assertTrue(result.references().isEmpty());
  }
}
