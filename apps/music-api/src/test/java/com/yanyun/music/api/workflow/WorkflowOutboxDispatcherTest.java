package com.yanyun.music.api.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.workdomain.CreationMode;
import com.yanyun.music.workdomain.GenerationStage;
import com.yanyun.music.workdomain.PackageStatus;
import com.yanyun.music.workdomain.WorkStatus;
import com.yanyun.music.workflow.SongProductionWorkflow;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.SongProductionWorkflowResult;
import com.yanyun.music.workflow.TemporalSongProductionWorkflowIds;
import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.WorkRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class WorkflowOutboxDispatcherTest {

  private final WorkflowOutboxRepository outboxRepository = mock(WorkflowOutboxRepository.class);
  private final SongProductionWorkflowStarter workflowStarter =
      mock(SongProductionWorkflowStarter.class);
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
    when(workflowStarter.start(any())).thenReturn(UUID.randomUUID().toString());

    dispatcher().drainOnce();

    verify(workflowStarter).start(any());
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
    when(workflowStarter.start(any())).thenThrow(new IllegalStateException("boom"));

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

    verify(workflowStarter, never()).start(any());
    verify(outboxRepository).markSucceeded(eventId);
  }

  @Test
  void localStarterDelegatesToSpringWorkflow() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    SongProductionWorkflowInput input = input(workId, jobId);
    SongProductionWorkflow songProductionWorkflow = mock(SongProductionWorkflow.class);
    when(songProductionWorkflow.produce(input))
        .thenReturn(SongProductionWorkflowResult.packageReady(jobId.toString(), "PACKAGE_READY"));

    LocalSongProductionWorkflowStarter starter =
        new LocalSongProductionWorkflowStarter(songProductionWorkflow);

    assertEquals(jobId.toString(), starter.start(input));
    verify(songProductionWorkflow).produce(input);
  }

  @Test
  void workflowOutboxDispatchTargetDefaultsToLocal() {
    WorkflowDispatchProperties properties = new WorkflowDispatchProperties();

    assertEquals(
        WorkflowDispatchProperties.DispatchTarget.LOCAL,
        properties.getOutbox().getDispatchTarget());

    properties.getOutbox().setDispatchTarget(null);

    assertEquals(
        WorkflowDispatchProperties.DispatchTarget.LOCAL,
        properties.getOutbox().getDispatchTarget());
  }

  @Test
  void temporalStarterUsesDeterministicWorkflowIdAndConfiguredTaskQueue() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    SongProductionWorkflowInput input = input(workId, jobId);
    TemporalWorkflowClientProperties properties = new TemporalWorkflowClientProperties();
    properties.setTaskQueue("song-production-test");
    AtomicBoolean invoked = new AtomicBoolean(false);
    TemporalSongProductionWorkflowStarter starter =
        new TemporalSongProductionWorkflowStarter(
            properties,
            (workflowId, taskQueue, actualInput) -> {
              invoked.set(true);
              assertEquals(TemporalSongProductionWorkflowIds.workflowId(input), workflowId);
              assertEquals("song-production-test", taskQueue);
              assertEquals(input, actualInput);
            });

    String workflowId = starter.start(input);

    assertEquals(TemporalSongProductionWorkflowIds.workflowId(input), workflowId);
    assertTrue(invoked.get());
  }

  @Test
  void temporalStarterTreatsAlreadyStartedAsIdempotentSuccess() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    SongProductionWorkflowInput input = input(workId, jobId);
    TemporalWorkflowClientProperties properties = new TemporalWorkflowClientProperties();
    TemporalSongProductionWorkflowStarter starter =
        new TemporalSongProductionWorkflowStarter(
            properties,
            (workflowId, taskQueue, actualInput) -> {
              throw new WorkflowExecutionAlreadyStarted("already started");
            });

    assertEquals(TemporalSongProductionWorkflowIds.workflowId(input), starter.start(input));
  }

  @Test
  void temporalStarterWrapsStartFailureWithSanitizedMessage() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    SongProductionWorkflowInput input = input(workId, jobId);
    TemporalWorkflowClientProperties properties = new TemporalWorkflowClientProperties();
    TemporalSongProductionWorkflowStarter starter =
        new TemporalSongProductionWorkflowStarter(
            properties,
            (workflowId, taskQueue, actualInput) -> {
              throw new IllegalStateException("raw transport details");
            });

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> starter.start(input));

    assertEquals("Temporal workflow start failed", exception.getMessage());
  }

  private WorkflowOutboxDispatcher dispatcher() {
    return new WorkflowOutboxDispatcher(
        outboxRepository, properties, workflowStarter, workRepository, objectMapper);
  }

  private WorkflowDispatchProperties outboxProperties() {
    WorkflowDispatchProperties properties = new WorkflowDispatchProperties();
    properties.setDispatchMode(WorkflowDispatchProperties.DispatchMode.OUTBOX);
    properties.getOutbox().setDispatcherEnabled(true);
    return properties;
  }

  private String inputJson(UUID workId) throws JsonProcessingException {
    return objectMapper.writeValueAsString(input(workId));
  }

  private SongProductionWorkflowInput input(UUID workId) {
    return input(workId, UUID.randomUUID());
  }

  private SongProductionWorkflowInput input(UUID workId, UUID jobId) {
    return new SongProductionWorkflowInput(
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
        jobId.toString());
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

  private static class WorkflowExecutionAlreadyStarted extends RuntimeException {

    private WorkflowExecutionAlreadyStarted(String message) {
      super(message);
    }
  }
}
