package com.yanyun.music.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanyun.music.workflow.SongProductionActivities;
import com.yanyun.music.workflow.TemporalSongProductionWorkflowImpl;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.junit.jupiter.api.Test;

class TemporalWorkerConfigTest {

  @Test
  void registersSongProductionWorkflowAndActivitiesOnConfiguredTaskQueue() {
    WorkerFactory workerFactory = mock(WorkerFactory.class);
    Worker worker = mock(Worker.class);
    SongProductionActivities activities = mock(SongProductionActivities.class);
    TemporalWorkerProperties properties =
        new TemporalWorkerProperties("localhost:7233", "default", "song-production-local");
    TemporalWorkerConfig config = new TemporalWorkerConfig();

    when(workerFactory.newWorker(properties.taskQueue())).thenReturn(worker);

    Worker registeredWorker = config.songProductionWorker(workerFactory, properties, activities);

    assertThat(registeredWorker).isSameAs(worker);
    verify(workerFactory).newWorker("song-production-local");
    verify(worker).registerWorkflowImplementationTypes(TemporalSongProductionWorkflowImpl.class);
    verify(worker).registerActivitiesImplementations(activities);
  }
}
