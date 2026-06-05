package com.yanyun.music.deepseek;

import java.util.List;

public record DeepSeekLyricsRequest(
    String operation,
    String prompt,
    String userInput,
    String currentLyrics,
    String instruction,
    String requestedTitle,
    String musicStyle,
    String vocalPreference,
    List<String> yanyunReferences) {

  public DeepSeekLyricsRequest {
    yanyunReferences = yanyunReferences == null ? List.of() : List.copyOf(yanyunReferences);
  }
}
