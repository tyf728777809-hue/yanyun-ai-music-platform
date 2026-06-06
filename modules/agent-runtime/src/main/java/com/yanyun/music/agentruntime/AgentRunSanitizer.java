package com.yanyun.music.agentruntime;

import java.util.regex.Pattern;

public final class AgentRunSanitizer {

  private static final int MAX_FAILURE_MESSAGE_LENGTH = 500;
  private static final Pattern BEARER_TOKEN =
      Pattern.compile("Bearer\\s+[A-Za-z0-9._~+/=-]+", Pattern.CASE_INSENSITIVE);
  private static final Pattern KEY_VALUE =
      Pattern.compile(
          "(?i)(access[_-]?key|secret[_-]?key|api[_-]?key|token|jwt)\\s*[:=]\\s*[^,\\s}]+");

  private AgentRunSanitizer() {}

  public static String sanitizeFailureMessage(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String sanitized = BEARER_TOKEN.matcher(value).replaceAll("Bearer [REDACTED]");
    sanitized = KEY_VALUE.matcher(sanitized).replaceAll("$1=[REDACTED]");
    sanitized = sanitized.replaceAll("[\\r\\n\\t]+", " ").trim();
    return sanitized.length() <= MAX_FAILURE_MESSAGE_LENGTH
        ? sanitized
        : sanitized.substring(0, MAX_FAILURE_MESSAGE_LENGTH);
  }
}
