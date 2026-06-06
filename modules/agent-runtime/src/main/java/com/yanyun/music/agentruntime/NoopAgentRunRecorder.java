package com.yanyun.music.agentruntime;

public final class NoopAgentRunRecorder implements AgentRunRecorder {

  public static final NoopAgentRunRecorder INSTANCE = new NoopAgentRunRecorder();

  private NoopAgentRunRecorder() {}

  @Override
  public void record(AgentRunRecord record) {
    // Intentionally empty for isolated unit tests and disabled audit paths.
  }
}
