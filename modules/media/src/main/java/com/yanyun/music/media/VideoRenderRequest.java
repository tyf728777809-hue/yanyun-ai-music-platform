package com.yanyun.music.media;

public record VideoRenderRequest(
    String workId,
    String songTitle,
    String lyricsText,
    String audioObjectKey,
    String audioMimeType,
    String coverObjectKey,
    Integer durationMs) {

  public VideoRenderRequest {
    if (workId == null || workId.isBlank()) {
      throw new IllegalArgumentException("workId is required");
    }
    if (audioObjectKey == null || audioObjectKey.isBlank()) {
      throw new IllegalArgumentException("audioObjectKey is required");
    }
    if (coverObjectKey == null || coverObjectKey.isBlank()) {
      throw new IllegalArgumentException("coverObjectKey is required");
    }
  }
}
