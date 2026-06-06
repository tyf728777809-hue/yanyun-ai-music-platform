package com.yanyun.music.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MockModerationAdapterTest {

  @Test
  void publishPackagePreCheckAllowsByDefault() {
    MockModerationAdapter adapter = new MockModerationAdapter();

    ModerationDecision decision = adapter.preCheckPublishPackage("user-1", "work-1");

    assertTrue(decision.allowed());
    assertNull(decision.code());
  }

  @Test
  void publishPackagePreCheckBlocksConfiguredUserId() {
    MockModerationAdapter adapter =
        new MockModerationAdapter(List.of("mock_package_block_smoke"), List.of());

    ModerationDecision decision =
        adapter.preCheckPublishPackage("mock_package_block_smoke", "work-1");

    assertFalse(decision.allowed());
    assertEquals("MOCK_PACKAGE_BLOCKED", decision.code());
    assertEquals("作品暂不能交给社区发布。", decision.message());
  }

  @Test
  void publishPackagePreCheckBlocksConfiguredWorkIdAndIgnoresBlankValues() {
    MockModerationAdapter adapter =
        new MockModerationAdapter(List.of(" "), List.of("", "work-blocked"));

    ModerationDecision decision = adapter.preCheckPublishPackage("user-1", "work-blocked");

    assertFalse(decision.allowed());
    assertEquals("MOCK_PACKAGE_BLOCKED", decision.code());
  }
}
