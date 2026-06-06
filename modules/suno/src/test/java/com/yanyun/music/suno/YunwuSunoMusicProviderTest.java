package com.yanyun.music.suno;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.yanyun.music.musicprovider.MusicGenerationRequest;
import com.yanyun.music.musicprovider.MusicGenerationResult;
import com.yanyun.music.musicprovider.MusicGenerationStatus;
import com.yanyun.music.musicprovider.MusicProviderType;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class YunwuSunoMusicProviderTest {

  private static final String TEST_API_KEY = "test-yunwu-key";

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
  void postsCustomSongAndReturnsAudioSourceUrl() throws Exception {
    server = startSuccessServer();
    YunwuSunoMusicProvider provider = new YunwuSunoMusicProvider(realProperties(), objectMapper);

    MusicGenerationResult result =
        provider.submit(
            new MusicGenerationRequest(
                "work-1",
                "[Verse]\nWind rises over the old pass",
                "cinematic folk,female voice",
                "FEMALE",
                Map.of("title", "Border Ballad")));

    assertEquals(MusicGenerationStatus.SUCCEEDED, result.status());
    assertEquals(MusicProviderType.SUNO, result.providerType());
    assertEquals("task-1", result.providerTaskId());
    assertEquals("yunwu:suno:chirp-v5", result.modelName());
    assertEquals("https://cdn.example.test/song.mp3", result.audioSourceUrl());
    assertEquals("audio/mpeg", result.audioContentType());
    assertEquals(181_500, result.durationMs());
    assertEquals("Bearer " + TEST_API_KEY, authorizationHeader);
    assertEquals("chirp-v5", requestBody.path("mv").asText());
    assertEquals("Border Ballad", requestBody.path("title").asText());
    assertEquals("cinematic folk,female voice", requestBody.path("tags").asText());
  }

  @Test
  void disabledRealSwitchReturnsControlledFailureWithoutHttpCall() {
    YunwuProperties properties = realProperties();
    properties.setRealCallsEnabled(false);
    YunwuSunoMusicProvider provider = new YunwuSunoMusicProvider(properties, objectMapper);

    MusicGenerationResult result =
        provider.submit(new MusicGenerationRequest("work-1", "lyrics", "prompt", "AUTO", Map.of()));

    assertEquals(MusicGenerationStatus.FAILED, result.status());
    assertEquals("MUSIC_GENERATION_FAILED", result.failureCode());
  }

  @Test
  void mapsAuthorizationFailureToProviderAuthFailed() throws Exception {
    server = startFailureServer();
    YunwuSunoMusicProvider provider = new YunwuSunoMusicProvider(realProperties(), objectMapper);

    MusicGenerationResult result =
        provider.submit(new MusicGenerationRequest("work-1", "lyrics", "prompt", "AUTO", Map.of()));

    assertEquals(MusicGenerationStatus.FAILED, result.status());
    assertEquals("PROVIDER_AUTH_FAILED", result.failureCode());
  }

  private HttpServer startSuccessServer() throws IOException {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    httpServer.createContext(
        "/suno/submit/music",
        exchange -> {
          authorizationHeader = exchange.getRequestHeaders().getFirst("Authorization");
          requestBody = objectMapper.readTree(exchange.getRequestBody());
          byte[] body =
              """
              {
                "code": "success",
                "data": "task-1",
                "message": ""
              }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    httpServer.createContext(
        "/suno/fetch/task-1",
        exchange -> {
          byte[] body =
              """
              {
                "code": "success",
                "data": {
                  "status": "SUCCESS",
                  "clips": [
                    {
                      "audio_url": "https://cdn.example.test/song.mp3",
                      "duration": 181.5
                    }
                  ]
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

  private HttpServer startFailureServer() throws IOException {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    httpServer.createContext(
        "/suno/submit/music",
        exchange -> {
          byte[] body =
              """
              {
                "code": 403,
                "message": "forbidden: permission denied"
              }
              """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(403, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    httpServer.start();
    return httpServer;
  }

  private YunwuProperties realProperties() {
    YunwuProperties properties = new YunwuProperties();
    properties.setRealCallsEnabled(true);
    properties.setBaseUrl(URI.create(server == null ? "http://127.0.0.1:1" : baseUrl()));
    properties.setApiKey(TEST_API_KEY);
    properties.setRequestTimeout(Duration.ofSeconds(5));
    properties.setPollInterval(Duration.ZERO);
    properties.setMaxPollAttempts(1);
    properties.setSunoModel("chirp-v5");
    return properties;
  }

  private String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }
}
