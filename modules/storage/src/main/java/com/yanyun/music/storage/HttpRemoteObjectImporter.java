package com.yanyun.music.storage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HttpRemoteObjectImporter implements RemoteObjectImporter {

  private static final long DEFAULT_MAX_BYTES = 50L * 1024L * 1024L;
  private static final int DEFAULT_MAX_ATTEMPTS = 3;

  private final ObjectStorageClient objectStorageClient;
  private final HttpClient httpClient;
  private final long maxBytes;
  private final Duration timeout;
  private final int maxAttempts;

  public HttpRemoteObjectImporter(ObjectStorageClient objectStorageClient) {
    this(
        objectStorageClient,
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build(),
        DEFAULT_MAX_BYTES,
        Duration.ofSeconds(60));
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
    this.maxAttempts = DEFAULT_MAX_ATTEMPTS;
  }

  @Override
  public StoredObject importObject(RemoteObjectImportRequest request) {
    URI sourceUri = URI.create(request.sourceUrl().trim());
    requireHttpScheme(sourceUri);
    HttpRequest httpRequest =
        HttpRequest.newBuilder(sourceUri)
            .GET()
            .timeout(timeout)
            .version(HttpClient.Version.HTTP_1_1)
            .header("Accept", "*/*")
            .build();
    IOException lastIoException = null;
    InterruptedException lastInterruptedException = null;
    IllegalStateException lastRetryableResponse = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        HttpResponse<byte[]> response =
            httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
          IllegalStateException failure =
              new IllegalStateException(
                  "Remote object download failed with HTTP " + response.statusCode());
          if (!isRetryableStatus(response.statusCode()) || attempt == maxAttempts) {
            throw failure;
          }
          lastRetryableResponse = failure;
          sleepBeforeRetry(attempt);
          continue;
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
        lastIoException = exception;
        if (attempt == maxAttempts) {
          break;
        }
        try {
          sleepBeforeRetry(attempt);
        } catch (InterruptedException interruptedException) {
          lastInterruptedException = interruptedException;
          Thread.currentThread().interrupt();
          break;
        }
      } catch (InterruptedException exception) {
        lastInterruptedException = exception;
        Thread.currentThread().interrupt();
        break;
      }
    }
    if (lastInterruptedException != null) {
      throw new IllegalStateException(
          "Remote object download interrupted", lastInterruptedException);
    }
    if (lastIoException != null) {
      throw new IllegalStateException("Remote object download failed", lastIoException);
    }
    if (lastRetryableResponse != null) {
      throw lastRetryableResponse;
    }
    throw new IllegalStateException("Remote object download failed");
  }

  private boolean isRetryableStatus(int statusCode) {
    return statusCode == 429 || statusCode >= 500;
  }

  private void sleepBeforeRetry(int attempt) throws InterruptedException {
    Thread.sleep(Math.min(250L * attempt, 1000L));
  }

  private void requireHttpScheme(URI sourceUri) {
    String scheme = sourceUri.getScheme();
    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
      throw new IllegalArgumentException("sourceUrl must use http or https");
    }
  }
}
