package com.yanyun.music.creativeagent;

import java.util.List;
import java.util.Map;

public record CoverPromptResult(
    String visualPrompt,
    String negativePrompt,
    int width,
    int height,
    List<String> styleConstraints,
    Map<String, Object> providerOptions,
    String textPrompt,
    List<String> typographyRequirements) {

  public CoverPromptResult(
      String visualPrompt,
      String negativePrompt,
      int width,
      int height,
      List<String> styleConstraints,
      Map<String, Object> providerOptions) {
    this(
        visualPrompt,
        negativePrompt,
        width,
        height,
        styleConstraints,
        providerOptions,
        null,
        List.of());
  }

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
    textPrompt = textPrompt == null || textPrompt.isBlank() ? null : textPrompt.trim();
    typographyRequirements =
        typographyRequirements == null ? List.of() : List.copyOf(typographyRequirements);
  }
}
