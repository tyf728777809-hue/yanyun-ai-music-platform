package com.yanyun.music.workflow;

public final class TemporalSongProductionWorkflowIds {

  private static final String PREFIX = "song-production";

  private TemporalSongProductionWorkflowIds() {}

  public static String workflowId(SongProductionWorkflowInput input) {
    if (input == null) {
      throw new IllegalArgumentException("input is required");
    }
    if (input.workId() == null || input.workId().isBlank()) {
      throw new IllegalArgumentException("workId is required");
    }
    if (input.jobId() == null || input.jobId().isBlank()) {
      throw new IllegalArgumentException("jobId is required");
    }
    return PREFIX + ":" + input.workId().trim() + ":" + input.jobId().trim();
  }
}
