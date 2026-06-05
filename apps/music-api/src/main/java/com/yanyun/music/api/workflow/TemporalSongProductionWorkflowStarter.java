package com.yanyun.music.api.workflow;

import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.TemporalSongProductionWorkflow;
import com.yanyun.music.workflow.TemporalSongProductionWorkflowIds;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemporalSongProductionWorkflowStarter implements SongProductionWorkflowStarter {

  private static final Logger log =
      LoggerFactory.getLogger(TemporalSongProductionWorkflowStarter.class);

  private final TemporalWorkflowClientProperties properties;
  private final TemporalWorkflowStartOperation startOperation;

  public TemporalSongProductionWorkflowStarter(
      WorkflowClient workflowClient, TemporalWorkflowClientProperties properties) {
    this(properties, new WorkflowClientStartOperation(workflowClient));
  }

  TemporalSongProductionWorkflowStarter(
      TemporalWorkflowClientProperties properties, TemporalWorkflowStartOperation startOperation) {
    this.properties = properties;
    this.startOperation = startOperation;
  }

  @Override
  public String start(SongProductionWorkflowInput input) {
    String workflowId = TemporalSongProductionWorkflowIds.workflowId(input);
    try {
      startOperation.start(workflowId, properties.getTaskQueue(), input);
      return workflowId;
    } catch (WorkflowExecutionAlreadyStarted exception) {
      log.info("Temporal song production workflow already started. workflowId={}", workflowId);
      return workflowId;
    } catch (RuntimeException exception) {
      if (causedByAlreadyStarted(exception)) {
        log.info("Temporal song production workflow already started. workflowId={}", workflowId);
        return workflowId;
      }
      throw new IllegalStateException("Temporal workflow start failed", exception);
    }
  }

  private boolean causedByAlreadyStarted(Throwable exception) {
    Throwable current = exception;
    while (current != null) {
      String className = current.getClass().getName();
      if (WorkflowExecutionAlreadyStarted.class.getName().equals(className)
          || className.endsWith("WorkflowExecutionAlreadyStarted")) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  @FunctionalInterface
  interface TemporalWorkflowStartOperation {

    void start(String workflowId, String taskQueue, SongProductionWorkflowInput input);
  }

  private static final class WorkflowClientStartOperation
      implements TemporalWorkflowStartOperation {

    private final WorkflowClient workflowClient;

    private WorkflowClientStartOperation(WorkflowClient workflowClient) {
      this.workflowClient = workflowClient;
    }

    @Override
    public void start(String workflowId, String taskQueue, SongProductionWorkflowInput input) {
      TemporalSongProductionWorkflow workflow =
          workflowClient.newWorkflowStub(
              TemporalSongProductionWorkflow.class,
              WorkflowOptions.newBuilder()
                  .setWorkflowId(workflowId)
                  .setTaskQueue(taskQueue)
                  .build());
      WorkflowClient.start(workflow::produce, input);
    }
  }
}
