package com.yanyun.music.workflow;

public record SongProductionWorkflowResult(
    boolean packageReady, String packageStatus, String failureCode, String failureMessage) {}
