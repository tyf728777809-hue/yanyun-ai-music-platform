package com.yanyun.music.knowledge;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public final class JdbcKnowledgeService implements KnowledgeService {

  private final JdbcTemplate jdbcTemplate;
  private final KnowledgeProperties properties;
  private final KnowledgeEmbeddingService embeddingService;

  public JdbcKnowledgeService(
      JdbcTemplate jdbcTemplate,
      KnowledgeProperties properties,
      KnowledgeEmbeddingService embeddingService) {
    if (jdbcTemplate == null) {
      throw new IllegalArgumentException("jdbcTemplate is required");
    }
    this.jdbcTemplate = jdbcTemplate;
    this.properties = properties == null ? new KnowledgeProperties() : properties;
    this.embeddingService =
        embeddingService == null ? new DeterministicKnowledgeEmbeddingService() : embeddingService;
  }

  @Override
  public KnowledgeRetrievalResult retrieve(KnowledgeRetrievalRequest request) {
    if (request == null) {
      return new KnowledgeRetrievalResult(properties.getKbVersion(), List.of());
    }
    try {
      Map<String, KnowledgeReference> references = new LinkedHashMap<>();
      List<String> entityIds = entityIds(request);
      for (KnowledgeReference reference : entityReferences(entityIds, request.limit())) {
        references.put(reference.chunkId(), reference);
      }
      int remaining =
          Math.max(0, Math.min(request.limit(), properties.getMaxReferences()) - references.size());
      if (remaining > 0) {
        for (KnowledgeReference reference : semanticReferences(request, remaining)) {
          references.putIfAbsent(reference.chunkId(), reference);
        }
      }
      return new KnowledgeRetrievalResult(
          properties.getKbVersion(),
          references.values().stream()
              .limit(Math.min(request.limit(), properties.getMaxReferences()))
              .toList());
    } catch (DataAccessException exception) {
      return new KnowledgeRetrievalResult(properties.getKbVersion() + ":unavailable", List.of());
    }
  }

  private List<String> entityIds(KnowledgeRetrievalRequest request) {
    String normalizedQuery = DeterministicKnowledgeEmbeddingService.normalize(request.query());
    if (normalizedQuery.isBlank()) {
      return List.of();
    }
    String sql =
        """
        SELECT e.id::text AS id, max(length(a.normalized_alias)) AS alias_length
        FROM knowledge_entity_aliases a
        JOIN knowledge_entities e ON e.id = a.entity_id
        WHERE e.kb_version = ?
          AND ? LIKE '%' || a.normalized_alias || '%'
        GROUP BY e.id
        ORDER BY alias_length DESC
        LIMIT ?
        """;
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> rs.getString("id"),
        properties.getKbVersion(),
        normalizedQuery,
        properties.getEntityLimit());
  }

  private List<KnowledgeReference> entityReferences(List<String> entityIds, int limit) {
    if (entityIds == null || entityIds.isEmpty()) {
      return List.of();
    }
    List<KnowledgeReference> references = new ArrayList<>();
    String sql =
        """
        SELECT c.id::text AS chunk_id,
               COALESCE(c.entity_name, e.canonical_name, d.title, c.heading_path, c.id::text) AS title,
               d.file_path,
               c.heading_path,
               c.summary_for_prompt,
               c.emotional_arc,
               c.usable_imagery_json::text AS usable_imagery_json,
               c.avoid_claims_json::text AS avoid_claims_json,
               c.content,
               c.chunk_index,
               c.fact_level
        FROM knowledge_chunks c
        JOIN knowledge_documents d ON d.id = c.document_id
        LEFT JOIN knowledge_entities e ON e.id = c.entity_id
        WHERE c.kb_version = ?
          AND c.entity_id::text = ?
        ORDER BY c.chunk_index ASC
        LIMIT ?
        """;
    int perEntityLimit = Math.max(1, (int) Math.ceil((double) limit / entityIds.size()));
    for (String entityId : entityIds) {
      references.addAll(
          jdbcTemplate.query(
              sql, referenceMapper(), properties.getKbVersion(), entityId, perEntityLimit));
      if (references.size() >= limit) {
        break;
      }
    }
    return references.stream().limit(limit).toList();
  }

  private List<KnowledgeReference> semanticReferences(
      KnowledgeRetrievalRequest request, int limit) {
    double[] embedding = embeddingService.embed(queryText(request));
    String sql =
        """
        SELECT c.id::text AS chunk_id,
               COALESCE(c.entity_name, d.title, c.heading_path, c.id::text) AS title,
               d.file_path,
               c.heading_path,
               c.summary_for_prompt,
               c.emotional_arc,
               c.usable_imagery_json::text AS usable_imagery_json,
               c.avoid_claims_json::text AS avoid_claims_json,
               c.content,
               c.chunk_index,
               c.fact_level
        FROM knowledge_chunks c
        JOIN knowledge_documents d ON d.id = c.document_id
        WHERE c.kb_version = ?
          AND c.embedding IS NOT NULL
        ORDER BY c.embedding <=> ?::vector
        LIMIT ?
        """;
    return jdbcTemplate.query(
        sql, referenceMapper(), properties.getKbVersion(), vectorLiteral(embedding), limit);
  }

  private String queryText(KnowledgeRetrievalRequest request) {
    return String.join(
        "\n",
        Objects.toString(request.query(), ""),
        request.tags() == null ? "" : String.join(",", request.tags()));
  }

  private RowMapper<KnowledgeReference> referenceMapper() {
    return (rs, rowNum) ->
        new KnowledgeReference(
            rs.getString("chunk_id"),
            rs.getString("title"),
            rs.getString("file_path"),
            rs.getString("heading_path"),
            promptContent(rs),
            rs.getString("fact_level"),
            KnowledgeSourceClass.fromFactLevel(rs.getString("fact_level")));
  }

  private String promptContent(ResultSet rs) throws SQLException {
    List<String> sections = new ArrayList<>();
    addSourceClassGuidance(sections, rs.getString("fact_level"));
    addSection(sections, "创作摘要", rs.getString("summary_for_prompt"));
    addSection(sections, "情绪弧线", rs.getString("emotional_arc"));
    addSection(sections, "可用意象", compactJson(rs.getString("usable_imagery_json")));
    addSection(sections, "禁写误区", compactJson(rs.getString("avoid_claims_json")));
    if (sections.isEmpty()) {
      addSection(sections, "资料摘要", rs.getString("content"));
    }
    return String.join("\n", sections);
  }

  private void addSourceClassGuidance(List<String> sections, String factLevel) {
    if (KnowledgeSourceClass.fromFactLevel(factLevel) == KnowledgeSourceClass.PENDING_CLUES) {
      sections.add("资料口径: 待核创作线索，可作为暗线、传闻、情绪或意象使用，不要写成官方定论。");
    }
  }

  private void addSection(List<String> sections, String label, String value) {
    if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value.trim())) {
      sections.add(label + ": " + value.trim());
    }
  }

  private String compactJson(String value) {
    if (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) {
      return "";
    }
    return value
        .replace('"', ' ')
        .replace('[', ' ')
        .replace(']', ' ')
        .trim()
        .replaceAll("\\s+", " ");
  }

  public static String vectorLiteral(double[] vector) {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < vector.length; i++) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append(String.format(java.util.Locale.ROOT, "%.8f", vector[i]));
    }
    return builder.append(']').toString();
  }
}
