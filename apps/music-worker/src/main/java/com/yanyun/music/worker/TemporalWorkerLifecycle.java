package com.yanyun.music.worker;

import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public class TemporalWorkerLifecycle implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(TemporalWorkerLifecycle.class);
  private static final long WORKER_FACTORY_SHUTDOWN_SECONDS = 10L;
  private static final long SERVICE_STUBS_SHUTDOWN_SECONDS = 5L;

  private final WorkerFactory workerFactory;
  private final WorkflowServiceStubs workflowServiceStubs;
  private final TemporalWorkerProperties properties;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicBoolean shutDown = new AtomicBoolean(false);

  public TemporalWorkerLifecycle(
      WorkerFactory workerFactory,
      WorkflowServiceStubs workflowServiceStubs,
      TemporalWorkerProperties properties) {
    this.workerFactory = workerFactory;
    this.workflowServiceStubs = workflowServiceStubs;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (started.compareAndSet(false, true)) {
      workerFactory.start();
      log.info(
          "Temporal worker factory started. target={}, namespace={}, taskQueue={}",
          properties.target(),
          properties.namespace(),
          properties.taskQueue());
    }
  }

  @PreDestroy
  public void shutdown() {
    if (!shutDown.compareAndSet(false, true)) {
      return;
    }

    workerFactory.shutdown();
    workerFactory.awaitTermination(WORKER_FACTORY_SHUTDOWN_SECONDS, TimeUnit.SECONDS);
    workflowServiceStubs.shutdown();
    workflowServiceStubs.awaitTermination(SERVICE_STUBS_SHUTDOWN_SECONDS, TimeUnit.SECONDS);
    log.info(
        "Temporal worker resources stopped. target={}, namespace={}, taskQueue={}",
        properties.target(),
        properties.namespace(),
        properties.taskQueue());
  }
}
