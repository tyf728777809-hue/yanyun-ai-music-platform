package com.yanyun.music.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class JdbcKnowledgeServiceTest {

  @Test
  void retrievesAllKnowledgeLevelsAndKeepsFactLevelInResultProjection() {
    CapturingJdbcTemplate jdbcTemplate = new CapturingJdbcTemplate();
    KnowledgeProperties properties = new KnowledgeProperties();
    properties.setRetrievalMode("pgvector");
    properties.setKbVersion("kb-test");
    properties.setMaxReferences(3);
    properties.setEntityLimit(1);

    JdbcKnowledgeService service =
        new JdbcKnowledgeService(
            jdbcTemplate, properties, new DeterministicKnowledgeEmbeddingService());

    KnowledgeRetrievalResult result =
        service.retrieve(new KnowledgeRetrievalRequest("寻心 弱水岸", List.of(), 3));

    assertEquals("kb-test", result.kbVersion());
    assertEquals(3, jdbcTemplate.sqlStatements.size());
    assertTrue(jdbcTemplate.sqlStatements.get(0).contains("knowledge_entity_aliases"));
    assertTrue(!jdbcTemplate.sqlStatements.get(0).contains("e.fact_level NOT IN"));
    assertTrue(jdbcTemplate.sqlStatements.get(1).contains("c.fact_level"));
    assertTrue(!jdbcTemplate.sqlStatements.get(1).contains("needs_ingame_recording"));
    assertTrue(jdbcTemplate.sqlStatements.get(2).contains("c.fact_level"));
    assertTrue(!jdbcTemplate.sqlStatements.get(2).contains("pending_clues"));
  }

  @Test
  void spreadsEntityReferencesAcrossMatchedEntities() {
    CapturingJdbcTemplate jdbcTemplate = new CapturingJdbcTemplate("entity-a", "entity-b");
    KnowledgeProperties properties = new KnowledgeProperties();
    properties.setRetrievalMode("pgvector");
    properties.setKbVersion("kb-test");
    properties.setMaxReferences(4);
    properties.setEntityLimit(2);

    JdbcKnowledgeService service =
        new JdbcKnowledgeService(
            jdbcTemplate, properties, new DeterministicKnowledgeEmbeddingService());

    service.retrieve(new KnowledgeRetrievalRequest("寒香寻 神仙渡", List.of(), 4));

    assertEquals(4, jdbcTemplate.sqlStatements.size());
    assertEquals(2, jdbcTemplate.args.get(1)[2]);
    assertEquals(2, jdbcTemplate.args.get(2)[2]);
  }

  private static final class CapturingJdbcTemplate extends JdbcTemplate {

    private final List<String> sqlStatements = new ArrayList<>();
    private final List<Object[]> args = new ArrayList<>();
    private final List<String> matchedEntityIds;

    private CapturingJdbcTemplate(String... matchedEntityIds) {
      this.matchedEntityIds = Arrays.asList(matchedEntityIds);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
      sqlStatements.add(sql);
      this.args.add(args);
      if (sql.contains("knowledge_entity_aliases")) {
        if (matchedEntityIds.isEmpty()) {
          return List.of((T) "entity-id");
        }
        return matchedEntityIds.stream().map(entityId -> (T) entityId).toList();
      }
      return List.of();
    }
  }
}
