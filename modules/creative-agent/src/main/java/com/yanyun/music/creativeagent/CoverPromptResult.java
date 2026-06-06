package com.yanyun.music.creativeagent;

import java.util.List;
import java.util.Map;

public record CoverPromptResult(
    String visualPrompt,
    String negativePrompt,
    int width,
    int height,
    List<String> styleConstraints,
    Map<String, Object> providerOptions) {

  public CoverPromptResult {
    if (visualPrompt == null || visualPrompt.isBlank()) {
      throw new IllegalArgumentException("visualPrompt is required");
    }
    width = width <= 0 ? 1920 : width;
    height = height <= 0 ? 1080 : height;
    negativePrompt = negativePrompt == null ? "" : negativePrompt.trim();
    styleConstraints =
        styleConstraints == null || styleConstraints.isEmpty()
            ? List.of("16:9 composition", "no text overlay", "yanyun-inspired")
            : List.copyOf(styleConstraints);
    providerOptions = providerOptions == null ? Map.of() : Map.copyOf(providerOptions);
  }
}
