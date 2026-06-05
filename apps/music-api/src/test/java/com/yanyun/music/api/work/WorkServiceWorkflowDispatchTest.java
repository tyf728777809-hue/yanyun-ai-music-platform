package com.yanyun.music.api.work;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.api.work.WorkDtos.ConfirmWorkRequest;
import com.yanyun.music.api.work.WorkDtos.JobAcceptedResponse;
import com.yanyun.music.api.workflow.WorkflowDispatchProperties;
import com.yanyun.music.api.workflow.WorkflowOutboxService;
import com.yanyun.music.lyrics.LyricsGenerationService;
import com.yanyun.music.moderation.ModerationAdapter;
import com.yanyun.music.publish.PublishAdapter;
import com.yanyun.music.quota.QuotaAdapter;
import com.yanyun.music.workdomain.CreationMode;
import com.yanyun.music.workdomain.GenerationStage;
import com.yanyun.music.workdomain.PackageStatus;
import com.yanyun.music.workdomain.WorkStatus;
import com.yanyun.music.workflow.SongProductionWorkflow;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.SongProductionWorkflowResult;
import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.LyricsDraftRow;
import com.yanyun.music.workpersistence.WorkRepository.WorkRow;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WorkServiceWorkflowDispatchTest {

  private final WorkRepository workRepository = mock(WorkRepository.class);
  private final QuotaAdapter quotaAdapter = mock(QuotaAdapter.class);
  private final ModerationAdapter moderationAdapter = mock(ModerationAdapter.class);
  private final PublishAdapter publishAdapter = mock(PublishAdapter.class);
  private final LyricsGenerationService lyricsGenerationService =
      mock(LyricsGenerationService.class);
  private final SongProductionWorkflow songProductionWorkflow = mock(SongProductionWorkflow.class);
  private final WorkflowOutboxService workflowOutboxService = mock(WorkflowOutboxService.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void confirmWorkKeepsSynchronousDispatchByDefault() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(
            Optional.of(work(workId, WorkStatus.LYRICS_READY, GenerationStage.WAITING_CONFIRM)))
        .thenReturn(Optional.of(work(workId, WorkStatus.GENERATED, GenerationStage.PACKAGE_READY)));
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft(workId, draftId)));
    when(songProductionWorkflow.produce(any()))
        .thenReturn(SongProductionWorkflowResult.packageReady(jobId.toString(), "PACKAGE_READY"));

    JobAcceptedResponse response =
        service(syncProperties())
            .confirmWork("user-1", workId, new ConfirmWorkRequest(draftId, null, null));

    assertThat(response.status()).isEqualTo(WorkStatus.GENERATED);
    assertThat(response.generationStage()).isEqualTo(GenerationStage.PACKAGE_READY);
    assertThat(response.jobId()).isEqualTo(jobId);
    verify(songProductionWorkflow).produce(any());
    verify(workflowOutboxService, never()).enqueueSongProduction(any(), any());
    verify(workRepository, never()).reserveSongProduction(any(), any(), anyInt());
  }

  @Test
  void confirmWorkEnqueuesOutboxWhenDispatchModeIsOutbox() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    WorkRow initial = work(workId, WorkStatus.LYRICS_READY, GenerationStage.WAITING_CONFIRM);
    WorkRow queued = work(workId, WorkStatus.GENERATING, GenerationStage.QUOTA_LOCKING);
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(initial))
        .thenReturn(Optional.of(queued));
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft(workId, draftId)));
    when(workRepository.reserveSongProduction(workId, "user-1", initial.version()))
        .thenReturn(true);

    JobAcceptedResponse response =
        service(outboxProperties())
            .confirmWork("user-1", workId, new ConfirmWorkRequest(draftId, null, null));

    assertThat(response.status()).isEqualTo(WorkStatus.GENERATING);
    assertThat(response.generationStage()).isEqualTo(GenerationStage.QUOTA_LOCKING);
    assertThat(response.jobId()).isNotNull();
    verify(workRepository)
        .insertGenerationJob(
            eq(response.jobId()),
            eq(workId),
            eq("SONG_PRODUCTION"),
            eq("RUNNING"),
            eq(GenerationStage.QUOTA_LOCKING),
            any(OffsetDateTime.class),
            isNull());
    ArgumentCaptor<SongProductionWorkflowInput> input =
        ArgumentCaptor.forClass(SongProductionWorkflowInput.class);
    verify(workflowOutboxService).enqueueSongProduction(eq(workId), input.capture());
    assertThat(input.getValue().workId()).isEqualTo(workId.toString());
    assertThat(input.getValue().jobId()).isEqualTo(response.jobId().toString());
    verify(songProductionWorkflow, never()).produce(any());
  }

  private WorkService service(WorkflowDispatchProperties properties) {
    return new WorkService(
        workRepository,
        quotaAdapter,
        moderationAdapter,
        publishAdapter,
        lyricsGenerationService,
        songProductionWorkflow,
        properties,
        workflowOutboxService,
        objectMapper);
  }

  private WorkflowDispatchProperties syncProperties() {
    return new WorkflowDispatchProperties();
  }

  private WorkflowDispatchProperties outboxProperties() {
    WorkflowDispatchProperties properties = new WorkflowDispatchProperties();
    properties.setDispatchMode(WorkflowDispatchProperties.DispatchMode.OUTBOX);
    return properties;
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
        3);
  }

  private LyricsDraftRow draft(UUID workId, UUID draftId) {
    return new LyricsDraftRow(
        draftId,
        workId,
        1,
        "Mock title",
        "Mock summary",
        "Mock lyrics",
        "Mock prompt",
        "[]",
        "[]",
        "mock cover seed",
        null,
        "mock-yanyun-kb-v0",
        "{}",
        OffsetDateTime.now());
  }
}
