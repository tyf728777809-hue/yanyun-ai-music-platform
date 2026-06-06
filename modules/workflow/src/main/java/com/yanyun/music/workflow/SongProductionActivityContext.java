package com.yanyun.music.workflow;

public record SongProductionActivityContext(String workId, String jobId, String userId) {

  public SongProductionActivityContext {
    workId = requireNonBlank(workId, "workId");
    jobId = requireNonBlank(jobId, "jobId");
    userId = requireNonBlank(userId, "userId");
  }

  public static SongProductionActivityContext from(SongProductionWorkflowInput input) {
    if (input == null) {
      throw new IllegalArgumentException("input is required");
    }
    return new SongProductionActivityContext(input.workId(), input.jobId(), input.userId());
  }

  public String idempotencyKey(SongProductionStepName stepName) {
    if (stepName == null) {
      throw new IllegalArgumentException("stepName is required");
    }
    return workId + ":" + jobId + ":" + stepName.name();
  }

  private static String requireNonBlank(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }
}
