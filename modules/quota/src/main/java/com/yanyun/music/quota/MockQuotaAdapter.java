package com.yanyun.music.quota;

public final class MockQuotaAdapter implements QuotaAdapter {

  @Override
  public QuotaDecision getHint(String userId, int usedPolishCount) {
    return new QuotaDecision(
        false,
        "ON_PACKAGE_READY",
        999,
        Math.max(0, 2 - usedPolishCount),
        "Local mock quota available");
  }

  @Override
  public QuotaLock lockGenerateQuota(String userId, String workId) {
    return new QuotaLock(true, "mock-lock-" + workId, "Quota locked until package is ready");
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
