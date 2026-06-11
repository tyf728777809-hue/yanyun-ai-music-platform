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
import com.yanyun.music.api.work.WorkDtos.JobAcceptedResponse;
import com.yanyun.music.api.workflow.WorkflowDispatchProperties;
import com.yanyun.music.api.workflow.WorkflowOutboxService;
import com.yanyun.music.dreammaker.DreamMakerProperties;
import com.yanyun.music.lyrics.LyricsGenerationService;
import com.yanyun.music.moderation.ModerationAdapter;
import com.yanyun.music.musicprovider.MusicProviderSelection;
import com.yanyun.music.quota.QuotaAdapter;
import com.yanyun.music.storage.ObjectStorageClient;
import com.yanyun.music.storage.ObjectStorageDownloadUrl;
import com.yanyun.music.storage.ObjectStoragePutRequest;
import com.yanyun.music.suno.YunwuProperties;
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

  @Test
  void confirmWorkRejectsNonMockProviderInOutboxLocalDispatch() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(
            Optional.of(work(workId, WorkStatus.LYRICS_READY, GenerationStage.WAITING_CONFIRM)));
    when(workRepository.findLatestLyricsDraft(workId))
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
    when(workRepository.findLatestLyricsDraft(workId))
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
    when(workRepository.findLatestLyricsDraft(workId))
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
  void confirmWorkRejectsRealYunwuSunoProviderInSyncDispatch() {
    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    when(workRepository.findWorkForUser(workId, "user-1"))
        .thenReturn(
            Optional.of(work(workId, WorkStatus.LYRICS_READY, GenerationStage.WAITING_CONFIRM)));
    when(workRepository.findLatestLyricsDraft(workId))
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
    when(workRepository.findLatestLyricsDraft(workId))
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
            refreshedPackageJson,
            "http://localhost/yanyun-works-local/" + packageRow.packageObjectKey() + "?v=refreshed",
            OffsetDateTime.parse("2026-06-07T00:00:00Z"));
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
