package com.yanyun.music.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import com.yanyun.music.creativeagent.MusicPromptAgent;
import com.yanyun.music.creativeagent.MusicPromptRequest;
import com.yanyun.music.creativeagent.MusicPromptResult;
import java.math.BigDecimal;
import java.util.Map;

public final class RealDeepSeekMusicPromptAgent implements MusicPromptAgent {

  private static final String AGENT_NAME = "MusicPromptAgent";
  private static final String AGENT_VERSION = "v0.5";
  private static final String TEMPLATE_KEY = "music.prompt.v5";
  private static final int TEMPLATE_VERSION = 5;

  private final DeepSeekJsonChatClient client;
  private final ObjectMapper objectMapper;
  private final AgentRunRecorder agentRunRecorder;

  public RealDeepSeekMusicPromptAgent(
      DeepSeekProperties properties, ObjectMapper objectMapper, AgentRunRecorder agentRunRecorder) {
    this(new DeepSeekJsonChatClient(properties, objectMapper), objectMapper, agentRunRecorder);
  }

  RealDeepSeekMusicPromptAgent(
      DeepSeekJsonChatClient client, ObjectMapper objectMapper, AgentRunRecorder agentRunRecorder) {
    this.client = client;
    this.objectMapper = objectMapper;
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
  }

  @Override
  public MusicPromptResult generate(MusicPromptRequest request) {
    long startedAt = System.nanoTime();
    try {
      if (request == null) {
        throw new IllegalArgumentException("request is required");
      }
      MusicPromptResult result =
          parse(
              client.completeJson(systemPrompt(), userPrompt(request), BigDecimal.valueOf(0.35), 0),
              request);
      record(request, result, startedAt, null);
      return result;
    } catch (RuntimeException exception) {
      record(request, null, startedAt, exception);
      throw exception;
    }
  }

  private MusicPromptResult parse(JsonNode root, MusicPromptRequest request) {
    String title =
        DeepSeekAgentJson.firstNonBlank(DeepSeekAgentJson.text(root, "title"), request.songTitle());
    String lyricsWithTags =
        DeepSeekAgentJson.firstNonBlank(
            DeepSeekAgentJson.text(root, "lyrics_with_structure_tags", "lyricsWithStructureTags"),
            request.lyricsText());
    String stylePrompt =
        sanitizeSingerReferences(
            DeepSeekAgentJson.firstNonBlank(
                DeepSeekAgentJson.text(
                    root, "style_prompt", "stylePrompt", "music_prompt", "musicPrompt"),
                request.musicPromptSeed()));
    String excludePrompt =
        sanitizeSingerReferences(
            DeepSeekAgentJson.firstNonBlank(
                DeepSeekAgentJson.text(root, "exclude_prompt", "excludePrompt"),
                "no direct real singer imitation, no vocal clone, no unrelated IP names"));
    Map<String, Object> providerOptions =
        DeepSeekAgentJson.objectMap(
            objectMapper,
            root.path("provider_options").isMissingNode()
                ? root.path("providerOptions")
                : root.path("provider_options"));
    Map<String, Object> advancedOptions =
        DeepSeekAgentJson.objectMap(
            objectMapper,
            root.path("advanced_options").isMissingNode()
                ? root.path("advancedOptions")
                : root.path("advanced_options"));
    return new MusicPromptResult(
        stylePrompt,
        providerOptions.isEmpty()
            ? Map.of(
                "agent",
                AGENT_NAME,
                "agent_version",
                AGENT_VERSION,
                "provider_profile",
                DeepSeekAgentJson.firstNonBlank(request.musicProvider(), "SUNO"),
                "prompt_template_key",
                TEMPLATE_KEY,
                "prompt_template_version",
                TEMPLATE_VERSION)
            : providerOptions,
        title,
        lyricsWithTags,
        stylePrompt,
        excludePrompt,
        advancedOptions);
  }

  private String systemPrompt() {
    return """
        你是燕云十六声 AI 作曲平台的顶级音乐制作 Agent。
        你的任务是把已经通过燕云创作域判断的歌词，转换成 Suno / MiniMax 可执行的生成规格。

        规则：
        1. 歌词内容必须保持燕云十六声相关，不新增其他 IP 设定。
        2. 用户可以用现实歌手名表达风格偏好，但最终输出不得包含真实歌手名、仿唱、声线模仿、vocal clone。
        3. 不强制短 prompt；style_prompt 可以是中等长度，但必须清晰、可执行、无冲突。
        4. 面向 Suno Custom/Advanced 思路输出 title、lyrics_with_structure_tags、style_prompt、exclude_prompt、advanced_options。
        5. lyrics_with_structure_tags 保持完整歌词，但 style_prompt、exclude_prompt 和 options 必须克制，不写长篇解释。
        6. 只输出 JSON object。

        输出字段：
        {
          "title": "歌曲标题",
          "lyrics_with_structure_tags": "带 [Verse] [Chorus] 等结构标签的歌词",
          "style_prompt": "安全泛化后的音乐风格、情绪、人声、乐器、编曲说明",
          "exclude_prompt": "需要排除的方向",
          "advanced_options": {},
          "provider_options": {}
        }
        """
        .trim();
  }

  private String userPrompt(MusicPromptRequest request) {
    return String.join(
        "\n",
        fieldLine("work_id", request.workId(), 256),
        fieldLine("song_title", request.songTitle(), 120),
        fieldLine("song_summary", request.songSummary(), 800),
        fieldLine("lyrics_text", request.lyricsText(), 8000),
        fieldLine("music_prompt_seed", request.musicPromptSeed(), 1200),
        fieldLine("vocal_preference", request.vocalPreference(), 160),
        fieldLine("music_provider", request.musicProvider(), 80));
  }

  private String fieldLine(String fieldName, String value, int maxLength) {
    return fieldName + "=" + trimToLength(value, maxLength);
  }

  private String sanitizeSingerReferences(String value) {
    if (value == null) {
      return null;
    }
    String sanitized = value.replace("周杰伦", "Chinese pop R&B with light rap rhythmic phrasing");
    sanitized =
        sanitized.replaceAll("(?i)jay\\s*chou", "Chinese pop R&B with light rap rhythmic phrasing");
    sanitized = sanitized.replace("仿唱", "");
    sanitized = sanitized.replace("声线模仿", "");
    sanitized = sanitized.replaceAll("(?i)vocal\\s*clone", "");
    return sanitized.trim();
  }

  private void record(
      MusicPromptRequest request,
      MusicPromptResult result,
      long startedAt,
      RuntimeException exception) {
    agentRunRecorder.record(
        new AgentRunRecord(
            request == null ? null : request.workId(),
            null,
            AGENT_NAME,
            AGENT_VERSION,
            "MUSIC_PROMPT",
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
            exception == null ? null : "MUSIC_PROMPT_AGENT_FAILED",
            exception == null ? null : exception.getMessage()));
  }

  private String outputFingerprint(MusicPromptResult result) {
    return String.join(
        "\n",
        result.musicPrompt(),
        result.providerOptions().toString(),
        nullToEmpty(result.title()),
        nullToEmpty(result.lyricsWithStructureTags()),
        nullToEmpty(result.stylePrompt()),
        nullToEmpty(result.excludePrompt()),
        result.advancedOptions().toString());
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
