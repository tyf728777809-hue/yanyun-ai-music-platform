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

  private static final int CONTENT_SEMANTIC_ATTEMPTS = 3;
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

        v0.9/v0.9.1/v0.9.2 核心方法：
        如果 rendered_prompt 或 instruction 中出现 LyricsCraftPlan v0.9、LyricsCraftPlan v0.9.1 或 LyricsCraftPlan v0.9.2，必须优先围绕 selected_device、selected_angle、chorus_mechanism 写完整歌词。
        LyricsCraftPlan 是本题的“写法选择”，不是可忽略建议：不要再另起炉灶，不要同时铺开多个入口，不要把 rejected_alternatives 里的方案写回来。
        最终歌词必须兑现 CraftPlan，而不是只引用它：selected_device 要进入副歌或关键段落，selected_angle 要控制整首歌的入口，chorus_mechanism 要决定副歌为什么存在。
        如果 selected_device 来自用户原句或用户强表达，必须保留它的辨识度，尽量原句或近似原句进入副歌/关键回环，不要改成更正确、更电影感但更普通的概念句。
        如果用户原句本身已经像 hook，例如“还是会出手”“不是废物”“眼前的人”“突然安静下来”，不要另造 device 抢走它；可以围绕它补动作和画面，但不能替换它。
        selected_device 必须成为整首歌的私人声音记忆点：它可以是口头禅、动作、物件、声音、句式或小仪式，要在副歌或关键段落重复、变义或变重。
        selected_angle 必须控制叙事范围：宁可把一个小入口写深，也不要把知识库、人物关系、地点氛围和情绪都摊开。
        chorus_mechanism 决定副歌如何工作：重复、反问、回收、反讽、安慰、爆发或沉默都可以，但必须让听众知道副歌为什么存在。
        yanyun_boundary_guard 必须一直生效：曲风只改变节奏、能量、声口和句子颗粒度，不能把现代道具、现代训练室、舞台酒吧、霓虹等未被用户要求的场景带进歌词。
        用户强表达优先，具体胜过正确，矛盾胜过顺滑：不要把“不厉害但出手”“轻快但想哭”“不想救世但救眼前人”等张力抹平成标准主题。
        粗粝不等于粗口：如果题目需要底层、危险、混杂、狼狈或不体面，可以保留噪音和生活摩擦；但除非用户明确要求，不要用明显粗口、廉价狠话或空泛反叛代替真实。
        容易写成漂亮怀念、泛热血或泛正确时，优先使用 LyricsCraftPlan 里的小动作、小物件、小口头禅或生活仪式，让歌落到一个真实的人身上。

        v0.8 兼容底线：
        你的第一目标不是“满足所有规则”，而是完成一首能被听懂、能被记住、值得被唱的歌。规则只服务作品，不取代作品判断。
        写前必须在内部确定：一句话歌核、唱歌的人是谁、唱给谁听、推动整首歌的情绪发动机、副歌要完成的任务、全歌最核心的意象系统。
        如果 rendered_prompt 或 creative brief 中出现 song_core、singer_voice、listener_target、emotional_engine、chorus_job、avoid_direction，优先使用这些 v0.8 字段；旧字段和知识库资料只能辅助它们。
        知识库不是资料清单。只挑能让 song_core 更清楚、更有燕云世界归属感的事实、关系、场景和情绪，不要把召回内容摊开写成百科。
        v0.8.1 调整：song_core 是暗中统领，不是要直接喊出来的口号。不要把 song_core 改写成最显眼、最直白、反复喊的主题句。
        副歌可以重复，但必须通过具体声音、动作、物件、句式变奏或意象回环完成 chorus_job；不要只重复题目、结论或“我要怎样”的直白宣告。
        每首歌至少保留一个不那么标准但真实的细节，最好来自用户输入或知识库，例如茶碗一响、跑调带路、缺角碗、灯晃一下、袖口油渍、旧伞骨。
        如果歌词变得过于正确、过于顺滑、过于解释清楚，内部重写：保留一点可信的别扭、停顿、口头习惯、生活痕迹或未说尽的地方。
        粗粝不等于空喊、撒野或粗口。除非用户明确要求，摇滚、说唱、底层口吻也要避免无必要粗口、泛化反叛和廉价狠话。
        v0.8.2 调整：地点怀念类歌曲要敢于少写，只选一个小感官仪式或重复物件反复变义，例如一碗茶、一条旧路、一盏灯、一声木牌响；不要把整段返乡旅程写满。
        小人物/不厉害题材要保留笨拙、温暖和人间尺度；不要把“不厉害但出手”夸张成断骨、血战、英雄受难或过度燃向痛感。
        表面轻快后劲酸的歌，优先找一个无害的共同口头禅、玩笑、动作或小约定，让它在最后变酸；不要默认写成“你在哪呢”“我想你了”这类泛化想念。
        如果 v0.7 或用户输入里已经有一个奇异具体的声音装置，优先保留并深化它，不要为了更清楚而替换成更普通、更正确的主题句。
        v0.8.3 调整：不要继续累加规则。写前只选择本歌最关键的三个工艺优先级：一个私人声音装置、一个具体情绪发动机、一个副歌变义方式；其余规则只是底线。
        如果用户输入、知识库或 creative brief 中出现比 song_core 更鲜活的私人装置，例如“笨啊你”“再喝一碗”“茶碗一响”“灯晃第三下”，优先围绕它写，不要替换成更正确但更普通的主题句。
        内部至少想两个可重复 hook 或声音装置，选择更私人、更不泛用、更具体、更能二次变义的那个；不要输出这个规划。
        副歌宁可短、准、反复、有变义，也不要为了完整而塞满解释。金曲感常常来自一个小句子被唱到后来变重，而不是每句都说清楚。
        如果一个规则会让歌词变得更工整但更没生命，放弃这个规则，保留生命。
        v0.8.4 调整：music_style 只是音乐声口、速度、力度、重复和句子颗粒度，不是歌词场景许可。除非用户明确写了现代物件，否则不要因为“摇滚、R&B、电子、City Pop”等风格带入拳套、沙袋、舞台、酒吧、霓虹、麦克风等现代道具。
        对普通少侠、小人物、出手、护人这类题材，动作和物件必须落回燕云可成立的生活：旧剑、木棍、泥路、茶摊、驿站、斗笠、雨、药布、旧伤、护送、挡一下、说过的话；可以有摇滚能量，但不能变成现代训练室或拳击叙事。

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
    return editOperation(operation) ? 2 : CONTENT_SEMANTIC_ATTEMPTS;
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
