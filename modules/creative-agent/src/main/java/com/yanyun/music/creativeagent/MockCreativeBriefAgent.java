package com.yanyun.music.creativeagent;

import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import java.util.List;
import java.util.Locale;

public final class MockCreativeBriefAgent implements CreativeBriefAgent {

  private static final String AGENT_NAME = "CreativeBriefAgent";
  private static final String AGENT_VERSION = "v0.1";
  private static final String MODEL_NAME = "mock-creative-brief";
  private static final String TEMPLATE_KEY = "creative.brief.v1";
  private static final int TEMPLATE_VERSION = 1;

  private final AgentRunRecorder agentRunRecorder;

  public MockCreativeBriefAgent() {
    this(NoopAgentRunRecorder.INSTANCE);
  }

  public MockCreativeBriefAgent(AgentRunRecorder agentRunRecorder) {
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
  }

  @Override
  public CreativeBriefResult generate(CreativeBriefRequest request) {
    long startedAt = System.nanoTime();
    try {
      CreativeBriefResult result = generateResult(request);
      record(request, result, startedAt, null);
      return result;
    } catch (RuntimeException exception) {
      record(request, null, startedAt, exception);
      throw exception;
    }
  }

  private CreativeBriefResult generateResult(CreativeBriefRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    String seed =
        firstNonBlank(
            request.userInput(),
            firstNonBlank(request.instruction(), firstNonBlank(request.currentLyrics(), "Yanyun")));
    String theme = trimToLength(seed.replaceAll("[\\r\\n\\t]+", " "), 72);
    String musicDirection = firstNonBlank(request.musicStyle(), "ancient chinese folk pop");
    List<String> moodTags = moodTags(musicDirection, seed);
    return new CreativeBriefResult(
        "Shape a song from " + trimToLength(seed.replaceAll("[\\r\\n\\t]+", " "), 96) + ".",
        theme,
        moodTags,
        narrativeViewpoint(request.operation()),
        musicDirection,
        request.yanyunReferences(),
        List.of(
            "keep lyrics singable", "preserve yanyun tone", "avoid unsupported real-person claims"),
        List.of());
  }

  private List<String> moodTags(String musicDirection, String seed) {
    String normalized = (musicDirection + " " + seed).toLowerCase(Locale.ROOT);
    if (normalized.contains("rock") || normalized.contains("摇滚")) {
      return List.of("heroic", "driving");
    }
    if (normalized.contains("sad")
        || normalized.contains("tear")
        || normalized.contains("悲")
        || normalized.contains("泪")) {
      return List.of("melancholic", "restrained");
    }
    return List.of("cinematic", "warm");
  }

  private String narrativeViewpoint(String operation) {
    if ("LYRICS".equalsIgnoreCase(operation)) {
      return "user-lyrics-centered";
    }
    if ("POLISH".equalsIgnoreCase(operation) || "CONTINUE".equalsIgnoreCase(operation)) {
      return "revision-centered";
    }
    return "story-centered";
  }

  private void record(
      CreativeBriefRequest request,
      CreativeBriefResult result,
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
            exception == null ? null : "CREATIVE_BRIEF_AGENT_FAILED",
            exception == null ? null : exception.getMessage()));
  }

  private String inputFingerprint(CreativeBriefRequest request) {
    return String.join(
        "\n",
        nullToEmpty(request.userId()),
        nullToEmpty(request.workId()),
        nullToEmpty(request.operation()),
        nullToEmpty(request.userInput()),
        nullToEmpty(request.currentLyrics()),
        nullToEmpty(request.instruction()),
        nullToEmpty(request.requestedTitle()),
        nullToEmpty(request.musicStyle()),
        nullToEmpty(request.vocalPreference()),
        request.yanyunReferences().toString());
  }

  private String outputFingerprint(CreativeBriefResult result) {
    return String.join(
        "\n",
        result.userIntentSummary(),
        result.theme(),
        result.moodTags().toString(),
        result.narrativeViewpoint(),
        result.musicDirection(),
        result.yanyunReferences().toString(),
        result.constraints().toString(),
        result.riskNotes().toString());
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

  private String trimToLength(String value, int maxLength) {
    String trimmed = value == null ? "" : value.trim();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }
}
