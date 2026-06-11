package com.yanyun.music.storage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class HttpRemoteObjectImporter implements RemoteObjectImporter {

  private static final long DEFAULT_MAX_BYTES = 50L * 1024L * 1024L;
  private static final int DEFAULT_MAX_ATTEMPTS = 3;
  private static final int MAX_REDIRECTS = 5;

  private final ObjectStorageClient objectStorageClient;
  private final HttpClient httpClient;
  private final long maxBytes;
  private final Duration timeout;
  private final int maxAttempts;
  private final boolean allowPrivateNetwork;

  public HttpRemoteObjectImporter(ObjectStorageClient objectStorageClient) {
    this(
        objectStorageClient,
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build(),
        DEFAULT_MAX_BYTES,
        Duration.ofSeconds(60),
        false);
  }

  public HttpRemoteObjectImporter(
      ObjectStorageClient objectStorageClient,
      HttpClient httpClient,
      long maxBytes,
      Duration timeout) {
    this(objectStorageClient, httpClient, maxBytes, timeout, false);
  }

  public HttpRemoteObjectImporter(
      ObjectStorageClient objectStorageClient,
      HttpClient httpClient,
      long maxBytes,
      Duration timeout,
      boolean allowPrivateNetwork) {
    if (objectStorageClient == null) {
      throw new IllegalArgumentException("objectStorageClient is required");
    }
    this.objectStorageClient = objectStorageClient;
    this.httpClient = httpClient == null ? HttpClient.newHttpClient() : httpClient;
    this.maxBytes = maxBytes <= 0 ? DEFAULT_MAX_BYTES : maxBytes;
    this.timeout = timeout == null || timeout.isNegative() ? Duration.ofSeconds(60) : timeout;
    this.maxAttempts = DEFAULT_MAX_ATTEMPTS;
    this.allowPrivateNetwork = allowPrivateNetwork;
  }

  @Override
  public StoredObject importObject(RemoteObjectImportRequest request) {
    URI sourceUri = URI.create(request.sourceUrl().trim());
    requireSafeHttpUri(sourceUri);
    IOException lastIoException = null;
    InterruptedException lastInterruptedException = null;
    IllegalStateException lastRetryableResponse = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        DownloadedObject downloaded = download(sourceUri, request.contentType());
        return objectStorageClient.putObject(
            new ObjectStoragePutRequest(
                request.objectKey(), downloaded.contentType(), downloaded.body()));
      } catch (HttpStatusException exception) {
        IllegalStateException failure =
            new IllegalStateException(
                "Remote object download failed with HTTP " + exception.statusCode());
        if (!isRetryableStatus(exception.statusCode()) || attempt == maxAttempts) {
          throw failure;
        }
        lastRetryableResponse = failure;
        try {
          sleepBeforeRetry(attempt);
        } catch (InterruptedException interruptedException) {
          lastInterruptedException = interruptedException;
          Thread.currentThread().interrupt();
          break;
        }
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

  private DownloadedObject download(URI sourceUri, String fallbackContentType)
      throws IOException, InterruptedException {
    URI current = sourceUri;
    for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
      requireSafeHttpUri(current);
      HttpRequest httpRequest =
          HttpRequest.newBuilder(current)
              .GET()
              .timeout(timeout)
              .version(HttpClient.Version.HTTP_1_1)
              .header("Accept", "*/*")
              .build();
      HttpResponse<InputStream> response =
          httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
      int statusCode = response.statusCode();
      if (isRedirect(statusCode)) {
        current =
            response
                .headers()
                .firstValue("Location")
                .map(current::resolve)
                .orElseThrow(
                    () -> new IllegalStateException("Remote object redirect has no Location"));
        continue;
      }
      if (statusCode < 200 || statusCode >= 300) {
        throw new HttpStatusException(statusCode);
      }
      long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
      if (contentLength > maxBytes) {
        throw new IllegalStateException("Remote object exceeds max allowed size");
      }
      String contentType =
          response.headers().firstValue("Content-Type").orElse(fallbackContentType);
      try (InputStream inputStream = response.body()) {
        return new DownloadedObject(contentType, readLimited(inputStream));
      }
    }
    throw new IllegalStateException("Remote object redirected too many times");
  }

  private byte[] readLimited(InputStream inputStream) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    long total = 0L;
    int read;
    while ((read = inputStream.read(buffer)) != -1) {
      total += read;
      if (total > maxBytes) {
        throw new IllegalStateException("Remote object exceeds max allowed size");
      }
      output.write(buffer, 0, read);
    }
    return output.toByteArray();
  }

  private boolean isRetryableStatus(int statusCode) {
    return statusCode == 429 || statusCode >= 500;
  }

  private void sleepBeforeRetry(int attempt) throws InterruptedException {
    Thread.sleep(Math.min(250L * attempt, 1000L));
  }

  private void requireSafeHttpUri(URI sourceUri) {
    String scheme = sourceUri.getScheme();
    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
      throw new IllegalArgumentException("sourceUrl must use http or https");
    }
    if (sourceUri.getHost() == null || sourceUri.getHost().isBlank()) {
      throw new IllegalArgumentException("sourceUrl host is required");
    }
    if (allowPrivateNetwork) {
      return;
    }
    try {
      for (InetAddress address : InetAddress.getAllByName(sourceUri.getHost())) {
        if (address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress()) {
          throw new IllegalArgumentException("sourceUrl private network host is not allowed");
        }
      }
    } catch (IOException exception) {
      throw new IllegalArgumentException("sourceUrl host cannot be resolved", exception);
    }
  }

  private boolean isRedirect(int statusCode) {
    return statusCode == 301
        || statusCode == 302
        || statusCode == 303
        || statusCode == 307
        || statusCode == 308;
  }

  private record DownloadedObject(String contentType, byte[] body) {}

  private static final class HttpStatusException extends IOException {
    private final int statusCode;

    private HttpStatusException(int statusCode) {
      this.statusCode = statusCode;
    }

    private int statusCode() {
      return statusCode;
    }
  }
}
