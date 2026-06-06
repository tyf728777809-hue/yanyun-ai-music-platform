package com.yanyun.music.creativeagent;

import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import java.util.Map;

public final class MockCoverPromptAgent implements CoverPromptAgent {

  private static final String AGENT_NAME = "CoverPromptAgent";
  private static final String AGENT_VERSION = "v0.1";
  private static final String MODEL_NAME = "mock-cover-prompt";
  private static final String TEMPLATE_KEY = "cover.prompt.v1";
  private static final int TEMPLATE_VERSION = 1;

  private final AgentRunRecorder agentRunRecorder;

  public MockCoverPromptAgent() {
    this(NoopAgentRunRecorder.INSTANCE);
  }

  public MockCoverPromptAgent(AgentRunRecorder agentRunRecorder) {
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
  }

  @Override
  public CoverPromptResult generate(CoverPromptRequest request) {
    long startedAt = System.nanoTime();
    try {
      CoverPromptResult result = generateResult(request);
      record(request, result, startedAt, null);
      return result;
    } catch (RuntimeException exception) {
      record(request, null, startedAt, exception);
      throw exception;
    }
  }

  private CoverPromptResult generateResult(CoverPromptRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    String title = firstNonBlank(request.songTitle(), "Yanyun AI Music Cover");
    String seed =
        firstNonBlank(
            request.coverPromptSeed(),
            firstNonBlank(request.songSummary(), firstNonBlank(request.lyricsText(), title)));
    String prompt =
        "16:9 cinematic key art for song '%s', yanyun-inspired ancient frontier, %s, music mood: %s"
            .formatted(
                title,
                trimToLength(seed.replaceAll("[\\r\\n\\t]+", " "), 120),
                firstNonBlank(request.musicPrompt(), "ancient chinese folk pop"));
    return new CoverPromptResult(
        prompt,
        "low quality, blurry, watermark, text, logo",
        safeSize(request.width(), 1920),
        safeSize(request.height(), 1080),
        java.util.List.of("16:9 composition", "no text overlay", "cover-safe subject framing"),
        Map.of(
            "agent",
            AGENT_NAME,
            "agent_version",
            AGENT_VERSION,
            "prompt_template_key",
            TEMPLATE_KEY,
            "prompt_template_version",
            TEMPLATE_VERSION));
  }

  private void record(
      CoverPromptRequest request,
      CoverPromptResult result,
      long startedAt,
      RuntimeException exception) {
    agentRunRecorder.record(
        new AgentRunRecord(
            request == null ? null : request.workId(),
            null,
            AGENT_NAME,
            AGENT_VERSION,
            "COVER_PROMPT",
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
            exception == null ? null : "COVER_PROMPT_AGENT_FAILED",
            exception == null ? null : exception.getMessage()));
  }

  private String inputFingerprint(CoverPromptRequest request) {
    return String.join(
        "\n",
        nullToEmpty(request.workId()),
        nullToEmpty(request.songTitle()),
        nullToEmpty(request.songSummary()),
        nullToEmpty(request.lyricsText()),
        nullToEmpty(request.musicPrompt()),
        nullToEmpty(request.coverPromptSeed()),
        request.width() == null ? "" : request.width().toString(),
        request.height() == null ? "" : request.height().toString());
  }

  private String outputFingerprint(CoverPromptResult result) {
    return String.join(
        "\n",
        result.visualPrompt(),
        result.negativePrompt(),
        Integer.toString(result.width()),
        Integer.toString(result.height()),
        result.styleConstraints().toString(),
        result.providerOptions().toString());
  }

  private int elapsedMs(long startedAt) {
    long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
    return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, elapsed);
  }

  private int safeSize(Integer value, int fallback) {
    return value == null || value <= 0 ? fallback : value;
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private String trimToLength(String value, int maxLength) {
    String trimmed = value == null ? "" : value.trim();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }
}
