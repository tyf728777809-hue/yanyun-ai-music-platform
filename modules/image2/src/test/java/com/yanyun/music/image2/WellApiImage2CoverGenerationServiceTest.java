package com.yanyun.music.image2;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class WellApiImage2CoverGenerationServiceTest {

  private static final String TEST_API_KEY = "test-wellapi-key";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private HttpServer server;
  private JsonNode requestBody;
  private String authorizationHeader;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void postsGptImage2RequestAndReturnsRemoteCoverForImport() throws Exception {
    server = startImageServer();
    WellApiImage2CoverGenerationService service =
        new WellApiImage2CoverGenerationService(realProperties(), objectMapper);

    CoverGenerationResult result = service.generateCover(request());

    assertEquals("Bearer " + TEST_API_KEY, authorizationHeader);
    assertEquals("gpt-image-2", requestBody.path("model").asText());
    assertEquals("2048x1152", requestBody.path("size").asText());
    assertEquals("medium", requestBody.path("quality").asText());
    assertEquals("jpeg", requestBody.path("format").asText());
    assertEquals("covers/work-123.jpeg", result.asset().objectKey());
    assertEquals("image/jpeg", result.asset().mimeType());
    assertEquals("wellapi-image2", result.asset().checksum());
    assertEquals(
        "https://cdn.example.test/cover.jpeg",
        result.asset().metadata().get(CoverGenerationService.SOURCE_URL_METADATA_KEY));
  }

  @Test
  void refusesToSubmitWhenRealSwitchIsDisabled() {
    Image2Properties properties = realProperties();
    properties.setRealCallsEnabled(false);
    WellApiImage2CoverGenerationService service =
        new WellApiImage2CoverGenerationService(properties, objectMapper);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> service.generateCover(request()));

    assertTrue(exception.getMessage().contains("IMAGE_REAL_CALLS_ENABLED"));
  }

  @Test
  void acceptsProviderBase64PayloadForDirectStorageImport() throws Exception {
    server = startBase64OnlyServer();
    WellApiImage2CoverGenerationService service =
        new WellApiImage2CoverGenerationService(realProperties(), objectMapper);

    CoverGenerationResult result = service.generateCover(request());

    assertEquals(
        "ZmFrZS1pbWFnZQ==",
        result.asset().metadata().get(CoverGenerationService.INLINE_BASE64_METADATA_KEY));
    assertEquals("covers/work-123.jpeg", result.asset().objectKey());
  }

  private HttpServer startImageServer() throws IOException {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    httpServer.createContext(
        "/v1/images/generations",
        exchange -> {
          authorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");
          requestBody = objectMapper.readTree(exchange.getRequestBody());
          byte[] body =
              """
              {
                "id": "img-task-1",
                "data": [
                  {
                    "url": "https://cdn.example.test/cover.jpeg"
                  }
                ]
              }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    httpServer.start();
    return httpServer;
  }

  private HttpServer startBase64OnlyServer() throws IOException {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    httpServer.createContext(
        "/v1/images/generations",
        exchange -> {
          byte[] body =
              """
              {
                "data": [
                  {
                    "b64_json": "ZmFrZS1pbWFnZQ=="
                  }
                ]
              }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    httpServer.start();
    return httpServer;
  }

  private Image2Properties realProperties() {
    Image2Properties properties = new Image2Properties();
    properties.setRealCallsEnabled(true);
    properties.setBaseUrl(URI.create(server == null ? "http://127.0.0.1:1" : baseUrl()));
    properties.setApiKey(TEST_API_KEY);
    properties.setRequestTimeout(Duration.ofSeconds(5));
    properties.setOutputFormat("jpeg");
    return properties;
  }

  private CoverGenerationRequest request() {
    return new CoverGenerationRequest(
        "work-123",
        "Border Ballad",
        "Yanyun themed original song",
        "[Verse]\nWind rises over the old pass",
        "cinematic folk",
        "Yanyun border town at dusk, mountains, lanterns, cinematic cover art",
        "modern city, realistic portrait",
        2048,
        1152,
        Map.of("prompt_source", "cover-prompt-agent"));
  }

  private String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }
}
