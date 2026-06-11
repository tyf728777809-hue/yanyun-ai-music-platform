package com.yanyun.music.quota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MockQuotaAdapterTest {

  @Test
  void locksUntilConfiguredTrialLimitIsExhausted() {
    MockQuotaAdapter adapter = new MockQuotaAdapter(2);

    assertEquals(2, adapter.getHint("user-1", 0).remainingGenerateCount());
    assertTrue(adapter.lockGenerateQuota("user-1", "work-1").locked());
    assertEquals(1, adapter.getHint("user-1", 0).remainingGenerateCount());
    assertTrue(adapter.lockGenerateQuota("user-1", "work-2").locked());
    assertEquals(0, adapter.getHint("user-1", 0).remainingGenerateCount());

    QuotaLock exhausted = adapter.lockGenerateQuota("user-1", "work-3");

    assertFalse(exhausted.locked());
    assertTrue(exhausted.message().contains("quota exhausted"));
  }

  @Test
  void zeroLimitRejectsImmediately() {
    MockQuotaAdapter adapter = new MockQuotaAdapter(0);

    assertEquals(0, adapter.getHint("user-1", 1).remainingGenerateCount());
    assertEquals(1, adapter.getHint("user-1", 1).remainingPolishCount());
    assertFalse(adapter.lockGenerateQuota("user-1", "work-1").locked());
  }
}
