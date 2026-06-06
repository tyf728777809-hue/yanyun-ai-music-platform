package com.yanyun.music.creativeagent;

import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import java.util.List;
import java.util.Map;

public final class MockModerationAgent implements ModerationAgent {

  private static final String AGENT_NAME = "ModerationAgent";
  private static final String AGENT_VERSION = "v0.1";
  private static final String MODEL_NAME = "mock-moderation-agent";
  private static final String TEMPLATE_KEY = "moderation.agent.v1";
  private static final int TEMPLATE_VERSION = 1;

  private final AgentRunRecorder agentRunRecorder;

  public MockModerationAgent() {
    this(NoopAgentRunRecorder.INSTANCE);
  }

  public MockModerationAgent(AgentRunRecorder agentRunRecorder) {
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
  }

  @Override
  public ModerationAgentResult preCheck(ModerationAgentRequest request) {
    long startedAt = System.nanoTime();
    try {
      ModerationAgentResult result = preCheckResult(request);
      record(request, result, startedAt, null);
      return result;
    } catch (RuntimeException exception) {
      record(request, null, startedAt, exception);
      throw exception;
    }
  }

  private ModerationAgentResult preCheckResult(ModerationAgentRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    if (request.text() != null && request.text().contains("[BLOCK]")) {
      return new ModerationAgentResult(
          request.target(),
          ModerationAgentDecision.BLOCK,
          List.of("MOCK_AGENT_BLOCKED"),
          "Mock moderation agent blocked content.",
          "RETURN_TO_EDIT",
          metadata(request.target()));
    }
    return new ModerationAgentResult(
        request.target(),
        ModerationAgentDecision.PASS,
        List.of(),
        null,
        "PASS",
        metadata(request.target()));
  }

  private Map<String, Object> metadata(ModerationTarget target) {
    return Map.of(
        "agent",
        AGENT_NAME,
        "agent_version",
        AGENT_VERSION,
        "target",
        target.name(),
        "prompt_template_key",
        TEMPLATE_KEY,
        "prompt_template_version",
        TEMPLATE_VERSION);
  }

  private void record(
      ModerationAgentRequest request,
      ModerationAgentResult result,
      long startedAt,
      RuntimeException exception) {
    agentRunRecorder.record(
        new AgentRunRecord(
            request == null ? null : request.workId(),
            null,
            AGENT_NAME,
            AGENT_VERSION,
            request == null ? "UNKNOWN" : request.target().operationName(),
            MODEL_NAME,
            TEMPLATE_KEY,
            TEMPLATE_VERSION,
            request == null ? null : AgentRunHashing.sha256(inputFingerprint(request)),
            result == null ? null : AgentRunHashing.sha256(outputFingerprint(result)),
            exception == null ? AgentRunStatus.SUCCEEDED : AgentRunStatus.FAILED,
            elapsedMs(startedAt),
            null,
            null,
            null,
            exception == null ? null : "MODERATION_AGENT_FAILED",
            exception == null ? null : exception.getMessage()));
  }

  private String inputFingerprint(ModerationAgentRequest request) {
    return String.join(
        "\n",
        nullToEmpty(request.workId()),
        request.target().name(),
        nullToEmpty(request.text()),
        request.context().toString());
  }

  private String outputFingerprint(ModerationAgentResult result) {
    return String.join(
        "\n",
        result.target().name(),
        result.decision().name(),
        result.riskCodes().toString(),
        nullToEmpty(result.message()),
        nullToEmpty(result.recommendedAction()),
        result.metadata().toString());
  }

  private int elapsedMs(long startedAt) {
    long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
    return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, elapsed);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
