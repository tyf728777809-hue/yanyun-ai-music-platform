package com.yanyun.music.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class LocalObjectStorageClient implements ObjectStorageClient {

  private final Path rootDirectory;
  private final String publicBaseUrl;

  public LocalObjectStorageClient(Path rootDirectory, String publicBaseUrl) {
    if (rootDirectory == null) {
      throw new IllegalArgumentException("rootDirectory is required");
    }
    if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
      throw new IllegalArgumentException("publicBaseUrl is required");
    }
    this.rootDirectory = rootDirectory.toAbsolutePath().normalize();
    this.publicBaseUrl = trimTrailingSlash(publicBaseUrl);
  }

  @Override
  public StoredObject putObject(ObjectStoragePutRequest request) {
    Path target = rootDirectory.resolve(request.objectKey()).normalize();
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
          request.objectKey(),
          publicBaseUrl + "/" + request.objectKey(),
          request.contentType(),
          content.length);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to write object: " + request.objectKey(), exception);
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
