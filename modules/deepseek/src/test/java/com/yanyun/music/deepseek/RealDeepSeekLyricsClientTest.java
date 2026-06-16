package com.yanyun.music.deepseek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RealDeepSeekLyricsClientTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void sendsOpenAiCompatibleJsonChatRequestAndParsesResponse() throws IOException {
    AtomicReference<JsonNode> capturedBody = new AtomicReference<>();
    AtomicReference<String> capturedAuthorization = new AtomicReference<>();
    server =
        startServer(
            exchange -> {
              capturedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
              capturedBody.set(
                  objectMapper.readTree(
                      new String(
                          exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
              respondJson(exchange, 200, chatResponseJson());
            });
    RealDeepSeekLyricsClient client =
        new RealDeepSeekLyricsClient(properties(serverBaseUri(), true, true), objectMapper);

    DeepSeekLyricsResponse response = client.generate(request());

    assertEquals("Bearer test-api-key", capturedAuthorization.get());
    assertEquals("deepseek-v4-pro", capturedBody.get().path("model").asText());
    assertEquals("json_object", capturedBody.get().path("response_format").path("type").asText());
    String systemPrompt = capturedBody.get().path("messages").get(0).path("content").asText();
    assertTrue(systemPrompt.contains("顶级中文作词 Agent"));
    assertTrue(systemPrompt.contains("世界级中文作词家"));
    assertTrue(systemPrompt.contains("不编造官方设定"));
    assertTrue(systemPrompt.contains("开放作词路径"));
    assertTrue(systemPrompt.contains("叙事型、意象型、口语型"));
    assertTrue(systemPrompt.contains("Hook 不限定为金句"));
    assertTrue(systemPrompt.contains("music_style 只影响语言声口"));
    assertTrue(systemPrompt.contains("避开第一反应俗套"));
    assertTrue(systemPrompt.contains("creative_core"));
    assertTrue(systemPrompt.contains("song_thesis"));
    assertTrue(systemPrompt.contains("这首歌到底在唱什么"));
    assertTrue(systemPrompt.contains("视角必须稳定"));
    assertTrue(systemPrompt.contains("副歌必须承担明确功能"));
    assertTrue(systemPrompt.contains("真正的润色，不是重写"));
    assertTrue(systemPrompt.contains("主韵脚"));
    assertTrue(systemPrompt.contains("整首歌不得完全无韵"));
    assertTrue(systemPrompt.contains("canonical name"));
    assertTrue(systemPrompt.contains("不要把用户错字当成正式名字输出"));
    assertTrue(systemPrompt.contains("不能只写泛江湖"));
    assertTrue(systemPrompt.contains("7-14 字短句"));
    assertTrue(systemPrompt.contains("具体动作、物件或场景"));
    assertTrue(systemPrompt.contains("叙事流水账"));
    assertTrue(systemPrompt.contains("万能诗性词连续堆叠"));
    assertTrue(systemPrompt.contains("3 个以上万能词"));
    assertTrue(systemPrompt.contains("禁止只靠“酒、剑、月、风”"));
    assertTrue(systemPrompt.contains("剧情解释太满"));
    assertTrue(systemPrompt.contains("只输出 JSON object"));
    assertTrue(systemPrompt.contains("0.90-1.00"));
    assertEquals("边城旧梦", response.songTitle());
    assertTrue(response.lyricsText().contains("[Verse]"));
    assertEquals("国风民谣，女声，古筝，笛子，武侠叙事", response.musicPrompt());
    assertFalse(response.riskNotes().isEmpty());
    assertEquals("deepseek-v4-pro", client.modelName());
  }

  @Test
  void parsesJsonObjectWrappedInMarkdownFence() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    200,
                    chatResponseContent(
                        """
                        ```json
                        {
                          "song_title": "边城旧梦",
                          "song_summary": "燕云边城里故人重逢的原创歌曲。",
                          "lyrics_text": "[Verse]\\n雁门风起过长街",
                          "music_prompt": "国风民谣，女声",
                          "cover_prompt_seed": "燕云边城夜色",
                          "risk_notes": [],
                          "quality_score": 0.9
                        }
                        ```
                        """)));
    RealDeepSeekLyricsClient client =
        new RealDeepSeekLyricsClient(properties(serverBaseUri(), true, true), objectMapper);

    DeepSeekLyricsResponse response = client.generate(request());

    assertEquals("边城旧梦", response.songTitle());
    assertTrue(response.lyricsText().contains("[Verse]"));
  }

  @Test
  void retriesWhenProviderReturnsEmptyChoiceContent() throws IOException {
    AtomicInteger requestCount = new AtomicInteger();
    server =
        startServer(
            exchange -> {
              if (requestCount.incrementAndGet() == 1) {
                respondJson(exchange, 200, chatResponseContent(""));
              } else {
                respondJson(exchange, 200, chatResponseJson());
              }
            });
    RealDeepSeekLyricsClient client =
        new RealDeepSeekLyricsClient(properties(serverBaseUri(), true, true), objectMapper);

    DeepSeekLyricsResponse response = client.generate(request());

    assertEquals(2, requestCount.get());
    assertEquals("边城旧梦", response.songTitle());
    assertTrue(response.lyricsText().contains("[Verse]"));
  }

  @Test
  void retriesWhenProviderReturnsInvalidContentJson() throws IOException {
    AtomicInteger requestCount = new AtomicInteger();
    server =
        startServer(
            exchange -> {
              if (requestCount.incrementAndGet() == 1) {
                respondJson(exchange, 200, chatResponseContent("{not-json"));
              } else {
                respondJson(exchange, 200, chatResponseJson());
              }
            });
    RealDeepSeekLyricsClient client =
        new RealDeepSeekLyricsClient(properties(serverBaseUri(), true, true), objectMapper);

    DeepSeekLyricsResponse response = client.generate(request());

    assertEquals(2, requestCount.get());
    assertEquals("边城旧梦", response.songTitle());
    assertTrue(response.lyricsText().contains("[Verse]"));
  }

  @Test
  void retriesWhenProviderOmitsLyricsText() throws IOException {
    AtomicInteger requestCount = new AtomicInteger();
    server =
        startServer(
            exchange -> {
              if (requestCount.incrementAndGet() == 1) {
                respondJson(
                    exchange,
                    200,
                    chatResponseContent(
                        """
                        {"song_title":"边城旧梦","song_summary":"缺少歌词正文。"}
                        """));
              } else {
                respondJson(exchange, 200, chatResponseJson());
              }
            });
    RealDeepSeekLyricsClient client =
        new RealDeepSeekLyricsClient(properties(serverBaseUri(), true, true), objectMapper);

    DeepSeekLyricsResponse response = client.generate(request());

    assertEquals(2, requestCount.get());
    assertEquals("边城旧梦", response.songTitle());
    assertTrue(response.lyricsText().contains("[Verse]"));
  }

  @Test
  void exhaustsSemanticRetriesForInvalidContentJson() throws IOException {
    AtomicInteger requestCount = new AtomicInteger();
    server =
        startServer(
            exchange -> {
              requestCount.incrementAndGet();
              respondJson(exchange, 200, chatResponseContent("{not-json"));
            });
    RealDeepSeekLyricsClient client =
        new RealDeepSeekLyricsClient(properties(serverBaseUri(), true, true), objectMapper);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> client.generate(request()));

    assertEquals(3, requestCount.get());
    assertTrue(exception.getMessage().contains("DeepSeek response content JSON is invalid"));
  }

  @Test
  void refusesToCallHttpWhenRealSwitchIsDisabled() throws IOException {
    AtomicInteger requestCount = new AtomicInteger();
    server =
        startServer(
            exchange -> {
              requestCount.incrementAndGet();
              respondJson(exchange, 200, chatResponseJson());
            });
    RealDeepSeekLyricsClient client =
        new RealDeepSeekLyricsClient(properties(serverBaseUri(), false, true), objectMapper);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> client.generate(request()));

    assertTrue(exception.getMessage().contains("DEEPSEEK_REAL_CALLS_ENABLED"));
    assertEquals(0, requestCount.get());
  }

  @Test
  void refusesToCallHttpWhenAgentSwitchIsDisabled() throws IOException {
    AtomicInteger requestCount = new AtomicInteger();
    server =
        startServer(
            exchange -> {
              requestCount.incrementAndGet();
              respondJson(exchange, 200, chatResponseJson());
            });
    RealDeepSeekLyricsClient client =
        new RealDeepSeekLyricsClient(properties(serverBaseUri(), true, false), objectMapper);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> client.generate(request()));

    assertTrue(exception.getMessage().contains("AGENT_REAL_CALLS_ENABLED"));
    assertEquals(0, requestCount.get());
  }

  @Test
  void sanitizesProviderErrorMessage() throws IOException {
    server =
        startServer(
            exchange ->
                respondJson(
                    exchange,
                    401,
                    """
                    {"error":{"message":"invalid Bearer secret-token"}}
                    """));
    RealDeepSeekLyricsClient client =
        new RealDeepSeekLyricsClient(properties(serverBaseUri(), true, true), objectMapper);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> client.generate(request()));

    assertTrue(exception.getMessage().contains("Bearer <redacted>"));
    assertFalse(exception.getMessage().contains("secret-token"));
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

  private DeepSeekLyricsRequest request() {
    return new DeepSeekLyricsRequest(
        "LYRICS",
        "rendered prompt",
        "雁门旧事",
        null,
        null,
        "边城旧梦",
        "孤独但热血",
        "国风民谣",
        "FEMALE",
        List.of("清河", "不羡仙"));
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

  private String chatResponseJson() throws IOException {
    String content =
        objectMapper.writeValueAsString(
            java.util.Map.of(
                "song_title",
                "边城旧梦",
                "song_summary",
                "燕云边城里故人重逢的原创歌曲。",
                "lyrics_text",
                "[Verse]\n雁门风起过长街\n灯影照见旧山河",
                "music_prompt",
                "国风民谣，女声，古筝，笛子，武侠叙事",
                "cover_prompt_seed",
                "燕云边城夜色，灯火，远山",
                "risk_notes",
                List.of("check-originality"),
                "quality_score",
                0.91));
    return objectMapper.writeValueAsString(
        java.util.Map.of(
            "id",
            "chatcmpl-test",
            "choices",
            List.of(java.util.Map.of("message", java.util.Map.of("content", content)))));
  }

  private String chatResponseContent(String content) throws IOException {
    return objectMapper.writeValueAsString(
        java.util.Map.of(
            "id",
            "chatcmpl-test",
            "choices",
            List.of(java.util.Map.of("message", java.util.Map.of("content", content)))));
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
