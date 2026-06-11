package com.yanyun.music.creativeagent;

import java.util.Map;

public record MusicPromptResult(
    String musicPrompt,
    Map<String, Object> providerOptions,
    String title,
    String lyricsWithStructureTags,
    String stylePrompt,
    String excludePrompt,
    Map<String, Object> advancedOptions) {

  public MusicPromptResult(String musicPrompt, Map<String, Object> providerOptions) {
    this(musicPrompt, providerOptions, null, null, musicPrompt, null, Map.of());
  }

  public MusicPromptResult {
    musicPrompt = firstNonBlank(musicPrompt, stylePrompt);
    if (musicPrompt == null || musicPrompt.isBlank()) {
      throw new IllegalArgumentException("musicPrompt is required");
    }
    providerOptions = providerOptions == null ? Map.of() : Map.copyOf(providerOptions);
    title = blankToNull(title);
    lyricsWithStructureTags = blankToNull(lyricsWithStructureTags);
    stylePrompt = firstNonBlank(stylePrompt, musicPrompt);
    excludePrompt = blankToNull(excludePrompt);
    advancedOptions = advancedOptions == null ? Map.of() : Map.copyOf(advancedOptions);
  }

  private static String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
