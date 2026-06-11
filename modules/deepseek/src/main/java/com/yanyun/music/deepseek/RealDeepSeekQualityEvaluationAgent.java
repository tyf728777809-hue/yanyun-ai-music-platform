package com.yanyun.music.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import com.yanyun.music.creativeagent.QualityDecision;
import com.yanyun.music.creativeagent.QualityEvaluationAgent;
import com.yanyun.music.creativeagent.QualityEvaluationRequest;
import com.yanyun.music.creativeagent.QualityEvaluationResult;
import com.yanyun.music.creativeagent.QualityGate;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

public final class RealDeepSeekQualityEvaluationAgent implements QualityEvaluationAgent {

  private static final String AGENT_NAME = "QualityEvaluationAgent";
  private static final String AGENT_VERSION = "v0.5";
  private static final String TEMPLATE_KEY = "quality.evaluation.v5";
  private static final int TEMPLATE_VERSION = 5;

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
              client.completeJson(
                  systemPrompt(), userPrompt(request), BigDecimal.valueOf(0.10), 1400),
              request);
      record(request, result, startedAt, null);
      return result;
    } catch (RuntimeException exception) {
      record(request, null, startedAt, exception);
      throw exception;
    }
  }

  private QualityEvaluationResult parse(JsonNode root, QualityEvaluationRequest request) {
    java.util.List<String> reasons = DeepSeekAgentJson.stringList(root.path("reasons"));
    QualityDecision decision =
        localSafetyDecision(
            request,
            DeepSeekAgentJson.qualityDecision(DeepSeekAgentJson.text(root, "decision")),
            reasons);
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
    if (request.gate() == QualityGate.LYRICS
        && (joined.contains("高达") || joined.contains("gundam") || joined.contains("原神"))) {
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
      if (modelDecision == QualityDecision.REWRITE
          && isSafeTitleTypographyCover(request)
          && reasonsOnlyDescribeSafeTitleTypography(modelReasons)) {
        return QualityDecision.PASS;
      }
    }
    return modelDecision;
  }

  private boolean isSafeTitleTypographyCover(QualityEvaluationRequest request) {
    if (!isSixteenByNine(request.coverWidth(), request.coverHeight())) {
      return false;
    }
    String context = normalize(request.context());
    return context.contains("song title")
        || context.contains("title typography")
        || context.contains("main cover title")
        || context.contains("歌名")
        || context.contains("主标题")
        || context.contains("标题字");
  }

  private boolean reasonsOnlyDescribeSafeTitleTypography(java.util.List<String> reasons) {
    if (reasons == null || reasons.isEmpty()) {
      return true;
    }
    String joined = normalize(reasons);
    for (String segment : joined.split("[\\n;；。.]")) {
      if (isNegativeConstraintSegment(segment)) {
        continue;
      }
      if (segment.contains("low quality")
          || segment.contains("unclear")
          || segment.contains("not readable")
          || segment.contains("unreadable")
          || segment.contains("garbled")
          || segment.contains("乱码")
          || segment.contains("低质")
          || segment.contains("不清晰")
          || segment.contains("不可读")) {
        return false;
      }
    }
    return !containsUnsafeCoverInstructionText(joined);
  }

  private boolean containsUnsafeCoverInstruction(Map<String, Object> context) {
    for (Map.Entry<String, Object> entry : context.entrySet()) {
      String key = normalize(entry.getKey());
      if (isNegativePromptField(key)) {
        continue;
      }
      if (containsUnsafeCoverInstructionText(String.valueOf(entry.getValue()))) {
        return true;
      }
    }
    return false;
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

  private boolean isSixteenByNine(Integer width, Integer height) {
    if (width == null || height == null || width <= 0 || height <= 0) {
      return false;
    }
    double actual = (double) width / (double) height;
    return Math.abs(actual - (16.0 / 9.0)) < 0.01;
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
        || segment.contains("avoid")
        || segment.contains("exclude")
        || segment.contains("negative prompt")
        || segment.contains("negative_prompt")
        || segment.contains("不得")
        || segment.contains("不要")
        || segment.contains("禁止")
        || segment.contains("不允许")
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
        1. LYRICS：歌词必须和燕云十六声相关，不得写其他 IP，不能只是泛古风。
        2. MUSIC：音乐 prompt 可以保留开放风格，但不得残留真实歌手名、仿唱、声线模仿。
        3. COVER：封面 prompt 可以要求高质量标题字，但不得要求假歌手、假版权、假厂牌、乱码、UI、水印。
        4. COVER：若 prompt 只允许歌名主标题，且没有要求假歌手、假版权、假厂牌、乱码、UI、水印、排行榜或二维码，应判 PASS，不要因为只有标题字而要求重写。
        5. PUBLISH_PACKAGE：只检查 audio/cover/video/timeline 元数据完整性，不审图片内容。
        6. 不要默认高分；只输出 JSON object。

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
        "song_title=" + nullToEmpty(request.songTitle()),
        "lyrics_text=" + nullToEmpty(request.lyricsText()),
        "music_provider=" + nullToEmpty(request.musicProvider()),
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
        "context=" + request.context());
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
}
