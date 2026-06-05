package com.yanyun.music.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class MockPublishAdapterTest {

  private static final Pattern PACKAGE_KEY =
      Pattern.compile(
          "yanyun-ai-music/staging/\\d{4}/\\d{2}/\\d{2}/work-1/package/publish-package\\.json");

  @Test
  void createsStructuredPackageObjectKey() {
    PublishHandoff handoff = new MockPublishAdapter("staging").preparePackage("work-1");

    assertTrue(PACKAGE_KEY.matcher(handoff.packageObjectKey()).matches());
    assertEquals("", handoff.packageUrl());
    assertTrue(handoff.expiresAt().isAfter(java.time.OffsetDateTime.now()));
  }
}
