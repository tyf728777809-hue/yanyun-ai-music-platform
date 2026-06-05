package com.yanyun.music.api.workflow;

import com.yanyun.music.workflow.SongProductionWorkflowInput;

public interface SongProductionWorkflowStarter {

  String start(SongProductionWorkflowInput input);
}
