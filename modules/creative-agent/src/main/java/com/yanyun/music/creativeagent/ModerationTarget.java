package com.yanyun.music.creativeagent;

public enum ModerationTarget {
  USER_INPUT("USER_INPUT_PRECHECK"),
  LYRICS("LYRICS_PRECHECK"),
  MUSIC_PROMPT("MUSIC_PROMPT_PRECHECK"),
  COVER_PROMPT("COVER_PROMPT_PRECHECK"),
  PUBLISH_PACKAGE("PUBLISH_PACKAGE_PRECHECK");

  private final String operationName;

  ModerationTarget(String operationName) {
    this.operationName = operationName;
  }

  public String operationName() {
    return operationName;
  }
}
