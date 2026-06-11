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
              client.completeJson(
                  systemPrompt(), userPrompt(request), BigDecimal.valueOf(0.55), 1800),
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
    String visualPrompt =
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "visual_prompt", "visualPrompt"),
            request.coverPromptSeed());
    String textPrompt =
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "text_prompt", "textPrompt"),
            "Use only the song title as premium cover title typography.");
    return new CoverPromptResult(
        visualPrompt,
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "negative_prompt", "negativePrompt"),
            "low quality, fake singer name, fake label, fake copyright, garbled text, UI, watermark"),
        intValue(root, "width", request.width() == null ? 1920 : request.width()),
        intValue(root, "height", request.height() == null ? 1080 : request.height()),
        listOrDefault(
            root.path("style_constraints").isMissingNode()
                ? root.path("styleConstraints")
                : root.path("style_constraints"),
            List.of("16:9 premium album cover", "high-quality title typography")),
        providerOptions.isEmpty()
            ? Map.of(
                "agent",
                AGENT_NAME,
                "agent_version",
                AGENT_VERSION,
                "prompt_template_key",
                TEMPLATE_KEY,
                "prompt_template_version",
                TEMPLATE_VERSION)
            : providerOptions,
        textPrompt,
        listOrDefault(
            root.path("typography_requirements").isMissingNode()
                ? root.path("typographyRequirements")
                : root.path("typography_requirements"),
            List.of("clear readable Chinese title", "no fake singer credits")));
  }

  private String systemPrompt() {
    return """
        你是燕云十六声 AI 作曲平台的顶级专辑封面视觉 Agent。
        你的任务是为 Image2 生成完整成品专辑封面 prompt，可以包含高质量标题文字。

        规则：
        1. 允许直接生成歌名主标题，文字必须高级、清晰、像正式音乐发行封面。
        2. 不允许假歌手名、假版权、假厂牌、乱码、低质小字、UI、水印、排行榜样式。
        3. 封面必须服务歌曲情绪和燕云十六声气质，但不得编造具体官方剧情。
        4. 当前默认 16:9，用作视频封面和音乐作品底板。
        5. 只输出 JSON object。

        输出字段：
        {
          "visual_prompt": "英文 Image2 prompt，描述画面、构图、光线、色彩、专辑感和文字布局",
          "text_prompt": "文字策略，只允许歌名主标题和极少量非署名装饰文字",
          "negative_prompt": "低质、假署名、假版权、乱码、UI、水印等负向约束",
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
        "work_id=" + nullToEmpty(request.workId()),
        "song_title=" + nullToEmpty(request.songTitle()),
        "song_summary=" + nullToEmpty(request.songSummary()),
        "lyrics_text=" + nullToEmpty(request.lyricsText()),
        "music_prompt=" + nullToEmpty(request.musicPrompt()),
        "cover_prompt_seed=" + nullToEmpty(request.coverPromptSeed()),
        "width=" + (request.width() == null ? "" : request.width()),
        "height=" + (request.height() == null ? "" : request.height()));
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
}
