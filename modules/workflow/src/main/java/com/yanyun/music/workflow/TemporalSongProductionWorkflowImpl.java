package com.yanyun.music.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class TemporalSongProductionWorkflowImpl implements TemporalSongProductionWorkflow {

  private static final Duration ACTIVITY_START_TO_CLOSE_TIMEOUT = Duration.ofMinutes(30);

  private final SongProductionActivities activities;

  public TemporalSongProductionWorkflowImpl() {
    this(
        Workflow.newActivityStub(
            SongProductionActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(ACTIVITY_START_TO_CLOSE_TIMEOUT)
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                .build()));
  }

  TemporalSongProductionWorkflowImpl(SongProductionActivities activities) {
    this.activities = activities;
  }

  @Override
  public SongProductionWorkflowResult produce(SongProductionWorkflowInput input) {
    return activities.produce(input);
  }
}
