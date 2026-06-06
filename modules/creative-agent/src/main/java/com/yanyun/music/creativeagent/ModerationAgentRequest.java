package com.yanyun.music.creativeagent;

import java.util.Map;

public record ModerationAgentRequest(
    String workId, ModerationTarget target, String text, Map<String, Object> context) {

  public ModerationAgentRequest {
    target = target == null ? ModerationTarget.MUSIC_PROMPT : target;
    context = context == null ? Map.of() : Map.copyOf(context);
  }
}
