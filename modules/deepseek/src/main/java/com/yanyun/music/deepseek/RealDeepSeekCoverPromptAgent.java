package com.yanyun.music.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import com.yanyun.music.creativeagent.CoverPromptAgent;
import com.yanyun.music.creativeagent.CoverPromptRequest;
import com.yanyun.music.creativeagent.CoverPromptResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class RealDeepSeekCoverPromptAgent implements CoverPromptAgent {

  private static final String AGENT_NAME = "CoverPromptAgent";
  private static final String AGENT_VERSION = "v0.5";
  private static final String TEMPLATE_KEY = "cover.prompt.v5";
  private static final int TEMPLATE_VERSION = 5;

  private final DeepSeekJsonChatClient client;
  private final ObjectMapper objectMapper;
  private final AgentRunRecorder agentRunRecorder;

  public RealDeepSeekCoverPromptAgent(
      DeepSeekProperties properties, ObjectMapper objectMapper, AgentRunRecorder agentRunRecorder) {
    this(new DeepSeekJsonChatClient(properties, objectMapper), objectMapper, agentRunRecorder);
  }

  RealDeepSeekCoverPromptAgent(
      DeepSeekJsonChatClient client, ObjectMapper objectMapper, AgentRunRecorder agentRunRecorder) {
    this.client = client;
    this.objectMapper = objectMapper;
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
  }

  @Override
  public CoverPromptResult generate(CoverPromptRequest request) {
    long startedAt = System.nanoTime();
    try {
      if (request == null) {
        throw new IllegalArgumentException("request is required");
      }
      CoverPromptResult result =
          parse(
              client.completeJson(systemPrompt(), userPrompt(request), BigDecimal.valueOf(0.55), 0),
              request);
      record(request, result, startedAt, null);
      return result;
    } catch (RuntimeException exception) {
      record(request, null, startedAt, exception);
      throw exception;
    }
  }

  private CoverPromptResult parse(JsonNode root, CoverPromptRequest request) {
    Map<String, Object> providerOptions =
        DeepSeekAgentJson.objectMap(
            objectMapper,
            root.path("provider_options").isMissingNode()
                ? root.path("providerOptions")
                : root.path("provider_options"));
    providerOptions = withControlledTitlePolicy(providerOptions, request.songTitle());
    String visualPrompt =
        controlledTitleVisualPrompt(
            DeepSeekAgentJson.firstNonBlank(
                DeepSeekAgentJson.text(root, "visual_prompt", "visualPrompt"),
                DeepSeekAgentJson.firstNonBlank(
                    request.coverPromptSeed(), defaultVisualPrompt(request))),
            request.songTitle());
    return new CoverPromptResult(
        visualPrompt,
        controlledTitleNegativePrompt(
            DeepSeekAgentJson.firstNonBlank(
                DeepSeekAgentJson.text(root, "negative_prompt", "negativePrompt"),
                "low quality, fake singer name, fake label, fake copyright, garbled text, UI, watermark")),
        intValue(root, "width", request.width() == null ? 1920 : request.width()),
        intValue(root, "height", request.height() == null ? 1080 : request.height()),
        listOrDefault(
            root.path("style_constraints").isMissingNode()
                ? root.path("styleConstraints")
                : root.path("style_constraints"),
            List.of("16:9 premium album cover", "controlled song title typography")),
        providerOptions,
        controlledTitleTextPrompt(request.songTitle()),
        listOrDefault(
            root.path("typography_requirements").isMissingNode()
                ? root.path("typographyRequirements")
                : root.path("typography_requirements"),
            List.of(
                "render only the exact song title as the main title",
                "no singer, credits, label, copyright, watermark, or small text")));
  }

  private String systemPrompt() {
    return """
        你是燕云十六声 AI 作曲平台的顶级专辑封面视觉 Agent。
        你的任务是为 Image2 生成完整成品专辑封面 prompt。

        规则：
        1. 当前版本允许 Image2 在封面图内生成作品歌名，但只允许一个文字元素：song_title 的精确标题。
        2. 禁止生成任何其他文字，包含歌手名、演唱、作词、作曲、制作人、厂牌、版权、小字、Logo、水印、二维码或随机英文。
        3. 封面必须服务歌曲情绪和燕云十六声气质，但不得编造具体官方剧情。
        4. 当前默认 16:9，用作视频封面和音乐作品底板；歌名必须成为高级、克制、可读的封面主标题，而不是廉价海报字。
        5. 生成 prompt 需要完整可执行，但不要长篇解释：visual_prompt 控制在约 1200 字符内，negative_prompt 控制在约 400 字符内。
        6. style_constraints 和 typography_requirements 各不超过 5 条。
        7. text_prompt 必须输出 "CONTROLLED_TITLE_TEXT" 策略，只允许 song_title。
        8. 只输出 JSON object。

        输出字段：
        {
          "visual_prompt": "英文 Image2 prompt，描述画面、构图、光线、色彩、专辑感和受控歌名标题要求",
          "text_prompt": "CONTROLLED_TITLE_TEXT: only the exact song_title may appear",
          "negative_prompt": "低质、假署名、假版权、随机小字、乱码、UI、水印等负向约束",
          "width": 1920,
          "height": 1080,
          "style_constraints": [],
          "typography_requirements": [],
          "provider_options": {}
        }
        """
        .trim();
  }

  private String userPrompt(CoverPromptRequest request) {
    return String.join(
        "\n",
        fieldLine("work_id", request.workId(), 256),
        fieldLine("song_title", request.songTitle(), 120),
        fieldLine("song_summary", request.songSummary(), 800),
        fieldLine("lyrics_excerpt", request.lyricsText(), 1800),
        fieldLine("music_prompt", request.musicPrompt(), 1200),
        fieldLine("cover_prompt_seed", request.coverPromptSeed(), 1200),
        "width=" + (request.width() == null ? "" : request.width()),
        "height=" + (request.height() == null ? "" : request.height()));
  }

  private String defaultVisualPrompt(CoverPromptRequest request) {
    return "Premium cinematic 16:9 album cover for a Yanyun-inspired original song, "
        + "restrained composition, strong visual focus, atmospheric Chinese frontier mood, "
        + "high-quality controlled title typography integrated into the composition";
  }

  private String controlledTitleVisualPrompt(String visualPrompt, String songTitle) {
    String prompt = trimToLength(visualPrompt, 1100);
    prompt =
        prompt
            .replaceAll(
                "(?i)no\\s+text\\s+in\\s+(the\\s+)?image",
                "single controlled song title typography")
            .replaceAll(
                "(?i)no\\s+(chinese\\s+characters|english\\s+letters|song\\s+title|title\\s+typography)",
                "controlled exact song title only")
            .replaceAll("(无文字|禁止图片内文字|不要文字|无歌名|不要歌名)", "只保留受控歌名标题");
    return prompt
        + ". Include exactly one text element: the song title \""
        + safeTitle(songTitle)
        + "\" as the main cover title. Do not add any artist name, singer credit, lyrics by, "
        + "composed by, label, copyright, logo, watermark, subtitle, slogan, QR code, or small text.";
  }

  private String controlledTitleNegativePrompt(String negativePrompt) {
    String prompt = trimToLength(negativePrompt, 300);
    return prompt
        + ", extra text beyond the exact song title, random text, garbled text, misspelled title, "
        + "artist name, singer name, performer credit, lyrics by, composed by, fake label, "
        + "fake copyright, record label, logo, watermark, UI, QR code, ranking chart, small credits";
  }

  private String controlledTitleTextPrompt(String songTitle) {
    return "CONTROLLED_TITLE_TEXT: render only the exact song title \""
        + safeTitle(songTitle)
        + "\". No other text.";
  }

  private Map<String, Object> withControlledTitlePolicy(
      Map<String, Object> providerOptions, String songTitle) {
    java.util.LinkedHashMap<String, Object> merged = new java.util.LinkedHashMap<>();
    if (providerOptions != null) {
      merged.putAll(providerOptions);
    }
    merged.putIfAbsent("agent", AGENT_NAME);
    merged.putIfAbsent("agent_version", AGENT_VERSION);
    merged.putIfAbsent("prompt_template_key", TEMPLATE_KEY);
    merged.putIfAbsent("prompt_template_version", TEMPLATE_VERSION);
    merged.put("text_policy", "CONTROLLED_TITLE_TEXT");
    merged.put("allowed_title", safeTitle(songTitle));
    return merged;
  }

  private String safeTitle(String songTitle) {
    return trimToLength(songTitle, 40).replace("\"", "").replace("\n", " ").trim();
  }

  private String fieldLine(String fieldName, String value, int maxLength) {
    return fieldName + "=" + trimToLength(value, maxLength);
  }

  private int intValue(JsonNode root, String fieldName, int fallback) {
    int value = root.path(fieldName).asInt(fallback);
    return value <= 0 ? fallback : value;
  }

  private List<String> listOrDefault(JsonNode node, List<String> fallback) {
    List<String> values = DeepSeekAgentJson.stringList(node);
    return values.isEmpty() ? fallback : values;
  }

  private void record(
      CoverPromptRequest request,
      CoverPromptResult result,
      long startedAt,
      RuntimeException exception) {
    agentRunRecorder.record(
        new AgentRunRecord(
            request == null ? null : request.workId(),
            null,
            AGENT_NAME,
            AGENT_VERSION,
            "COVER_PROMPT",
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
            exception == null ? null : "COVER_PROMPT_AGENT_FAILED",
            exception == null ? null : exception.getMessage()));
  }

  private String outputFingerprint(CoverPromptResult result) {
    return String.join(
        "\n",
        result.visualPrompt(),
        result.negativePrompt(),
        Integer.toString(result.width()),
        Integer.toString(result.height()),
        result.styleConstraints().toString(),
        result.providerOptions().toString(),
        nullToEmpty(result.textPrompt()),
        result.typographyRequirements().toString());
  }

  private int elapsedMs(long startedAt) {
    long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
    return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, elapsed);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private String trimToLength(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }
}
