package com.yanyun.music.workflow;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface SongProductionActivities {

  @ActivityMethod
  SongProductionWorkflowResult produce(SongProductionWorkflowInput input);
}
