package com.yanyun.music.api.workflow;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "yanyun.temporal")
public class TemporalWorkflowClientProperties {

  private String target = "localhost:7233";
  private String namespace = "default";
  private String taskQueue = "song-production-local";

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = textOrDefault(target, "localhost:7233");
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = textOrDefault(namespace, "default");
  }

  public String getTaskQueue() {
    return taskQueue;
  }

  public void setTaskQueue(String taskQueue) {
    this.taskQueue = textOrDefault(taskQueue, "song-production-local");
  }

  private String textOrDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
