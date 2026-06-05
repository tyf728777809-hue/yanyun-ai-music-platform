package com.yanyun.music.worker;

import com.yanyun.music.workflow.SongProductionActivities;
import com.yanyun.music.workflow.TemporalSongProductionWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TemporalWorkerProperties.class)
public class TemporalWorkerConfig {

  @Bean(destroyMethod = "")
  public WorkflowServiceStubs workflowServiceStubs(TemporalWorkerProperties properties) {
    WorkflowServiceStubsOptions options =
        WorkflowServiceStubsOptions.newBuilder()
            .setTarget(properties.target())
            .setRpcTimeout(Duration.ofSeconds(30))
            .build();
    return WorkflowServiceStubs.newServiceStubs(options);
  }

  @Bean
  public WorkflowClient workflowClient(
      WorkflowServiceStubs workflowServiceStubs, TemporalWorkerProperties properties) {
    WorkflowClientOptions options =
        WorkflowClientOptions.newBuilder().setNamespace(properties.namespace()).build();
    return WorkflowClient.newInstance(workflowServiceStubs, options);
  }

  @Bean(destroyMethod = "")
  public WorkerFactory workerFactory(WorkflowClient workflowClient) {
    return WorkerFactory.newInstance(workflowClient);
  }

  @Bean
  public Worker songProductionWorker(
      WorkerFactory workerFactory,
      TemporalWorkerProperties properties,
      SongProductionActivities activities) {
    Worker worker = workerFactory.newWorker(properties.taskQueue());
    worker.registerWorkflowImplementationTypes(TemporalSongProductionWorkflowImpl.class);
    worker.registerActivitiesImplementations(activities);
    return worker;
  }

  @Bean
  public TemporalWorkerLifecycle temporalWorkerLifecycle(
      WorkerFactory workerFactory,
      Worker songProductionWorker,
      WorkflowServiceStubs workflowServiceStubs,
      TemporalWorkerProperties properties) {
    return new TemporalWorkerLifecycle(workerFactory, workflowServiceStubs, properties);
  }
}
