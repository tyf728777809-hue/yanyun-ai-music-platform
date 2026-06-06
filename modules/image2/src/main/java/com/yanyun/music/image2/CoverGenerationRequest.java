package com.yanyun.music.image2;

import java.util.Map;

public record CoverGenerationRequest(
    String workId,
    String songTitle,
    String songSummary,
    String lyricsText,
    String musicPrompt,
    String visualPrompt,
    String negativePrompt,
    int width,
    int height,
    Map<String, Object> providerOptions) {

  public CoverGenerationRequest {
    if (workId == null || workId.isBlank()) {
      throw new IllegalArgumentException("workId is required");
    }
    width = width <= 0 ? 1920 : width;
    height = height <= 0 ? 1080 : height;
    providerOptions = providerOptions == null ? Map.of() : Map.copyOf(providerOptions);
  }
}
