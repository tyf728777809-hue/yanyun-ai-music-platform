package com.yanyun.music.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DeterministicKnowledgeEmbeddingServiceTest {

  @Test
  void embeddingIsStableAndNormalized() {
    DeterministicKnowledgeEmbeddingService service = new DeterministicKnowledgeEmbeddingService();

    double[] first = service.embed("寒香寻 不羡仙 归家");
    double[] second = service.embed("寒香寻 不羡仙 归家");

    assertEquals(DeterministicKnowledgeEmbeddingService.DIMENSIONS, first.length);
    assertEquals(DeterministicKnowledgeEmbeddingService.DIMENSIONS, second.length);
    double norm = 0.0d;
    for (int i = 0; i < first.length; i++) {
      assertEquals(first[i], second[i]);
      norm += first[i] * first[i];
    }
    assertTrue(Math.abs(1.0d - norm) < 0.000001d);
  }

  @Test
  void vectorLiteralUsesPgvectorSyntax() {
    String literal = JdbcKnowledgeService.vectorLiteral(new double[] {1.0d, 0.5d, -0.25d});

    assertEquals("[1.00000000,0.50000000,-0.25000000]", literal);
  }
}
