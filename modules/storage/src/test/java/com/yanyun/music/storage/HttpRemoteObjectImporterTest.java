package com.yanyun.music.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpRemoteObjectImporterTest {

  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void importsHttpObjectIntoStorageClient() throws Exception {
    server =
        startServer("audio/mpeg", "audio-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    CapturingStorageClient storageClient = new CapturingStorageClient();
    HttpRemoteObjectImporter importer =
        new HttpRemoteObjectImporter(
            storageClient, HttpClient.newHttpClient(), 1024L, Duration.ofSeconds(2));

    StoredObject stored =
        importer.importObject(
            new RemoteObjectImportRequest(serverUrl(), "audio/work-1.mp3", "audio/mpeg"));

    assertEquals("audio/work-1.mp3", stored.objectKey());
    assertEquals("audio/mpeg", stored.contentType());
    assertEquals("audio-bytes", storageClient.lastContent);
  }

  @Test
  void rejectsNonHttpSourceUrl() {
    HttpRemoteObjectImporter importer =
        new HttpRemoteObjectImporter(
            new CapturingStorageClient(), HttpClient.newHttpClient(), 1024L, Duration.ZERO);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            importer.importObject(
                new RemoteObjectImportRequest("file:///tmp/a.mp3", "audio/a.mp3", "audio/mpeg")));
  }

  private HttpServer startServer(String contentType, byte[] body) throws IOException {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    httpServer.createContext(
        "/audio.mp3",
        exchange -> {
          exchange.getResponseHeaders().set("Content-Type", contentType);
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    httpServer.start();
    return httpServer;
  }

  private String serverUrl() {
    return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/audio.mp3")
        .toString();
  }

  private static final class CapturingStorageClient implements ObjectStorageClient {

    private String lastContent;

    @Override
    public StoredObject putObject(ObjectStoragePutRequest request) {
      lastContent = new String(request.content(), java.nio.charset.StandardCharsets.UTF_8);
      return new StoredObject(
          request.objectKey(),
          "http://localhost/" + request.objectKey(),
          request.contentType(),
          request.content().length);
    }

    @Override
    public ObjectStorageDownloadUrl createDownloadUrl(String objectKey) {
      return new ObjectStorageDownloadUrl(
          objectKey, "http://localhost/" + objectKey, OffsetDateTime.now().plusHours(1));
    }
  }
}
