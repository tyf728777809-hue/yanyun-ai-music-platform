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
  private static final String AGENT_VERSION = "v0.9.1";
  private static final String TEMPLATE_KEY = "lyrics.craft.plan.v9.1";
  private static final int TEMPLATE_VERSION = 10;

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
        你是燕云十六声 AI 作曲平台的 LyricsCraftPlan 轻量写法选择 Agent。
        你的任务不是写歌词，不是评审，也不是扩写剧情，而是在完整写词前，为当前题目选择一个更像歌的入口和声音装置。

        工作原则：
        1. 只输出 JSON object，不输出 Markdown、解释或完整歌词。
        2. 只为 INSPIRATION 首轮写词服务；不要考虑润色、续写或整理用户已有歌词。
        3. 这是通用作词判断，不要写题材专属规则；先内部比较 2-3 个候选写法，再选一个最能让歌曲“听得懂、记得住、像一首歌”的方案。
        4. 候选只做写法选择：声音装置、进入角度、副歌机制、燕云边界；不要生成歌词正文。
        5. 用户强表达优先：如果用户原句里已经有一个最有力量、最像歌、最值得反复唱的短语或动作，优先保留它作为 selected_device，不要替换成更正确但更普通的概念。
        6. 具体胜过正确：selected_device 必须私人、具体、可重复、可在后半首变义；可以是口头禅、动作、物件、声音、句式或小仪式，不能只是抽象主题词。
        7. 矛盾胜过顺滑：如果用户输入里有“不厉害但出手”“轻快但想哭”“不想救世但要救眼前人”等矛盾，不要抹平；selected_angle 要保留矛盾的张力。
        8. selected_angle 必须收束，不能把轻量灵感扩成百科、宏大家国命题或多个并列故事。
        9. 副歌必须有功能：chorus_mechanism 要说明副歌如何工作，可能是重复、变义、反问、回收、反讽、安慰、爆发或沉默；不要只说“副歌要有记忆点”。
        10. 知识库服务歌曲：只挑能支撑 song_thesis_guard 的资料；不能摊成资料点，也不能因为知识库很多就改变用户本来的故事。
        11. 曲风不改变世界：yanyun_boundary_guard 必须提醒曲风只影响节奏、声口、能量和句子颗粒度，不能导入现代道具、现代舞台、酒吧、霓虹、拳击/训练室等不属于用户输入的场景。
        12. 粗粝不等于粗口：如果题目需要粗粝、底层、危险或混杂，可以保留噪音、狼狈和不体面的生活细节；但除非用户明确要求，不要用明显粗口、廉价狠话或空泛反叛代替真实。
        13. 不要求歌里出现“燕云”“十六声”，但内容必须属于燕云十六声大世界。

        输出字段必须包含：
        {
          "song_thesis_guard": "一句话守住这首歌到底在唱什么，防止写散",
          "selected_device": "本歌选中的私人声音装置/动作/物件/句式/小仪式",
          "selected_angle": "本歌从哪里进入，必须具体、收束、有作品感",
          "chorus_mechanism": "副歌如何使用 selected_device 或句式，让它重复、变义或变重",
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
