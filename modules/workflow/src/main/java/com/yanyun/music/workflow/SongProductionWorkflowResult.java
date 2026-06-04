package com.yanyun.music.workflow;

public record SongProductionWorkflowResult(
    String jobId,
    boolean packageReady,
    String packageStatus,
    String failureCode,
    String failureMessage) {

  public static SongProductionWorkflowResult packageReady(String jobId, String packageStatus) {
    return new SongProductionWorkflowResult(jobId, true, packageStatus, null, null);
  }

  public static SongProductionWorkflowResult failed(
      String jobId, String packageStatus, String failureCode, String failureMessage) {
    return new SongProductionWorkflowResult(
        jobId, false, packageStatus, failureCode, failureMessage);
  }
}
