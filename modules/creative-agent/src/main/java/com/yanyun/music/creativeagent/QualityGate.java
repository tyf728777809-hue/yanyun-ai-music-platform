package com.yanyun.music.creativeagent;

public enum QualityGate {
  LYRICS("LYRICS_QUALITY_GATE"),
  MUSIC("MUSIC_QUALITY_GATE"),
  COVER("COVER_QUALITY_GATE"),
  VIDEO("VIDEO_QUALITY_GATE"),
  PUBLISH_PACKAGE("PACKAGE_QUALITY_GATE");

  private final String operationName;

  QualityGate(String operationName) {
    this.operationName = operationName;
  }

  public String operationName() {
    return operationName;
  }
}
