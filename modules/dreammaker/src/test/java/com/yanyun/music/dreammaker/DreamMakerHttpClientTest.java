package com.yanyun.music.dreammaker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DreamMakerHttpClientTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneOffset.UTC);
  private static final String TEST_ACCESS_KEY = "test-access-key";
  private static final String TEST_SECRET_KEY = "test-secret-key";
  private static final String TEST_USER_ACCESS_TOKEN = "test-user-access-token";

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void submitsAndParsesStatusWithoutRealProviderCall() throws Exception {
    server = startServer();
    DreamMakerHttpClient client =
        new DreamMakerHttpClient(
            properties(TEST_ACCESS_KEY, TEST_SECRET_KEY),
            new ObjectMapper(),
            HttpClient.newHttpClient(),
            FIXED_CLOCK);

    DreamMakerSubmitResponse submit =
        client.submit(new DreamMakerRunRequest("suno", "music-gen", Map.of("prompt", "lyrics")));
    DreamMakerStatusResponse status =
        client.status(new DreamMakerStatusRequest("suno", "music-gen", submit.taskId()));

    assertEquals(0, submit.code());
    assertEquals("task-1", submit.taskId());
    assertEquals(DreamMakerTaskStatus.SUCCEEDED, status.status());
    assertEquals("success", status.providerStatus());
    assertEquals(1, status.outputFiles().size());
    assertEquals(baseUrl() + "/files/song.mp3", status.outputFiles().getFirst().url());
    assertEquals(181500, status.outputFiles().getFirst().durationMs());
  }

  @Test
  void rejectsMissingAccessKeyOrSecretKeyBeforeHttpCall() {
    DreamMakerHttpClient client =
        new DreamMakerHttpClient(
            properties("", TEST_SECRET_KEY, true), new ObjectMapper(), HttpClient.newHttpClient());

    DreamMakerClientException exception =
        assertThrows(
            DreamMakerClientException.class,
            () -> client.submit(new DreamMakerRunRequest("suno", "music-gen", Map.of())));

    assertEquals(DreamMakerFailureMapper.PROVIDER_AUTH_FAILED, exception.failureCode());
  }

  @Test
  void rejectsRealProviderCallWhenManualEnableFlagIsDisabled() {
    DreamMakerHttpClient client =
        new DreamMakerHttpClient(
            properties(TEST_ACCESS_KEY, TEST_SECRET_KEY, false),
            new ObjectMapper(),
            HttpClient.newHttpClient());

    DreamMakerClientException exception =
        assertThrows(
            DreamMakerClientException.class,
            () -> client.submit(new DreamMakerRunRequest("suno", "music-gen", Map.of())));

    assertEquals(DreamMakerFailureMapper.MUSIC_GENERATION_FAILED, exception.failureCode());
  }

  @Test
  void mapsNon2xxResponseAndRedactsProviderMessage() throws Exception {
    server = startFailureServer(429, "rate limit Bearer fake-token-value token=plain");
    DreamMakerHttpClient client =
        new DreamMakerHttpClient(
            properties(TEST_ACCESS_KEY, TEST_SECRET_KEY),
            new ObjectMapper(),
            HttpClient.newHttpClient(),
            FIXED_CLOCK);

    DreamMakerClientException exception =
        assertThrows(
            DreamMakerClientException.class,
            () -> client.submit(new DreamMakerRunRequest("suno", "music-gen", Map.of())));

    assertEquals(DreamMakerFailureMapper.RATE_LIMITED, exception.failureCode());
    assertTrue(exception.getMessage().contains("Bearer <redacted>"));
    assertTrue(exception.getMessage().contains("token=<redacted>"));
  }

  @Test
  void mapsAuthorizationFailureToProviderAuthFailed() throws Exception {
    server = startFailureServer(403, "forbidden: app requires company intranet or permission");
    DreamMakerHttpClient client =
        new DreamMakerHttpClient(
            properties(TEST_ACCESS_KEY, TEST_SECRET_KEY),
            new ObjectMapper(),
            HttpClient.newHttpClient(),
            FIXED_CLOCK);

    DreamMakerClientException exception =
        assertThrows(
            DreamMakerClientException.class,
            () -> client.submit(new DreamMakerRunRequest("suno", "music-gen", Map.of())));

    assertEquals(DreamMakerFailureMapper.PROVIDER_AUTH_FAILED, exception.failureCode());
    assertTrue(exception.getMessage().contains("company intranet"));
  }

  private HttpServer startServer() throws IOException {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    httpServer.createContext(
        "/api/v1/apps/suno/run",
        exchange -> {
          assertDreamMakerAuth(exchange.getRequestHeaders().getFirst("Authorization"));
          assertEquals(
              TEST_USER_ACCESS_TOKEN, exchange.getRequestHeaders().getFirst("X-Access-Token"));
          byte[] body =
              "{\"code\":0,\"message\":\"ok\",\"data\":{\"task_id\":\"task-1\"}}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    httpServer.createContext(
        "/api/v1/apps/suno/status",
        exchange -> {
          assertDreamMakerAuth(exchange.getRequestHeaders().getFirst("Authorization"));
          assertEquals(
              TEST_USER_ACCESS_TOKEN, exchange.getRequestHeaders().getFirst("X-Access-Token"));
          byte[] body =
              """
              {
                "code": 0,
                "message": "ok",
                "data": {
                  "status": "success",
                  "output": {
                    "output": [
                      {
                        "name": "song.mp3",
                        "url": "/files/song.mp3",
                        "file_type": "audio",
                        "duration": "181.5"
                      }
                    ]
                  }
                }
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

  private HttpServer startFailureServer(int statusCode, String message) throws IOException {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    httpServer.createContext(
        "/api/v1/apps/suno/run",
        exchange -> {
          assertDreamMakerAuth(exchange.getRequestHeaders().getFirst("Authorization"));
          byte[] body =
              """
              {
                "code": %d,
                "message": "%s"
              }
              """
                  .formatted(statusCode, message)
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(statusCode, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    httpServer.start();
    return httpServer;
  }

  private void assertDreamMakerAuth(String authorization) {
    try {
      String token = authorization.substring("Bearer ".length());
      String[] parts = token.split("\\.");
      assertEquals(3, parts.length);

      ObjectMapper mapper = new ObjectMapper();
      JsonNode header = mapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
      JsonNode payload = mapper.readTree(Base64.getUrlDecoder().decode(parts[1]));

      assertEquals("HS256", header.path("alg").asText());
      assertEquals("JWT", header.path("typ").asText());
      assertEquals(TEST_ACCESS_KEY, payload.path("iss").asText());
      assertEquals(
          FIXED_CLOCK.instant().plusSeconds(1800L).getEpochSecond(), payload.path("exp").asLong());
      assertEquals(
          FIXED_CLOCK.instant().minusSeconds(5L).getEpochSecond(), payload.path("nbf").asLong());
      assertEquals(signature(parts[0] + "." + parts[1]), parts[2]);
    } catch (Exception exception) {
      throw new AssertionError("DreamMaker JWT auth header is invalid", exception);
    }
  }

  private String signature(String signingInput) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
  }

  private DreamMakerProperties properties(String accessKey, String secretKey) {
    return properties(accessKey, secretKey, true);
  }

  private DreamMakerProperties properties(String accessKey, String secretKey, boolean enabled) {
    DreamMakerProperties properties = new DreamMakerProperties();
    properties.setBaseUrl(URI.create(server == null ? "http://127.0.0.1:1" : baseUrl()));
    properties.setAccessKey(accessKey);
    properties.setSecretKey(secretKey);
    properties.setUserAccessToken(TEST_USER_ACCESS_TOKEN);
    properties.setRealCallsEnabled(enabled);
    properties.setRequestTimeout(Duration.ofSeconds(2));
    return properties;
  }

  private String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }
}
