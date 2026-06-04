package com.yanyun.music.musicprovider;

import java.util.Map;

public record MusicGenerationRequest(
    String workId,
    String lyricsText,
    String musicPrompt,
    String vocalPreference,
    Map<String, Object> providerOptions) {

  public MusicGenerationRequest {
    providerOptions = providerOptions == null ? Map.of() : Map.copyOf(providerOptions);
  }
}
