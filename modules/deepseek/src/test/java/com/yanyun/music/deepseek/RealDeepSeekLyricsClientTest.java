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
