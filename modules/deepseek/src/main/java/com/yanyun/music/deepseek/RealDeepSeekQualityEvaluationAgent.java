package com.yanyun.music.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import com.yanyun.music.creativeagent.CreativeBoundaryTerms;
import com.yanyun.music.creativeagent.QualityDecision;
import com.yanyun.music.creativeagent.QualityEvaluationAgent;
import com.yanyun.music.creativeagent.QualityEvaluationRequest;
import com.yanyun.music.creativeagent.QualityEvaluationResult;
import com.yanyun.music.creativeagent.QualityGate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public final class RealDeepSeekQualityEvaluationAgent implements QualityEvaluationAgent {

  private static final String AGENT_NAME = "QualityEvaluationAgent";
  private static final String AGENT_VERSION = "v0.7";
  private static final String TEMPLATE_KEY = "quality.evaluation.v7";
  private static final int TEMPLATE_VERSION = 8;

  private final DeepSeekJsonChatClient client;
  private final ObjectMapper objectMapper;
  private final AgentRunRecorder agentRunRecorder;

  public RealDeepSeekQualityEvaluationAgent(
      DeepSeekProperties properties, ObjectMapper objectMapper, AgentRunRecorder agentRunRecorder) {
    this(new DeepSeekJsonChatClient(properties, objectMapper), objectMapper, agentRunRecorder);
  }

  RealDeepSeekQualityEvaluationAgent(
      DeepSeekJsonChatClient client, ObjectMapper objectMapper, AgentRunRecorder agentRunRecorder) {
    this.client = client;
    this.objectMapper = objectMapper;
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
  }

  @Override
  public QualityEvaluationResult evaluate(QualityEvaluationRequest request) {
    long startedAt = System.nanoTime();
    try {
      if (request == null) {
        throw new IllegalArgumentException("request is required");
      }
      QualityEvaluationResult result =
          parse(
              client.completeJson(systemPrompt(), userPrompt(request), BigDecimal.valueOf(0.10), 0),
              request);
      record(request, result, startedAt, null);
      return result;
    } catch (RuntimeException exception) {
      record(request, null, startedAt, exception);
      throw exception;
    }
  }

  private QualityEvaluationResult parse(JsonNode root, QualityEvaluationRequest request) {
    java.util.List<String> reasons =
        new ArrayList<>(DeepSeekAgentJson.stringList(root.path("reasons")));
    QualityDecision modelDecision =
        DeepSeekAgentJson.qualityDecision(DeepSeekAgentJson.text(root, "decision"));
    QualityDecision decision = localSafetyDecision(request, modelDecision, reasons);
    if (decision != modelDecision
        && reasons.isEmpty()
        && (request.gate() == QualityGate.LYRICS || request.gate() == QualityGate.MUSIC)) {
      reasons.add("内容存在非燕云题材、其他 IP 混入或仿唱风险，需要重写。");
    }
    int score = root.path("score").asInt(decision == QualityDecision.PASS ? 90 : 60);
    if (decision == QualityDecision.PASS && score < 80) {
      score = 80;
    }
    boolean retryable =
        root.path("retryable")
            .asBoolean(decision == QualityDecision.REWRITE || decision == QualityDecision.RETRY);
    if (decision == QualityDecision.PASS
        || decision == QualityDecision.BLOCK
        || decision == QualityDecision.MANUAL_REVIEW) {
      retryable = false;
    }
    return new QualityEvaluationResult(
        DeepSeekAgentJson.qualityGate(DeepSeekAgentJson.text(root, "gate"), request.gate()),
        decision,
        score,
        reasons,
        DeepSeekAgentJson.text(root, "recommended_action", "recommendedAction"),
        retryable,
        DeepSeekAgentJson.objectMap(objectMapper, root.path("metadata")).isEmpty()
            ? Map.of(
                "agent",
                AGENT_NAME,
                "agent_version",
                AGENT_VERSION,
                "gate",
                request.gate().name(),
                "prompt_template_key",
                TEMPLATE_KEY,
                "prompt_template_version",
                TEMPLATE_VERSION)
            : DeepSeekAgentJson.objectMap(objectMapper, root.path("metadata")));
  }

  private QualityDecision localSafetyDecision(
      QualityEvaluationRequest request,
      QualityDecision modelDecision,
      java.util.List<String> modelReasons) {
    String joined = (request.lyricsText() + " " + request.context()).toLowerCase();
    if (request.gate() == QualityGate.LYRICS && CreativeBoundaryTerms.containsOtherIpTerm(joined)) {
      return QualityDecision.REWRITE;
    }
    if (request.gate() == QualityGate.LYRICS && usesCorrectedEntityTypoAsOfficialName(request)) {
      modelReasons.add("歌词使用了知识库已纠正的错字或非标准称呼，需要改为标准实体名后重写。");
      return QualityDecision.REWRITE;
    }
    if (request.gate() == QualityGate.LYRICS && misusesPendingCluesAsConfirmed(request)) {
      modelReasons.add("歌词把待核线索写成官方定论，需要改成暗线、传闻、心境或留白表达。");
      return QualityDecision.REWRITE;
    }
    if (request.gate() == QualityGate.MUSIC
        && (joined.contains("周杰伦") || joined.contains("jay chou") || joined.contains("仿唱"))) {
      return QualityDecision.REWRITE;
    }
    if (request.gate() == QualityGate.COVER) {
      if (containsUnsafeCoverInstruction(request.context())) {
        return QualityDecision.BLOCK;
      }
      if (isControlledTitleCoverPrompt(request.context())
          || reasonsOnlyDescribeControlledTitlePolicy(modelReasons)) {
        return QualityDecision.PASS;
      }
      if (containsPositiveCoverTextInstruction(request.context())) {
        return QualityDecision.REWRITE;
      }
      if (isNoTextCoverPrompt(request.context()) && reasonsOnlyDescribeNoTextPolicy(modelReasons)) {
        return QualityDecision.PASS;
      }
    }
    return modelDecision;
  }

  private boolean usesCorrectedEntityTypoAsOfficialName(QualityEvaluationRequest request) {
    Object labels = request.context().get("knowledge_resolved_entities");
    if (labels == null) {
      return false;
    }
    String output =
        String.join(
            "\n",
            nullToEmpty(request.songTitle()),
            nullToEmpty(request.lyricsText()),
            String.valueOf(request.context().getOrDefault("song_summary", "")));
    for (String label : entityLabels(labels)) {
      if (!label.contains("kind=fuzzy")) {
        continue;
      }
      String canonical = between(label, "/", " matched=");
      String matched = between(label, "matched=", " kind=");
      if (!canonical.isBlank()
          && !matched.isBlank()
          && !canonical.equals(matched)
          && output.contains(matched)) {
        return true;
      }
    }
    return false;
  }

  private boolean misusesPendingCluesAsConfirmed(QualityEvaluationRequest request) {
    String context = normalize(request.context());
    if (!context.contains("pending_clues")) {
      return false;
    }
    String output =
        normalize(
            String.join(
                "\n",
                nullToEmpty(request.songTitle()),
                nullToEmpty(request.lyricsText()),
                String.valueOf(request.context().getOrDefault("song_summary", ""))));
    return output.contains("官方")
        || output.contains("已确认")
        || output.contains("实锤")
        || output.contains("定论")
        || output.contains("确定是")
        || output.contains("明确是")
        || output.contains("最终结局");
  }

  private java.util.List<String> entityLabels(Object labels) {
    if (labels instanceof Iterable<?> iterable) {
      java.util.List<String> result = new ArrayList<>();
      for (Object label : iterable) {
        result.add(String.valueOf(label));
      }
      return result;
    }
    return java.util.List.of(String.valueOf(labels));
  }

  private String between(String value, String startMarker, String endMarker) {
    int start = value.indexOf(startMarker);
    if (start < 0) {
      return "";
    }
    start += startMarker.length();
    int end = value.indexOf(endMarker, start);
    if (end < 0) {
      return "";
    }
    return value.substring(start, end).trim();
  }

  private boolean containsUnsafeCoverInstruction(Map<String, Object> context) {
    for (Map.Entry<String, Object> entry : context.entrySet()) {
      if (containsUnsafeCoverInstructionValue(normalize(entry.getKey()), entry.getValue())) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private boolean containsUnsafeCoverInstructionValue(String key, Object value) {
    if (isNegativePromptField(key)) {
      return false;
    }
    if (value instanceof Map<?, ?> map) {
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (containsUnsafeCoverInstructionValue(normalize(entry.getKey()), entry.getValue())) {
          return true;
        }
      }
      return false;
    }
    if (value instanceof Iterable<?> iterable) {
      for (Object item : iterable) {
        if (containsUnsafeCoverInstructionValue(key, item)) {
          return true;
        }
      }
      return false;
    }
    return containsUnsafeCoverInstructionText(String.valueOf(value));
  }

  private boolean containsPositiveCoverTextInstruction(Map<String, Object> context) {
    for (Map.Entry<String, Object> entry : context.entrySet()) {
      String key = normalize(entry.getKey());
      if (isNegativePromptField(key) || key.contains("text_policy")) {
        continue;
      }
      String normalized = normalize(entry.getValue());
      for (String segment : normalized.split("[\\n;；。.]")) {
        if (isNegativeConstraintSegment(segment)) {
          continue;
        }
        if (segment.contains("song title")
            || segment.contains("title typography")
            || segment.contains("main cover title")
            || segment.contains("calligraphy lettering")
            || segment.contains("text on cover")
            || segment.contains("歌名")
            || segment.contains("主标题")
            || segment.contains("标题字")
            || segment.contains("封面文字")
            || segment.contains("书法字")) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isNoTextCoverPrompt(Map<String, Object> context) {
    String joined = normalize(context);
    return joined.contains("no_text_in_image")
        || joined.contains("no text")
        || joined.contains("no chinese characters")
        || joined.contains("no song title")
        || joined.contains("无文字")
        || joined.contains("禁止图片内文字");
  }

  private boolean isControlledTitleCoverPrompt(Map<String, Object> context) {
    String joined = normalize(context);
    return joined.contains("controlled_title_text")
        || joined.contains("only the exact song title")
        || joined.contains("exact song title")
        || joined.contains("only the song title")
        || joined.contains("single controlled song title")
        || joined.contains("唯一文本")
        || joined.contains("只允许")
            && (joined.contains("歌名") || joined.contains("标题"))
            && !joined.contains("no_text_in_image");
  }

  private boolean reasonsOnlyDescribeControlledTitlePolicy(java.util.List<String> reasons) {
    if (reasons == null || reasons.isEmpty()) {
      return false;
    }
    String joined = normalize(reasons);
    if (coverQualityRiskInReasons(joined)) {
      return false;
    }
    boolean titleMentioned =
        joined.contains("song title")
            || joined.contains("exact title")
            || joined.contains("title typography")
            || joined.contains("main cover title")
            || joined.contains("歌名")
            || joined.contains("主标题")
            || joined.contains("标题字");
    boolean controlled =
        joined.contains("only")
            || joined.contains("unique")
            || joined.contains("single")
            || joined.contains("controlled")
            || joined.contains("仅允许")
            || joined.contains("只允许")
            || joined.contains("唯一")
            || joined.contains("无违规")
            || joined.contains("符合规则")
            || joined.contains("符合规范");
    return titleMentioned && controlled;
  }

  private boolean reasonsOnlyDescribeNoTextPolicy(java.util.List<String> reasons) {
    if (reasons == null || reasons.isEmpty()) {
      return true;
    }
    String joined = normalize(reasons);
    if (coverQualityRiskInReasons(joined)) {
      return false;
    }
    return joined.contains("no text")
        || joined.contains("disallow")
        || joined.contains("without text")
        || joined.contains("matching guidelines")
        || joined.contains("符合排版")
        || joined.contains("禁止文字")
        || (joined.contains("禁止") && joined.contains("文字"))
        || (joined.contains("不包含") && joined.contains("文字"))
        || joined.contains("无文字")
        || joined.contains("符合规范");
  }

  private boolean coverQualityRiskInReasons(String joined) {
    for (String segment : joined.split("[\\n;；。.]")) {
      if (isNegativeConstraintSegment(segment)) {
        continue;
      }
      if (containsCoverQualityRiskTerm(segment)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsCoverQualityRiskTerm(String segment) {
    return segment.contains("low quality")
        || segment.contains("cheap")
        || segment.contains("unrelated")
        || segment.contains("not yanyun")
        || segment.contains("copyright risk")
        || segment.contains("fake singer")
        || segment.contains("fake artist")
        || segment.contains("fake copyright")
        || segment.contains("fake label")
        || segment.contains("watermark")
        || segment.contains("qr code")
        || segment.contains("garbled")
        || segment.contains("extra text")
        || segment.contains("random small text")
        || segment.contains("低质")
        || segment.contains("廉价")
        || segment.contains("跑题")
        || segment.contains("无关")
        || segment.contains("版权风险")
        || segment.contains("假歌手")
        || segment.contains("假厂牌")
        || segment.contains("假版权")
        || segment.contains("水印")
        || segment.contains("二维码")
        || segment.contains("乱码")
        || segment.contains("额外文字")
        || segment.contains("多余文字")
        || segment.contains("随机小字");
  }

  private boolean containsUnsafeCoverInstructionText(String value) {
    String normalized = normalize(value);
    for (String segment : normalized.split("[\\n;；。.]")) {
      if (containsUnsafeCoverTerm(segment) && !isNegativeConstraintSegment(segment)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsUnsafeCoverTerm(String segment) {
    return segment.contains("fake singer")
        || segment.contains("fake artist")
        || segment.contains("fake copyright")
        || segment.contains("fake label")
        || segment.contains("fake credit")
        || segment.contains("record label")
        || segment.contains("copyright")
        || segment.contains("©")
        || segment.contains("sung by")
        || segment.contains("performed by")
        || segment.contains("lyrics by")
        || segment.contains("composed by")
        || segment.contains("feat")
        || segment.contains("featuring")
        || segment.contains("假歌手")
        || segment.contains("假署名")
        || segment.contains("假版权")
        || segment.contains("假厂牌")
        || segment.contains("厂牌")
        || segment.contains("版权")
        || segment.contains("演唱")
        || segment.contains("作词")
        || segment.contains("作曲")
        || segment.contains("watermark")
        || segment.contains("garbled text")
        || segment.contains("水印")
        || segment.contains("乱码")
        || segment.matches(".*\\bui\\b.*")
        || segment.contains("排行榜")
        || segment.contains("二维码");
  }

  private boolean isNegativePromptField(String key) {
    return key.contains("negative")
        || key.contains("exclude")
        || key.contains("forbidden")
        || key.contains("avoid");
  }

  private boolean isNegativeConstraintSegment(String segment) {
    return segment.contains("no ")
        || segment.contains("without")
        || segment.contains("do not")
        || segment.contains("don't")
        || segment.contains("disallow")
        || segment.contains("avoid")
        || segment.contains("exclude")
        || segment.contains("negative prompt")
        || segment.contains("negative_prompt")
        || segment.contains("不得")
        || segment.contains("不要")
        || segment.contains("禁止")
        || segment.contains("不允许")
        || segment.contains("未要求")
        || segment.contains("避免")
        || segment.contains("无")
        || segment.contains("没有");
  }

  private String normalize(Object value) {
    return value == null ? "" : value.toString().toLowerCase(Locale.ROOT);
  }

  private String systemPrompt() {
    return """
        你是燕云十六声 AI 作曲平台的严格质量审稿 Agent。
        你不做图片视觉审核，不 OCR，不判断封面画面是否好看。
        你只审核文本、prompt、创作边界和发布包元数据。

        重点规则：
        1. LYRICS：歌词必须有《燕云十六声》大世界归属感，不得写其他 IP，不能只是泛古风；但不要求出现“燕云”“十六声”等字面关键词。
        2. LYRICS：不要用固定模板审稿；先判断歌词实际选择了哪种作词路径，例如叙事、意象、口语、对白/独白、群像、反差、重复、反讽、留白或反套路。
        3. LYRICS：必须检查歌词是否值得被唱：是否有独特入口，是否摆脱第一反应俗套，是否有听众能记住的声音记忆点，是否像歌而不是漂亮作文。
        4. LYRICS：声音记忆点不限定为金句；可以是句子、重复句式、口头禅、声音动作、意象回环或节奏记忆。
        5. LYRICS：完整但普通、题材正确但没有独特入口、每句都对但没有一处让人想再听，必须 REWRITE，score 上限 79。
        6. LYRICS：必须检查歌词音乐性，包括副歌主韵脚或节奏回环、整首是否几乎无韵、句长是否适合中文人声演唱。
        7. LYRICS：故事清楚但不像歌、只像分行叙事文本、缺少声音记忆点时，不能给高分；严重时应返回 REWRITE。
        8. LYRICS：不要求格律诗式押韵，允许自然近韵、换韵和口语化表达；但为了押韵而硬凑、倒装、变土，也不能 PASS 高分。
        9. LYRICS：必须判断主旨清晰度：歌词是否能让听众听懂“这首歌到底在唱什么”；如果段落各自好看但主线散乱，必须 REWRITE，recommended_action 用 rewrite_angle 或 rewrite_voice。
        10. LYRICS：必须检查段落推进：Verse/Pre/Chorus/Bridge 是否有意义加深、视角推进或情绪转弯；如果只是同一种漂亮情绪反复堆叠，不能给高分。
        11. LYRICS：必须检查视角一致：谁在唱、唱给谁、站在哪里是否稳定；如果第一/第二/旁白无意识漂移导致不清楚，建议 rewrite_voice。
        12. LYRICS：必须检查副歌功能：副歌是否承担宣告、反问、安慰、爆发、重复执念、回收主题或反讽；如果副歌只是漂亮句子堆叠，建议 rewrite_memory_point 或 rewrite_angle。
        13. LYRICS：POLISH 质量门必须检查是否真实润色：是否保留原歌核心、视角、主旨和主要可用句；如果变成另一首歌，应 REWRITE，recommended_action 用 rewrite_voice 或 rewrite_singability。
        14. LYRICS：允许使用“明月、山河、江湖、风烟、长夜、流浪、故乡”等词，但必须检查是否连续堆叠成填充词；如果没有具体动作、物件或场景支撑，应扣分或建议重写。
        15. LYRICS：如果 context.knowledge_resolved_entities 显示用户点名的人物、剧情、地点、门派或玩法已映射到 canonical entity，歌词必须贴合该实体的核心经历、关系、冲突、场景或玩家体验。
        16. LYRICS：点名角色但歌词只是泛江湖、泛燕云、无关任务氛围，必须 REWRITE；点名剧情但没有对应核心冲突或场景，也必须 REWRITE。
        17. LYRICS：如果歌词、标题或摘要把用户错字/非标准称呼当成正式名字输出，而 context 已给出 canonical name，必须 REWRITE。
        18. LYRICS：如果歌词引入未被用户输入或知识库上下文支撑的核心人物、剧情或组织作为主轴，必须 REWRITE 或 MANUAL_REVIEW。
        19. LYRICS：如果 context.knowledge_reference_source_classes 包含 PENDING_CLUES，这些内容只能作为暗线、传闻、意象、心境或留白使用，不能写成官方已确认事实、明确结局或人物关系定论。
        20. LYRICS：recommended_action 必须优先使用这些值之一：rewrite_angle、rewrite_cliche、rewrite_voice、rewrite_memory_point、rewrite_singability、rewrite_grounding。不要发明接口状态。
        21. MUSIC：音乐 prompt 可以保留开放风格，但不得残留真实歌手名、仿唱、声线模仿。
        22. COVER：封面 prompt 可以要求高质量歌名主标题，且允许图片内出现唯一文本元素：作品歌名。
        23. COVER：若 prompt 只允许作品歌名主标题，且没有要求假歌手、假版权、假厂牌、随机小字、乱码、UI、水印、排行榜或二维码，应判 PASS，不要因为标题字而要求重写或阻断。
        24. PUBLISH_PACKAGE：只检查 audio/cover/video/timeline 元数据完整性，不审图片内容。
        25. LYRICS：检查是否保留用户故事核心，是否避免把普通玩家故事强行写成官方角色或救世英雄。
        26. 不要默认高分；reasons 不超过 5 条，每条简短可执行。
        27. 只输出 JSON object。

        输出字段：
        {
          "gate": "LYRICS | MUSIC | COVER | VIDEO | PUBLISH_PACKAGE",
          "decision": "PASS | REWRITE | RETRY | BLOCK | MANUAL_REVIEW",
          "score": 0,
          "reasons": ["具体可执行原因"],
          "recommended_action": "下一步动作",
          "retryable": true,
          "metadata": {"risk_level":"LOW | MEDIUM | HIGH"}
        }
        """
        .trim();
  }

  private String userPrompt(QualityEvaluationRequest request) {
    return String.join(
        "\n",
        "work_id=" + nullToEmpty(request.workId()),
        "gate=" + request.gate().name(),
        "song_title=" + trimToLength(request.songTitle(), 120),
        "lyrics_text=" + trimToLength(request.lyricsText(), 5000),
        "music_provider=" + trimToLength(request.musicProvider(), 80),
        "audio_object_key_present=" + present(request.audioObjectKey()),
        "audio_duration_ms=" + (request.audioDurationMs() == null ? "" : request.audioDurationMs()),
        "cover_object_key_present=" + present(request.coverObjectKey()),
        "cover_size="
            + nullToEmpty(request.coverWidth())
            + "x"
            + nullToEmpty(request.coverHeight()),
        "video_object_key_present=" + present(request.videoObjectKey()),
        "video_size="
            + nullToEmpty(request.videoWidth())
            + "x"
            + nullToEmpty(request.videoHeight()),
        "video_duration_ms=" + (request.videoDurationMs() == null ? "" : request.videoDurationMs()),
        "timeline_object_key_present=" + present(request.timelineObjectKey()),
        "context=" + trimToLength(request.context(), 5000));
  }

  private void record(
      QualityEvaluationRequest request,
      QualityEvaluationResult result,
      long startedAt,
      RuntimeException exception) {
    agentRunRecorder.record(
        new AgentRunRecord(
            request == null ? null : request.workId(),
            null,
            AGENT_NAME,
            AGENT_VERSION,
            request == null ? "UNKNOWN" : request.gate().operationName(),
            client.modelName(),
            TEMPLATE_KEY,
            TEMPLATE_VERSION,
            request == null ? null : AgentRunHashing.sha256(userPrompt(request)),
            result == null ? null : AgentRunHashing.sha256(outputFingerprint(result)),
            exception == null ? AgentRunStatus.SUCCEEDED : AgentRunStatus.FAILED,
            elapsedMs(startedAt),
            null,
            null,
            null,
            exception == null ? null : "QUALITY_EVALUATION_AGENT_FAILED",
            exception == null ? null : exception.getMessage()));
  }

  private String outputFingerprint(QualityEvaluationResult result) {
    return String.join(
        "\n",
        result.gate().name(),
        result.decision().name(),
        Integer.toString(result.score()),
        result.reasons().toString(),
        nullToEmpty(result.recommendedAction()),
        Boolean.toString(result.retryable()),
        result.metadata().toString());
  }

  private int elapsedMs(long startedAt) {
    long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
    return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, elapsed);
  }

  private String present(String value) {
    return value == null || value.isBlank() ? "false" : "true";
  }

  private String nullToEmpty(Object value) {
    return value == null ? "" : value.toString();
  }

  private String trimToLength(Object value, int maxLength) {
    if (value == null) {
      return "";
    }
    String trimmed = value.toString().trim();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }
}
