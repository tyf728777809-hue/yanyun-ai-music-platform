package com.yanyun.music.creativeagent;

import java.util.Map;

public record MusicPromptResult(String musicPrompt, Map<String, Object> providerOptions) {

  public MusicPromptResult {
    if (musicPrompt == null || musicPrompt.isBlank()) {
      throw new IllegalArgumentException("musicPrompt is required");
    }
    providerOptions = providerOptions == null ? Map.of() : Map.copyOf(providerOptions);
  }
}
