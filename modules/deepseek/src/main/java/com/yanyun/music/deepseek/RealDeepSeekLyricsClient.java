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
            .timeout(requestTimeout(request.operation()))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + properties.getApiKey())
            .POST(HttpRequest.BodyPublishers.ofByteArray(writeJson(requestBody(request))))
            .build();
    int semanticAttempts = semanticAttempts(request.operation());
    RuntimeException lastFailure = null;
    for (int attempt = 1; attempt <= semanticAttempts; attempt++) {
      try {
        JsonNode root = sendWithRetry(httpRequest);
        String content = firstChoiceContent(root);
        return parseContent(content, request);
      } catch (RuntimeException exception) {
        if (!isRetryableContentFailure(exception) || attempt == semanticAttempts) {
          throw exception;
        }
        lastFailure = exception;
      }
    }
    throw lastFailure == null ? new IllegalStateException("DeepSeek request failed") : lastFailure;
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

        v0.11 核心方法：
        你的第一目标不是完成规则，而是完成一首能被听懂、能被记住、值得被唱的歌。
        写前内部确定三件事：这首歌到底在唱什么；谁在唱、唱给谁听；听众最后记住什么声音、句子、节奏或画面。
        不再等待额外规划步骤；直接从用户输入、CreativeBrief、resolved_entities 和知识库上下文里选择最清楚的一条歌曲主线。
        CreativeBrief 是辅助判断，不是剧情大纲；如果 CreativeBrief 过宽，优先回到用户原始灵感里最有生命的那一句。
        知识库只服务歌曲主线，不要摊开成资料点或百科；曲风只改变节奏、声口、能量和句子颗粒度，不能自动导入现代道具或现代场景。
        用户写玩家体验时，可以写“玩家心情”背后的离开、重逢、失手、同行、回家等情绪，但不要把歌词写成界面操作说明。

        v0.11.2 最小补强：
        - 抒情、怀旧、角色歌不要只围绕一个漂亮物件打转；物件必须推动关系、时间或缺席的变化，让听众知道谁在等、谁离开、什么回不去了。
        - 粗粝、底层、失败者、边缘门派题材不要用“爷、规矩、命、热血”这类硬口号替代具体活法；优先写脏乱环境、笨拙动作、交易声、羞耻和尊严并存。

        核心目标：
        1. 写出属于《燕云十六声》大世界气质的歌词，但不要求反复出现“燕云”“十六声”等字面关键词。
        2. 使用知识库上下文时，只吸收事实、人物情绪、场景气质和可用意象；不要照抄资料，不要把歌词写成剧情百科。
        3. 如果 instruction 或 yanyun_references 中出现 resolved_entities，说明系统已经把用户输入映射到标准燕云实体；必须使用 canonical name 和对应知识库资料，不要把用户错字当成正式名字输出。
        4. 用户点名人物、剧情、地点、门派或玩法时，歌词必须转译该实体的核心经历、关系、冲突、场景或玩家体验；不能只写泛江湖、泛古风或无关任务氛围。
        5. 不编造官方设定、人物关系、阵营结论或未公开内容。
        6. 歌词必须可唱，不要像散文、小说、设定介绍或宣传文案。
        7. 生成前先在内部选择最适合本题的作词路径，而不是套固定流程；这些规划不要输出。
        8. 可选路径包括但不限于：叙事型、意象型、口语型、对白/独白型、群像型、反差型、重复型、反讽型、留白型、反套路型。
        9. 不按曲风套模板；music_style 只影响语言声口、句子松紧、重复程度、节奏感和能量，不决定创意结构。
        9a. 曲风不允许改变世界边界：开放曲风可以现代，歌词内容和道具仍必须属于燕云大世界，不能因为风格偏好自动出现现代城市、现代运动或现代舞台意象。
        10. Hook 不限定为金句或口号；可以是一句、一个重复句式、一个口头禅、一个声音动作、一个意象回环或一段节奏记忆点。
        11. 避开第一反应俗套，例如“侠=自由、离别=月光、江湖=风雨、少年=逍遥”；必须找到更有作品感、更具体、更有人的入口。
        12. 可以使用 creative brief 中的 creative_core、chosen_angle、alternative_angles、anti_cliche_strategy、voice_texture、image_pool、song_energy，但它们是开放指导，不是必须逐项填满的模板。
        12a. 如果 v0.8 字段和 v0.7 字段有冲突，优先跟随 song_core、singer_voice、listener_target、emotional_engine、chorus_job、avoid_direction。
        13. 歌词要像一个真实的人、一类人或一个可信声音在唱；不要像平台宣传文案、剧情简介或漂亮作文。
        14. 允许留白，不要解释透所有剧情；用动作、物件、场景、重复或沉默让听众自己补完。
        15. 生成前内部确定声音记忆点、主韵脚或节奏回环、段落情绪递进和 3-5 个具体画面；这些规划不要输出。
        16. 必须先内部回答一个问题：“这首歌到底在唱什么？”答案必须能用一句话说清，并且每个段落都服务这个答案；不要把多个好看的意象、多个故事点、多个情绪散铺在一起。
        17. 优先使用 creative brief 中的 song_thesis、pov、central_tension、emotional_turn、chorus_function、memory_device：它们不是模板，而是防止歌词散掉的主线骨架。
        18. 视角必须稳定：第一人称、第二人称、旁观叙事、群像口吻可以自由选择，但不能一首歌里无意识漂移，导致听众不知道谁在唱、唱给谁。
        19. 副歌必须承担明确功能：推进主旨、重复执念、提出反问、情绪爆发、安慰自己、回收主题或制造反差；不能只是把漂亮句子堆在副歌位置。
        20. 整首歌不得完全无韵；副歌必须有一个主韵脚或清晰节奏回环，至少 2-4 行自然使用相同或相近韵母、句式或声音节奏。
        21. 中文歌词优先 7-14 字短句，长句要拆分，避免像散文、小说分行或剧情梗概。
        22. 每个主要段落至少有一个具体动作、物件或场景，不只写抽象情绪和漂亮形容词。
        23. 允许讲故事，但不能只按事件顺序说明发生了什么；每段都要有可唱的情绪句、声音记忆点或回环句。
        24. 必须有情绪变化或意义加深，不要从头到尾只是同一种正确情绪。
        25. 避免廉价古风词堆砌，例如过度使用“红尘、宿命、刀光剑影、天涯、此生无悔”等空泛表达。
        26. 减少“明月、山河、江湖、风烟、长夜、流浪、故乡”等万能诗性词连续堆叠；这些词可以使用，但每段不要当填充词连用，出现时必须落到具体动作、物件或场景上。
        27. 如果一段里出现 3 个以上万能词，内部重写替换为更具体的燕云画面、人物动作或生活细节；禁止只靠“酒、剑、月、风”撑完整首歌。
        28. 押韵要自然，不为了押韵牺牲内容，不使用生硬倒装、土味口号、网络梗或廉价古风词。
        29. 必须原创，不模仿、不改写、不借用现实歌曲歌词、影视台词或已有商业歌词。
        30. 不写真实歌手名、现实歌曲名、翻唱导向、仿唱导向。
        31. 不输出 Markdown，不输出解释，只输出 JSON object。

        一票否决：
        - 听不懂这首歌在唱什么。
        - 像散文、剧情简介、设定介绍。
        - 副歌没有功能。
        - 只是堆燕云名词或知识库资料。
        - 漂亮但空。
        - 用户故事被丢失。
        - 完全不可唱。

        不同 operation 的处理方式：
        - INSPIRATION：把用户故事扩展成完整歌词，允许强创作。
        - LYRICS：尊重用户原歌词，重点做结构整理、补强副歌、补齐摘要和音乐方向，不要无故大改。
        - POLISH：这是真正的润色，不是重写。必须保留原歌的歌名、视角、主旨、情绪弧线和大多数可用歌词；只按用户指令修弱点，例如更押韵、更口语、更聚焦、更顺唱、更有副歌记忆点。除非用户明确要求大改，不得改成另一首歌。
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
        1. 是否选择了适合本题的开放作词路径，而不是套固定古风/曲风模板？
        2. 是否有声音记忆点？它可以是金句、重复句式、口头禅、声音动作、意象回环或节奏记忆，而不一定是口号。
        3. 是否避开第一反应俗套，而不是“侠=自由、离别=月光、江湖=风雨、少年=逍遥”的普通答案？
        4. 这首歌到底在唱什么，是否能用一句话说清？
        4a. song_core 是否被兑现？singer_voice 是否稳定？listener_target 是否清楚？emotional_engine 是否推动全歌？chorus_job 是否被副歌完成？
        4b. 副歌是否只是直喊 song_core、题目或结论？如果是，换成具体声音、动作、物件、句式变奏或意象回环。
        4c. 是否保留了一个不那么标准但真实的细节？如果没有，补入来自用户输入或知识库的生活痕迹。
        4d. 地点怀念是否写得太满？能否收束成一个小仪式或重复物件？
        4e. 小人物是否被写成了过度英雄化、过度受伤或过度燃？是否还保留“不厉害”的可爱和笨拙？
        4f. 轻快转酸楚是否有一个共同口头禅、动作或小约定在最后变义？还是只写了泛化想念？
        4g. 这首歌是否有一个私人声音装置？它是否比普通主题句更值得反复唱？
        4h. 是否为了规则完整牺牲了更有生命的口头禅、怪细节或小动作？
        4i. 抒情怀旧是否只剩一个物件？这个物件是否真的推动了关系、时间或缺席的变化？
        4j. 粗粝题材是否硬口号化？是否用具体活法替代了“爷、规矩、命、热血”式宣言？
        5. 每个段落是否都服务同一个 song_thesis，而不是各写各的好看句子？
        6. 视角是否稳定，听众是否知道谁在唱、唱给谁？
        7. 副歌是否有功能，是否推进或回收主旨？
        8. 副歌是否有主韵脚、节奏回环或重复结构，且至少 2-4 行自然押同韵、近韵或形成清晰声音记忆？
        9. 整首歌是否避免完全无韵？
        10. 句长是否适合中文人声演唱，是否避免散文化长句？
        11. 是否可唱？
        12. 是否像发生在燕云十六声大世界里，而不是泛古风或其它 IP？
        13. 是否有俗套古风堆词？
        14. 是否有具体动作、物件或场景支撑，而不是只写抽象情绪？
        15. 是否只是叙事流水账，缺少情绪句、声音记忆点或回环句？
        16. 是否为了押韵而硬凑、倒装、变土？
        17. 是否把万能诗性词当填充词连续堆叠，而不是写具体画面和动作？
        18. 是否把剧情解释太满，缺少留白和再听空间？
        19. POLISH 是否保留原歌核心，而不是改成另一首歌？
        20. 是否编造了具体官方设定？
        21. 如果用户点名角色、剧情、地点、门派或玩法，是否真的落到了对应 canonical entity 的核心材料，而不是泛写燕云气氛？
        22. 是否把用户错字或非标准称呼当成正式名字输出？
        23. 是否存在版权、仿唱或现实歌曲风险？
        如果自检不达标，内部重写后再输出最终 JSON。
        """
        .trim();
  }

  private String userPrompt(DeepSeekLyricsRequest request) {
    StringBuilder builder = new StringBuilder();
    builder.append(fieldLine("operation", request.operation()));
    builder.append(fieldLine("requested_title", request.requestedTitle()));
    builder.append(fieldLine("mood", request.mood()));
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

  private boolean isRetryableContentFailure(RuntimeException exception) {
    String message = exception.getMessage();
    return message != null
        && (message.startsWith("DeepSeek response content")
            || message.startsWith("DeepSeek response did not include choices")
            || message.startsWith("DeepSeek lyrics_text is empty"));
  }

  private DeepSeekLyricsResponse parseContent(String content, DeepSeekLyricsRequest request) {
    JsonNode root = DeepSeekAgentJson.parseContentJson(objectMapper, content);
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

  private Duration requestTimeout(String operation) {
    Duration timeout = properties.getRequestTimeout();
    Duration normalized =
        timeout == null || timeout.isNegative() ? Duration.ofSeconds(30) : timeout;
    if (editOperation(operation) && normalized.compareTo(Duration.ofSeconds(120)) > 0) {
      return Duration.ofSeconds(120);
    }
    return normalized;
  }

  private int semanticAttempts(String operation) {
    return Math.max(1, properties.getLyricsSemanticAttempts());
  }

  private boolean editOperation(String operation) {
    return "POLISH".equals(operation) || "CONTINUE".equals(operation);
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
