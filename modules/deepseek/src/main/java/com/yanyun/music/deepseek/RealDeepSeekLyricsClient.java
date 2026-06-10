package com.yanyun.music.deepseek;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class RealDeepSeekLyricsClient implements DeepSeekLyricsClient {

  private static final Pattern BEARER_TOKEN_PATTERN =
      Pattern.compile("Bearer\\s+[A-Za-z0-9._~+/=-]+", Pattern.CASE_INSENSITIVE);
  private static final Pattern API_KEY_PATTERN =
      Pattern.compile("sk-[A-Za-z0-9_-]{8,}", Pattern.CASE_INSENSITIVE);

  private final DeepSeekProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public RealDeepSeekLyricsClient(DeepSeekProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, HttpClient.newHttpClient());
  }

  RealDeepSeekLyricsClient(
      DeepSeekProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
    if (properties == null) {
      throw new IllegalArgumentException("properties is required");
    }
    if (objectMapper == null) {
      throw new IllegalArgumentException("objectMapper is required");
    }
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient == null ? HttpClient.newHttpClient() : httpClient;
  }

  @Override
  public DeepSeekLyricsResponse generate(DeepSeekLyricsRequest request) {
    ensureConfigured();
    HttpRequest httpRequest =
        HttpRequest.newBuilder(chatCompletionsUri())
            .timeout(requestTimeout())
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + properties.getApiKey())
            .POST(HttpRequest.BodyPublishers.ofByteArray(writeJson(requestBody(request))))
            .build();
    JsonNode root = sendWithRetry(httpRequest);
    String content = firstChoiceContent(root);
    return parseContent(content, request);
  }

  @Override
  public String modelName() {
    return properties.getModelName();
  }

  private Map<String, Object> requestBody(DeepSeekLyricsRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", properties.getModelName());
    body.put(
        "messages",
        List.of(
            Map.of("role", "system", "content", systemPrompt()),
            Map.of("role", "user", "content", userPrompt(request))));
    body.put("response_format", Map.of("type", "json_object"));
    body.put("temperature", properties.getTemperature());
    body.put("max_tokens", properties.getResponseMaxTokens());
    return body;
  }

  private String systemPrompt() {
    return """
        你是燕云十六声 AI 作曲平台的顶级中文作词 Agent。

        你的身份：
        你是世界级中文作词家、游戏主题曲歌词创作者、音乐叙事导演。

        你的任务：
        根据用户输入、当前歌词、修改指令、曲风偏好和人声偏好，生成适合 AI 音乐模型演唱的中文原创歌词，并同时输出歌名、歌曲摘要、音乐方向和封面视觉种子。创作目标是达到世界级金曲标准。

        核心目标：
        1. 写出有燕云十六声气质的歌词。
        2. 不编造官方设定、人物关系、阵营结论或未公开内容。
        3. 歌词必须可唱，不要像散文、小说、设定介绍或宣传文案。
        4. 歌词必须有强记忆点，适合用户听完愿意分享。
        5. 必须有清晰情绪推进，不要平铺直叙。
        6. 避免廉价古风词堆砌，例如过度使用“红尘、宿命、刀光剑影、天涯、此生无悔”等空泛表达。
        7. 必须原创，不模仿、不改写、不借用现实歌曲歌词、影视台词或已有商业歌词。
        8. 不写真实歌手名、现实歌曲名、翻唱导向、仿唱导向。
        9. 不输出 Markdown，不输出解释，只输出 JSON object。

        不同 operation 的处理方式：
        - INSPIRATION：把用户故事扩展成完整歌词，允许强创作。
        - LYRICS：尊重用户原歌词，重点做结构整理、补强副歌、补齐摘要和音乐方向，不要无故大改。
        - POLISH：保留原意，提升意象、可唱性、段落层次和副歌记忆点。
        - CONTINUE：沿着当前歌词继续写，保持风格一致，并补出自然的后续段落。

        歌词结构建议：
        [Verse 1]
        [Pre-Chorus]
        [Chorus]
        [Verse 2]
        [Bridge]
        [Final Chorus]
        [Outro]

        如果用户输入很短，可以生成标准完整结构。
        如果用户已有歌词结构，尽量保留并优化。

        质量标准：
        - 0.90-1.00：有明确故事、有高级意象、副歌强、可唱、燕云气质鲜明。
        - 0.80-0.89：整体可用，但副歌或意象仍可加强。
        - 0.70-0.79：勉强可用，存在俗套、平铺或记忆点不足。
        - 0.70 以下：必须视为低质量，需要重写。

        输出 JSON 字段必须包含：
        {
          "song_title": "中文歌名，短、有记忆点，不超过 10 个字",
          "song_summary": "一句话概括歌曲故事和情绪",
          "lyrics_text": "完整歌词，包含段落标签",
          "music_prompt": "简洁音乐方向，描述曲风、情绪、乐器、人声和编曲走向",
          "cover_prompt_seed": "封面视觉方向，不包含文字、logo、水印",
          "risk_notes": ["风险提示，没有风险则为空数组"],
          "quality_score": 0.0
        }

        生成前自检：
        1. 副歌是否有记忆点？
        2. 是否可唱？
        3. 是否有燕云十六声气质？
        4. 是否有俗套古风堆词？
        5. 是否编造了具体官方设定？
        6. 是否存在版权、仿唱或现实歌曲风险？
        如果自检不达标，内部重写后再输出最终 JSON。
        """
        .trim();
  }

  private String userPrompt(DeepSeekLyricsRequest request) {
    StringBuilder builder = new StringBuilder();
    builder.append(fieldLine("operation", request.operation()));
    builder.append(fieldLine("requested_title", request.requestedTitle()));
    builder.append(fieldLine("music_style", request.musicStyle()));
    builder.append(fieldLine("vocal_preference", request.vocalPreference()));
    builder.append(fieldLine("user_input", request.userInput()));
    builder.append(fieldLine("current_lyrics", request.currentLyrics()));
    builder.append(fieldLine("instruction", request.instruction()));
    builder.append(fieldLine("rendered_prompt", request.prompt()));
    builder.append("yanyun_references=");
    builder.append(request.yanyunReferences());
    builder.append('\n');
    return builder.toString();
  }

  private String fieldLine(String fieldName, String value) {
    return fieldName + "=" + trimToLength(value, 8000) + "\n";
  }

  private JsonNode sendWithRetry(HttpRequest request) {
    RuntimeException lastFailure = null;
    for (int attempt = 1; attempt <= properties.getMaxAttempts(); attempt++) {
      try {
        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= HttpURLConnection.HTTP_OK
            && response.statusCode() < HttpURLConnection.HTTP_MULT_CHOICE) {
          return objectMapper.readTree(response.body());
        }
        RuntimeException failure = httpFailure(response.statusCode(), response.body());
        if (response.statusCode() < 500 || attempt == properties.getMaxAttempts()) {
          throw failure;
        }
        lastFailure = failure;
      } catch (HttpTimeoutException exception) {
        lastFailure = new IllegalStateException("DeepSeek request timed out", exception);
      } catch (JsonProcessingException exception) {
        throw new IllegalStateException("DeepSeek response JSON is invalid", exception);
      } catch (IOException exception) {
        lastFailure = new IllegalStateException("DeepSeek request failed", exception);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("DeepSeek request interrupted", exception);
      }
    }
    throw lastFailure == null ? new IllegalStateException("DeepSeek request failed") : lastFailure;
  }

  private RuntimeException httpFailure(int statusCode, String body) {
    String fallback = "DeepSeek request failed with HTTP " + statusCode;
    if (body == null || body.isBlank()) {
      return new IllegalStateException(fallback);
    }
    try {
      JsonNode root = objectMapper.readTree(body);
      String providerMessage =
          firstNonBlank(
              text(root.path("error"), "message", "msg", "detail"),
              text(root, "message", "msg", "error"));
      return new IllegalStateException(sanitize(firstNonBlank(providerMessage, fallback)));
    } catch (JsonProcessingException exception) {
      return new IllegalStateException(fallback);
    }
  }

  private String firstChoiceContent(JsonNode root) {
    JsonNode choices = root.path("choices");
    if (!choices.isArray() || choices.isEmpty()) {
      throw new IllegalStateException("DeepSeek response did not include choices");
    }
    String content = choices.get(0).path("message").path("content").asText("");
    if (content.isBlank()) {
      throw new IllegalStateException("DeepSeek response content is empty");
    }
    return content;
  }

  private DeepSeekLyricsResponse parseContent(String content, DeepSeekLyricsRequest request) {
    try {
      JsonNode root = objectMapper.readTree(content);
      String lyricsText = firstNonBlank(text(root, "lyrics_text", "lyricsText"), "");
      if (lyricsText.isBlank()) {
        throw new IllegalStateException("DeepSeek lyrics_text is empty");
      }
      return new DeepSeekLyricsResponse(
          firstNonBlank(text(root, "song_title", "songTitle"), request.requestedTitle()),
          firstNonBlank(text(root, "song_summary", "songSummary"), "燕云主题原创歌曲。"),
          lyricsText,
          firstNonBlank(text(root, "music_prompt", "musicPrompt"), request.musicStyle()),
          firstNonBlank(text(root, "cover_prompt_seed", "coverPromptSeed"), "燕云山河国风封面"),
          stringList(
              root.path("risk_notes").isMissingNode()
                  ? root.path("riskNotes")
                  : root.path("risk_notes")),
          qualityScore(
              root.path("quality_score").isMissingNode()
                  ? root.path("qualityScore")
                  : root.path("quality_score")));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("DeepSeek response content JSON is invalid", exception);
    }
  }

  private List<String> stringList(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return List.of();
    }
    if (!node.isArray()) {
      String value = node.asText("");
      return value.isBlank() ? List.of() : List.of(value);
    }
    List<String> values = new ArrayList<>();
    for (JsonNode item : node) {
      String value = item.asText("");
      if (!value.isBlank()) {
        values.add(value);
      }
    }
    return values;
  }

  private BigDecimal qualityScore(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return BigDecimal.valueOf(0.8);
    }
    try {
      BigDecimal score = new BigDecimal(node.asText("0.8"));
      if (score.compareTo(BigDecimal.ZERO) < 0) {
        return BigDecimal.ZERO;
      }
      if (score.compareTo(BigDecimal.ONE) > 0) {
        return BigDecimal.ONE;
      }
      return score;
    } catch (NumberFormatException exception) {
      return BigDecimal.valueOf(0.8);
    }
  }

  private String text(JsonNode node, String... fieldNames) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    for (String fieldName : fieldNames) {
      JsonNode value = node.path(fieldName);
      if (!value.isMissingNode() && !value.isNull() && !value.asText("").isBlank()) {
        return value.asText();
      }
    }
    return null;
  }

  private void ensureConfigured() {
    if (!properties.isRealCallsEnabled()) {
      throw new IllegalStateException(
          "DeepSeek real calls are disabled; set DEEPSEEK_REAL_CALLS_ENABLED=true for manual integration");
    }
    if (!properties.isAgentRealCallsEnabled()) {
      throw new IllegalStateException(
          "AGENT_REAL_CALLS_ENABLED=true is required before DeepSeek real calls");
    }
    if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
      throw new IllegalStateException("DEEPSEEK_API_KEY is required for DeepSeek calls");
    }
    if (properties.getBaseUrl() == null) {
      throw new IllegalStateException("DEEPSEEK_BASE_URL is required for DeepSeek calls");
    }
    if (properties.getModelName() == null || properties.getModelName().isBlank()) {
      throw new IllegalStateException("DEEPSEEK_MODEL_NAME is required for DeepSeek calls");
    }
  }

  private URI chatCompletionsUri() {
    String base = properties.getBaseUrl().toString();
    if (!base.endsWith("/")) {
      base = base + "/";
    }
    return URI.create(base).resolve("chat/completions");
  }

  private Duration requestTimeout() {
    Duration timeout = properties.getRequestTimeout();
    return timeout == null || timeout.isNegative() ? Duration.ofSeconds(30) : timeout;
  }

  private byte[] writeJson(Object value) {
    try {
      return objectMapper.writeValueAsBytes(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("DeepSeek request JSON cannot be written", exception);
    }
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String trimToLength(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }

  private String sanitize(String value) {
    String sanitized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
    sanitized = BEARER_TOKEN_PATTERN.matcher(sanitized).replaceAll("Bearer <redacted>");
    sanitized = API_KEY_PATTERN.matcher(sanitized).replaceAll("<api-key-redacted>");
    return sanitized.length() <= 240 ? sanitized : sanitized.substring(0, 240);
  }
}
