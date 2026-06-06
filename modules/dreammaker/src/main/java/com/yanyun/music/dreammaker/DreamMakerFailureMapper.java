package com.yanyun.music.dreammaker;

import java.util.Locale;
import java.util.regex.Pattern;

public final class DreamMakerFailureMapper {

  public static final String MUSIC_GENERATION_FAILED = "MUSIC_GENERATION_FAILED";
  public static final String MUSIC_QUALITY_FAILED = "MUSIC_QUALITY_FAILED";
  public static final String PROVIDER_AUTH_FAILED = "PROVIDER_AUTH_FAILED";
  public static final String PROVIDER_TIMEOUT = "PROVIDER_TIMEOUT";
  public static final String RATE_LIMITED = "RATE_LIMITED";
  private static final Pattern BEARER_TOKEN_PATTERN =
      Pattern.compile("(?i)bearer\\s+[a-z0-9._~+/=-]+");
  private static final Pattern JWT_PATTERN =
      Pattern.compile("\\b[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b");
  private static final Pattern SECRET_FIELD_PATTERN =
      Pattern.compile("(?i)(access[_ -]?key|secret[_ -]?key|token)\\s*[:=]\\s*[^,;\\s]+");

  private DreamMakerFailureMapper() {}

  public static String fromHttpStatus(int statusCode) {
    if (statusCode == 401 || statusCode == 403) {
      return PROVIDER_AUTH_FAILED;
    }
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
    if (code == 401 || code == 403 || isAuthFailure(normalized)) {
      return PROVIDER_AUTH_FAILED;
    }
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
    String value = message == null ? "" : message.replaceAll("[\\r\\n\\t]+", " ").trim();
    if (value.isEmpty()) {
      return fallback;
    }
    value = BEARER_TOKEN_PATTERN.matcher(value).replaceAll("Bearer <redacted>");
    value = JWT_PATTERN.matcher(value).replaceAll("<jwt-redacted>");
    value = SECRET_FIELD_PATTERN.matcher(value).replaceAll("$1=<redacted>");
    if (value.length() > 240) {
      value = value.substring(0, 240);
    }
    return providerName + " provider error: " + value;
  }

  private static String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  private static boolean isAuthFailure(String normalized) {
    return normalized.contains("unauthorized")
        || normalized.contains("forbidden")
        || normalized.contains("permission")
        || normalized.contains("not authorized")
        || normalized.contains("authentication")
        || normalized.contains("credential")
        || normalized.contains("access key")
        || normalized.contains("secret key")
        || normalized.contains("signature")
        || normalized.contains("unsupported model")
        || normalized.contains("app not found");
  }
}
