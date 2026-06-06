package com.yanyun.music.creativeagent;

import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import java.util.Locale;
import java.util.Map;

public final class MockMusicPromptAgent implements MusicPromptAgent {

  private static final String AGENT_NAME = "MusicPromptAgent";
  private static final String AGENT_VERSION = "v0.1";
  private static final String MODEL_NAME = "mock-music-prompt";
  private static final String TEMPLATE_KEY = "music.prompt.v1";
  private static final int TEMPLATE_VERSION = 1;

  private final AgentRunRecorder agentRunRecorder;

  public MockMusicPromptAgent() {
    this(NoopAgentRunRecorder.INSTANCE);
  }

  public MockMusicPromptAgent(AgentRunRecorder agentRunRecorder) {
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
  }

  @Override
  public MusicPromptResult generate(MusicPromptRequest request) {
    long startedAt = System.nanoTime();
    try {
      MusicPromptResult result = generateResult(request);
      record(request, result, startedAt, null);
      return result;
    } catch (RuntimeException exception) {
      record(request, null, startedAt, exception);
      throw exception;
    }
  }

  private MusicPromptResult generateResult(MusicPromptRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    String provider = firstNonBlank(request.musicProvider(), "MOCK").toUpperCase(Locale.ROOT);
    String vocal = firstNonBlank(request.vocalPreference(), "AUTO");
    String base =
        firstNonBlank(
            request.musicPromptSeed(),
            "ancient chinese folk pop, cinematic, yanyun-inspired, steady verse chorus");
    String titlePart =
        request.songTitle() == null || request.songTitle().isBlank()
            ? ""
            : ", title mood: " + request.songTitle().trim();
    String prompt =
        base.trim()
            + titlePart
            + ", provider profile: "
            + provider.toLowerCase(Locale.ROOT)
            + ", vocal: "
            + vocal.toLowerCase(Locale.ROOT);
    return new MusicPromptResult(
        prompt,
        Map.of(
            "agent",
            AGENT_NAME,
            "agent_version",
            AGENT_VERSION,
            "provider_profile",
            provider,
            "prompt_template_key",
            TEMPLATE_KEY,
            "prompt_template_version",
            TEMPLATE_VERSION));
  }

  private void record(
      MusicPromptRequest request,
      MusicPromptResult result,
      long startedAt,
      RuntimeException exception) {
    agentRunRecorder.record(
        new AgentRunRecord(
            request == null ? null : request.workId(),
            null,
            AGENT_NAME,
            AGENT_VERSION,
            "MUSIC_PROMPT",
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
            exception == null ? null : "MUSIC_PROMPT_AGENT_FAILED",
            exception == null ? null : exception.getMessage()));
  }

  private String inputFingerprint(MusicPromptRequest request) {
    return String.join(
        "\n",
        nullToEmpty(request.workId()),
        nullToEmpty(request.songTitle()),
        nullToEmpty(request.songSummary()),
        nullToEmpty(request.lyricsText()),
        nullToEmpty(request.musicPromptSeed()),
        nullToEmpty(request.vocalPreference()),
        nullToEmpty(request.musicProvider()));
  }

  private String outputFingerprint(MusicPromptResult result) {
    return result.musicPrompt() + "\n" + result.providerOptions().toString();
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
