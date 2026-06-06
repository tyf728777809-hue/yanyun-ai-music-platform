package com.yanyun.music.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanyun.music.workflow.SongProductionActivities;
import com.yanyun.music.workflow.SongProductionStepActivities;
import com.yanyun.music.workflow.StepwiseSongProductionWorkflow;
import com.yanyun.music.workflow.TemporalSongProductionWorkflowImpl;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class TemporalWorkerConfigTest {

  @Test
  void registersSongProductionWorkflowAndActivitiesOnConfiguredTaskQueue() {
    WorkerFactory workerFactory = mock(WorkerFactory.class);
    Worker worker = mock(Worker.class);
    SongProductionActivities activities = mock(SongProductionActivities.class);
    ObjectProvider<SongProductionStepActivities> stepActivities = mockStepActivityProvider();
    TemporalWorkerProperties properties =
        new TemporalWorkerProperties("localhost:7233", "default", "song-production-local", null);
    TemporalWorkerConfig config = new TemporalWorkerConfig();

    when(workerFactory.newWorker(properties.taskQueue())).thenReturn(worker);

    Worker registeredWorker =
        config.songProductionWorker(workerFactory, properties, activities, stepActivities);

    assertThat(registeredWorker).isSameAs(worker);
    verify(workerFactory).newWorker("song-production-local");
    verify(worker).registerWorkflowImplementationTypes(TemporalSongProductionWorkflowImpl.class);
    verify(worker).registerActivitiesImplementations(activities);
    verify(stepActivities, never()).getObject();
  }

  @Test
  void registersStepwiseWorkflowAndStepActivitiesWhenExplicitlyConfigured() {
    WorkerFactory workerFactory = mock(WorkerFactory.class);
    Worker worker = mock(Worker.class);
    SongProductionActivities activities = mock(SongProductionActivities.class);
    SongProductionStepActivities stepActivities = mock(SongProductionStepActivities.class);
    ObjectProvider<SongProductionStepActivities> stepActivityProvider = mockStepActivityProvider();
    TemporalWorkerProperties properties =
        new TemporalWorkerProperties(
            "localhost:7233", "default", "song-production-local", "stepwise-recording");
    TemporalWorkerConfig config = new TemporalWorkerConfig();

    when(workerFactory.newWorker(properties.taskQueue())).thenReturn(worker);
    when(stepActivityProvider.getObject()).thenReturn(stepActivities);

    Worker registeredWorker =
        config.songProductionWorker(workerFactory, properties, activities, stepActivityProvider);

    assertThat(registeredWorker).isSameAs(worker);
    verify(workerFactory).newWorker("song-production-local");
    verify(worker).registerWorkflowImplementationTypes(StepwiseSongProductionWorkflow.class);
    verify(stepActivityProvider).getObject();
    verify(worker).registerActivitiesImplementations(stepActivities);
    verify(worker, never()).registerActivitiesImplementations(activities);
  }

  @SuppressWarnings("unchecked")
  private ObjectProvider<SongProductionStepActivities> mockStepActivityProvider() {
    return mock(ObjectProvider.class);
  }
}
