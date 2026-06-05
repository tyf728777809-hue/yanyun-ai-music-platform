package com.yanyun.music.api.workflow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.api.work.WorkRepository;
import com.yanyun.music.api.work.WorkRepository.WorkRow;
import com.yanyun.music.workdomain.CreationMode;
import com.yanyun.music.workdomain.GenerationStage;
import com.yanyun.music.workdomain.PackageStatus;
import com.yanyun.music.workdomain.WorkStatus;
import com.yanyun.music.workflow.SongProductionWorkflow;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.SongProductionWorkflowResult;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkflowOutboxDispatcherTest {

  private final WorkflowOutboxRepository outboxRepository = mock(WorkflowOutboxRepository.class);
  private final SongProductionWorkflow songProductionWorkflow = mock(SongProductionWorkflow.class);
  private final WorkRepository workRepository = mock(WorkRepository.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final WorkflowDispatchProperties properties = outboxProperties();

  @Test
  void drainsPendingSongProductionEventAndMarksSucceeded() throws Exception {
    UUID workId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    WorkflowOutboxEvent event = event(eventId, workId, inputJson(workId));
    when(outboxRepository.claimDue(anyInt(), any(), any())).thenReturn(List.of(event));
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(
            Optional.of(work(workId, WorkStatus.GENERATING, GenerationStage.QUOTA_LOCKING)));
    when(songProductionWorkflow.produce(any()))
        .thenReturn(
            SongProductionWorkflowResult.packageReady(
                UUID.randomUUID().toString(), "PACKAGE_READY"));

    dispatcher().drainOnce();

    verify(songProductionWorkflow).produce(any());
    verify(outboxRepository).markSucceeded(eventId);
  }

  @Test
  void marksFailedWhenWorkflowThrows() throws Exception {
    UUID workId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    WorkflowOutboxEvent event = event(eventId, workId, inputJson(workId));
    when(outboxRepository.claimDue(anyInt(), any(), any())).thenReturn(List.of(event));
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(
            Optional.of(work(workId, WorkStatus.GENERATING, GenerationStage.QUOTA_LOCKING)));
    when(songProductionWorkflow.produce(any())).thenThrow(new IllegalStateException("boom"));

    dispatcher().drainOnce();

    verify(outboxRepository)
        .markFailed(eq(eventId), eq("WORKFLOW_DISPATCH_FAILED"), eq("boom"), any());
  }

  @Test
  void terminalGeneratedWorkIsNotReprocessed() throws Exception {
    UUID workId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    WorkflowOutboxEvent event = event(eventId, workId, inputJson(workId));
    when(outboxRepository.claimDue(anyInt(), any(), any())).thenReturn(List.of(event));
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(work(workId, WorkStatus.GENERATED, GenerationStage.PACKAGE_READY)));

    dispatcher().drainOnce();

    verify(songProductionWorkflow, never()).produce(any());
    verify(outboxRepository).markSucceeded(eventId);
  }

  private WorkflowOutboxDispatcher dispatcher() {
    return new WorkflowOutboxDispatcher(
        outboxRepository, properties, songProductionWorkflow, workRepository, objectMapper);
  }

  private WorkflowDispatchProperties outboxProperties() {
    WorkflowDispatchProperties properties = new WorkflowDispatchProperties();
    properties.setDispatchMode(WorkflowDispatchProperties.DispatchMode.OUTBOX);
    properties.getOutbox().setDispatcherEnabled(true);
    return properties;
  }

  private String inputJson(UUID workId) throws JsonProcessingException {
    return objectMapper.writeValueAsString(
        new SongProductionWorkflowInput(
            workId.toString(),
            "user-1",
            UUID.randomUUID().toString(),
            "Mock title",
            "Mock summary",
            "Mock lyrics",
            "Mock prompt",
            "AUTO",
            "mock",
            true,
            UUID.randomUUID().toString()));
  }

  private WorkflowOutboxEvent event(UUID eventId, UUID workId, String payloadJson) {
    return new WorkflowOutboxEvent(
        eventId,
        "WORK",
        workId,
        WorkflowOutboxService.EVENT_TYPE_SONG_PRODUCTION_REQUESTED,
        payloadJson,
        WorkflowOutboxStatus.PROCESSING,
        0,
        3,
        null,
        OffsetDateTime.now(),
        "test-worker",
        null,
        null,
        null,
        OffsetDateTime.now(),
        OffsetDateTime.now());
  }

  private WorkRow work(UUID workId, WorkStatus status, GenerationStage stage) {
    return new WorkRow(
        workId,
        "YYM-20260605-ABCDEF",
        "user-1",
        CreationMode.LYRICS,
        status,
        stage,
        status == WorkStatus.GENERATED
            ? PackageStatus.PACKAGE_READY
            : PackageStatus.PACKAGE_NOT_READY,
        "Mock title",
        "Mock summary",
        0,
        0,
        null,
        null,
        null,
        null,
        false,
        false,
        0,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        status == WorkStatus.GENERATED ? OffsetDateTime.now() : null,
        1);
  }
}
