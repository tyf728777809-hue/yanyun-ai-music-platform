package com.yanyun.music.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import com.yanyun.music.creativeagent.CreativeBoundaryTerms;
import com.yanyun.music.creativeagent.CreativeBriefAgent;
import com.yanyun.music.creativeagent.CreativeBriefRequest;
import com.yanyun.music.creativeagent.CreativeBriefResult;
import com.yanyun.music.creativeagent.CreativeDomainDecision;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RealDeepSeekCreativeBriefAgent implements CreativeBriefAgent {

  private static final String AGENT_NAME = "CreativeBriefAgent";
  private static final String AGENT_VERSION = "v0.12";
  private static final String TEMPLATE_KEY = "creative.brief.v12";
  private static final int TEMPLATE_VERSION = 12;
  private final DeepSeekJsonChatClient client;
  private final AgentRunRecorder agentRunRecorder;
  private final Duration requestTimeout;
  private final int semanticAttempts;
  private final int responseMaxTokens;

  public RealDeepSeekCreativeBriefAgent(
      DeepSeekProperties properties, ObjectMapper objectMapper, AgentRunRecorder agentRunRecorder) {
    this(
        new DeepSeekJsonChatClient(properties, objectMapper),
        agentRunRecorder,
        properties == null ? Duration.ofSeconds(20) : properties.getCreativeBriefRequestTimeout(),
        properties == null ? 1 : properties.getCreativeBriefSemanticAttempts(),
        properties == null ? 900 : properties.getCreativeBriefResponseMaxTokens());
  }

  RealDeepSeekCreativeBriefAgent(DeepSeekJsonChatClient client, AgentRunRecorder agentRunRecorder) {
    this(client, agentRunRecorder, Duration.ofSeconds(20), 1, 900);
  }

  RealDeepSeekCreativeBriefAgent(
      DeepSeekJsonChatClient client,
      AgentRunRecorder agentRunRecorder,
      Duration requestTimeout,
      int semanticAttempts,
      int responseMaxTokens) {
    this.client = client;
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
    this.requestTimeout =
        requestTimeout == null || requestTimeout.isNegative()
            ? Duration.ofSeconds(20)
            : requestTimeout;
    this.semanticAttempts = Math.max(1, semanticAttempts);
    this.responseMaxTokens = Math.max(256, responseMaxTokens);
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
                      systemPrompt(),
                      userPrompt(request),
                      BigDecimal.valueOf(0.35),
                      responseMaxTokens,
                      requestTimeout,
                      semanticAttempts),
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
    List<String> yanyunReferences =
        DeepSeekAgentJson.stringList(
            root.path("yanyun_references").isMissingNode()
                ? root.path("yanyunReferences")
                : root.path("yanyun_references"));
    if (yanyunReferences.isEmpty()) {
      yanyunReferences = fallbackYanyunReferences(request);
    }
    return new CreativeBriefResult(
        decision,
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(
                root,
                "creative_intent",
                "user_intent_summary",
                "userIntentSummary",
                "song_core",
                "songCore"),
            "Shape a Yanyun song from the user request."),
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "theme", "song_core", "songCore"), "Yanyun player story"),
        DeepSeekAgentJson.stringList(
            root.path("mood_tags").isMissingNode()
                ? root.path("moodTags")
                : root.path("mood_tags")),
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(
                root, "narrative_viewpoint", "narrativeViewpoint", "singer_voice", "singerVoice"),
            "player-facing"),
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "music_direction", "musicDirection"),
            request.musicStyle()),
        yanyunReferences,
        List.of(
            "keep lyrics singable",
            "knowledge serves the song core",
            DeepSeekAgentJson.firstNonBlank(
                DeepSeekAgentJson.text(root, "knowledge_use_hint", "knowledgeUseHint"), "")),
        DeepSeekAgentJson.stringList(
            root.path("risk_notes").isMissingNode()
                ? root.path("riskNotes")
                : root.path("risk_notes")),
        DeepSeekAgentJson.text(root, "user_facing_message", "userFacingMessage"),
        DeepSeekAgentJson.text(root, "yanyun_rewrite_suggestion", "yanyunRewriteSuggestion"),
        DeepSeekAgentJson.stringList(
            root.path("freeform_opportunities").isMissingNode()
                ? root.path("freeformOpportunities")
                : root.path("freeform_opportunities")),
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "creative_core", "creativeCore"),
            DeepSeekAgentJson.text(root, "song_core", "songCore", "theme")),
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "chosen_angle", "chosenAngle"),
            DeepSeekAgentJson.text(
                root, "central_tension", "centralTension", "song_core", "songCore")),
        DeepSeekAgentJson.stringList(
            root.path("alternative_angles").isMissingNode()
                ? root.path("alternativeAngles")
                : root.path("alternative_angles")),
        DeepSeekAgentJson.text(root, "anti_cliche_strategy", "antiClicheStrategy"),
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "voice_texture", "voiceTexture"),
            DeepSeekAgentJson.text(root, "singer_voice", "singerVoice")),
        DeepSeekAgentJson.stringList(
            root.path("image_pool").isMissingNode()
                ? root.path("imagePool")
                : root.path("image_pool")),
        DeepSeekAgentJson.text(root, "song_energy", "songEnergy"),
        DeepSeekAgentJson.text(root, "song_thesis", "songThesis", "song_core", "songCore"),
        DeepSeekAgentJson.text(
            root, "pov", "point_of_view", "pointOfView", "singer_voice", "singerVoice"),
        DeepSeekAgentJson.text(root, "central_tension", "centralTension"),
        DeepSeekAgentJson.text(root, "emotional_turn", "emotionalTurn"),
        DeepSeekAgentJson.text(
            root, "chorus_function", "chorusFunction", "chorus_job", "chorusJob"),
        DeepSeekAgentJson.text(root, "memory_device", "memoryDevice", "chorus_job", "chorusJob"),
        DeepSeekAgentJson.text(root, "song_core", "songCore"),
        DeepSeekAgentJson.text(root, "singer_voice", "singerVoice"),
        DeepSeekAgentJson.text(root, "listener_target", "listenerTarget"),
        DeepSeekAgentJson.text(root, "emotional_engine", "emotionalEngine"),
        DeepSeekAgentJson.text(root, "chorus_job", "chorusJob"),
        DeepSeekAgentJson.text(root, "avoid_direction", "avoidDirection"));
  }

  private List<String> fallbackYanyunReferences(CreativeBriefRequest request) {
    List<String> explicit = request.yanyunReferences();
    if (explicit != null && !explicit.isEmpty()) {
      return explicit;
    }
    String normalized =
        (nullToEmpty(request.userInput())
                + " "
                + nullToEmpty(request.currentLyrics())
                + " "
                + nullToEmpty(request.instruction())
                + " "
                + nullToEmpty(request.requestedTitle()))
            .toLowerCase(Locale.ROOT);
    List<String> references = new ArrayList<>();
    if (normalized.contains("雁门")) {
      references.add("雁门关外风雪");
    }
    if (normalized.contains("清河")) {
      references.add("清河江湖旧事");
    }
    if (normalized.contains("十六声") || normalized.contains("燕云")) {
      references.add("燕云十六声玩家故事");
    }
    if (normalized.contains("江湖") || normalized.contains("侠")) {
      references.add("江湖游侠心境");
    }
    if (references.isEmpty()) {
      references.add("燕云十六声创作域");
    }
    return references.stream().distinct().limit(4).toList();
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
    boolean mentionsOtherIp = CreativeBoundaryTerms.containsOtherIpTerm(normalized);
    boolean mentionsYanyun = CreativeBoundaryTerms.containsYanyunTerm(normalized);
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
        你是燕云十六声 AI 作曲平台的轻量 CreativeBrief Agent。
        你的任务不是写歌词，不是写剧情大纲，而是在必要时给 LyricsAgent 一个短、准、可执行的歌曲入口。

        核心规则：
        1. 只判断一首歌该从哪里进入；不要展开百科、剧情梗概或完整策划案。
        2. 内容必须属于《燕云十六声》大世界；其他 IP、现实明星应援、商业广告歌等完全无关题材必须 REJECT。
        3. 可转译的非燕云情绪可 REWRITE_TO_YANYUN：删除其他 IP 专属名词，只保留情绪、结构或音乐能量。
        4. 不要求歌词出现“燕云”“十六声”；知识库只提供事实、关系、场景和情绪材料。
        5. 曲风只影响声口、节奏、能量和句子颗粒度；不能把现代道具、现代舞台或现代城市意象带进燕云世界。
        6. 普通玩家故事不要强行改成官方角色歌、家国大叙事或救世英雄。
        7. 你的输出必须短。每个字段 1 句以内，宁可少，不要泛。
        8. 只输出 JSON object，不输出 Markdown，不输出解释。

        JSON 输出字段只允许这些：
        {
          "domain_decision": "PASS | REWRITE_TO_YANYUN | REJECT",
          "song_core": "这首歌真正唱什么，一句话说清，不是剧情梗概",
          "singer_voice": "谁在唱、用什么口吻唱",
          "central_tension": "推动整首歌的核心张力",
          "chorus_job": "副歌要完成什么功能，不要建议直接喊主题句",
          "avoid_direction": "最该避开的失败方向",
          "knowledge_use_hint": "知识库材料如何服务歌曲主线；不用则写空字符串"
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
        "mood=" + nullToEmpty(request.mood()),
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
        result.freeformOpportunities().toString(),
        result.creativeCore(),
        result.chosenAngle(),
        result.alternativeAngles().toString(),
        result.antiClicheStrategy(),
        result.voiceTexture(),
        result.imagePool().toString(),
        result.songEnergy(),
        result.songThesis(),
        result.pov(),
        result.centralTension(),
        result.emotionalTurn(),
        result.chorusFunction(),
        result.memoryDevice(),
        result.songCore(),
        result.singerVoice(),
        result.listenerTarget(),
        result.emotionalEngine(),
        result.chorusJob(),
        result.avoidDirection());
  }

  private int elapsedMs(long startedAt) {
    long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
    return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, elapsed);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
