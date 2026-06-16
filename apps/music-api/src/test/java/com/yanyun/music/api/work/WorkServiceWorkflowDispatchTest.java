package com.yanyun.music.api.work;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.yanyun.music.api.work.WorkDtos.CreateWorkResponse;
import com.yanyun.music.api.work.WorkDtos.InspirationCreateRequest;
import com.yanyun.music.api.work.WorkDtos.JobAcceptedResponse;
import com.yanyun.music.api.work.WorkDtos.LyricsContinueRequest;
import com.yanyun.music.api.work.WorkDtos.LyricsPolishRequest;
import com.yanyun.music.api.workflow.WorkflowDispatchProperties;
import com.yanyun.music.api.workflow.WorkflowOutboxService;
import com.yanyun.music.dreammaker.DreamMakerProperties;
import com.yanyun.music.lyrics.LyricsGenerationService;
import com.yanyun.music.moderation.ModerationAdapter;
import com.yanyun.music.moderation.ModerationDecision;
import com.yanyun.music.musicprovider.MusicProviderSelection;
import com.yanyun.music.quota.QuotaAdapter;
import com.yanyun.music.quota.QuotaDecision;
import com.yanyun.music.storage.ObjectStorageClient;
import com.yanyun.music.storage.ObjectStorageDownloadUrl;
import com.yanyun.music.storage.ObjectStoragePutRequest;
import com.yanyun.music.suno.YunwuProperties;
import com.yanyun.music.workdomain.AvailableAction;
import com.yanyun.music.workdomain.CreationMode;
import com.yanyun.music.workdomain.FailureCode;
import com.yanyun.music.workdomain.GenerationStage;
import com.yanyun.music.workdomain.PackageStatus;
import com.yanyun.music.workdomain.WorkStatus;
import com.yanyun.music.workflow.SongProductionWorkflow;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.SongProductionWorkflowResult;
import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.LyricsDraftRow;
import com.yanyun.music.workpersistence.WorkRepository.LyricsEditJobRow;
import com.yanyun.music.workpersistence.WorkRepository.MediaAssetRow;
import com.yanyun.music.workpersistence.WorkRepository.PublishPackageRow;
import com.yanyun.music.workpersistence.WorkRepository.WorkRow;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class WorkServiceWorkflowDispatchTest {

  private final WorkRepository workRepository = mock(WorkRepository.class);
  private final QuotaAdapter quotaAdapter = mock(QuotaAdapter.class);
  private final ModerationAdapter moderationAdapter = mock(ModerationAdapter.class);
  private final ObjectStorageClient objectStorageClient = mock(ObjectStorageClient.class);
  private final LyricsGenerationService lyricsGenerationService =
      mock(LyricsGenerationService.class);
  private final SongProductionWorkflow songProductionWorkflow = mock(SongProductionWorkflow.class);
  private final WorkflowOutboxService workflowOutboxService = mock(WorkflowOutboxService.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void createFromInspirationEnqueuesLyricsGenerationWithoutCallingDeepSeekSynchronously() {
    when(moderationAdapter.preCheckUserInput("user-1", "清河小路上的少年"))
        .thenReturn(new ModerationDecision(true, null, null));
    when(quotaAdapter.getHint("user-1", 0))
        .thenReturn(new QuotaDecision(false, "PACKAGE_READY", 999, 2, "ok"));
    when(workRepository.findWorkForUser(any(UUID.class), eq("user-1")))
        .thenAnswer(
            invocation -> {
              UUID workId = invocation.getArgument(0);
              return Optional.of(
                  new WorkRow(
                      workId,
                      "YYM-20260615-ABCDEF",
                      "user-1",
                      CreationMode.INSPIRATION,
                      WorkStatus.LYRICS_GENERATING,
                      GenerationStage.LYRICS_GENERATING,
                      PackageStatus.PACKAGE_NOT_READY,
                      "正在写词",
                      "AI 正在根据你的灵感创作歌词。",
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
                      null,
                      0));
            });

    CreateWorkResponse response =
        service(syncProperties())
            .createFromInspiration(
                "user-1", new InspirationCreateRequest("清河小路上的少年", "轻快", "R&B", "男声主唱"));

    assertThat(response.status()).isEqualTo(WorkStatus.LYRICS_GENERATING);
    assertThat(response.generationStage()).isEqualTo(GenerationStage.LYRICS_GENERATING);
    assertThat(response.jobId()).isNotNull();
    ArgumentCaptor<LyricsEditJobRow> job = ArgumentCaptor.forClass(LyricsEditJobRow.class);
    verify(workRepository).insertLyricsEditJob(job.capture());
    assertThat(job.getValue().operation()).isEqualTo("CREATE_INSPIRATION");
    assertThat(job.getValue().sourceLyricsDraftId()).isNull();
    assertThat(job.getValue().requestPayloadJson()).contains("清河小路上的少年");
    verify(lyricsGenerationService, never()).generate(any());
  }

  @Test
  void polishLyricsEnqueuesAsyncJobWithoutCallingDeepSeekSynchronously() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    WorkRow work = work(workId, WorkStatus.LYRICS_READY, GenerationStage.WAITING_CONFIRM);
    LyricsEditJobRow activeJob = lyricsEditJob(UUID.randomUUID(), workId, "POLISH", "QUEUED");
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(work))
        .thenReturn(Optional.of(work));
    when(workRepository.findActiveLyricsEditJob(workId))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(activeJob));
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft(workId, draftId)));

    JobAcceptedResponse response =
        service(syncProperties()).polishLyrics("user-1", workId, new LyricsPolishRequest("更押韵一点"));

    assertThat(response.status()).isEqualTo(WorkStatus.LYRICS_READY);
    assertThat(response.generationStage()).isEqualTo(GenerationStage.WAITING_CONFIRM);
    assertThat(response.jobId()).isNotNull();
    assertThat(response.availableActions())
        .doesNotContain(
            com.yanyun.music.workdomain.AvailableAction.POLISH_LYRICS,
            com.yanyun.music.workdomain.AvailableAction.CONTINUE_LYRICS,
            com.yanyun.music.workdomain.AvailableAction.CONFIRM_WORK);
    ArgumentCaptor<LyricsEditJobRow> job = ArgumentCaptor.forClass(LyricsEditJobRow.class);
    verify(workRepository).insertLyricsEditJob(job.capture());
    assertThat(job.getValue().operation()).isEqualTo("POLISH");
    assertThat(job.getValue().instruction()).isEqualTo("更押韵一点");
    assertThat(job.getValue().status()).isEqualTo("QUEUED");
    assertThat(job.getValue().maxAttempts()).isEqualTo(1);
    assertThat(job.getValue().sourceLyricsDraftId()).isEqualTo(draftId);
    verify(lyricsGenerationService, never()).generate(any());
  }

  @Test
  void continueLyricsUsesDefaultInstructionAndEnqueuesAsyncJob() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    WorkRow work = work(workId, WorkStatus.LYRICS_READY, GenerationStage.WAITING_CONFIRM);
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(work))
        .thenReturn(Optional.of(work));
    when(workRepository.findActiveLyricsEditJob(workId)).thenReturn(Optional.empty());
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft(workId, draftId)));

    JobAcceptedResponse response =
        service(syncProperties()).continueLyrics("user-1", workId, new LyricsContinueRequest(""));

    assertThat(response.jobId()).isNotNull();
    ArgumentCaptor<LyricsEditJobRow> job = ArgumentCaptor.forClass(LyricsEditJobRow.class);
    verify(workRepository).insertLyricsEditJob(job.capture());
    assertThat(job.getValue().operation()).isEqualTo("CONTINUE");
    assertThat(job.getValue().instruction()).isEqualTo("Continue current lyrics.");
    assertThat(job.getValue().maxAttempts()).isEqualTo(1);
    verify(lyricsGenerationService, never()).generate(any());
  }

  @Test
  void confirmWorkRejectsWhileLyricsEditJobIsActive() {
    UUID workId = UUID.randomUUID();
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(
            Optional.of(work(workId, WorkStatus.LYRICS_READY, GenerationStage.WAITING_CONFIRM)));
    when(workRepository.findActiveLyricsEditJob(workId))
        .thenReturn(Optional.of(lyricsEditJob(UUID.randomUUID(), workId, "POLISH", "RUNNING")));

    assertThatThrownBy(() -> service(syncProperties()).confirmWork("user-1", workId, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("AI lyrics edit is still running");

    verify(workRepository, never()).findCurrentLyricsDraft(any(), any());
    verify(workRepository, never()).reserveSongProduction(any(), any(), anyInt());
  }

  @Test
  void confirmWorkKeepsSynchronousDispatchByDefault() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(
            Optional.of(work(workId, WorkStatus.LYRICS_READY, GenerationStage.WAITING_CONFIRM)))
        .thenReturn(Optional.of(work(workId, WorkStatus.GENERATED, GenerationStage.PACKAGE_READY)));
    when(workRepository.findCurrentLyricsDraft(workId, draftId))
        .thenReturn(Optional.of(draft(workId, draftId)));
    when(workRepository.reserveSongProduction(workId, "user-1", 3)).thenReturn(true);
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
    verify(workRepository).reserveSongProduction(workId, "user-1", 3);
    verify(workRepository)
        .insertGenerationJob(
            any(UUID.class),
            eq(workId),
            eq("SONG_PRODUCTION"),
            eq("RUNNING"),
            eq(GenerationStage.QUOTA_LOCKING),
            any(OffsetDateTime.class),
            isNull());
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
    when(workRepository.findCurrentLyricsDraft(workId, draftId))
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

  @Test
  void confirmWorkRejectsNonMockProviderInOutboxLocalDispatch() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(
            Optional.of(work(workId, WorkStatus.LYRICS_READY, GenerationStage.WAITING_CONFIRM)));
    when(workRepository.findCurrentLyricsDraft(workId, draftId))
        .thenReturn(Optional.of(draft(workId, draftId)));

    assertThatThrownBy(
            () ->
                service(outboxProperties())
                    .confirmWork("user-1", workId, new ConfirmWorkRequest(draftId, null, "suno")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("outbox + Temporal");

    verify(workRepository, never()).reserveSongProduction(any(), any(), anyInt());
    verify(workflowOutboxService, never()).enqueueSongProduction(any(), any());
    verify(songProductionWorkflow, never()).produce(any());
  }

  @Test
  void confirmWorkRejectsRealDreamMakerProviderInSyncDispatch() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(
            Optional.of(work(workId, WorkStatus.LYRICS_READY, GenerationStage.WAITING_CONFIRM)));
    when(workRepository.findCurrentLyricsDraft(workId, draftId))
        .thenReturn(Optional.of(draft(workId, draftId)));

    assertThatThrownBy(
            () ->
                service(
                        syncProperties(),
                        MusicProviderSelection.fromConfig("mock"),
                        realDreamMakerProperties())
                    .confirmWork("user-1", workId, new ConfirmWorkRequest(draftId, null, "suno")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("outbox + Temporal worker");

    verify(songProductionWorkflow, never()).produce(any());
    verify(workflowOutboxService, never()).enqueueSongProduction(any(), any());
    verify(workRepository, never()).reserveSongProduction(any(), any(), anyInt());
  }

  @Test
  void confirmWorkRejectsConfiguredRealDreamMakerProviderInSyncDispatch() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(
            Optional.of(work(workId, WorkStatus.LYRICS_READY, GenerationStage.WAITING_CONFIRM)));
    when(workRepository.findCurrentLyricsDraft(workId, draftId))
        .thenReturn(Optional.of(draft(workId, draftId)));

    assertThatThrownBy(
            () ->
                service(
                        syncProperties(),
                        MusicProviderSelection.fromConfig("minimax"),
                        realDreamMakerProperties())
                    .confirmWork("user-1", workId, new ConfirmWorkRequest(draftId, null, null)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("outbox + Temporal worker");

    verify(songProductionWorkflow, never()).produce(any());
    verify(workflowOutboxService, never()).enqueueSongProduction(any(), any());
    verify(workRepository, never()).reserveSongProduction(any(), any(), anyInt());
  }

  @Test
  void retryMusicReusesRecoverableProviderTaskBeforeSubmittingNewCharge() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    WorkRow failed = musicRetryableFailedWork(workId);
    WorkRow queued = work(workId, WorkStatus.GENERATING, GenerationStage.QUOTA_LOCKING);
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(failed))
        .thenReturn(Optional.of(queued));
    when(workRepository.findMediaAssets(workId)).thenReturn(List.of(coverAsset(workId)));
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft(workId, draftId)));
    when(workRepository.findLatestProviderTraceId(workId, "SUNO", "MUSIC_GENERATION"))
        .thenReturn(Optional.of("task-recover-1"));
    when(workRepository.reserveMusicRetry(workId, "user-1", failed.version(), 2)).thenReturn(true);

    JobAcceptedResponse response =
        service(
                temporalOutboxProperties(),
                MusicProviderSelection.fromConfig("suno"),
                new DreamMakerProperties(),
                new YunwuProperties(),
                "yunwu")
            .retryMusic("user-1", workId, null);

    assertThat(response.status()).isEqualTo(WorkStatus.GENERATING);
    assertThat(response.generationStage()).isEqualTo(GenerationStage.QUOTA_LOCKING);
    ArgumentCaptor<SongProductionWorkflowInput> input =
        ArgumentCaptor.forClass(SongProductionWorkflowInput.class);
    verify(workflowOutboxService).enqueueSongProduction(eq(workId), input.capture());
    assertThat(input.getValue().audioImportProviderTaskId()).isEqualTo("task-recover-1");
    assertThat(input.getValue().reuseExistingAudio()).isFalse();
    assertThat(input.getValue().reuseExistingCover()).isTrue();
    verify(songProductionWorkflow, never()).produce(any());
  }

  @Test
  void confirmWorkRejectsRealYunwuSunoProviderInSyncDispatch() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(
            Optional.of(work(workId, WorkStatus.LYRICS_READY, GenerationStage.WAITING_CONFIRM)));
    when(workRepository.findCurrentLyricsDraft(workId, draftId))
        .thenReturn(Optional.of(draft(workId, draftId)));

    assertThatThrownBy(
            () ->
                service(
                        syncProperties(),
                        MusicProviderSelection.fromConfig("mock"),
                        new DreamMakerProperties(),
                        realYunwuProperties(),
                        "yunwu")
                    .confirmWork("user-1", workId, new ConfirmWorkRequest(draftId, null, "suno")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("outbox + Temporal worker");

    verify(songProductionWorkflow, never()).produce(any());
    verify(workflowOutboxService, never()).enqueueSongProduction(any(), any());
    verify(workRepository, never()).reserveSongProduction(any(), any(), anyInt());
  }

  @Test
  void confirmWorkAllowsRealDreamMakerProviderOnlyWithTemporalOutbox() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    WorkRow initial = work(workId, WorkStatus.LYRICS_READY, GenerationStage.WAITING_CONFIRM);
    WorkRow queued = work(workId, WorkStatus.GENERATING, GenerationStage.QUOTA_LOCKING);
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(initial))
        .thenReturn(Optional.of(queued));
    when(workRepository.findCurrentLyricsDraft(workId, draftId))
        .thenReturn(Optional.of(draft(workId, draftId)));
    when(workRepository.reserveSongProduction(workId, "user-1", initial.version()))
        .thenReturn(true);

    JobAcceptedResponse response =
        service(
                temporalOutboxProperties(),
                MusicProviderSelection.fromConfig("mock"),
                realDreamMakerProperties())
            .confirmWork("user-1", workId, new ConfirmWorkRequest(draftId, null, "suno"));

    assertThat(response.status()).isEqualTo(WorkStatus.GENERATING);
    ArgumentCaptor<SongProductionWorkflowInput> input =
        ArgumentCaptor.forClass(SongProductionWorkflowInput.class);
    verify(workflowOutboxService).enqueueSongProduction(eq(workId), input.capture());
    assertThat(input.getValue().musicProvider()).isEqualTo("suno");
    verify(songProductionWorkflow, never()).produce(any());
  }

  @Test
  void retryMusicRejectsRealDreamMakerProviderInSyncDispatch() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(failedMusicWork(workId)));
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft(workId, draftId)));

    assertThatThrownBy(
            () ->
                service(
                        syncProperties(),
                        MusicProviderSelection.fromConfig("mock"),
                        realDreamMakerProperties())
                    .retryMusic("user-1", workId, new WorkDtos.RetryMusicRequest("minimax")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("outbox + Temporal worker");

    verify(workRepository, never()).reserveMusicRetry(any(), any(), anyInt(), anyInt());
    verify(songProductionWorkflow, never()).produce(any());
    verify(workflowOutboxService, never()).enqueueSongProduction(any(), any());
  }

  @Test
  void rebuildPublishPackageEnqueuesRecoveryWithExistingMedia() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    WorkRow failed = packageBuildFailedWork(workId);
    WorkRow queued = work(workId, WorkStatus.GENERATING, GenerationStage.QUOTA_LOCKING);
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(failed))
        .thenReturn(Optional.of(queued));
    when(workRepository.findMediaAssets(workId))
        .thenReturn(List.of(audioAsset(workId), coverAsset(workId), videoAsset(workId)));
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft(workId, draftId)));
    when(workRepository.reservePublishPackageRebuild(workId, "user-1", failed.version()))
        .thenReturn(true);

    JobAcceptedResponse response =
        service(temporalOutboxProperties()).rebuildPublishPackage("user-1", workId);

    assertThat(response.status()).isEqualTo(WorkStatus.GENERATING);
    assertThat(response.generationStage()).isEqualTo(GenerationStage.QUOTA_LOCKING);
    assertThat(response.jobId()).isNotNull();
    verify(workRepository).reservePublishPackageRebuild(workId, "user-1", failed.version());
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
    assertThat(input.getValue().reuseExistingAudio()).isTrue();
    assertThat(input.getValue().reuseExistingCover()).isTrue();
    assertThat(input.getValue().reuseExistingVideo()).isTrue();
    assertThat(input.getValue().workId()).isEqualTo(workId.toString());
    assertThat(input.getValue().jobId()).isEqualTo(response.jobId().toString());
    verify(songProductionWorkflow, never()).produce(any());
  }

  @Test
  void regenerateCoverEnqueuesPackageRetryAfterCoverGenerationFailedWithExistingAudio() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    WorkRow failed = coverGenerationFailedWork(workId);
    WorkRow queued = work(workId, WorkStatus.GENERATING, GenerationStage.QUOTA_LOCKING);
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(failed))
        .thenReturn(Optional.of(queued));
    when(workRepository.findMediaAssets(workId)).thenReturn(List.of(audioAsset(workId)));
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft(workId, draftId)));
    when(workRepository.reservePackageBuildRetry(workId, "user-1", failed.version()))
        .thenReturn(true);

    JobAcceptedResponse response =
        service(temporalOutboxProperties()).regenerateCover("user-1", workId);

    assertThat(response.status()).isEqualTo(WorkStatus.GENERATING);
    assertThat(response.generationStage()).isEqualTo(GenerationStage.QUOTA_LOCKING);
    ArgumentCaptor<SongProductionWorkflowInput> input =
        ArgumentCaptor.forClass(SongProductionWorkflowInput.class);
    verify(workflowOutboxService).enqueueSongProduction(eq(workId), input.capture());
    assertThat(input.getValue().reuseExistingAudio()).isTrue();
    assertThat(input.getValue().reuseExistingCover()).isFalse();
    assertThat(input.getValue().reuseExistingVideo()).isFalse();
    assertThat(input.getValue().jobId()).isEqualTo(response.jobId().toString());
    verify(songProductionWorkflow, never()).produce(any());
  }

  @Test
  void retryAudioImportEnqueuesRecoveryWithProviderTaskIdAndExistingCover() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    WorkRow failed = audioImportFailedWork(workId);
    WorkRow queued = work(workId, WorkStatus.GENERATING, GenerationStage.QUOTA_LOCKING);
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(failed))
        .thenReturn(Optional.of(queued));
    when(workRepository.findMediaAssets(workId)).thenReturn(List.of(coverAsset(workId)));
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft(workId, draftId)));
    when(workRepository.findLatestProviderTraceId(workId, "SUNO", "MUSIC_GENERATION"))
        .thenReturn(Optional.of("task-123"));
    when(workRepository.reserveAudioImportRetry(workId, "user-1", failed.version()))
        .thenReturn(true);

    JobAcceptedResponse response =
        service(
                temporalOutboxProperties(),
                MusicProviderSelection.fromConfig("suno"),
                new DreamMakerProperties(),
                new YunwuProperties(),
                "yunwu")
            .retryAudioImport("user-1", workId);

    assertThat(response.status()).isEqualTo(WorkStatus.GENERATING);
    assertThat(response.generationStage()).isEqualTo(GenerationStage.QUOTA_LOCKING);
    ArgumentCaptor<SongProductionWorkflowInput> input =
        ArgumentCaptor.forClass(SongProductionWorkflowInput.class);
    verify(workflowOutboxService).enqueueSongProduction(eq(workId), input.capture());
    assertThat(input.getValue().reuseExistingAudio()).isFalse();
    assertThat(input.getValue().reuseExistingCover()).isTrue();
    assertThat(input.getValue().reuseExistingVideo()).isFalse();
    assertThat(input.getValue().audioImportProviderTaskId()).isEqualTo("task-123");
    verify(workRepository).reserveAudioImportRetry(workId, "user-1", failed.version());
    verify(songProductionWorkflow, never()).produce(any());
  }

  @Test
  void retryAudioImportAcceptsLegacyPackageBuildFailureWhenAudioIsMissing() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    WorkRow failed = packageBuildFailedWork(workId);
    WorkRow queued = work(workId, WorkStatus.GENERATING, GenerationStage.QUOTA_LOCKING);
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(failed))
        .thenReturn(Optional.of(queued));
    when(workRepository.findMediaAssets(workId)).thenReturn(List.of(coverAsset(workId)));
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft(workId, draftId)));
    when(workRepository.findLatestProviderTraceId(workId, "SUNO", "MUSIC_GENERATION"))
        .thenReturn(Optional.of("task-legacy"));
    when(workRepository.reserveAudioImportRetry(workId, "user-1", failed.version()))
        .thenReturn(true);

    JobAcceptedResponse response =
        service(
                temporalOutboxProperties(),
                MusicProviderSelection.fromConfig("suno"),
                new DreamMakerProperties(),
                new YunwuProperties(),
                "yunwu")
            .retryAudioImport("user-1", workId);

    assertThat(response.status()).isEqualTo(WorkStatus.GENERATING);
    ArgumentCaptor<SongProductionWorkflowInput> input =
        ArgumentCaptor.forClass(SongProductionWorkflowInput.class);
    verify(workflowOutboxService).enqueueSongProduction(eq(workId), input.capture());
    assertThat(input.getValue().audioImportProviderTaskId()).isEqualTo("task-legacy");
    assertThat(input.getValue().reuseExistingCover()).isTrue();
    verify(workRepository).reserveAudioImportRetry(workId, "user-1", failed.version());
  }

  @Test
  void getWorkExposesAudioImportRetryForLegacyPackageBuildFailureWithMissingAudio() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    WorkRow failed = packageBuildFailedWork(workId);
    when(workRepository.findWorkForUser(workId, "user-1")).thenReturn(Optional.of(failed));
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft(workId, draftId)));
    when(workRepository.findActiveLyricsEditJob(workId)).thenReturn(Optional.empty());
    when(workRepository.findLatestLyricsEditFailure(workId)).thenReturn(Optional.empty());
    when(workRepository.findMediaAssets(workId)).thenReturn(List.of(coverAsset(workId)));
    when(workRepository.findLatestProviderTraceId(workId, "SUNO", "MUSIC_GENERATION"))
        .thenReturn(Optional.of("task-legacy"));
    when(quotaAdapter.getHint("user-1", 0))
        .thenReturn(new QuotaDecision(false, "PACKAGE_READY", 999, 2, "ok"));
    when(objectStorageClient.createDownloadUrl(any()))
        .thenReturn(
            new ObjectStorageDownloadUrl(
                "covers/" + workId + ".jpeg",
                "http://localhost/covers/" + workId + ".jpeg",
                OffsetDateTime.now().plusHours(1)));

    var response =
        service(
                syncProperties(),
                MusicProviderSelection.fromConfig("suno"),
                new DreamMakerProperties(),
                new YunwuProperties(),
                "yunwu")
            .getWork("user-1", workId);

    assertThat(response.availableActions())
        .containsExactly(AvailableAction.RETRY_AUDIO_IMPORT, AvailableAction.RETURN_TO_EDIT);
  }

  @Test
  void rerenderVideoEnqueuesRecoveryWithExistingAudioAndCover() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    WorkRow failed = videoRenderFailedWork(workId);
    WorkRow queued = work(workId, WorkStatus.GENERATING, GenerationStage.QUOTA_LOCKING);
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(failed))
        .thenReturn(Optional.of(queued));
    when(workRepository.findMediaAssets(workId))
        .thenReturn(List.of(audioAsset(workId), coverAsset(workId)));
    when(workRepository.findLatestLyricsDraft(workId))
        .thenReturn(Optional.of(draft(workId, draftId)));
    when(workRepository.reserveVideoRerender(workId, "user-1", failed.version())).thenReturn(true);

    JobAcceptedResponse response =
        service(temporalOutboxProperties()).rerenderVideo("user-1", workId);

    assertThat(response.status()).isEqualTo(WorkStatus.GENERATING);
    assertThat(response.generationStage()).isEqualTo(GenerationStage.QUOTA_LOCKING);
    verify(workRepository).reserveVideoRerender(workId, "user-1", failed.version());
    ArgumentCaptor<SongProductionWorkflowInput> input =
        ArgumentCaptor.forClass(SongProductionWorkflowInput.class);
    verify(workflowOutboxService).enqueueSongProduction(eq(workId), input.capture());
    assertThat(input.getValue().reuseExistingAudio()).isTrue();
    assertThat(input.getValue().reuseExistingCover()).isTrue();
    assertThat(input.getValue().reuseExistingVideo()).isFalse();
    assertThat(input.getValue().jobId()).isEqualTo(response.jobId().toString());
    verify(songProductionWorkflow, never()).produce(any());
  }

  @Test
  void refreshPublishPackageUrlUsesPersistedPackageObjectKey() {
    UUID workId = UUID.randomUUID();
    WorkRow generated = work(workId, WorkStatus.GENERATED, GenerationStage.PACKAGE_READY);
    MediaAssetRow audio =
        new MediaAssetRow(
            workId,
            "AUDIO",
            "audio/new.mp3",
            "audio/mpeg",
            1000L,
            "audio-checksum",
            null,
            null,
            1000,
            "{}");
    MediaAssetRow cover =
        new MediaAssetRow(
            workId,
            "COVER",
            "cover/new.png",
            "image/png",
            2000L,
            "cover-checksum",
            1920,
            1080,
            null,
            "{}");
    MediaAssetRow video =
        new MediaAssetRow(
            workId,
            "VIDEO",
            "video/new.mp4",
            "video/mp4",
            3000L,
            "video-checksum",
            1920,
            1080,
            1000,
            "{}");
    MediaAssetRow timeline =
        new MediaAssetRow(
            workId,
            "TIMELINE",
            "timeline/new.json",
            "application/json",
            400L,
            "timeline-checksum",
            null,
            null,
            null,
            "{}");
    PublishPackageRow packageRow =
        new PublishPackageRow(
            UUID.randomUUID(),
            workId,
            PackageStatus.PACKAGE_READY,
            """
            {
              "audio": {"url": "http://old/audio"},
              "cover": {"url": "http://old/cover"},
              "video": {"url": "http://old/video"},
              "lyrics": {"timeline_url": "http://old/timeline"}
            }
            """,
            "yanyun-ai-music/local/2026/06/06/" + workId + "/package/publish-package.json",
            "http://old-url",
            OffsetDateTime.parse("2026-06-06T01:00:00Z"),
            null,
            OffsetDateTime.parse("2026-06-06T00:00:00Z"),
            OffsetDateTime.parse("2026-06-06T00:00:00Z"));
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(generated))
        .thenReturn(Optional.of(generated));
    when(workRepository.findPublishPackage(workId)).thenReturn(Optional.of(packageRow));
    when(workRepository.findMediaAssets(workId)).thenReturn(List.of(audio, cover, video, timeline));
    when(objectStorageClient.createDownloadUrl(any()))
        .thenAnswer(
            invocation -> {
              String objectKey = invocation.getArgument(0);
              return new ObjectStorageDownloadUrl(
                  objectKey,
                  "http://localhost/yanyun-works-local/" + objectKey + "?v=refreshed",
                  OffsetDateTime.parse("2026-06-07T00:00:00Z"));
            });

    service(syncProperties()).refreshPublishPackageUrl("user-1", workId);

    verify(objectStorageClient).createDownloadUrl(packageRow.packageObjectKey());
    ArgumentCaptor<ObjectStoragePutRequest> putRequest =
        ArgumentCaptor.forClass(ObjectStoragePutRequest.class);
    verify(objectStorageClient).putObject(putRequest.capture());
    String refreshedPackageJson =
        new String(putRequest.getValue().content(), StandardCharsets.UTF_8);
    assertThat(refreshedPackageJson).contains("audio/new.mp3?v=refreshed");
    assertThat(refreshedPackageJson).contains("cover/new.png?v=refreshed");
    assertThat(refreshedPackageJson).contains("video/new.mp4?v=refreshed");
    assertThat(refreshedPackageJson).contains("timeline/new.json?v=refreshed");
    verify(workRepository)
        .updatePublishPackageUrl(
            workId,
            PackageStatus.PACKAGE_READY,
            refreshedPackageJson,
            "http://localhost/yanyun-works-local/" + packageRow.packageObjectKey() + "?v=refreshed",
            OffsetDateTime.parse("2026-06-07T00:00:00Z"));
  }

  @Test
  void refreshPublishPackageUrlPreservesFetchedPackageStatus() {
    UUID workId = UUID.randomUUID();
    WorkRow generated =
        workWithPackageStatus(
            workId,
            WorkStatus.GENERATED,
            GenerationStage.PACKAGE_READY,
            PackageStatus.PACKAGE_FETCHED);
    PublishPackageRow packageRow =
        new PublishPackageRow(
            UUID.randomUUID(),
            workId,
            PackageStatus.PACKAGE_FETCHED,
            "{\"audio\":{\"url\":\"http://old/audio\"}}",
            "yanyun-ai-music/local/2026/06/06/" + workId + "/package/publish-package.json",
            "http://old-url",
            OffsetDateTime.parse("2026-06-06T01:00:00Z"),
            null,
            OffsetDateTime.parse("2026-06-06T00:00:00Z"),
            OffsetDateTime.parse("2026-06-06T00:00:00Z"));
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(Optional.of(generated))
        .thenReturn(Optional.of(generated));
    when(workRepository.findPublishPackage(workId)).thenReturn(Optional.of(packageRow));
    when(workRepository.findMediaAssets(workId)).thenReturn(List.of());
    when(objectStorageClient.createDownloadUrl(any()))
        .thenReturn(
            new ObjectStorageDownloadUrl(
                packageRow.packageObjectKey(),
                "http://localhost/yanyun-works-local/package.json?v=refreshed",
                OffsetDateTime.parse("2026-06-07T00:00:00Z")));

    service(syncProperties()).refreshPublishPackageUrl("user-1", workId);

    verify(workRepository)
        .updatePublishPackageUrl(
            eq(workId),
            eq(PackageStatus.PACKAGE_FETCHED),
            any(),
            eq("http://localhost/yanyun-works-local/package.json?v=refreshed"),
            eq(OffsetDateTime.parse("2026-06-07T00:00:00Z")));
  }

  @Test
  void markPublishPackageFetchedRejectsExpiredPackageUrl() {
    UUID workId = UUID.randomUUID();
    WorkRow generated = work(workId, WorkStatus.GENERATED, GenerationStage.PACKAGE_READY);
    PublishPackageRow packageRow =
        new PublishPackageRow(
            UUID.randomUUID(),
            workId,
            PackageStatus.PACKAGE_READY,
            "{}",
            "yanyun-ai-music/local/2026/06/06/" + workId + "/package/publish-package.json",
            "http://old-url",
            OffsetDateTime.now().minusMinutes(1),
            null,
            OffsetDateTime.now().minusHours(1),
            OffsetDateTime.now().minusHours(1));
    when(workRepository.findWorkForUser(workId, "user-1")).thenReturn(Optional.of(generated));
    when(workRepository.findPublishPackage(workId)).thenReturn(Optional.of(packageRow));

    assertThatThrownBy(() -> service(syncProperties()).markPublishPackageFetched("user-1", workId))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("链接已过期");

    verify(workRepository, never()).markPackageFetched(workId);
  }

  @Test
  void getPublishPackageReflectsBlockedWorkWithoutPackageRow() {
    UUID workId = UUID.randomUUID();
    WorkRow blocked = packageBlockedWork(workId);
    when(workRepository.findWorkForUser(workId, "user-1")).thenReturn(Optional.of(blocked));
    when(workRepository.findPublishPackage(workId)).thenReturn(Optional.empty());

    var response = service(syncProperties()).getPublishPackage("user-1", workId);

    assertThat(response.packageStatus()).isEqualTo(PackageStatus.PACKAGE_BLOCKED);
    assertThat(response.blockedReason()).isEqualTo("作品暂不能交给社区发布。");
    assertThat(response.packageUrl()).isNull();
    assertThat(response.packageJson()).isNull();
    assertThat(response.availableActions())
        .containsExactlyInAnyOrderElementsOf(
            com.yanyun.music.workdomain.WorkStateMachine.availableActions(
                new com.yanyun.music.workdomain.WorkSnapshot(
                    WorkStatus.FAILED,
                    GenerationStage.FAILED,
                    PackageStatus.PACKAGE_BLOCKED,
                    FailureCode.PACKAGE_BLOCKED,
                    false,
                    0)));
  }

  private WorkService service(WorkflowDispatchProperties properties) {
    return service(
        properties,
        MusicProviderSelection.fromConfig("mock"),
        new DreamMakerProperties(),
        new YunwuProperties(),
        "yunwu");
  }

  private WorkService service(
      WorkflowDispatchProperties properties,
      MusicProviderSelection configuredMusicProvider,
      DreamMakerProperties dreamMakerProperties) {
    return service(
        properties,
        configuredMusicProvider,
        dreamMakerProperties,
        new YunwuProperties(),
        "dreammaker");
  }

  private WorkService service(
      WorkflowDispatchProperties properties,
      MusicProviderSelection configuredMusicProvider,
      DreamMakerProperties dreamMakerProperties,
      YunwuProperties yunwuProperties,
      String sunoBackend) {
    return new WorkService(
        workRepository,
        quotaAdapter,
        moderationAdapter,
        objectStorageClient,
        lyricsGenerationService,
        songProductionWorkflow,
        properties,
        workflowOutboxService,
        configuredMusicProvider,
        dreamMakerProperties,
        yunwuProperties,
        sunoBackend,
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

  private WorkflowDispatchProperties temporalOutboxProperties() {
    WorkflowDispatchProperties properties = outboxProperties();
    properties.getOutbox().setDispatchTarget(WorkflowDispatchProperties.DispatchTarget.TEMPORAL);
    return properties;
  }

  private DreamMakerProperties realDreamMakerProperties() {
    DreamMakerProperties properties = new DreamMakerProperties();
    properties.setRealCallsEnabled(true);
    properties.setAccessKey("configured-access-key");
    properties.setSecretKey("configured-secret-key");
    return properties;
  }

  private YunwuProperties realYunwuProperties() {
    YunwuProperties properties = new YunwuProperties();
    properties.setRealCallsEnabled(true);
    properties.setApiKey("configured-yunwu-key");
    return properties;
  }

  private WorkRow work(UUID workId, WorkStatus status, GenerationStage stage) {
    return workWithPackageStatus(
        workId,
        status,
        stage,
        status == WorkStatus.GENERATED
            ? PackageStatus.PACKAGE_READY
            : PackageStatus.PACKAGE_NOT_READY);
  }

  private WorkRow workWithPackageStatus(
      UUID workId, WorkStatus status, GenerationStage stage, PackageStatus packageStatus) {
    return new WorkRow(
        workId,
        "YYM-20260605-ABCDEF",
        "user-1",
        CreationMode.LYRICS,
        status,
        stage,
        packageStatus,
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

  private WorkRow failedMusicWork(UUID workId) {
    return new WorkRow(
        workId,
        "YYM-20260605-ABCDEF",
        "user-1",
        CreationMode.LYRICS,
        WorkStatus.FAILED,
        GenerationStage.FAILED,
        PackageStatus.PACKAGE_NOT_READY,
        "Mock title",
        "Mock summary",
        0,
        0,
        FailureCode.MUSIC_GENERATION_FAILED,
        "Music provider failed",
        true,
        OffsetDateTime.now(),
        false,
        false,
        0,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        null,
        3);
  }

  private WorkRow packageBlockedWork(UUID workId) {
    return new WorkRow(
        workId,
        "YYM-20260605-ABCDEF",
        "user-1",
        CreationMode.LYRICS,
        WorkStatus.FAILED,
        GenerationStage.FAILED,
        PackageStatus.PACKAGE_BLOCKED,
        "Mock title",
        "Mock summary",
        0,
        0,
        FailureCode.PACKAGE_BLOCKED,
        "作品暂不能交给社区发布。",
        false,
        OffsetDateTime.now(),
        false,
        false,
        0,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        null,
        3);
  }

  private WorkRow packageBuildFailedWork(UUID workId) {
    return new WorkRow(
        workId,
        "YYM-20260605-ABCDEF",
        "user-1",
        CreationMode.LYRICS,
        WorkStatus.FAILED,
        GenerationStage.FAILED,
        PackageStatus.PACKAGE_NOT_READY,
        "Mock title",
        "Mock summary",
        0,
        0,
        FailureCode.PACKAGE_BUILD_FAILED,
        "Cover prompt quality gate failed",
        false,
        OffsetDateTime.now(),
        false,
        false,
        0,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        null,
        3);
  }

  private WorkRow videoRenderFailedWork(UUID workId) {
    return new WorkRow(
        workId,
        "YYM-20260605-ABCDEF",
        "user-1",
        CreationMode.LYRICS,
        WorkStatus.FAILED,
        GenerationStage.FAILED,
        PackageStatus.PACKAGE_NOT_READY,
        "Mock title",
        "Mock summary",
        0,
        0,
        FailureCode.VIDEO_RENDER_FAILED,
        "Video render failed",
        true,
        OffsetDateTime.now(),
        false,
        false,
        0,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        null,
        3);
  }

  private WorkRow coverGenerationFailedWork(UUID workId) {
    return new WorkRow(
        workId,
        "YYM-20260605-ABCDEF",
        "user-1",
        CreationMode.LYRICS,
        WorkStatus.FAILED,
        GenerationStage.FAILED,
        PackageStatus.PACKAGE_NOT_READY,
        "Mock title",
        "Mock summary",
        0,
        0,
        FailureCode.COVER_GENERATION_FAILED,
        "Cover prompt quality gate failed",
        true,
        OffsetDateTime.now(),
        false,
        false,
        0,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        null,
        3);
  }

  private WorkRow audioImportFailedWork(UUID workId) {
    return new WorkRow(
        workId,
        "YYM-20260605-ABCDEF",
        "user-1",
        CreationMode.LYRICS,
        WorkStatus.FAILED,
        GenerationStage.FAILED,
        PackageStatus.PACKAGE_NOT_READY,
        "Mock title",
        "Mock summary",
        0,
        0,
        FailureCode.AUDIO_IMPORT_FAILED,
        "Audio import failed",
        true,
        OffsetDateTime.now(),
        false,
        false,
        0,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        null,
        3);
  }

  private WorkRow musicRetryableFailedWork(UUID workId) {
    return new WorkRow(
        workId,
        "YYM-20260605-ABCDEF",
        "user-1",
        CreationMode.LYRICS,
        WorkStatus.FAILED,
        GenerationStage.FAILED,
        PackageStatus.PACKAGE_NOT_READY,
        "Mock title",
        "Mock summary",
        0,
        0,
        FailureCode.MUSIC_GENERATION_FAILED,
        "Music generation failed",
        true,
        OffsetDateTime.now(),
        false,
        false,
        1,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        null,
        3);
  }

  private MediaAssetRow audioAsset(UUID workId) {
    return new MediaAssetRow(
        workId,
        "AUDIO",
        "audio/" + workId + ".mp3",
        "audio/mpeg",
        4_810_374L,
        "audio-checksum",
        null,
        null,
        213_520,
        "{}");
  }

  private MediaAssetRow coverAsset(UUID workId) {
    return new MediaAssetRow(
        workId,
        "COVER",
        "covers/" + workId + ".png",
        "image/png",
        810_374L,
        "cover-checksum",
        1920,
        1080,
        null,
        "{}");
  }

  private MediaAssetRow videoAsset(UUID workId) {
    return new MediaAssetRow(
        workId,
        "VIDEO",
        "videos/" + workId + ".mp4",
        "video/mp4",
        8_810_374L,
        "video-checksum",
        1920,
        1080,
        213_520,
        "{}");
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

  private LyricsEditJobRow lyricsEditJob(UUID jobId, UUID workId, String operation, String status) {
    return new LyricsEditJobRow(
        jobId,
        workId,
        "user-1",
        operation,
        "更押韵一点",
        UUID.randomUUID(),
        1,
        status,
        null,
        null,
        false,
        0,
        2,
        null,
        null,
        null,
        null,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        "{}");
  }
}
