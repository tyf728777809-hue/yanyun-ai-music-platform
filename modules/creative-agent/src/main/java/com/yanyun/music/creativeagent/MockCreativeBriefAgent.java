package com.yanyun.music.creativeagent;

import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class MockCreativeBriefAgent implements CreativeBriefAgent {

  private static final String AGENT_NAME = "CreativeBriefAgent";
  private static final String AGENT_VERSION = "v0.5";
  private static final String MODEL_NAME = "mock-creative-brief";
  private static final String TEMPLATE_KEY = "creative.brief.v5";
  private static final int TEMPLATE_VERSION = 5;
  private static final Set<String> OTHER_IP_TERMS =
      Set.of("高达", "gundam", "原神", "genshin", "星穹", "崩坏", "鸣潮", "王者荣耀", "火影", "海贼王");
  private static final Set<String> YANYUN_TERMS =
      Set.of("燕云", "十六声", "江湖", "武学", "奇术", "门派", "乱世", "家国", "寻声", "侠", "边城", "雁门");

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
    CreativeDomainDecision domainDecision = domainDecision(seed, request);
    String theme = trimToLength(seed.replaceAll("[\\r\\n\\t]+", " "), 72);
    String musicDirection = firstNonBlank(request.musicStyle(), "ancient chinese folk pop");
    List<String> moodTags = moodTags(musicDirection, seed);
    return new CreativeBriefResult(
        domainDecision,
        "Shape a song from " + trimToLength(seed.replaceAll("[\\r\\n\\t]+", " "), 96) + ".",
        theme,
        moodTags,
        narrativeViewpoint(request.operation()),
        musicDirection,
        request.yanyunReferences(),
        List.of(
            "lyrics content must belong to Yanyun Sixteen Sounds",
            "keep lyrics singable",
            "preserve yanyun tone",
            "do not use other IP names or lore in lyrics"),
        domainDecision == CreativeDomainDecision.PASS
            ? List.of()
            : List.of("creative_domain_requires_yanyun"),
        domainDecision == CreativeDomainDecision.REJECT
            ? "当前只支持燕云十六声相关创作。可以改成燕云里的江湖、武学、奇术、乱世同行或寻声记忆方向。"
            : null,
        domainDecision == CreativeDomainDecision.REWRITE_TO_YANYUN
            ? "保留用户想要的情绪和音乐风格，但把歌词内容转成燕云十六声语境，删除其他 IP 专属名词。"
            : null,
        List.of("keep player-specific story details when they fit Yanyun"));
  }

  private CreativeDomainDecision domainDecision(String seed, CreativeBriefRequest request) {
    String normalized =
        (seed
                + " "
                + nullToEmpty(request.currentLyrics())
                + " "
                + nullToEmpty(request.instruction())
                + " "
                + request.yanyunReferences())
            .toLowerCase(Locale.ROOT);
    boolean mentionsOtherIp =
        OTHER_IP_TERMS.stream()
            .anyMatch(term -> normalized.contains(term.toLowerCase(Locale.ROOT)));
    boolean mentionsYanyun =
        YANYUN_TERMS.stream().anyMatch(term -> normalized.contains(term.toLowerCase(Locale.ROOT)));
    if (mentionsOtherIp && !mentionsYanyun) {
      return CreativeDomainDecision.REJECT;
    }
    if (mentionsOtherIp || !mentionsYanyun) {
      return CreativeDomainDecision.REWRITE_TO_YANYUN;
    }
    return CreativeDomainDecision.PASS;
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
        result.domainDecision().name(),
        result.userIntentSummary(),
        result.theme(),
        result.moodTags().toString(),
        result.narrativeViewpoint(),
        result.musicDirection(),
        result.yanyunReferences().toString(),
        result.constraints().toString(),
        result.riskNotes().toString(),
        nullToEmpty(result.userFacingMessage()),
        nullToEmpty(result.yanyunRewriteSuggestion()),
        result.freeformOpportunities().toString());
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
