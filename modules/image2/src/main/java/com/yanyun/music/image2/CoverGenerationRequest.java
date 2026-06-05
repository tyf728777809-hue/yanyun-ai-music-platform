package com.yanyun.music.image2;

public record CoverGenerationRequest(
    String workId, String songTitle, String songSummary, String lyricsText, String musicPrompt) {

  public CoverGenerationRequest {
    if (workId == null || workId.isBlank()) {
      throw new IllegalArgumentException("workId is required");
    }
  }
}
