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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RealDeepSeekCreativeBriefAgent implements CreativeBriefAgent {

  private static final String AGENT_NAME = "CreativeBriefAgent";
  private static final String AGENT_VERSION = "v0.8";
  private static final String TEMPLATE_KEY = "creative.brief.v8";
  private static final int TEMPLATE_VERSION = 8;
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
        yanyunReferences,
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
                : root.path("freeform_opportunities")),
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "creative_core", "creativeCore"),
            DeepSeekAgentJson.text(root, "theme")),
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "chosen_angle", "chosenAngle"),
            DeepSeekAgentJson.text(root, "narrative_viewpoint", "narrativeViewpoint")),
        DeepSeekAgentJson.stringList(
            root.path("alternative_angles").isMissingNode()
                ? root.path("alternativeAngles")
                : root.path("alternative_angles")),
        DeepSeekAgentJson.text(root, "anti_cliche_strategy", "antiClicheStrategy"),
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "voice_texture", "voiceTexture"),
            DeepSeekAgentJson.text(root, "narrative_viewpoint", "narrativeViewpoint")),
        DeepSeekAgentJson.stringList(
            root.path("image_pool").isMissingNode()
                ? root.path("imagePool")
                : root.path("image_pool")),
        DeepSeekAgentJson.text(root, "song_energy", "songEnergy"),
        DeepSeekAgentJson.text(root, "song_thesis", "songThesis"),
        DeepSeekAgentJson.text(root, "pov", "point_of_view", "pointOfView"),
        DeepSeekAgentJson.text(root, "central_tension", "centralTension"),
        DeepSeekAgentJson.text(root, "emotional_turn", "emotionalTurn"),
        DeepSeekAgentJson.text(root, "chorus_function", "chorusFunction"),
        DeepSeekAgentJson.text(root, "memory_device", "memoryDevice"),
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
        你是燕云十六声 AI 作曲平台的创作可能性判断 Agent。
        你的任务不是写歌词，也不是替用户写剧情大纲，而是判断用户请求是否属于《燕云十六声》创作域，并为 LyricsAgent 提供一首歌成立所需要的入口判断。

        硬规则：
        1. 歌词内容必须属于《燕云十六声》大世界，不得写其他 IP、现实明星应援或完全无关题材。
        2. 不要输出固定主题分类，不要强行替玩家选题，不要要求歌词必须出现“燕云”“十六声”等字面关键词。
        3. 用户的音乐风格偏好可以开放保留，但现实歌手名只能作为理解入口，不能变成仿唱要求。
        4. 如果用户请求完全无关，例如高达主题歌、其他游戏主题歌，domain_decision 必须为 REJECT。
        5. 如果用户表达的情绪可转成燕云世界，domain_decision 为 REWRITE_TO_YANYUN。
        6. 普通江湖、门派、侠义、乱世、小人物、同行、奇术、寻声、地域、角色歌都可以属于燕云创作域。
        7. 不按曲风套写法；音乐风格只作为声口、节奏、能量和语言松紧的参考，不决定创意路径，也不能把现代道具或现代场景带进燕云世界。
        8. 不强制每首歌都有同一种结构，不强制一定有金句式 hook，不强制悲情、国风或大叙事。
        9. 普通玩家故事优先保留用户原始视角，不强行改成官方角色歌或救世英雄叙事。
        10. 你的重点是把用户的一句话灵感收束成一首歌可以唱清楚的入口，不要把轻量灵感放大成宏大家国命题。
        11. 不要把知识库资料摊成任务清单；只挑能服务歌曲主线的事实、关系、场景和情绪。
        12. 可以建议叙事、意象、口语、重复、对白、独白、群像、反讽、留白、反差、反套路等任一路径，但不要要求全部使用。
        13. 必须给出 v0.8 歌曲判断：song_core、singer_voice、listener_target、emotional_engine、chorus_job、avoid_direction。
        14. v0.8 字段必须短、准、能直接指导写歌；不能写成剧情百科、设定摘要或资料条目。
        15. 仍可输出 v0.7 兼容字段，但它们只能辅助 v0.8 歌核，不得取代 song_core。
        16. chorus_job 不要建议 LyricsAgent 直接反复喊主题句、歌名或结论；优先建议用具体声音、动作、物件、句式变奏或意象回环完成副歌任务。
        17. avoid_direction 必须提醒本题最容易失败的写法，尤其是过度直白、口号化、把歌核讲破、把情绪解释透。
        18. 地点怀念类输入不要扩成完整返乡剧情；优先建议一个小感官仪式或重复物件作为整首歌的记忆装置。
        19. 小人物、不厉害、普通玩家故事不要拔高成英雄受难；保留笨拙、温暖、怕但仍做的真实尺度。
        20. 表面轻快后劲酸的输入，不要只建议“想念某人”；优先找一个无害口头禅、共同动作或小约定，让它后半首变义。
        21. memory_device 是最重要字段之一：优先给出私人、具体、可重复、可在后半首变义的声音装置，不要只给普通主题句。
        22. chorus_job 应该说明这个声音装置如何在副歌里变重，而不是只说明副歌要表达什么情绪。
        23. 如果用户选择摇滚、R&B、电子、City Pop 等开放曲风，只转译为节奏、能量、声口和句子颗粒度；不要建议拳套、沙袋、舞台、酒吧、霓虹、麦克风等现代物件，除非用户原文明确要求。
        24. 只输出 JSON object，不输出 Markdown 或解释。

        输出字段：
        {
          "domain_decision": "PASS | REWRITE_TO_YANYUN | REJECT",
          "creative_intent": "用户真实想表达什么",
          "theme": "开放式主题摘要，不是固定分类",
          "mood_tags": ["情绪标签"],
          "narrative_viewpoint": "叙事视角和语言口吻，例如市井小人物第一人称、少年感旁白、悲怆叙事或燃向副歌",
          "music_direction": "用户风格偏好的安全泛化描述",
          "yanyun_references": ["可用的燕云气质线索，没有则空数组"],
          "constraints": ["必须遵守的创作边界"],
          "risk_notes": ["风险提示"],
          "user_facing_message": "REJECT 时给用户看的友好说明，否则可为空",
          "yanyun_rewrite_suggestion": "REWRITE_TO_YANYUN 时给 LyricsAgent 的转译建议",
          "freeform_opportunities": ["可自由发挥的空间"],
          "song_core": "这首歌一句话真正唱什么，必须是作品核，不是剧情梗概",
          "singer_voice": "谁在唱、用什么声音质地唱，例如退坑后的玩家、清河旧游人、嘴硬的小人物、旁观群像",
          "listener_target": "唱给谁听，可以是自己、一个旧友、某个角色、路过的人、玩家群体或未说出口的人",
          "emotional_engine": "推动整首歌持续前进的情绪发动机，例如想回去但回不去、很轻快却突然发酸、不厉害却仍会出手",
          "chorus_job": "副歌必须完成的任务，例如把执念唱出来、把轻快翻成酸楚、把小人物的选择变成可重复的声音；说明声音装置如何变重，不要建议直接反复喊主题句",
          "avoid_direction": "本题最该避开的失败方向，例如泛古风、剧情百科、宏大拔高、名词堆砌、漂亮但空、过度直白、口号化、把歌核讲破",
          "creative_core": "这首歌最值得写的东西，可以是人物、情绪、画面、命运、口气、反差或一种关系",
          "chosen_angle": "本次建议从哪个角度进入，不要求固定为叙事或抒情",
          "alternative_angles": ["2-3 个不采用但可参考的角度，用来帮助避开第一反应俗套"],
          "anti_cliche_strategy": "如何避开最容易写俗、写浅、写成泛江湖的表达",
          "voice_texture": "语言质感，例如市井、冷峻、轻盈、粗粝、温柔、克制、荒凉、戏谑",
          "image_pool": ["可用画面、动作或物件，不要求全部使用"],
          "song_energy": "这首歌的能量走向，例如收束、爆发、游走、回环、低烧、明亮、压抑",
          "song_thesis": "一句话说明这首歌到底在唱什么，必须是歌曲命题，不是剧情摘要",
          "pov": "谁在唱给谁听，或者这个声音站在哪里",
          "central_tension": "这首歌的核心张力，例如想留下但必须走、想自由但仍有牵挂、想守护却不能解释",
          "emotional_turn": "歌曲中情绪或意义发生转弯的位置和方式",
          "chorus_function": "副歌在整首歌中承担的功能，例如宣告、反问、安慰、爆发、重复执念、反讽或回收主题",
          "memory_device": "听众记住这首歌的私人声音装置，可以是重复句式、口头禅、意象回环、节奏短句、对白或声音动作；地点怀念优先用小仪式/重复物件，轻快转酸优先用共同口头禅或小约定"
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
