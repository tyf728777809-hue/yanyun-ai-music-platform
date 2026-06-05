package com.yanyun.music.storage;

import java.nio.file.Path;
import java.util.Locale;

public final class ObjectStorageClientFactory {

  private ObjectStorageClientFactory() {}

  public static ObjectStorageClient create(ObjectStorageProperties properties) {
    ObjectStorageProperties effective =
        properties == null ? new ObjectStorageProperties() : properties;
    String provider =
        effective.getProvider() == null || effective.getProvider().isBlank()
            ? "local"
            : effective.getProvider().trim().toLowerCase(Locale.ROOT);
    return switch (provider) {
      case "local" ->
          new LocalObjectStorageClient(
              Path.of(effective.getLocalRootDirectory()),
              effective.getPublicBaseUrl(),
              effective.getUrlTtl());
      case "s3", "minio" -> new S3ObjectStorageClient(effective);
      default ->
          throw new IllegalArgumentException("Unsupported object storage provider: " + provider);
    };
  }
}
