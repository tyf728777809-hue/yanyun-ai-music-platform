package com.yanyun.music.api.integration.dreammaker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.yanyun.music.dreammaker.DreamMakerClientException;
import com.yanyun.music.dreammaker.DreamMakerFailureMapper;
import com.yanyun.music.dreammaker.DreamMakerRunRequest;
import com.yanyun.music.dreammaker.DreamMakerStatusRequest;
import com.yanyun.music.dreammaker.DreamMakerStatusResponse;
import com.yanyun.music.dreammaker.DreamMakerSubmitResponse;
import com.yanyun.music.dreammaker.DreamMakerTaskStatus;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DreamMakerHttpClientTest {

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
            properties("test-api-key"), new ObjectMapper(), HttpClient.newHttpClient());

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
  void rejectsMissingApiKeyBeforeHttpCall() {
    DreamMakerHttpClient client =
        new DreamMakerHttpClient(properties(""), new ObjectMapper(), HttpClient.newHttpClient());

    DreamMakerClientException exception =
        assertThrows(
            DreamMakerClientException.class,
            () -> client.submit(new DreamMakerRunRequest("suno", "music-gen", Map.of())));

    assertEquals(DreamMakerFailureMapper.MUSIC_GENERATION_FAILED, exception.failureCode());
  }

  private HttpServer startServer() throws IOException {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    httpServer.createContext(
        "/api/v1/apps/suno/run",
        exchange -> {
          assertEquals(
              "Bearer test-api-key", exchange.getRequestHeaders().getFirst("Authorization"));
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
          assertEquals(
              "Bearer test-api-key", exchange.getRequestHeaders().getFirst("Authorization"));
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

  private DreamMakerProperties properties(String apiKey) {
    DreamMakerProperties properties = new DreamMakerProperties();
    properties.setBaseUrl(URI.create(server == null ? "http://127.0.0.1:1" : baseUrl()));
    properties.setApiKey(apiKey);
    properties.setRequestTimeout(Duration.ofSeconds(2));
    return properties;
  }

  private String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }
}
