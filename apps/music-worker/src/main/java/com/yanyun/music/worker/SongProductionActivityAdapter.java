package com.yanyun.music.worker;

import com.yanyun.music.workflow.SongProductionActivities;
import com.yanyun.music.workflow.SongProductionWorkflow;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.SongProductionWorkflowResult;
import org.springframework.stereotype.Component;

@Component
public class SongProductionActivityAdapter implements SongProductionActivities {

  private final SongProductionWorkflow delegate;

  public SongProductionActivityAdapter(SongProductionWorkflow delegate) {
    this.delegate = delegate;
  }

  @Override
  public SongProductionWorkflowResult produce(SongProductionWorkflowInput input) {
    return delegate.produce(input);
  }
}
