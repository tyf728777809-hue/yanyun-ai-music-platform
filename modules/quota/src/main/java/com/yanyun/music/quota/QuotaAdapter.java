package com.yanyun.music.quota;

public interface QuotaAdapter {

  QuotaDecision getHint(String userId, int usedPolishCount);

  QuotaLock lockGenerateQuota(String userId, String workId);

  QuotaCommit commitGenerateQuota(String userId, String lockId);

  QuotaRelease releaseGenerateQuota(String userId, String lockId, String reason);
}
