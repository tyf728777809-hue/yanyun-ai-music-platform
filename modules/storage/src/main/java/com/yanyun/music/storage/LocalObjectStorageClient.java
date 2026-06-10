package com.yanyun.music.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.OffsetDateTime;

public final class LocalObjectStorageClient implements ObjectStorageClient {

  private final Path rootDirectory;
  private final String publicBaseUrl;
  private final Duration urlTtl;

  public LocalObjectStorageClient(Path rootDirectory, String publicBaseUrl) {
    this(rootDirectory, publicBaseUrl, Duration.ofHours(24));
  }

  public LocalObjectStorageClient(Path rootDirectory, String publicBaseUrl, Duration urlTtl) {
    if (rootDirectory == null) {
      throw new IllegalArgumentException("rootDirectory is required");
    }
    if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
      throw new IllegalArgumentException("publicBaseUrl is required");
    }
    if (urlTtl == null || urlTtl.isNegative() || urlTtl.isZero()) {
      throw new IllegalArgumentException("urlTtl must be positive");
    }
    this.rootDirectory = rootDirectory.toAbsolutePath().normalize();
    this.publicBaseUrl = trimTrailingSlash(publicBaseUrl);
    this.urlTtl = urlTtl;
  }

  @Override
  public StoredObject putObject(ObjectStoragePutRequest request) {
    String objectKey = ObjectStorageKeys.requireSafeObjectKey(request.objectKey());
    Path target = rootDirectory.resolve(objectKey).normalize();
    if (!target.startsWith(rootDirectory)) {
      throw new IllegalArgumentException("objectKey escapes storage root");
    }
    try {
      if (target.getParent() != null) {
        Files.createDirectories(target.getParent());
      }
      byte[] content = request.content();
      Files.write(
          target,
          content,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.WRITE);
      return new StoredObject(
          objectKey, createDownloadUrl(objectKey).url(), request.contentType(), content.length);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to write object: " + objectKey, exception);
    }
  }

  @Override
  public ObjectStorageDownloadUrl createDownloadUrl(String objectKey) {
    String safeObjectKey = ObjectStorageKeys.requireSafeObjectKey(objectKey);
    return new ObjectStorageDownloadUrl(
        safeObjectKey, publicBaseUrl + "/" + safeObjectKey, OffsetDateTime.now().plus(urlTtl));
  }

  @Override
  public byte[] getObject(String objectKey) {
    String safeObjectKey = ObjectStorageKeys.requireSafeObjectKey(objectKey);
    Path target = rootDirectory.resolve(safeObjectKey).normalize();
    if (!target.startsWith(rootDirectory)) {
      throw new IllegalArgumentException("objectKey escapes storage root");
    }
    try {
      return Files.readAllBytes(target);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read object: " + safeObjectKey, exception);
    }
  }

  private String trimTrailingSlash(String value) {
    String trimmed = value.trim();
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
  }
}
