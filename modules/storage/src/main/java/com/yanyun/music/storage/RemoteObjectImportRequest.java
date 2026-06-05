package com.yanyun.music.storage;

public record RemoteObjectImportRequest(String sourceUrl, String objectKey, String contentType) {

  public RemoteObjectImportRequest {
    if (sourceUrl == null || sourceUrl.isBlank()) {
      throw new IllegalArgumentException("sourceUrl is required");
    }
    if (objectKey == null || objectKey.isBlank()) {
      throw new IllegalArgumentException("objectKey is required");
    }
    contentType =
        contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
  }
}
