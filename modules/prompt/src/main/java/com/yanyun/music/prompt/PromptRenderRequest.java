package com.yanyun.music.prompt;

import java.util.List;

public record PromptRenderRequest(
    String templateKey,
    String operation,
    String userInput,
    String currentLyrics,
    String instruction,
    String musicStyle,
    String vocalPreference,
    List<String> knowledgeReferences) {

  public PromptRenderRequest {
    knowledgeReferences =
        knowledgeReferences == null ? List.of() : List.copyOf(knowledgeReferences);
  }
}
