package com.yanyun.music.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yanyun.music.workflow.StepwiseSongProductionWorkflow;
import com.yanyun.music.workflow.TemporalSongProductionWorkflowImpl;
import org.junit.jupiter.api.Test;

class TemporalWorkerPropertiesTest {

  @Test
  void storesTemporalConnectionSettingsAndDefaultsToLegacyWorkflowMode() {
    TemporalWorkerProperties properties =
        new TemporalWorkerProperties("localhost:7233", "default", "song-production-local");

    assertThat(properties.target()).isEqualTo("localhost:7233");
    assertThat(properties.namespace()).isEqualTo("default");
    assertThat(properties.taskQueue()).isEqualTo("song-production-local");
    assertThat(properties.songProductionWorkflowMode()).isEqualTo("legacy");
    assertThat(properties.stepwiseRecordingWorkflowMode()).isFalse();
    assertThat(properties.workflowImplementationType())
        .isEqualTo(TemporalSongProductionWorkflowImpl.class);
  }

  @Test
  void acceptsStepwiseRecordingWorkflowMode() {
    TemporalWorkerProperties properties =
        new TemporalWorkerProperties(
            "localhost:7233", "default", "song-production-local", " STEPWISE-RECORDING ");

    assertThat(properties.songProductionWorkflowMode()).isEqualTo("stepwise-recording");
    assertThat(properties.stepwiseRecordingWorkflowMode()).isTrue();
    assertThat(properties.workflowImplementationType())
        .isEqualTo(StepwiseSongProductionWorkflow.class);
  }

  @Test
  void rejectsUnsupportedWorkflowMode() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TemporalWorkerProperties(
                "localhost:7233", "default", "song-production-local", "unknown"));
  }
}
