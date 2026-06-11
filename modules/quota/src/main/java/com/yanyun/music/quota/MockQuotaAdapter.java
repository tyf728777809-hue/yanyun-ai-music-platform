package com.yanyun.music.quota;

import java.util.concurrent.atomic.AtomicInteger;

public final class MockQuotaAdapter implements QuotaAdapter {

  private static final int DEFAULT_MAX_GENERATE_LOCKS = 999;

  private final AtomicInteger remainingGenerateLocks;

  public MockQuotaAdapter() {
    this(DEFAULT_MAX_GENERATE_LOCKS);
  }

  public MockQuotaAdapter(int maxGenerateLocks) {
    this.remainingGenerateLocks = new AtomicInteger(Math.max(0, maxGenerateLocks));
  }

  @Override
  public QuotaDecision getHint(String userId, int usedPolishCount) {
    return new QuotaDecision(
        false,
        "ON_PACKAGE_READY",
        remainingGenerateLocks.get(),
        Math.max(0, 2 - usedPolishCount),
        "Local mock quota available");
  }

  @Override
  public QuotaLock lockGenerateQuota(String userId, String workId) {
    while (true) {
      int remaining = remainingGenerateLocks.get();
      if (remaining <= 0) {
        return new QuotaLock(false, "mock-lock-" + workId, "Local public trial quota exhausted");
      }
      if (remainingGenerateLocks.compareAndSet(remaining, remaining - 1)) {
        return new QuotaLock(true, "mock-lock-" + workId, "Quota locked until package is ready");
      }
    }
  }

  @Override
  public QuotaCommit commitGenerateQuota(String userId, String lockId) {
    return new QuotaCommit(true, "Quota committed by mock adapter");
  }

  @Override
  public QuotaRelease releaseGenerateQuota(String userId, String lockId, String reason) {
    return new QuotaRelease(true, reason == null ? "Quota released" : reason);
  }
}
