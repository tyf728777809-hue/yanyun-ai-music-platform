package com.yanyun.music.api.workflow;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    prefix = "yanyun.workflow.outbox",
    name = "dispatch-target",
    havingValue = "temporal")
public class TemporalSongProductionWorkflowClientConfig {

  @Bean(destroyMethod = "shutdown")
  WorkflowServiceStubs temporalWorkflowServiceStubs(TemporalWorkflowClientProperties properties) {
    return WorkflowServiceStubs.newServiceStubs(
        WorkflowServiceStubsOptions.newBuilder().setTarget(properties.getTarget()).build());
  }

  @Bean
  WorkflowClient temporalWorkflowClient(
      WorkflowServiceStubs temporalWorkflowServiceStubs,
      TemporalWorkflowClientProperties properties) {
    return WorkflowClient.newInstance(
        temporalWorkflowServiceStubs,
        WorkflowClientOptions.newBuilder().setNamespace(properties.getNamespace()).build());
  }

  @Bean
  SongProductionWorkflowStarter temporalSongProductionWorkflowStarter(
      WorkflowClient temporalWorkflowClient, TemporalWorkflowClientProperties properties) {
    return new TemporalSongProductionWorkflowStarter(temporalWorkflowClient, properties);
  }
}
