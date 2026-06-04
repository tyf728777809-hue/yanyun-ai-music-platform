package com.yanyun.music.worker;

import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TemporalConnectionVerifier implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(TemporalConnectionVerifier.class);

  private final TemporalWorkerProperties properties;

  public TemporalConnectionVerifier(TemporalWorkerProperties properties) {
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    WorkflowServiceStubsOptions options =
        WorkflowServiceStubsOptions.newBuilder()
            .setTarget(properties.target())
            .setRpcTimeout(Duration.ofSeconds(5))
            .setHealthCheckAttemptTimeout(Duration.ofSeconds(2))
            .setHealthCheckTimeout(Duration.ofSeconds(10))
            .build();

    WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(options);
    try {
      service.connect(Duration.ofSeconds(10));
      log.info(
          "Temporal connection verified. target={}, namespace={}, taskQueue={}",
          properties.target(),
          properties.namespace(),
          properties.taskQueue());
    } catch (RuntimeException error) {
      throw new IllegalStateException(
          "Unable to connect Temporal local service. target="
              + properties.target()
              + ", namespace="
              + properties.namespace()
              + ", taskQueue="
              + properties.taskQueue(),
          error);
    } finally {
      service.shutdown();
      service.awaitTermination(1, TimeUnit.SECONDS);
    }
  }
}
