package com.yanyun.music.production;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.image2.MockCoverGenerationService;
import com.yanyun.music.media.MockVideoRenderService;
import com.yanyun.music.media.VideoRenderService;
import com.yanyun.music.moderation.ModerationAdapter;
import com.yanyun.music.moderation.ModerationDecision;
import com.yanyun.music.musicprovider.MusicGenerationRequest;
import com.yanyun.music.musicprovider.MusicGenerationResult;
import com.yanyun.music.musicprovider.MusicProvider;
import com.yanyun.music.musicprovider.MusicProviderRegistry;
import com.yanyun.music.musicprovider.MusicProviderSelection;
import com.yanyun.music.musicprovider.MusicProviderType;
import com.yanyun.music.publish.PublishAdapter;
import com.yanyun.music.publish.PublishHandoff;
import com.yanyun.music.quota.QuotaAdapter;
import com.yanyun.music.quota.QuotaCommit;
import com.yanyun.music.quota.QuotaLock;
import com.yanyun.music.quota.QuotaRelease;
import com.yanyun.music.storage.ObjectStorageClient;
import com.yanyun.music.storage.ObjectStorageDownloadUrl;
import com.yanyun.music.storage.ObjectStoragePutRequest;
import com.yanyun.music.storage.RemoteObjectImportRequest;
import com.yanyun.music.storage.RemoteObjectImporter;
import com.yanyun.music.storage.StoredObject;
import com.yanyun.music.workdomain.FailureCode;
import com.yanyun.music.workdomain.GenerationStage;
import com.yanyun.music.workdomain.PackageStatus;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.SongProductionWorkflowResult;
import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.MediaAssetRow;
import com.yanyun.music.workpersistence.WorkRepository.ProviderCallRow;
import com.yanyun.music.workpersistence.WorkRepository.PublishPackageRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MockSongProductionWorkflowTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final WorkRepository workRepository = mock(WorkRepository.class);
  private final QuotaAdapter quotaAdapter = mock(QuotaAdapter.class);
  private final ModerationAdapter moderationAdapter = mock(ModerationAdapter.class);
  private final PublishAdapter publishAdapter = mock(PublishAdapter.class);
  private final ObjectStorageClient objectStorageClient = mock(ObjectStorageClient.class);
  private final RemoteObjectImporter remoteObjectImporter = mock(RemoteObjectImporter.class);

  @Test
  void producesPublishReadyPackageAndCompletesJob() throws Exception {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    MusicProvider musicProvider = mockMusicProvider();
    when(workRepository.insertGenerationJob(
            eq(workId),
            eq("SONG_PRODUCTION"),
            eq("RUNNING"),
            eq(GenerationStage.QUOTA_LOCKING),
            any(OffsetDateTime.class),
            isNull()))
        .thenReturn(jobId);
    when(quotaAdapter.lockGenerateQuota("user-1", workId.toString()))
        .thenReturn(new QuotaLock(true, "lock-1", "locked"));
    when(musicProvider.submit(any()))
        .thenReturn(
            MusicGenerationResult.succeeded(
                MusicProviderType.MOCK, "task-1", "audio/" + workId + ".mp3", 123_000, "ok"));
    when(publishAdapter.preparePackage(workId.toString()))
        .thenReturn(
            new PublishHandoff(
                "packages/" + workId + ".json",
                "http://localhost/packages/" + workId + ".json",
                OffsetDateTime.parse("2026-06-05T12:00:00Z")));
    when(moderationAdapter.preCheckPublishPackage("user-1", workId.toString()))
        .thenReturn(ModerationDecision.allow());
    when(objectStorageClient.putObject(any()))
        .thenReturn(
            new StoredObject(
                "packages/" + workId + ".json",
                "http://localhost/packages/" + workId + ".json",
                "application/json",
                512L));
    when(quotaAdapter.commitGenerateQuota("user-1", "lock-1"))
        .thenReturn(new QuotaCommit(true, "committed"));

    SongProductionWorkflowResult result = workflowWith(musicProvider).produce(input(workId));

    assertThat(result.jobId()).isEqualTo(jobId.toString());
    assertThat(result.packageReady()).isTrue();
    assertThat(result.packageStatus()).isEqualTo(PackageStatus.PACKAGE_READY.name());

    ArgumentCaptor<MusicGenerationRequest> musicRequest =
        ArgumentCaptor.forClass(MusicGenerationRequest.class);
    verify(musicProvider).submit(musicRequest.capture());
    assertThat(musicRequest.getValue().workId()).isEqualTo(workId.toString());
    assertThat(musicRequest.getValue().lyricsText()).isEqualTo("Mock lyrics");
    assertThat(musicRequest.getValue().musicPrompt()).isEqualTo("Mock prompt");

    ArgumentCaptor<ProviderCallRow> providerCall = ArgumentCaptor.forClass(ProviderCallRow.class);
    verify(workRepository).insertProviderCall(providerCall.capture());
    assertThat(providerCall.getValue().workId()).isEqualTo(workId);
    assertThat(providerCall.getValue().jobId()).isEqualTo(jobId);
    assertThat(providerCall.getValue().provider()).isEqualTo("MOCK");
    assertThat(providerCall.getValue().operation()).isEqualTo("MUSIC_GENERATION");
    assertThat(providerCall.getValue().modelName()).isEqualTo("mock");
    assertThat(providerCall.getValue().providerTraceId()).isEqualTo("task-1");
    assertThat(providerCall.getValue().status()).isEqualTo("SUCCEEDED");
    assertThat(providerCall.getValue().requestHash()).hasSize(64);
    assertThat(providerCall.getValue().promptHash()).hasSize(64);

    ArgumentCaptor<MediaAssetRow> mediaAsset = ArgumentCaptor.forClass(MediaAssetRow.class);
    verify(workRepository, org.mockito.Mockito.times(4)).upsertMediaAsset(mediaAsset.capture());
    assertThat(mediaAsset.getAllValues())
        .extracting(MediaAssetRow::assetType)
        .containsExactlyInAnyOrder("AUDIO", "COVER", "VIDEO", "TIMELINE");
    MediaAssetRow coverAsset =
        mediaAsset.getAllValues().stream()
            .filter(asset -> "COVER".equals(asset.assetType()))
            .findFirst()
            .orElseThrow();
    MediaAssetRow videoAsset =
        mediaAsset.getAllValues().stream()
            .filter(asset -> "VIDEO".equals(asset.assetType()))
            .findFirst()
            .orElseThrow();
    MediaAssetRow timelineAsset =
        mediaAsset.getAllValues().stream()
            .filter(asset -> "TIMELINE".equals(asset.assetType()))
            .findFirst()
            .orElseThrow();
    assertThat(coverAsset.metadataJson()).contains("mock-image2");
    assertThat(videoAsset.metadataJson()).contains("mock-remotion-ffmpeg");
    assertThat(timelineAsset.metadataJson()).contains("mobile-first");
    assertThat(coverAsset.width()).isEqualTo(1920);
    assertThat(coverAsset.height()).isEqualTo(1080);
    assertThat(videoAsset.width()).isEqualTo(1920);
    assertThat(videoAsset.height()).isEqualTo(1080);

    ArgumentCaptor<PublishPackageRow> publishPackage =
        ArgumentCaptor.forClass(PublishPackageRow.class);
    verify(workRepository).upsertPublishPackage(publishPackage.capture());
    assertThat(publishPackage.getValue().packageStatus()).isEqualTo(PackageStatus.PACKAGE_READY);
    assertThat(publishPackage.getValue().packageObjectKey())
        .isEqualTo("packages/" + workId + ".json");
    JsonNode packageJson = objectMapper.readTree(publishPackage.getValue().packageJson());
    assertThat(packageJson.path("work_id").asText()).isEqualTo(workId.toString());
    assertThat(packageJson.path("video").path("url").asText())
        .endsWith("videos/" + workId + ".mp4");
    assertThat(packageJson.path("metadata").path("song_title").asText()).isEqualTo("Mock title");
    assertThat(publishPackage.getValue().packageUrl())
        .isEqualTo("http://localhost/packages/" + workId + ".json");

    ArgumentCaptor<ObjectStoragePutRequest> storagePut =
        ArgumentCaptor.forClass(ObjectStoragePutRequest.class);
    verify(objectStorageClient).putObject(storagePut.capture());
    assertThat(storagePut.getValue().objectKey()).isEqualTo("packages/" + workId + ".json");
    assertThat(storagePut.getValue().contentType()).isEqualTo("application/json");
    assertThat(new String(storagePut.getValue().content(), java.nio.charset.StandardCharsets.UTF_8))
        .contains("\"work_id\":\"" + workId + "\"");
    verifyNoInteractions(remoteObjectImporter);

    verify(workRepository)
        .insertQuotaTransaction(workId, "user-1", "lock-1", "LOCK_GENERATE", "LOCKED", "locked");
    verify(workRepository)
        .insertQuotaTransaction(
            workId, "user-1", "lock-1", "COMMIT_GENERATE", "COMMITTED", "committed");
    verify(workRepository).markPackageReady(workId, "Mock title", "Mock summary", true, true);
    verify(workRepository)
        .completeGenerationJob(jobId, "SUCCEEDED", GenerationStage.PACKAGE_READY, null, null);
    verify(quotaAdapter, never()).releaseGenerateQuota(any(), any(), any());
  }

  @Test
  void releasesLockedQuotaAndFailsJobWhenMusicProviderFails() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    MusicProvider musicProvider = mockMusicProvider();
    when(workRepository.insertGenerationJob(
            eq(workId),
            eq("SONG_PRODUCTION"),
            eq("RUNNING"),
            eq(GenerationStage.QUOTA_LOCKING),
            any(OffsetDateTime.class),
            isNull()))
        .thenReturn(jobId);
    when(quotaAdapter.lockGenerateQuota("user-1", workId.toString()))
        .thenReturn(new QuotaLock(true, "lock-1", "locked"));
    when(musicProvider.submit(any()))
        .thenReturn(
            MusicGenerationResult.failed(
                MusicProviderType.MOCK,
                "task-1",
                "mock",
                "PROVIDER_FAILED",
                "provider failed Authorization: Bearer fake-token-value token=plain"));
    when(quotaAdapter.releaseGenerateQuota(
            "user-1", "lock-1", FailureCode.MUSIC_GENERATION_FAILED.name()))
        .thenReturn(new QuotaRelease(true, "released"));

    SongProductionWorkflowResult result = workflowWith(musicProvider).produce(input(workId));

    assertThat(result.packageReady()).isFalse();
    assertThat(result.failureCode()).isEqualTo(FailureCode.MUSIC_GENERATION_FAILED.name());
    assertThat(result.failureMessage())
        .isEqualTo("provider failed Authorization: Bearer <redacted> token=<redacted>");

    verify(workRepository)
        .insertQuotaTransaction(workId, "user-1", "lock-1", "LOCK_GENERATE", "LOCKED", "locked");
    verify(workRepository)
        .insertQuotaTransaction(
            workId, "user-1", "lock-1", "RELEASE_GENERATE", "RELEASED", "released");
    ArgumentCaptor<ProviderCallRow> providerCall = ArgumentCaptor.forClass(ProviderCallRow.class);
    verify(workRepository).insertProviderCall(providerCall.capture());
    assertThat(providerCall.getValue().provider()).isEqualTo("MOCK");
    assertThat(providerCall.getValue().modelName()).isEqualTo("mock");
    assertThat(providerCall.getValue().providerTraceId()).isEqualTo("task-1");
    assertThat(providerCall.getValue().status()).isEqualTo("FAILED");
    assertThat(providerCall.getValue().errorCode()).isEqualTo("PROVIDER_FAILED");
    assertThat(providerCall.getValue().errorMessage())
        .isEqualTo("provider failed Authorization: Bearer <redacted> token=<redacted>");
    verify(workRepository)
        .markFailure(
            workId,
            FailureCode.MUSIC_GENERATION_FAILED,
            "provider failed Authorization: Bearer <redacted> token=<redacted>",
            true);
    verify(workRepository)
        .completeGenerationJob(
            jobId,
            "FAILED",
            GenerationStage.FAILED,
            FailureCode.MUSIC_GENERATION_FAILED,
            "provider failed Authorization: Bearer <redacted> token=<redacted>");
    verify(quotaAdapter, never()).commitGenerateQuota(any(), any());
    verify(workRepository, never()).upsertMediaAsset(any());
    verify(workRepository, never()).upsertPublishPackage(any());
    verify(workRepository, never()).markPackageReady(any(), any(), any(), eq(true), eq(true));
    verifyNoInteractions(
        publishAdapter, moderationAdapter, objectStorageClient, remoteObjectImporter);
  }

  @Test
  void importsProviderAudioSourceBeforeWritingMediaAsset() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    MusicProvider musicProvider = mockMusicProvider(MusicProviderType.SUNO);
    when(workRepository.insertGenerationJob(
            eq(workId),
            eq("SONG_PRODUCTION"),
            eq("RUNNING"),
            eq(GenerationStage.QUOTA_LOCKING),
            any(OffsetDateTime.class),
            isNull()))
        .thenReturn(jobId);
    when(quotaAdapter.lockGenerateQuota("user-1", workId.toString()))
        .thenReturn(new QuotaLock(true, "lock-1", "locked"));
    when(musicProvider.submit(any()))
        .thenReturn(
            MusicGenerationResult.succeededFromSource(
                MusicProviderType.SUNO,
                "task-1",
                "https://provider.example/audio.mp3",
                "audio/mpeg",
                123_000,
                "ok"));
    when(remoteObjectImporter.importObject(any()))
        .thenReturn(
            new StoredObject(
                "audio/" + workId + ".mp3",
                "http://localhost/audio/" + workId + ".mp3",
                "audio/mpeg",
                3_000_000L));
    when(publishAdapter.preparePackage(workId.toString()))
        .thenReturn(
            new PublishHandoff(
                "packages/" + workId + ".json",
                "http://localhost/packages/" + workId + ".json",
                OffsetDateTime.parse("2026-06-05T12:00:00Z")));
    when(moderationAdapter.preCheckPublishPackage("user-1", workId.toString()))
        .thenReturn(ModerationDecision.allow());
    when(objectStorageClient.putObject(any()))
        .thenReturn(
            new StoredObject(
                "packages/" + workId + ".json",
                "http://localhost/packages/" + workId + ".json",
                "application/json",
                512L));
    when(quotaAdapter.commitGenerateQuota("user-1", "lock-1"))
        .thenReturn(new QuotaCommit(true, "committed"));

    SongProductionWorkflowResult result =
        workflowWith(musicProvider, MusicProviderType.SUNO).produce(input(workId));

    assertThat(result.packageReady()).isTrue();
    ArgumentCaptor<RemoteObjectImportRequest> importRequest =
        ArgumentCaptor.forClass(RemoteObjectImportRequest.class);
    verify(remoteObjectImporter).importObject(importRequest.capture());
    assertThat(importRequest.getValue().sourceUrl())
        .isEqualTo("https://provider.example/audio.mp3");
    assertThat(importRequest.getValue().objectKey()).isEqualTo("audio/" + workId + ".mp3");

    ArgumentCaptor<MediaAssetRow> mediaAsset = ArgumentCaptor.forClass(MediaAssetRow.class);
    verify(workRepository, org.mockito.Mockito.times(4)).upsertMediaAsset(mediaAsset.capture());
    MediaAssetRow audioAsset =
        mediaAsset.getAllValues().stream()
            .filter(asset -> "AUDIO".equals(asset.assetType()))
            .findFirst()
            .orElseThrow();
    assertThat(audioAsset.objectKey()).isEqualTo("audio/" + workId + ".mp3");
    assertThat(audioAsset.fileSizeBytes()).isEqualTo(3_000_000L);
    assertThat(audioAsset.checksum()).isEqualTo("provider-audio");
  }

  @Test
  void marksMusicFailureNonRetryableWhenRetryLimitWillBeExhausted() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    MusicProvider musicProvider = mockMusicProvider();
    when(workRepository.insertGenerationJob(
            eq(workId),
            eq("SONG_PRODUCTION"),
            eq("RUNNING"),
            eq(GenerationStage.QUOTA_LOCKING),
            any(OffsetDateTime.class),
            isNull()))
        .thenReturn(jobId);
    when(quotaAdapter.lockGenerateQuota("user-1", workId.toString()))
        .thenReturn(new QuotaLock(true, "lock-1", "locked"));
    when(musicProvider.submit(any()))
        .thenReturn(
            MusicGenerationResult.failed(
                MusicProviderType.MOCK, "task-1", "PROVIDER_FAILED", "provider failed"));
    when(quotaAdapter.releaseGenerateQuota(
            "user-1", "lock-1", FailureCode.MUSIC_GENERATION_FAILED.name()))
        .thenReturn(new QuotaRelease(true, "released"));

    SongProductionWorkflowResult result =
        workflowWith(musicProvider).produce(input(workId, null, false));

    assertThat(result.packageReady()).isFalse();
    verify(workRepository)
        .markFailure(workId, FailureCode.MUSIC_GENERATION_FAILED, "provider failed", false);
  }

  @Test
  void usesConfiguredMusicProviderSelection() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    MusicProvider musicProvider = mockMusicProvider(MusicProviderType.MINIMAX);
    when(workRepository.insertGenerationJob(
            eq(workId),
            eq("SONG_PRODUCTION"),
            eq("RUNNING"),
            eq(GenerationStage.QUOTA_LOCKING),
            any(OffsetDateTime.class),
            isNull()))
        .thenReturn(jobId);
    when(quotaAdapter.lockGenerateQuota("user-1", workId.toString()))
        .thenReturn(new QuotaLock(true, "lock-1", "locked"));
    when(musicProvider.submit(any()))
        .thenReturn(
            MusicGenerationResult.succeeded(
                MusicProviderType.MINIMAX, "task-1", "audio/" + workId + ".mp3", 123_000, "ok"));
    when(publishAdapter.preparePackage(workId.toString()))
        .thenReturn(
            new PublishHandoff(
                "packages/" + workId + ".json",
                "http://localhost/packages/" + workId + ".json",
                OffsetDateTime.parse("2026-06-05T12:00:00Z")));
    when(moderationAdapter.preCheckPublishPackage("user-1", workId.toString()))
        .thenReturn(ModerationDecision.allow());
    when(objectStorageClient.putObject(any()))
        .thenReturn(
            new StoredObject(
                "packages/" + workId + ".json",
                "http://localhost/packages/" + workId + ".json",
                "application/json",
                512L));
    when(quotaAdapter.commitGenerateQuota("user-1", "lock-1"))
        .thenReturn(new QuotaCommit(true, "committed"));

    SongProductionWorkflowResult result =
        workflowWith(musicProvider, MusicProviderType.MINIMAX).produce(input(workId));

    assertThat(result.packageReady()).isTrue();
    verify(musicProvider).submit(any());
    verify(workRepository).markPackageReady(workId, "Mock title", "Mock summary", true, true);
  }

  @Test
  void requestMusicProviderOverridesConfiguredSelection() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    MusicProvider mockProvider = mockMusicProvider(MusicProviderType.MOCK);
    when(workRepository.insertGenerationJob(
            eq(workId),
            eq("SONG_PRODUCTION"),
            eq("RUNNING"),
            eq(GenerationStage.QUOTA_LOCKING),
            any(OffsetDateTime.class),
            isNull()))
        .thenReturn(jobId);
    when(quotaAdapter.lockGenerateQuota("user-1", workId.toString()))
        .thenReturn(new QuotaLock(true, "lock-1", "locked"));
    when(mockProvider.submit(any()))
        .thenReturn(
            MusicGenerationResult.succeeded(
                MusicProviderType.MOCK, "task-1", "audio/" + workId + ".mp3", 123_000, "ok"));
    when(publishAdapter.preparePackage(workId.toString()))
        .thenReturn(
            new PublishHandoff(
                "packages/" + workId + ".json",
                "http://localhost/packages/" + workId + ".json",
                OffsetDateTime.parse("2026-06-05T12:00:00Z")));
    when(moderationAdapter.preCheckPublishPackage("user-1", workId.toString()))
        .thenReturn(ModerationDecision.allow());
    when(objectStorageClient.putObject(any()))
        .thenReturn(
            new StoredObject(
                "packages/" + workId + ".json",
                "http://localhost/packages/" + workId + ".json",
                "application/json",
                512L));
    when(quotaAdapter.commitGenerateQuota("user-1", "lock-1"))
        .thenReturn(new QuotaCommit(true, "committed"));

    SongProductionWorkflowResult result =
        workflowWith(mockProvider, MusicProviderType.SUNO).produce(input(workId, "mock"));

    assertThat(result.packageReady()).isTrue();
    verify(mockProvider).submit(any());
    verify(workRepository).markPackageReady(workId, "Mock title", "Mock summary", true, true);
  }

  @Test
  void releasesLockedQuotaAndFailsJobWhenConfiguredProviderThrows() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    MusicProvider musicProvider = mockMusicProvider(MusicProviderType.SUNO);
    when(workRepository.insertGenerationJob(
            eq(workId),
            eq("SONG_PRODUCTION"),
            eq("RUNNING"),
            eq(GenerationStage.QUOTA_LOCKING),
            any(OffsetDateTime.class),
            isNull()))
        .thenReturn(jobId);
    when(quotaAdapter.lockGenerateQuota("user-1", workId.toString()))
        .thenReturn(new QuotaLock(true, "lock-1", "locked"));
    when(musicProvider.submit(any()))
        .thenThrow(new UnsupportedOperationException("Suno real music generation is not ready"));
    when(quotaAdapter.releaseGenerateQuota(
            "user-1", "lock-1", FailureCode.MUSIC_GENERATION_FAILED.name()))
        .thenReturn(new QuotaRelease(true, "released"));

    SongProductionWorkflowResult result =
        workflowWith(musicProvider, MusicProviderType.SUNO).produce(input(workId));

    assertThat(result.packageReady()).isFalse();
    assertThat(result.failureCode()).isEqualTo(FailureCode.MUSIC_GENERATION_FAILED.name());
    assertThat(result.failureMessage()).isEqualTo("Suno real music generation is not ready");
    verify(workRepository)
        .insertQuotaTransaction(
            workId, "user-1", "lock-1", "RELEASE_GENERATE", "RELEASED", "released");
    verify(workRepository)
        .markFailure(
            workId,
            FailureCode.MUSIC_GENERATION_FAILED,
            "Suno real music generation is not ready",
            true);
    verify(workRepository)
        .completeGenerationJob(
            jobId,
            "FAILED",
            GenerationStage.FAILED,
            FailureCode.MUSIC_GENERATION_FAILED,
            "Suno real music generation is not ready");
    verifyNoInteractions(
        publishAdapter, moderationAdapter, objectStorageClient, remoteObjectImporter);
  }

  @Test
  void releasesLockedQuotaAndFailsJobWhenPackageStorageFails() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    MusicProvider musicProvider = mockMusicProvider();
    when(workRepository.insertGenerationJob(
            eq(workId),
            eq("SONG_PRODUCTION"),
            eq("RUNNING"),
            eq(GenerationStage.QUOTA_LOCKING),
            any(OffsetDateTime.class),
            isNull()))
        .thenReturn(jobId);
    when(quotaAdapter.lockGenerateQuota("user-1", workId.toString()))
        .thenReturn(new QuotaLock(true, "lock-1", "locked"));
    when(musicProvider.submit(any()))
        .thenReturn(
            MusicGenerationResult.succeeded(
                MusicProviderType.MOCK, "task-1", "audio/" + workId + ".mp3", 123_000, "ok"));
    when(publishAdapter.preparePackage(workId.toString()))
        .thenReturn(
            new PublishHandoff(
                "packages/" + workId + ".json",
                "http://localhost/packages/" + workId + ".json",
                OffsetDateTime.parse("2026-06-05T12:00:00Z")));
    when(moderationAdapter.preCheckPublishPackage("user-1", workId.toString()))
        .thenReturn(ModerationDecision.allow());
    when(objectStorageClient.putObject(any()))
        .thenThrow(new IllegalStateException("storage unavailable"));
    when(quotaAdapter.releaseGenerateQuota(
            "user-1", "lock-1", FailureCode.PACKAGE_BUILD_FAILED.name()))
        .thenReturn(new QuotaRelease(true, "released"));

    SongProductionWorkflowResult result = workflowWith(musicProvider).produce(input(workId));

    assertThat(result.packageReady()).isFalse();
    assertThat(result.failureCode()).isEqualTo(FailureCode.PACKAGE_BUILD_FAILED.name());
    assertThat(result.failureMessage()).isEqualTo("storage unavailable");

    verify(workRepository)
        .insertQuotaTransaction(
            workId, "user-1", "lock-1", "RELEASE_GENERATE", "RELEASED", "released");
    verify(workRepository)
        .markFailure(workId, FailureCode.PACKAGE_BUILD_FAILED, "storage unavailable", true);
    verify(workRepository)
        .completeGenerationJob(
            jobId,
            "FAILED",
            GenerationStage.FAILED,
            FailureCode.PACKAGE_BUILD_FAILED,
            "storage unavailable");
    verify(quotaAdapter, never()).commitGenerateQuota(any(), any());
    verify(workRepository, never()).upsertPublishPackage(any());
    verify(workRepository, never()).markPackageReady(any(), any(), any(), eq(true), eq(true));
  }

  @Test
  void releasesLockedQuotaAndFailsJobWhenVideoRenderFails() {
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    MusicProvider musicProvider = mockMusicProvider();
    when(workRepository.insertGenerationJob(
            eq(workId),
            eq("SONG_PRODUCTION"),
            eq("RUNNING"),
            eq(GenerationStage.QUOTA_LOCKING),
            any(OffsetDateTime.class),
            isNull()))
        .thenReturn(jobId);
    when(quotaAdapter.lockGenerateQuota("user-1", workId.toString()))
        .thenReturn(new QuotaLock(true, "lock-1", "locked"));
    when(musicProvider.submit(any()))
        .thenReturn(
            MusicGenerationResult.succeeded(
                MusicProviderType.MOCK, "task-1", "audio/" + workId + ".mp3", 123_000, "ok"));
    when(quotaAdapter.releaseGenerateQuota(
            "user-1", "lock-1", FailureCode.PACKAGE_BUILD_FAILED.name()))
        .thenReturn(new QuotaRelease(true, "released"));
    VideoRenderService failingVideoRenderer =
        request -> {
          throw new IllegalStateException("video render unavailable");
        };

    SongProductionWorkflowResult result =
        workflowWith(musicProvider, MusicProviderType.MOCK, failingVideoRenderer)
            .produce(input(workId));

    assertThat(result.packageReady()).isFalse();
    assertThat(result.failureCode()).isEqualTo(FailureCode.PACKAGE_BUILD_FAILED.name());
    assertThat(result.failureMessage()).isEqualTo("video render unavailable");

    ArgumentCaptor<MediaAssetRow> mediaAsset = ArgumentCaptor.forClass(MediaAssetRow.class);
    verify(workRepository, org.mockito.Mockito.times(2)).upsertMediaAsset(mediaAsset.capture());
    assertThat(mediaAsset.getAllValues())
        .extracting(MediaAssetRow::assetType)
        .containsExactly("AUDIO", "COVER");
    verify(workRepository)
        .insertQuotaTransaction(
            workId, "user-1", "lock-1", "RELEASE_GENERATE", "RELEASED", "released");
    verify(workRepository)
        .markFailure(workId, FailureCode.PACKAGE_BUILD_FAILED, "video render unavailable", true);
    verify(workRepository)
        .completeGenerationJob(
            jobId,
            "FAILED",
            GenerationStage.FAILED,
            FailureCode.PACKAGE_BUILD_FAILED,
            "video render unavailable");
    verify(quotaAdapter, never()).commitGenerateQuota(any(), any());
    verify(workRepository, never()).upsertPublishPackage(any());
    verify(workRepository, never()).markPackageReady(any(), any(), any(), eq(true), eq(true));
    verifyNoInteractions(
        publishAdapter, moderationAdapter, objectStorageClient, remoteObjectImporter);
  }

  private MockSongProductionWorkflow workflowWith(MusicProvider musicProvider) {
    return workflowWith(musicProvider, MusicProviderType.MOCK);
  }

  private MockSongProductionWorkflow workflowWith(
      MusicProvider musicProvider, MusicProviderType providerType) {
    return workflowWith(musicProvider, providerType, new MockVideoRenderService());
  }

  private MockSongProductionWorkflow workflowWith(
      MusicProvider musicProvider,
      MusicProviderType providerType,
      VideoRenderService videoRenderer) {
    stubObjectStorageDownloadUrls();
    return new MockSongProductionWorkflow(
        workRepository,
        quotaAdapter,
        moderationAdapter,
        publishAdapter,
        new MusicProviderRegistry(List.of(musicProvider)),
        new MusicProviderSelection(providerType),
        objectStorageClient,
        remoteObjectImporter,
        new MockCoverGenerationService(),
        videoRenderer,
        objectMapper);
  }

  private void stubObjectStorageDownloadUrls() {
    when(objectStorageClient.createDownloadUrl(any()))
        .thenAnswer(
            invocation -> {
              String objectKey = invocation.getArgument(0, String.class);
              return new ObjectStorageDownloadUrl(
                  objectKey,
                  "http://localhost/" + objectKey,
                  OffsetDateTime.parse("2026-06-05T12:00:00Z"));
            });
  }

  private MusicProvider mockMusicProvider() {
    return mockMusicProvider(MusicProviderType.MOCK);
  }

  private MusicProvider mockMusicProvider(MusicProviderType providerType) {
    MusicProvider musicProvider = mock(MusicProvider.class);
    when(musicProvider.providerType()).thenReturn(providerType);
    return musicProvider;
  }

  private SongProductionWorkflowInput input(UUID workId) {
    return input(workId, null, true);
  }

  private SongProductionWorkflowInput input(UUID workId, String musicProvider) {
    return input(workId, musicProvider, true);
  }

  private SongProductionWorkflowInput input(
      UUID workId, String musicProvider, boolean musicRetryAllowedAfterFailure) {
    return new SongProductionWorkflowInput(
        workId.toString(),
        "user-1",
        UUID.randomUUID().toString(),
        "Mock title",
        "Mock summary",
        "Mock lyrics",
        "Mock prompt",
        "AUTO",
        musicProvider,
        musicRetryAllowedAfterFailure);
  }
}
