package com.yanyun.music.api.workflow;

public enum WorkflowOutboxStatus {
  PENDING,
  PROCESSING,
  SUCCEEDED,
  FAILED,
  SKIPPED
}
