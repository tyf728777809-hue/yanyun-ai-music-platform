package com.yanyun.music.storage;

import java.time.OffsetDateTime;

public record ObjectStorageDownloadUrl(String objectKey, String url, OffsetDateTime expiresAt) {

  public ObjectStorageDownloadUrl {
    if (objectKey == null || objectKey.isBlank()) {
      throw new IllegalArgumentException("objectKey is required");
    }
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("url is required");
    }
    if (expiresAt == null) {
      throw new IllegalArgumentException("expiresAt is required");
    }
  }
}
