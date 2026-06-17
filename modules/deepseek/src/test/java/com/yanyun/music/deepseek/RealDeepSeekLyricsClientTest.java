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
    assertTrue(systemPrompt.contains("v0.9/v0.9.1/v0.9.2 核心方法"));
    assertTrue(systemPrompt.contains("LyricsCraftPlan v0.9"));
    assertTrue(systemPrompt.contains("LyricsCraftPlan v0.9.1"));
    assertTrue(systemPrompt.contains("LyricsCraftPlan v0.9.2"));
    assertTrue(systemPrompt.contains("最终歌词必须兑现 CraftPlan"));
    assertTrue(systemPrompt.contains("原句或近似原句进入副歌"));
    assertTrue(systemPrompt.contains("不要另造 device 抢走它"));
    assertTrue(systemPrompt.contains("具体胜过正确"));
    assertTrue(systemPrompt.contains("矛盾胜过顺滑"));
    assertTrue(systemPrompt.contains("selected_device"));
    assertTrue(systemPrompt.contains("selected_angle"));
    assertTrue(systemPrompt.contains("chorus_mechanism"));
    assertTrue(systemPrompt.contains("yanyun_boundary_guard"));
    assertTrue(systemPrompt.contains("不要把 rejected_alternatives 里的方案写回来"));
    assertTrue(systemPrompt.contains("粗粝不等于粗口"));
    assertTrue(systemPrompt.contains("容易写成漂亮怀念、泛热血或泛正确"));
    assertFalse(systemPrompt.contains("对九流门、市井底层、鬼市、门派倾覆、乱世小人物"));
    assertFalse(systemPrompt.contains("对寒香寻、清河、神仙渡、不羡仙"));
    assertTrue(systemPrompt.contains("第一目标不是“满足所有规则”"));
    assertTrue(systemPrompt.contains("一句话歌核"));
    assertTrue(systemPrompt.contains("song_core"));
    assertTrue(systemPrompt.contains("singer_voice"));
    assertTrue(systemPrompt.contains("listener_target"));
    assertTrue(systemPrompt.contains("emotional_engine"));
    assertTrue(systemPrompt.contains("chorus_job"));
    assertTrue(systemPrompt.contains("avoid_direction"));
    assertTrue(systemPrompt.contains("v0.8.1 调整"));
    assertTrue(systemPrompt.contains("song_core 是暗中统领"));
    assertTrue(systemPrompt.contains("不要把 song_core 改写成最显眼"));
    assertTrue(systemPrompt.contains("具体声音、动作、物件、句式变奏或意象回环"));
    assertTrue(systemPrompt.contains("不那么标准但真实的细节"));
    assertTrue(systemPrompt.contains("过于正确、过于顺滑、过于解释清楚"));
    assertTrue(systemPrompt.contains("粗粝不等于空喊"));
    assertTrue(systemPrompt.contains("v0.8.2 调整"));
    assertTrue(systemPrompt.contains("地点怀念类歌曲要敢于少写"));
    assertTrue(systemPrompt.contains("小人物/不厉害题材要保留笨拙"));
    assertTrue(systemPrompt.contains("无害的共同口头禅"));
    assertTrue(systemPrompt.contains("优先保留并深化它"));
    assertTrue(systemPrompt.contains("v0.8.3 调整"));
    assertTrue(systemPrompt.contains("不要继续累加规则"));
    assertTrue(systemPrompt.contains("私人声音装置"));
    assertTrue(systemPrompt.contains("至少想两个可重复 hook"));
    assertTrue(systemPrompt.contains("副歌宁可短、准、反复、有变义"));
    assertTrue(systemPrompt.contains("保留生命"));
    assertTrue(systemPrompt.contains("v0.8.4 调整"));
    assertTrue(systemPrompt.contains("不是歌词场景许可"));
    assertTrue(systemPrompt.contains("不能变成现代训练室或拳击叙事"));
    assertTrue(systemPrompt.contains("曲风不允许改变世界边界"));
    assertTrue(systemPrompt.contains("知识库不是资料清单"));
    assertTrue(systemPrompt.contains("不编造官方设定"));
    assertTrue(systemPrompt.contains("开放作词路径"));
    assertTrue(systemPrompt.contains("叙事型、意象型、口语型"));
    assertTrue(systemPrompt.contains("Hook 不限定为金句"));
    assertTrue(systemPrompt.contains("music_style 只影响语言声口"));
    assertTrue(systemPrompt.contains("避开第一反应俗套"));
    assertTrue(systemPrompt.contains("creative_core"));
    assertTrue(systemPrompt.contains("song_thesis"));
    assertTrue(systemPrompt.contains("这首歌到底在唱什么"));
    assertTrue(systemPrompt.contains("副歌是否只是直喊 song_core"));
    assertTrue(systemPrompt.contains("是否保留了一个不那么标准但真实的细节"));
    assertTrue(systemPrompt.contains("地点怀念是否写得太满"));
    assertTrue(systemPrompt.contains("小人物是否被写成了过度英雄化"));
    assertTrue(systemPrompt.contains("轻快转酸楚是否有一个共同口头禅"));
    assertTrue(systemPrompt.contains("这首歌是否有一个私人声音装置"));
    assertTrue(systemPrompt.contains("是否为了规则完整牺牲了更有生命"));
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
    assertTrue(systemPrompt.contains("一票否决"));
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
  void editOperationsUseShorterSemanticRetryBudgetForInvalidContentJson() throws IOException {
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
        assertThrows(IllegalStateException.class, () -> client.generate(request("POLISH")));

    assertEquals(2, requestCount.get());
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
    return request("LYRICS");
  }

  private DeepSeekLyricsRequest request(String operation) {
    return new DeepSeekLyricsRequest(
        operation,
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
