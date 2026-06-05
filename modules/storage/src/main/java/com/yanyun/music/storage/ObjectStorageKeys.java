package com.yanyun.music.storage;

final class ObjectStorageKeys {

  private ObjectStorageKeys() {}

  static String requireSafeObjectKey(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      throw new IllegalArgumentException("objectKey is required");
    }
    String normalized = objectKey.trim();
    if (normalized.startsWith("/") || normalized.contains("\\") || normalized.contains("://")) {
      throw new IllegalArgumentException("objectKey is invalid");
    }
    String[] segments = normalized.split("/");
    for (String segment : segments) {
      if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
        throw new IllegalArgumentException("objectKey is invalid");
      }
    }
    return normalized;
  }
}
