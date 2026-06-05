package com.yanyun.music.dreammaker;

public record DreamMakerOutputFile(
    String name, String url, String coverUrl, String fileType, Integer durationMs) {

  public boolean isAudio() {
    return fileType == null || fileType.isBlank() || "audio".equalsIgnoreCase(fileType.trim());
  }
}
