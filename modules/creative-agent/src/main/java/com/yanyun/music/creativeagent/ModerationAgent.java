package com.yanyun.music.creativeagent;

public interface ModerationAgent {

  ModerationAgentResult preCheck(ModerationAgentRequest request);
}
