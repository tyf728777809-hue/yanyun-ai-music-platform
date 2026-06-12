package com.yanyun.music.deepseek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.creativeagent.CoverPromptRequest;
import com.yanyun.music.creativeagent.CoverPromptResult;
import com.yanyun.music.creativeagent.CreativeBriefRequest;
import com.yanyun.music.creativeagent.CreativeBriefResult;
import com.yanyun.music.creativeagent.CreativeDomainDecision;
import com.yanyun.music.creativeagent.MusicPromptRequest;
import com.yanyun.music.creativeagent.MusicPromptResult;
import com.yanyun.music.creativeagent.QualityDecision;
import com.yanyun.music.creativeagent.QualityEvaluationRequest;
import com.yanyun.music.creativeagent.QualityEvaluationResult;
import com.yanyun.music.creativeagent.QualityGate;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RealDeepSeekCreativeAgentsTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void creativeBriefRejectsUnrelatedIpBeforeCallingDeepSeek() throws IOException {
    AtomicInteger requestCount = new AtomicInteger();
    server =
        startServer(
            exchange -> {
              requestCount.incrementAndGet();
              respondJson(exchange, 200, chatResponse(Map.of("domain_decision", "PASS")));
            });
    List<AgentRunRecord> records = new ArrayList<>();
    RealDeepSeekCreativeBriefAgent agent =
        new RealDeepSeekCreativeBriefAgent(client(), records::add);

    CreativeBriefResult result =
        agent.generate(
            new CreativeBriefRequest(
                "user-1",
                "work-1",
                "INSPIRATION",
                "写一首高达歌",
                null,
                null,
                null,
                null,
                null,
                List.of()));

    assertEquals(CreativeDomainDecision.REJECT, result.domainDecision());
    assertEquals(0, requestCount.get());
    assertEquals("CreativeBriefAgent", records.getFirst().agentName());
    assertEquals("v0.5", records.getFirst().agentVersion());
  }

  @Test
  void creativeBriefAddsFallbackYanyunReferencesWhenModelLeavesThemEmpty() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "domain_decision",
                            "PASS",
                            "creative_intent",
                            "玩家想写雁门风雪里的江湖归来。",
                            "theme",
                            "雁门埋刀",
                            "mood_tags",
                            List.of("苍凉", "释然"),
                            "narrative_viewpoint",
                            "player-facing",
                            "music_direction",
                            "cinematic Chinese folk ballad",
                            "yanyun_references",
                            List.of(),
                            "constraints",
                            List.of("must remain Yanyun-related"),
                            "risk_notes",
                            List.of(),
                            "freeform_opportunities",
                            List.of()))));
    RealDeepSeekCreativeBriefAgent agent =
        new RealDeepSeekCreativeBriefAgent(client(), new ArrayList<AgentRunRecord>()::add);

    CreativeBriefResult result =
        agent.generate(
            new CreativeBriefRequest(
                "user-1",
                "work-1",
                "INSPIRATION",
                "我是燕云十六声玩家，想写一首雁门风雪里无名游侠埋刀归来的歌。",
                null,
                null,
                null,
                null,
                null,
                List.of()));

    assertTrue(result.yanyunReferences().contains("雁门关外风雪"));
    assertTrue(result.yanyunReferences().contains("燕云十六声玩家故事"));
    assertTrue(result.yanyunReferences().contains("江湖游侠心境"));
  }

  @Test
  void musicPromptSanitizesRealSingerReferencesFromModelOutput() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "title",
                            "燕云行",
                            "lyrics_with_structure_tags",
                            "[Verse]\n燕云路远",
                            "style_prompt",
                            "周杰伦 仿唱 国风 R&B 轻说唱",
                            "exclude_prompt",
                            "no Jay Chou vocal clone",
                            "provider_options",
                            Map.of("provider_profile", "SUNO")))));
    RealDeepSeekMusicPromptAgent agent =
        new RealDeepSeekMusicPromptAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    MusicPromptResult result =
        agent.generate(
            new MusicPromptRequest(
                "work-1", "燕云行", "summary", "[Verse]\n燕云路远", "周杰伦风格", null, "SUNO"));

    assertTrue(result.musicPrompt().contains("Chinese pop R&B"));
    assertFalse(result.musicPrompt().contains("周杰伦"));
    assertFalse(result.musicPrompt().contains("仿唱"));
    assertFalse(result.excludePrompt().toLowerCase().contains("jay chou"));
  }

  @Test
  void musicPromptParsesJsonObjectWrappedInMarkdownFence() throws IOException {
    String content =
        """
        ```json
        {
          "title": "燕云行",
          "lyrics_with_structure_tags": "[Verse]\\n燕云路远",
          "style_prompt": "cinematic Chinese folk ballad with warm female vocal",
          "exclude_prompt": "no vocal clone",
          "provider_options": {"provider_profile": "SUNO"}
        }
        ```
        """;
    server = startServer(exchange -> respondJson(exchange, 200, chatResponseContent(content)));
    RealDeepSeekMusicPromptAgent agent =
        new RealDeepSeekMusicPromptAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    MusicPromptResult result =
        agent.generate(
            new MusicPromptRequest(
                "work-1", "燕云行", "summary", "[Verse]\n燕云路远", "国风民谣", null, "SUNO"));

    assertEquals("燕云行", result.title());
    assertTrue(result.musicPrompt().contains("Chinese folk ballad"));
  }

  @Test
  void musicPromptRetriesOnceWhenDeepSeekReturnsEmptyContent() throws IOException {
    AtomicInteger requestCount = new AtomicInteger();
    server =
        startServer(
            exchange -> {
              if (requestCount.incrementAndGet() == 1) {
                respondJson(exchange, 200, chatResponseContent(""));
                return;
              }
              respondJson(
                  exchange,
                  200,
                  chatResponse(
                      Map.of(
                          "title",
                          "燕云行",
                          "lyrics_with_structure_tags",
                          "[Verse]\\n燕云路远",
                          "style_prompt",
                          "cinematic Chinese folk ballad with warm female vocal",
                          "exclude_prompt",
                          "no vocal clone",
                          "provider_options",
                          Map.of("provider_profile", "SUNO"))));
            });
    RealDeepSeekMusicPromptAgent agent =
        new RealDeepSeekMusicPromptAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    MusicPromptResult result =
        agent.generate(
            new MusicPromptRequest(
                "work-1", "燕云行", "summary", "[Verse]\n燕云路远", "国风民谣", null, "SUNO"));

    assertEquals(2, requestCount.get());
    assertEquals("燕云行", result.title());
    assertTrue(result.musicPrompt().contains("Chinese folk ballad"));
  }

  @Test
  void jsonChatClientExtractsFirstJsonObjectFromWrappedContent() {
    JsonNode root =
        new DeepSeekJsonChatClient(
                properties(URI.create("http://127.0.0.1"), true, true), objectMapper)
            .parseContentJson(
                "好的，下面是 JSON： {\"title\":\"燕云行\",\"nested\":{\"brace\":\"{ok}\"}} 完成。");

    assertEquals("燕云行", root.path("title").asText());
    assertEquals("{ok}", root.path("nested").path("brace").asText());
  }

  @Test
  void coverPromptForcesNoTextEvenWhenModelSuggestsTitleTypography() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "visual_prompt",
                            "premium 16:9 album cover with clear Chinese title typography",
                            "text_prompt",
                            "Use only the song title as main title",
                            "negative_prompt",
                            "fake singer, fake copyright, watermark",
                            "width",
                            1920,
                            "height",
                            1080,
                            "typography_requirements",
                            List.of("clear readable Chinese title")))));
    RealDeepSeekCoverPromptAgent agent =
        new RealDeepSeekCoverPromptAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    CoverPromptResult result =
        agent.generate(
            new CoverPromptRequest(
                "work-1", "燕云行", "summary", "lyrics", "style", "seed", 1920, 1080));

    assertFalse(result.visualPrompt().contains("with clear Chinese title typography"));
    assertTrue(result.visualPrompt().contains("No text in the image"));
    assertTrue(result.negativePrompt().contains("Chinese characters"));
    assertEquals("NO_TEXT_IN_IMAGE", result.textPrompt());
    assertEquals("NO_TEXT_IN_IMAGE", result.providerOptions().get("text_policy"));
    assertEquals(1920, result.width());
    assertEquals(1080, result.height());
  }

  @Test
  void coverPromptUsesConfiguredMaxTokensAndTrimsLongLyricsInput() throws IOException {
    AtomicReference<JsonNode> capturedBody = new AtomicReference<>();
    server =
        startServer(
            exchange -> {
              capturedBody.set(
                  objectMapper.readTree(
                      new String(
                          exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
              respondJson(
                  exchange,
                  200,
                  chatResponse(
                      Map.of(
                          "visual_prompt",
                          "premium 16:9 album cover with clear Chinese title typography",
                          "text_prompt",
                          "Use only the song title as main title",
                          "negative_prompt",
                          "fake singer, fake copyright, watermark",
                          "width",
                          1920,
                          "height",
                          1080)));
            });
    DeepSeekProperties properties = properties(serverBaseUri(), true, true);
    properties.setResponseMaxTokens(4096);
    RealDeepSeekCoverPromptAgent agent =
        new RealDeepSeekCoverPromptAgent(
            new DeepSeekJsonChatClient(properties, objectMapper),
            objectMapper,
            new ArrayList<AgentRunRecord>()::add);

    CoverPromptResult result =
        agent.generate(
            new CoverPromptRequest(
                "work-1", "燕云行", "summary", "燕云".repeat(3000), "style", "seed", 1920, 1080));

    String userPrompt = capturedBody.get().path("messages").get(1).path("content").asText();
    assertEquals(4096, capturedBody.get().path("max_tokens").asInt());
    assertTrue(userPrompt.contains("lyrics_excerpt="));
    assertFalse(userPrompt.contains("lyrics_text="));
    assertTrue(userPrompt.length() < 3800);
    assertEquals(1920, result.width());
  }

  @Test
  void qualityAgentLocalSafetyRejectsSingerImitationEvenWhenModelPasses() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "MUSIC",
                            "decision",
                            "PASS",
                            "score",
                            95,
                            "reasons",
                            List.of(),
                            "recommended_action",
                            "PASS",
                            "retryable",
                            false))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.MUSIC,
                "燕云行",
                "燕云路远，十六州风雪未歇",
                "SUNO",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of("music_prompt", "周杰伦 仿唱")));

    assertEquals(QualityDecision.REWRITE, result.decision());
  }

  @Test
  void qualityAgentLocalSafetyRejectsGenericLyricsWithoutYanyunAnchor() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "LYRICS",
                            "decision",
                            "PASS",
                            "score",
                            92,
                            "reasons",
                            List.of(),
                            "recommended_action",
                            "PASS",
                            "retryable",
                            false))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.LYRICS,
                "江湖月",
                "[Verse]\n江湖夜雨十年灯\n孤舟一叶过长风",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of(
                    "song_summary", "一个游侠在雨夜告别旧梦。",
                    "music_prompt", "cinematic Chinese folk ballad",
                    "cover_prompt_seed", "moonlit river and lone boat")));

    assertEquals(QualityDecision.REWRITE, result.decision());
    assertTrue(result.reasons().contains("歌词缺少明确的燕云十六声锚点，容易变成泛古风武侠。"));
  }

  @Test
  void qualityAgentLocalSafetyRejectsCoverTitleTypographyUntilOcrGateExists() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "COVER",
                            "decision",
                            "REWRITE",
                            "score",
                            65,
                            "reasons",
                            List.of(
                                "Cover prompt only contains song title '雁门夜雪' in calligraphy, no fake singer, copyright, label, UI, watermarks, or garbled text."),
                            "recommended_action",
                            "REWRITE_AGENT_OUTPUT",
                            "retryable",
                            true))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.COVER,
                "燕云行",
                "lyrics",
                null,
                null,
                null,
                null,
                1920,
                1080,
                null,
                null,
                null,
                null,
                null,
                Map.of(
                    "visual_prompt",
                    "premium 16:9 album cover with clear Chinese song title typography",
                    "text_prompt",
                    "Use only the song title as the main cover title. Do not invent singer, label, copyright, or small credits.",
                    "negative_prompt",
                    "low quality, fake singer name, fake label, fake copyright, watermark, UI, garbled text",
                    "typography_requirements",
                    List.of("clear readable Chinese title", "no fake singer credits"))));

    assertEquals(QualityDecision.REWRITE, result.decision());
    assertTrue(result.retryable());
  }

  @Test
  void qualityAgentLocalSafetyAllowsNoTextCoverPolicyWhenModelFlagsItAsRewrite()
      throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "COVER",
                            "decision",
                            "REWRITE",
                            "score",
                            65,
                            "reasons",
                            List.of(
                                "Cover prompt explicitly disallows any text, signatures, watermarks, or fake artist/label info, matching guidelines."),
                            "recommended_action",
                            "REWRITE_AGENT_OUTPUT",
                            "retryable",
                            true))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.COVER,
                "燕云行",
                "lyrics",
                null,
                null,
                null,
                null,
                1920,
                1080,
                null,
                null,
                null,
                null,
                null,
                Map.of(
                    "visual_prompt",
                    "premium 16:9 album cover, no text in the image, no Chinese characters",
                    "negative_prompt",
                    "low quality, text, Chinese characters, fake singer name",
                    "text_policy",
                    "NO_TEXT_IN_IMAGE")));

    assertEquals(QualityDecision.PASS, result.decision());
    assertEquals(80, result.score());
    assertFalse(result.retryable());
  }

  @Test
  void qualityAgentLocalSafetyAllowsChineseNoTextCoverPolicyReason() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "COVER",
                            "decision",
                            "REWRITE",
                            "score",
                            65,
                            "reasons",
                            List.of("封面 prompt 明确禁止任何文字、歌手名、版权信息，无违规元素；画面构图预留了后期文字叠加空间，符合排版设计要求"),
                            "recommended_action",
                            "REWRITE_AGENT_OUTPUT",
                            "retryable",
                            true))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.COVER,
                "燕云行",
                "lyrics",
                null,
                null,
                null,
                null,
                1920,
                1080,
                null,
                null,
                null,
                null,
                null,
                Map.of(
                    "visual_prompt",
                    "premium 16:9 album cover, no text in the image, no Chinese characters",
                    "negative_prompt",
                    "low quality, text, Chinese characters, fake singer name",
                    "text_policy",
                    "NO_TEXT_IN_IMAGE")));

    assertEquals(QualityDecision.PASS, result.decision());
    assertFalse(result.retryable());
  }

  @Test
  void qualityAgentLocalSafetyAllowsNoTextCoverPolicyWithNonStandardModelDecision()
      throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "COVER",
                            "decision",
                            "REWRITE_AGENT_OUTPUT",
                            "score",
                            65,
                            "reasons",
                            List.of("封面prompt禁止文字，未要求假歌手/版权/水印等违规内容"),
                            "recommended_action",
                            "REWRITE_AGENT_OUTPUT",
                            "retryable",
                            true))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.COVER,
                "燕云行",
                "lyrics",
                null,
                null,
                null,
                null,
                1920,
                1080,
                null,
                null,
                null,
                null,
                null,
                Map.of(
                    "visual_prompt",
                    "premium 16:9 album cover, no text in the image, no Chinese characters",
                    "negative_prompt",
                    "low quality, text, Chinese characters, fake singer name",
                    "text_policy",
                    "NO_TEXT_IN_IMAGE")));

    assertEquals(QualityDecision.PASS, result.decision());
    assertFalse(result.retryable());
  }

  @Test
  void qualityAgentLocalSafetyRejectsPositiveFakeCoverCreditsEvenWhenModelPasses()
      throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponse(
                        Map.of(
                            "gate",
                            "COVER",
                            "decision",
                            "PASS",
                            "score",
                            92,
                            "reasons",
                            List.of(),
                            "recommended_action",
                            "PASS",
                            "retryable",
                            false))));
    RealDeepSeekQualityEvaluationAgent agent =
        new RealDeepSeekQualityEvaluationAgent(
            client(), objectMapper, new ArrayList<AgentRunRecord>()::add);

    QualityEvaluationResult result =
        agent.evaluate(
            new QualityEvaluationRequest(
                "work-1",
                QualityGate.COVER,
                "燕云行",
                "lyrics",
                null,
                null,
                null,
                null,
                1920,
                1080,
                null,
                null,
                null,
                null,
                null,
                Map.of("visual_prompt", "premium album cover with fake singer credits")));

    assertEquals(QualityDecision.BLOCK, result.decision());
    assertFalse(result.retryable());
  }

  private DeepSeekJsonChatClient client() {
    return new DeepSeekJsonChatClient(properties(serverBaseUri(), true, true), objectMapper);
  }

  private DeepSeekProperties properties(
      URI baseUrl, boolean realCallsEnabled, boolean agentEnabled) {
    DeepSeekProperties properties = new DeepSeekProperties();
    properties.setBaseUrl(baseUrl);
    properties.setApiKey("test-api-key");
    properties.setModelName("deepseek-v4-pro");
    properties.setRealCallsEnabled(realCallsEnabled);
    properties.setAgentRealCallsEnabled(agentEnabled);
    properties.setRequestTimeout(Duration.ofSeconds(5));
    return properties;
  }

  private HttpServer startServer(ExchangeHandler handler) throws IOException {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    httpServer.createContext(
        "/chat/completions",
        exchange -> {
          try {
            handler.handle(exchange);
          } finally {
            exchange.close();
          }
        });
    httpServer.start();
    return httpServer;
  }

  private URI serverBaseUri() {
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
  }

  private String chatResponse(Map<String, Object> contentFields) throws IOException {
    String content = objectMapper.writeValueAsString(contentFields);
    return chatResponseContent(content);
  }

  private String chatResponseContent(String content) throws IOException {
    return objectMapper.writeValueAsString(
        Map.of(
            "id",
            "chatcmpl-test",
            "choices",
            List.of(Map.of("message", Map.of("content", content)))));
  }

  private void respondJson(
      com.sun.net.httpserver.HttpExchange exchange, int statusCode, String body)
      throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(statusCode, bytes.length);
    exchange.getResponseBody().write(bytes);
  }

  @FunctionalInterface
  private interface ExchangeHandler {
    void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException;
  }
}
