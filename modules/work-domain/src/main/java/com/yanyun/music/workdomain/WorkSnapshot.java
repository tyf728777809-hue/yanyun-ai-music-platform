package com.yanyun.music.workdomain;

public record WorkSnapshot(
    WorkStatus status,
    GenerationStage generationStage,
    PackageStatus packageStatus,
    FailureCode failureCode,
    boolean retryable) {}
