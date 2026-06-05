package com.yanyun.music.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TemporalSongProductionWorkflowIdsTest {

  @Test
  void buildsDeterministicWorkflowIdFromWorkAndJob() {
    SongProductionWorkflowInput input =
        new SongProductionWorkflowInput(
            "work-1", "user-1", "draft-1", "title", "summary", "lyrics", "prompt", "AUTO", "mock",
            true, "job-1");

    assertEquals(
        "song-production:work-1:job-1", TemporalSongProductionWorkflowIds.workflowId(input));
  }

  @Test
  void rejectsMissingJobId() {
    SongProductionWorkflowInput input =
        new SongProductionWorkflowInput(
            "work-1", "user-1", "draft-1", "title", "summary", "lyrics", "prompt", "AUTO", "mock",
            true);

    assertThrows(
        IllegalArgumentException.class, () -> TemporalSongProductionWorkflowIds.workflowId(input));
  }
}
