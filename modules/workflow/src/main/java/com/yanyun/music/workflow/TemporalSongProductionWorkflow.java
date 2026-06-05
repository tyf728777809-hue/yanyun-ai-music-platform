package com.yanyun.music.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface TemporalSongProductionWorkflow {

  @WorkflowMethod
  SongProductionWorkflowResult produce(SongProductionWorkflowInput input);
}
