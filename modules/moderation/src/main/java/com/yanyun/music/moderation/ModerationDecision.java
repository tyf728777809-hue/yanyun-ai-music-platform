package com.yanyun.music.moderation;

public record ModerationDecision(boolean allowed, String code, String message) {

  public static ModerationDecision allow() {
    return new ModerationDecision(true, null, null);
  }

  public static ModerationDecision blocked(String code, String message) {
    return new ModerationDecision(false, code, message);
  }
}
