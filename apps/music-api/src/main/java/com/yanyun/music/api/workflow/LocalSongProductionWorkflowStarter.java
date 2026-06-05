package com.yanyun.music.api.workflow;

import com.yanyun.music.workflow.SongProductionWorkflow;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.SongProductionWorkflowResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
    prefix = "yanyun.workflow.outbox",
    name = "dispatch-target",
    havingValue = "local",
    matchIfMissing = true)
public class LocalSongProductionWorkflowStarter implements SongProductionWorkflowStarter {

  private final SongProductionWorkflow songProductionWorkflow;

  public LocalSongProductionWorkflowStarter(SongProductionWorkflow songProductionWorkflow) {
    this.songProductionWorkflow = songProductionWorkflow;
  }

  @Override
  public String start(SongProductionWorkflowInput input) {
    SongProductionWorkflowResult result = songProductionWorkflow.produce(input);
    if (result != null && result.jobId() != null && !result.jobId().isBlank()) {
      return result.jobId();
    }
    return input.jobId();
  }
}
