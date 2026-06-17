package com.yanyun.music.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import com.yanyun.music.creativeagent.CreativeBriefResult;
import com.yanyun.music.creativeagent.LyricsCraftPlanRequest;
import com.yanyun.music.creativeagent.LyricsCraftPlanResult;
import com.yanyun.music.creativeagent.LyricsCraftPlanner;
import java.math.BigDecimal;
import java.util.List;

public final class RealDeepSeekLyricsCraftPlanner implements LyricsCraftPlanner {

  private static final String AGENT_NAME = "LyricsCraftPlanner";
  private static final String AGENT_VERSION = "v0.10";
  private static final String TEMPLATE_KEY = "lyrics.craft.plan.v10";
  private static final int TEMPLATE_VERSION = 12;

  private final DeepSeekJsonChatClient client;
  private final AgentRunRecorder agentRunRecorder;

  public RealDeepSeekLyricsCraftPlanner(
      DeepSeekProperties properties, ObjectMapper objectMapper, AgentRunRecorder agentRunRecorder) {
    this(new DeepSeekJsonChatClient(properties, objectMapper), agentRunRecorder);
  }

  RealDeepSeekLyricsCraftPlanner(DeepSeekJsonChatClient client, AgentRunRecorder agentRunRecorder) {
    this.client = client;
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
  }

  @Override
  public LyricsCraftPlanResult plan(LyricsCraftPlanRequest request) {
    long startedAt = System.nanoTime();
    try {
      if (request == null) {
        throw new IllegalArgumentException("request is required");
      }
      LyricsCraftPlanResult result =
          parse(
              client.completeJson(
                  systemPrompt(), userPrompt(request), BigDecimal.valueOf(0.35), 1200));
      record(request, result, startedAt, null);
      return result;
    } catch (RuntimeException exception) {
      record(request, null, startedAt, exception);
      throw exception;
    }
  }

  private LyricsCraftPlanResult parse(JsonNode root) {
    return new LyricsCraftPlanResult(
        DeepSeekAgentJson.text(root, "planning_decision", "planningDecision"),
        DeepSeekAgentJson.text(root, "user_phrase_assessment", "userPhraseAssessment"),
        DeepSeekAgentJson.text(root, "lyric_surface_mode", "lyricSurfaceMode"),
        DeepSeekAgentJson.text(root, "device_decision", "deviceDecision"),
        DeepSeekAgentJson.text(root, "latency_budget", "latencyBudget"),
        DeepSeekAgentJson.text(root, "confidence"),
        DeepSeekAgentJson.text(root, "song_thesis_guard", "songThesisGuard"),
        DeepSeekAgentJson.text(root, "selected_device", "selectedDevice"),
        DeepSeekAgentJson.text(root, "selected_angle", "selectedAngle"),
        DeepSeekAgentJson.text(root, "chorus_mechanism", "chorusMechanism"),
        DeepSeekAgentJson.text(root, "yanyun_boundary_guard", "yanyunBoundaryGuard"),
        DeepSeekAgentJson.stringList(
            root.path("rejected_alternatives").isMissingNode()
                ? root.path("rejectedAlternatives")
                : root.path("rejected_alternatives")));
  }

  private String systemPrompt() {
    return """
        你是燕云十六声 AI 作曲平台的 LyricsCraftPlan v0.10 结构化决策 Agent。
        你的任务不是写歌词，也不是替歌曲做复杂导演，而是判断这次首轮灵感写词是否值得多一次规划调用；只有值得时才给出最小写法决策。

        工作原则：
        1. 只输出 JSON object，不输出 Markdown、解释或完整歌词。
        2. 只为 INSPIRATION 首轮写词服务；不要考虑润色、续写或整理用户已有歌词。
        3. 首先判断用户输入里是否已有强表达。已有强 hook、强口头禅、强矛盾或强动作时，优先 SKIP_PLAN 或 USE_USER_PHRASE，不要另造装置。
        4. 只有当输入散、抽象、缺少可唱入口，或知识库实体需要明确表层模式时，才 USE_PLAN。
        5. planning_decision=SKIP_PLAN 时，selected_device/selected_angle/chorus_mechanism 可以留空或极短，不要为了填字段硬编。
        6. lyric_surface_mode 必须先判断歌词表层：
           - IN_WORLD：歌词完全发生在燕云世界内，不出现玩家、跑图、退坑、队伍、界面等现代/游戏外词。
           - PLAYER_META：用户明确写玩家经历时才用，可保留少量玩家体验词，但不能写成论坛帖。
           - CHARACTER_SONG：用户点名角色或明确要求人物经历。
           - ORDINARY_STORY：普通小人物、玩家自创江湖故事、地点怀念。
        7. device_decision 只做写法选择：USE_USER_PHRASE、USE_SMALL_DEVICE、USE_DIRECT_REFRAIN、NO_DEVICE。不是每首歌都需要装置。
        8. latency_budget 必须诚实：如果多一次规划不太可能提升歌词，输出 NOT_WORTH_EXTRA_CALL。
        9. 曲风只影响节奏、声口、能量和句子颗粒度，不能导入现代道具或现代场景。
        10. 不要求歌里出现“燕云”“十六声”，但内容必须属于燕云十六声大世界。

        输出字段必须包含：
        {
          "planning_decision": "USE_PLAN | SKIP_PLAN",
          "user_phrase_assessment": "STRONG_HOOK | WEAK_PHRASE | NONE",
          "lyric_surface_mode": "IN_WORLD | PLAYER_META | CHARACTER_SONG | ORDINARY_STORY",
          "device_decision": "USE_USER_PHRASE | USE_SMALL_DEVICE | USE_DIRECT_REFRAIN | NO_DEVICE",
          "latency_budget": "WORTH_EXTRA_CALL | NOT_WORTH_EXTRA_CALL",
          "confidence": "HIGH | MEDIUM | LOW",
          "song_thesis_guard": "一句话守住这首歌到底在唱什么，防止写散",
          "selected_device": "仅在 USE_PLAN 时填写；可以是用户原句、小动作、物件、声音、句式或空",
          "selected_angle": "仅在 USE_PLAN 时填写；本歌从哪里进入，必须具体、收束",
          "chorus_mechanism": "仅在 USE_PLAN 时填写；副歌如何重复、变义、反问、回收或直接推进",
          "yanyun_boundary_guard": "燕云世界边界与曲风边界提醒",
          "rejected_alternatives": ["被放弃的写法及原因，2-3 条"]
        }
        """
        .trim();
  }

  private String userPrompt(LyricsCraftPlanRequest request) {
    CreativeBriefResult brief = request.creativeBrief();
    return String.join(
        "\n",
        "operation=" + nullToEmpty(request.operation()),
        "user_input=" + nullToEmpty(request.userInput()),
        "mood=" + nullToEmpty(request.mood()),
        "music_style=" + nullToEmpty(request.musicStyle()),
        "vocal_preference=" + nullToEmpty(request.vocalPreference()),
        "requested_title=" + nullToEmpty(request.requestedTitle()),
        "creative_brief.song_core=" + (brief == null ? "" : nullToEmpty(brief.songCore())),
        "creative_brief.singer_voice=" + (brief == null ? "" : nullToEmpty(brief.singerVoice())),
        "creative_brief.emotional_engine="
            + (brief == null ? "" : nullToEmpty(brief.emotionalEngine())),
        "creative_brief.chorus_job=" + (brief == null ? "" : nullToEmpty(brief.chorusJob())),
        "creative_brief.avoid_direction="
            + (brief == null ? "" : nullToEmpty(brief.avoidDirection())),
        "creative_brief.memory_device=" + (brief == null ? "" : nullToEmpty(brief.memoryDevice())),
        "resolved_entities=" + request.resolvedEntities(),
        "yanyun_references=" + request.yanyunReferences(),
        "knowledge_reference_summaries=" + trimList(request.knowledgeReferenceSummaries(), 6, 360));
  }

  private List<String> trimList(List<String> values, int limit, int maxLength) {
    if (values == null) {
      return List.of();
    }
    return values.stream().limit(limit).map(value -> trimToLength(value, maxLength)).toList();
  }

  private String trimToLength(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value == null ? "" : value;
    }
    return value.substring(0, Math.max(0, maxLength)) + "...";
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
            exception == null ? null : "LYRICS_CRAFT_PLAN_FAILED",
            exception == null ? null : exception.getMessage()));
  }

  private String inputFingerprint(LyricsCraftPlanRequest request) {
    return userPrompt(request);
  }

  private String outputFingerprint(LyricsCraftPlanResult result) {
    return String.join(
        "\n",
        result.songThesisGuard(),
        result.planningDecision(),
        result.userPhraseAssessment(),
        result.lyricSurfaceMode(),
        result.deviceDecision(),
        result.latencyBudget(),
        result.confidence(),
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

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
