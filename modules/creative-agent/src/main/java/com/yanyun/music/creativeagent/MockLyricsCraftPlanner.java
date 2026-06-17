package com.yanyun.music.creativeagent;

import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import java.util.List;

public final class MockLyricsCraftPlanner implements LyricsCraftPlanner {

  private static final String AGENT_NAME = "LyricsCraftPlanner";
  private static final String AGENT_VERSION = "mock-v0.9";
  private static final String MODEL_NAME = "mock-lyrics-craft-planner";
  private static final String TEMPLATE_KEY = "lyrics.craft.plan.v9";
  private static final int TEMPLATE_VERSION = 9;

  private final AgentRunRecorder agentRunRecorder;

  public MockLyricsCraftPlanner() {
    this(NoopAgentRunRecorder.INSTANCE);
  }

  public MockLyricsCraftPlanner(AgentRunRecorder agentRunRecorder) {
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
  }

  @Override
  public LyricsCraftPlanResult plan(LyricsCraftPlanRequest request) {
    long startedAt = System.nanoTime();
    try {
      LyricsCraftPlanResult result =
          new LyricsCraftPlanResult(
              "Keep one clear song thesis from the user's Yanyun inspiration.",
              "one repeatable concrete sound, object, or spoken habit",
              "enter through the user's smallest believable scene instead of a grand summary",
              "let the chorus repeat the device once, then make it mean more the second time",
              "Keep lyrics inside the Yanyun world; music style must not import modern props.",
              List.of("generic heroic slogan", "encyclopedia plot summary"));
      record(request, result, startedAt, null);
      return result;
    } catch (RuntimeException exception) {
      record(request, null, startedAt, exception);
      throw exception;
    }
  }

  private void record(
      LyricsCraftPlanRequest request,
      LyricsCraftPlanResult result,
      long startedAt,
      RuntimeException exception) {
    agentRunRecorder.record(
        new AgentRunRecord(
            request == null ? null : request.workId(),
            null,
            AGENT_NAME,
            AGENT_VERSION,
            request == null ? "UNKNOWN" : firstNonBlank(request.operation(), "UNKNOWN"),
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
            exception == null ? null : "LYRICS_CRAFT_PLAN_FAILED",
            exception == null ? null : exception.getMessage()));
  }

  private String inputFingerprint(LyricsCraftPlanRequest request) {
    return String.join(
        "\n",
        nullToEmpty(request.userId()),
        nullToEmpty(request.workId()),
        nullToEmpty(request.operation()),
        nullToEmpty(request.userInput()),
        nullToEmpty(request.instruction()),
        nullToEmpty(request.mood()),
        nullToEmpty(request.musicStyle()),
        nullToEmpty(request.vocalPreference()),
        request.yanyunReferences().toString(),
        request.resolvedEntities().toString(),
        request.knowledgeReferenceSummaries().toString());
  }

  private String outputFingerprint(LyricsCraftPlanResult result) {
    return String.join(
        "\n",
        result.songThesisGuard(),
        result.selectedDevice(),
        result.selectedAngle(),
        result.chorusMechanism(),
        result.yanyunBoundaryGuard(),
        result.rejectedAlternatives().toString());
  }

  private int elapsedMs(long startedAt) {
    long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
    return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, elapsed);
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
