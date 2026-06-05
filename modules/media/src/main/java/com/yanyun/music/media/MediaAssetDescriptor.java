package com.yanyun.music.media;

import java.util.Map;

public record MediaAssetDescriptor(
    String assetType,
    String objectKey,
    String mimeType,
    long fileSizeBytes,
    String checksum,
    Integer width,
    Integer height,
    Integer durationMs,
    Map<String, Object> metadata) {

  public MediaAssetDescriptor {
    if (assetType == null || assetType.isBlank()) {
      throw new IllegalArgumentException("assetType is required");
    }
    if (objectKey == null || objectKey.isBlank()) {
      throw new IllegalArgumentException("objectKey is required");
    }
    if (mimeType == null || mimeType.isBlank()) {
      throw new IllegalArgumentException("mimeType is required");
    }
    if (fileSizeBytes < 0) {
      throw new IllegalArgumentException("fileSizeBytes must be non-negative");
    }
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
  }
}
