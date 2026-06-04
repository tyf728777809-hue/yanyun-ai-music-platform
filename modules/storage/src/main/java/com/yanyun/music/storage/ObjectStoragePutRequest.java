package com.yanyun.music.storage;

import java.util.Arrays;

public record ObjectStoragePutRequest(String objectKey, String contentType, byte[] content) {

  public ObjectStoragePutRequest {
    if (objectKey == null || objectKey.isBlank()) {
      throw new IllegalArgumentException("objectKey is required");
    }
    if (contentType == null || contentType.isBlank()) {
      throw new IllegalArgumentException("contentType is required");
    }
    content = content == null ? new byte[0] : Arrays.copyOf(content, content.length);
  }

  @Override
  public byte[] content() {
    return Arrays.copyOf(content, content.length);
  }
}
