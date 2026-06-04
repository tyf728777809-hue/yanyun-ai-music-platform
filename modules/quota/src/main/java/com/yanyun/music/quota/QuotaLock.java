package com.yanyun.music.quota;

public record QuotaLock(boolean locked, String lockId, String message) {}
