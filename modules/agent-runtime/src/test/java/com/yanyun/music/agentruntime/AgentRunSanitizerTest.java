package com.yanyun.music.agentruntime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AgentRunSanitizerTest {

  @Test
  void redactsTokensAndKeys() {
    String sanitized =
        AgentRunSanitizer.sanitizeFailureMessage(
            "failed Authorization Bearer abc.def.ghi SecretKey: real-secret token=raw-token");

    assertTrue(sanitized.contains("Bearer [REDACTED]"));
    assertTrue(sanitized.contains("SecretKey=[REDACTED]"));
    assertTrue(sanitized.contains("token=[REDACTED]"));
    assertFalse(sanitized.contains("abc.def.ghi"));
    assertFalse(sanitized.contains("real-secret"));
    assertFalse(sanitized.contains("raw-token"));
  }

  @Test
  void truncatesLongMessages() {
    String sanitized = AgentRunSanitizer.sanitizeFailureMessage("x".repeat(800));

    assertTrue(sanitized.length() <= 500);
  }
}
