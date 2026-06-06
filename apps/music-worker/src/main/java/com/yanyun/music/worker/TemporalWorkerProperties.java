package com.yanyun.music.worker;

import com.yanyun.music.workflow.StepwiseSongProductionWorkflow;
import com.yanyun.music.workflow.TemporalSongProductionWorkflowImpl;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yanyun.temporal")
public record TemporalWorkerProperties(
    String target, String namespace, String taskQueue, String songProductionWorkflowMode) {

  static final String WORKFLOW_MODE_LEGACY = "legacy";
  static final String WORKFLOW_MODE_STEPWISE_RECORDING = "stepwise-recording";

  public TemporalWorkerProperties {
    songProductionWorkflowMode = normalizeWorkflowMode(songProductionWorkflowMode);
  }

  boolean stepwiseRecordingWorkflowMode() {
    return WORKFLOW_MODE_STEPWISE_RECORDING.equals(songProductionWorkflowMode);
  }

  Class<?> workflowImplementationType() {
    return stepwiseRecordingWorkflowMode()
        ? StepwiseSongProductionWorkflow.class
        : TemporalSongProductionWorkflowImpl.class;
  }

  private static String normalizeWorkflowMode(String mode) {
    if (mode == null || mode.isBlank()) {
      return WORKFLOW_MODE_LEGACY;
    }
    String normalized = mode.trim().toLowerCase();
    if (WORKFLOW_MODE_LEGACY.equals(normalized)
        || WORKFLOW_MODE_STEPWISE_RECORDING.equals(normalized)) {
      return normalized;
    }
    throw new IllegalArgumentException(
        "Unsupported song production workflow mode: "
            + mode
            + ". Supported modes: legacy, stepwise-recording");
  }
}
