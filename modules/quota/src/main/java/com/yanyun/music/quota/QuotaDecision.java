package com.yanyun.music.quota;

public record QuotaDecision(
    boolean locked,
    String commitTiming,
    int remainingGenerateCount,
    int remainingPolishCount,
    String message) {}
