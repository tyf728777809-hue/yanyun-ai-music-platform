package com.yanyun.music.creativeagent;

import java.util.List;

public record CreativeBriefRequest(
    String userId,
    String workId,
    String operation,
    String userInput,
    String currentLyrics,
    String instruction,
    String requestedTitle,
    String musicStyle,
    String vocalPreference,
    List<String> yanyunReferences) {

  public CreativeBriefRequest {
    yanyunReferences = yanyunReferences == null ? List.of() : List.copyOf(yanyunReferences);
  }
}
