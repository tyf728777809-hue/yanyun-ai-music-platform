package com.yanyun.music.api.production;

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
import com.yanyun.music.api.work.WorkRepository;
import com.yanyun.music.api.work.WorkRepository.MediaAssetRow;
import com.yanyun.music.api.work.WorkRepository.PublishPackageRow;
import com.yanyun.music.moderation.ModerationAdapter;
import com.yanyun.music.moderation.ModerationDecision;
import com.yanyun.music.musicprovider.MusicGenerationRequest;
import com.yanyun.music.musicprovider.MusicGenerationResult;
import com.yanyun.music.musicprovider.MusicProvider;
import com.yanyun.music.musicprovider.MusicProviderRegistry;
import com.yanyun.music.musicprovider.MusicProviderType;
import com.yanyun.music.publish.PublishAdapter;
import com.yanyun.music.publish.PublishHandoff;
import com.yanyun.music.quota.QuotaAdapter;
import com.yanyun.music.quota.QuotaCommit;
import com.yanyun.music.quota.QuotaLock;
import com.yanyun.music.quota.QuotaRelease;
import com.yanyun.music.workdomain.FailureCode;
import com.yanyun.music.workdomain.GenerationStage;
import com.yanyun.music.workdomain.PackageStatus;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.SongProductionWorkflowResult;
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

    ArgumentCaptor<MediaAssetRow> mediaAsset = ArgumentCaptor.forClass(MediaAssetRow.class);
    verify(workRepository, org.mockito.Mockito.times(4)).upsertMediaAsset(mediaAsset.capture());
    assertThat(mediaAsset.getAllValues())
        .extracting(MediaAssetRow::assetType)
        .containsExactlyInAnyOrder("AUDIO", "COVER", "VIDEO", "TIMELINE");

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
                MusicProviderType.MOCK, "task-1", "PROVIDER_FAILED", "provider failed"));
    when(quotaAdapter.releaseGenerateQuota(
            "user-1", "lock-1", FailureCode.MUSIC_GENERATION_FAILED.name()))
        .thenReturn(new QuotaRelease(true, "released"));

    SongProductionWorkflowResult result = workflowWith(musicProvider).produce(input(workId));

    assertThat(result.packageReady()).isFalse();
    assertThat(result.failureCode()).isEqualTo(FailureCode.MUSIC_GENERATION_FAILED.name());
    assertThat(result.failureMessage()).isEqualTo("provider failed");

    verify(workRepository)
        .insertQuotaTransaction(workId, "user-1", "lock-1", "LOCK_GENERATE", "LOCKED", "locked");
    verify(workRepository)
        .insertQuotaTransaction(
            workId, "user-1", "lock-1", "RELEASE_GENERATE", "RELEASED", "released");
    verify(workRepository)
        .markFailure(workId, FailureCode.MUSIC_GENERATION_FAILED, "provider failed", true);
    verify(workRepository)
        .completeGenerationJob(
            jobId,
            "FAILED",
            GenerationStage.FAILED,
            FailureCode.MUSIC_GENERATION_FAILED,
            "provider failed");
    verify(quotaAdapter, never()).commitGenerateQuota(any(), any());
    verify(workRepository, never()).upsertMediaAsset(any());
    verify(workRepository, never()).upsertPublishPackage(any());
    verify(workRepository, never()).markPackageReady(any(), any(), any(), eq(true), eq(true));
    verifyNoInteractions(publishAdapter, moderationAdapter);
  }

  private MockSongProductionWorkflow workflowWith(MusicProvider musicProvider) {
    return new MockSongProductionWorkflow(
        workRepository,
        quotaAdapter,
        moderationAdapter,
        publishAdapter,
        new MusicProviderRegistry(List.of(musicProvider)),
        objectMapper);
  }

  private MusicProvider mockMusicProvider() {
    MusicProvider musicProvider = mock(MusicProvider.class);
    when(musicProvider.providerType()).thenReturn(MusicProviderType.MOCK);
    return musicProvider;
  }

  private SongProductionWorkflowInput input(UUID workId) {
    return new SongProductionWorkflowInput(
        workId.toString(),
        "user-1",
        UUID.randomUUID().toString(),
        "Mock title",
        "Mock summary",
        "Mock lyrics",
        "Mock prompt",
        "AUTO");
  }
}
