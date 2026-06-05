package com.yanyun.music.dreammaker;

import java.util.Locale;

public final class DreamMakerFailureMapper {

  public static final String MUSIC_GENERATION_FAILED = "MUSIC_GENERATION_FAILED";
  public static final String MUSIC_QUALITY_FAILED = "MUSIC_QUALITY_FAILED";
  public static final String PROVIDER_TIMEOUT = "PROVIDER_TIMEOUT";
  public static final String RATE_LIMITED = "RATE_LIMITED";

  private DreamMakerFailureMapper() {}

  public static String fromHttpStatus(int statusCode) {
    if (statusCode == 408 || statusCode == 504) {
      return PROVIDER_TIMEOUT;
    }
    if (statusCode == 429) {
      return RATE_LIMITED;
    }
    return MUSIC_GENERATION_FAILED;
  }

  public static String fromProviderError(int code, String message) {
    String normalized = normalize(message);
    if (normalized.contains("timeout") || normalized.contains("timed out")) {
      return PROVIDER_TIMEOUT;
    }
    if (normalized.contains("rate")
        || normalized.contains("quota")
        || normalized.contains("limit")
        || normalized.contains("throttle")) {
      return RATE_LIMITED;
    }
    if (normalized.contains("quality") || normalized.contains("audio")) {
      return MUSIC_QUALITY_FAILED;
    }
    return MUSIC_GENERATION_FAILED;
  }

  public static String sanitizedMessage(String providerName, String message, String fallback) {
    String value = message == null ? "" : message.trim();
    if (value.isEmpty()) {
      return fallback;
    }
    if (value.length() > 240) {
      value = value.substring(0, 240);
    }
    return providerName + " provider error: " + value;
  }

  private static String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }
}
