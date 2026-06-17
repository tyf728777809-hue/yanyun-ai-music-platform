package com.yanyun.music.creativeagent;

import java.util.List;

public record LyricsCraftPlanRequest(
    String userId,
    String workId,
    String operation,
    String userInput,
    String currentLyrics,
    String instruction,
    String requestedTitle,
    String mood,
    String musicStyle,
    String vocalPreference,
    CreativeBriefResult creativeBrief,
    List<String> yanyunReferences,
    List<String> resolvedEntities,
    List<String> knowledgeReferenceSummaries) {

  public LyricsCraftPlanRequest {
    yanyunReferences = yanyunReferences == null ? List.of() : List.copyOf(yanyunReferences);
    resolvedEntities = resolvedEntities == null ? List.of() : List.copyOf(resolvedEntities);
    knowledgeReferenceSummaries =
        knowledgeReferenceSummaries == null ? List.of() : List.copyOf(knowledgeReferenceSummaries);
  }
}
