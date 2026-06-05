package com.yanyun.music.knowledge;

import java.util.List;

public record KnowledgeRetrievalRequest(String query, List<String> tags, int limit) {

  public KnowledgeRetrievalRequest {
    tags = tags == null ? List.of() : List.copyOf(tags);
    limit = Math.max(1, limit);
  }
}
