package com.yanyun.music.storage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HttpRemoteObjectImporter implements RemoteObjectImporter {

  private static final long DEFAULT_MAX_BYTES = 50L * 1024L * 1024L;

  private final ObjectStorageClient objectStorageClient;
  private final HttpClient httpClient;
  private final long maxBytes;
  private final Duration timeout;

  public HttpRemoteObjectImporter(ObjectStorageClient objectStorageClient) {
    this(
        objectStorageClient, HttpClient.newHttpClient(), DEFAULT_MAX_BYTES, Duration.ofSeconds(60));
  }

  public HttpRemoteObjectImporter(
      ObjectStorageClient objectStorageClient,
      HttpClient httpClient,
      long maxBytes,
      Duration timeout) {
    if (objectStorageClient == null) {
      throw new IllegalArgumentException("objectStorageClient is required");
    }
    this.objectStorageClient = objectStorageClient;
    this.httpClient = httpClient == null ? HttpClient.newHttpClient() : httpClient;
    this.maxBytes = maxBytes <= 0 ? DEFAULT_MAX_BYTES : maxBytes;
    this.timeout = timeout == null || timeout.isNegative() ? Duration.ofSeconds(60) : timeout;
  }

  @Override
  public StoredObject importObject(RemoteObjectImportRequest request) {
    URI sourceUri = URI.create(request.sourceUrl().trim());
    requireHttpScheme(sourceUri);
    HttpRequest httpRequest =
        HttpRequest.newBuilder(sourceUri).GET().timeout(timeout).header("Accept", "*/*").build();
    try {
      HttpResponse<byte[]> response =
          httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException(
            "Remote object download failed with HTTP " + response.statusCode());
      }
      byte[] body = response.body() == null ? new byte[0] : response.body();
      if (body.length > maxBytes) {
        throw new IllegalStateException("Remote object exceeds max allowed size");
      }
      String contentType =
          response.headers().firstValue("Content-Type").orElse(request.contentType());
      return objectStorageClient.putObject(
          new ObjectStoragePutRequest(request.objectKey(), contentType, body));
    } catch (IOException exception) {
      throw new IllegalStateException("Remote object download failed", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Remote object download interrupted", exception);
    }
  }

  private void requireHttpScheme(URI sourceUri) {
    String scheme = sourceUri.getScheme();
    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
      throw new IllegalArgumentException("sourceUrl must use http or https");
    }
  }
}
