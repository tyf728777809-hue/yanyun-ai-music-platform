package com.yanyun.music.api.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.workdomain.PackageStatus;
import com.yanyun.music.workdomain.WorkStatus;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.WorkRow;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
    prefix = "yanyun.workflow.outbox",
    name = "dispatcher-enabled",
    havingValue = "true")
public class WorkflowOutboxDispatcher {

  private static final Logger log = LoggerFactory.getLogger(WorkflowOutboxDispatcher.class);

  private final WorkflowOutboxRepository outboxRepository;
  private final WorkflowDispatchProperties properties;
  private final SongProductionWorkflowStarter workflowStarter;
  private final WorkRepository workRepository;
  private final ObjectMapper objectMapper;
  private final String workerId;

  public WorkflowOutboxDispatcher(
      WorkflowOutboxRepository outboxRepository,
      WorkflowDispatchProperties properties,
      SongProductionWorkflowStarter workflowStarter,
      WorkRepository workRepository,
      ObjectMapper objectMapper) {
    this.outboxRepository = outboxRepository;
    this.properties = properties;
    this.workflowStarter = workflowStarter;
    this.workRepository = workRepository;
    this.objectMapper = objectMapper;
    this.workerId = workerId();
  }

  @Scheduled(
      fixedDelayString = "#{@workflowDispatchProperties.getOutbox().getPollInterval().toMillis()}")
  public void scheduledDrain() {
    if (!properties.outboxMode()) {
      return;
    }
    drainOnce();
  }

  public int drainOnce() {
    List<WorkflowOutboxEvent> events =
        outboxRepository.claimDue(
            properties.getOutbox().getBatchSize(),
            workerId,
            properties.getOutbox().getLockTimeout());
    for (WorkflowOutboxEvent event : events) {
      dispatch(event);
    }
    return events.size();
  }

  private void dispatch(WorkflowOutboxEvent event) {
    if (!WorkflowOutboxService.EVENT_TYPE_SONG_PRODUCTION_REQUESTED.equals(event.eventType())) {
      outboxRepository.markSkipped(
          event.id(), "Unsupported workflow event type: " + event.eventType());
      return;
    }
    try {
      SongProductionWorkflowInput input =
          objectMapper.readValue(event.payloadJson(), SongProductionWorkflowInput.class);
      if (alreadyTerminal(input)) {
        outboxRepository.markSucceeded(event.id());
        return;
      }
      workflowStarter.start(input);
      outboxRepository.markSucceeded(event.id());
    } catch (JsonProcessingException exception) {
      outboxRepository.markFailed(
          event.id(),
          "WORKFLOW_PAYLOAD_INVALID",
          "Workflow outbox payload is invalid",
          properties.getOutbox().getRetryDelay());
    } catch (RuntimeException exception) {
      String message =
          exception.getMessage() == null || exception.getMessage().isBlank()
              ? "Workflow dispatch failed"
              : exception.getMessage();
      log.warn("Workflow outbox dispatch failed. eventId={}", event.id(), exception);
      outboxRepository.markFailed(
          event.id(), "WORKFLOW_DISPATCH_FAILED", message, properties.getOutbox().getRetryDelay());
    }
  }

  private boolean alreadyTerminal(SongProductionWorkflowInput input) {
    UUID workId = UUID.fromString(input.workId());
    Optional<WorkRow> work = workRepository.findWorkForUser(workId, input.userId());
    if (work.isEmpty()) {
      return false;
    }
    WorkRow row = work.get();
    return row.status() == WorkStatus.GENERATED
        || row.packageStatus() == PackageStatus.PACKAGE_READY
        || row.packageStatus() == PackageStatus.PACKAGE_FETCHED;
  }

  private String workerId() {
    try {
      return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
    } catch (UnknownHostException exception) {
      return "workflow-outbox-" + UUID.randomUUID();
    }
  }
}
