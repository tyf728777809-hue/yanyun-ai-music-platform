package com.yanyun.music.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import com.yanyun.music.creativeagent.CreativeBriefAgent;
import com.yanyun.music.creativeagent.CreativeBriefRequest;
import com.yanyun.music.creativeagent.CreativeBriefResult;
import com.yanyun.music.creativeagent.CreativeDomainDecision;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RealDeepSeekCreativeBriefAgent implements CreativeBriefAgent {

  private static final String AGENT_NAME = "CreativeBriefAgent";
  private static final String AGENT_VERSION = "v0.5";
  private static final String TEMPLATE_KEY = "creative.brief.v5";
  private static final int TEMPLATE_VERSION = 5;
  private static final Set<String> OTHER_IP_TERMS =
      Set.of("高达", "gundam", "原神", "genshin", "星穹", "崩坏", "鸣潮", "王者荣耀", "火影", "海贼王");
  private static final Set<String> YANYUN_TERMS =
      Set.of("燕云", "十六声", "江湖", "武学", "奇术", "门派", "乱世", "家国", "寻声", "侠", "边城", "雁门");

  private final DeepSeekJsonChatClient client;
  private final AgentRunRecorder agentRunRecorder;

  public RealDeepSeekCreativeBriefAgent(
      DeepSeekProperties properties, ObjectMapper objectMapper, AgentRunRecorder agentRunRecorder) {
    this(new DeepSeekJsonChatClient(properties, objectMapper), agentRunRecorder);
  }

  RealDeepSeekCreativeBriefAgent(DeepSeekJsonChatClient client, AgentRunRecorder agentRunRecorder) {
    this.client = client;
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
  }

  @Override
  public CreativeBriefResult generate(CreativeBriefRequest request) {
    long startedAt = System.nanoTime();
    try {
      if (request == null) {
        throw new IllegalArgumentException("request is required");
      }
      CreativeDomainDecision deterministicDecision = deterministicDecision(request);
      CreativeBriefResult result =
          deterministicDecision == CreativeDomainDecision.REJECT
              ? deterministicReject(request)
              : parse(
                  client.completeJson(
                      systemPrompt(), userPrompt(request), BigDecimal.valueOf(0.45), 1600),
                  request,
                  deterministicDecision);
      record(request, result, startedAt, null);
      return result;
    } catch (RuntimeException exception) {
      record(request, null, startedAt, exception);
      throw exception;
    }
  }

  private CreativeBriefResult parse(
      JsonNode root, CreativeBriefRequest request, CreativeDomainDecision deterministicDecision) {
    CreativeDomainDecision modelDecision =
        DeepSeekAgentJson.creativeDomainDecision(
            DeepSeekAgentJson.text(root, "domain_decision", "domainDecision"));
    CreativeDomainDecision decision =
        deterministicDecision == CreativeDomainDecision.REWRITE_TO_YANYUN
                && modelDecision == CreativeDomainDecision.PASS
            ? CreativeDomainDecision.REWRITE_TO_YANYUN
            : modelDecision;
    return new CreativeBriefResult(
        decision,
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(
                root, "creative_intent", "user_intent_summary", "userIntentSummary"),
            "Shape a Yanyun song from the user request."),
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "theme"), "Yanyun player story"),
        DeepSeekAgentJson.stringList(
            root.path("mood_tags").isMissingNode()
                ? root.path("moodTags")
                : root.path("mood_tags")),
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "narrative_viewpoint", "narrativeViewpoint"),
            "player-facing"),
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "music_direction", "musicDirection"),
            request.musicStyle()),
        DeepSeekAgentJson.stringList(
            root.path("yanyun_references").isMissingNode()
                ? root.path("yanyunReferences")
                : root.path("yanyun_references")),
        DeepSeekAgentJson.stringList(root.path("constraints")),
        DeepSeekAgentJson.stringList(
            root.path("risk_notes").isMissingNode()
                ? root.path("riskNotes")
                : root.path("risk_notes")),
        DeepSeekAgentJson.text(root, "user_facing_message", "userFacingMessage"),
        DeepSeekAgentJson.text(root, "yanyun_rewrite_suggestion", "yanyunRewriteSuggestion"),
        DeepSeekAgentJson.stringList(
            root.path("freeform_opportunities").isMissingNode()
                ? root.path("freeformOpportunities")
                : root.path("freeform_opportunities")));
  }

  private CreativeDomainDecision deterministicDecision(CreativeBriefRequest request) {
    String normalized =
        (nullToEmpty(request.userInput())
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

  private CreativeBriefResult deterministicReject(CreativeBriefRequest request) {
    String seed =
        DeepSeekAgentJson.firstNonBlank(
            request.userInput(),
            DeepSeekAgentJson.firstNonBlank(request.instruction(), request.currentLyrics()));
    return new CreativeBriefResult(
        CreativeDomainDecision.REJECT,
        "Rejected unrelated non-Yanyun request.",
        DeepSeekAgentJson.firstNonBlank(seed, "non-yanyun request"),
        List.of(),
        "none",
        DeepSeekAgentJson.firstNonBlank(request.musicStyle(), "unspecified"),
        request.yanyunReferences(),
        List.of("lyrics content must belong to Yanyun Sixteen Sounds"),
        List.of("creative_domain_rejected"),
        "当前只支持燕云十六声相关创作。可以改成燕云里的江湖、武学、奇术、乱世同行或寻声记忆方向。",
        "删除其他 IP 专属名词，只保留情绪、结构或音乐风格，并转成燕云十六声语境。",
        List.of());
  }

  private String systemPrompt() {
    return """
        你是燕云十六声 AI 作曲平台的创意理解 Agent。
        你的任务不是写歌词，而是判断用户请求是否属于《燕云十六声》创作域，并输出开放式创作简报。

        硬规则：
        1. 歌词内容必须服务燕云十六声，不得写其他 IP、现实明星应援或完全无关题材。
        2. 不要输出固定主题分类，不要强行替玩家选题。
        3. 用户的音乐风格偏好可以开放保留，但现实歌手名只能作为理解入口，不能变成仿唱要求。
        4. 如果用户请求完全无关，例如高达主题歌、其他游戏主题歌，domain_decision 必须为 REJECT。
        5. 如果用户表达的情绪可转成燕云语境，domain_decision 为 REWRITE_TO_YANYUN。
        6. 只输出 JSON object，不输出 Markdown 或解释。

        输出字段：
        {
          "domain_decision": "PASS | REWRITE_TO_YANYUN | REJECT",
          "creative_intent": "用户真实想表达什么",
          "theme": "开放式主题摘要，不是固定分类",
          "mood_tags": ["情绪标签"],
          "narrative_viewpoint": "叙事视角",
          "music_direction": "用户风格偏好的安全泛化描述",
          "yanyun_references": ["可用的燕云气质线索，没有则空数组"],
          "constraints": ["必须遵守的创作边界"],
          "risk_notes": ["风险提示"],
          "user_facing_message": "REJECT 时给用户看的友好说明，否则可为空",
          "yanyun_rewrite_suggestion": "REWRITE_TO_YANYUN 时给 LyricsAgent 的转译建议",
          "freeform_opportunities": ["可自由发挥的空间"]
        }
        """
        .trim();
  }

  private String userPrompt(CreativeBriefRequest request) {
    return String.join(
        "\n",
        "operation=" + nullToEmpty(request.operation()),
        "user_input=" + nullToEmpty(request.userInput()),
        "current_lyrics=" + nullToEmpty(request.currentLyrics()),
        "instruction=" + nullToEmpty(request.instruction()),
        "requested_title=" + nullToEmpty(request.requestedTitle()),
        "music_style=" + nullToEmpty(request.musicStyle()),
        "vocal_preference=" + nullToEmpty(request.vocalPreference()),
        "yanyun_references=" + request.yanyunReferences());
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
            request == null
                ? "UNKNOWN"
                : DeepSeekAgentJson.firstNonBlank(request.operation(), "UNKNOWN"),
            client.modelName(),
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
    return userPrompt(request);
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

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
