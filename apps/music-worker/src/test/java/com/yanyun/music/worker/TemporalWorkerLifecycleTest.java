package com.yanyun.music.worker;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.boot.ApplicationArguments;

class TemporalWorkerLifecycleTest {

  @Test
  void startsWorkerFactoryOnce() throws Exception {
    WorkerFactory workerFactory = mock(WorkerFactory.class);
    WorkflowServiceStubs workflowServiceStubs = mock(WorkflowServiceStubs.class);
    TemporalWorkerLifecycle lifecycle =
        new TemporalWorkerLifecycle(workerFactory, workflowServiceStubs, properties());

    lifecycle.run(mock(ApplicationArguments.class));
    lifecycle.run(mock(ApplicationArguments.class));

    verify(workerFactory, times(1)).start();
  }

  @Test
  void shutsDownWorkerFactoryBeforeServiceStubs() {
    WorkerFactory workerFactory = mock(WorkerFactory.class);
    WorkflowServiceStubs workflowServiceStubs = mock(WorkflowServiceStubs.class);
    TemporalWorkerLifecycle lifecycle =
        new TemporalWorkerLifecycle(workerFactory, workflowServiceStubs, properties());

    lifecycle.shutdown();
    lifecycle.shutdown();

    InOrder shutdownOrder = inOrder(workerFactory, workflowServiceStubs);
    shutdownOrder.verify(workerFactory).shutdown();
    shutdownOrder.verify(workerFactory).awaitTermination(10L, TimeUnit.SECONDS);
    shutdownOrder.verify(workflowServiceStubs).shutdown();
    shutdownOrder.verify(workflowServiceStubs).awaitTermination(5L, TimeUnit.SECONDS);
    verify(workerFactory, times(1)).shutdown();
    verify(workflowServiceStubs, times(1)).shutdown();
  }

  private TemporalWorkerProperties properties() {
    return new TemporalWorkerProperties("localhost:7233", "default", "song-production-local");
  }
}
