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
  void coverPromptAllowsTitleTypography() throws IOException {
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

    assertTrue(result.visualPrompt().contains("title typography"));
    assertTrue(result.textPrompt().contains("song title"));
    assertEquals(1920, result.width());
    assertEquals(1080, result.height());
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
                "lyrics",
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
  void qualityAgentLocalSafetyAllowsCoverTitleTypographyWithNegativeConstraints()
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

    assertEquals(QualityDecision.PASS, result.decision());
    assertEquals(80, result.score());
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
