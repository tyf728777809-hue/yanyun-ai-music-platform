package com.yanyun.music.api.production;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.api.work.WorkRepository;
import com.yanyun.music.api.work.WorkRepository.MediaAssetRow;
import com.yanyun.music.api.work.WorkRepository.PublishPackageRow;
import com.yanyun.music.moderation.ModerationAdapter;
import com.yanyun.music.moderation.ModerationDecision;
import com.yanyun.music.musicprovider.MusicGenerationRequest;
import com.yanyun.music.musicprovider.MusicGenerationResult;
import com.yanyun.music.musicprovider.MusicGenerationStatus;
import com.yanyun.music.musicprovider.MusicProviderRegistry;
import com.yanyun.music.musicprovider.MusicProviderSelection;
import com.yanyun.music.publish.PublishAdapter;
import com.yanyun.music.publish.PublishHandoff;
import com.yanyun.music.quota.QuotaAdapter;
import com.yanyun.music.quota.QuotaCommit;
import com.yanyun.music.quota.QuotaLock;
import com.yanyun.music.quota.QuotaRelease;
import com.yanyun.music.storage.ObjectStorageClient;
import com.yanyun.music.storage.ObjectStoragePutRequest;
import com.yanyun.music.storage.StoredObject;
import com.yanyun.music.workdomain.FailureCode;
import com.yanyun.music.workdomain.GenerationStage;
import com.yanyun.music.workdomain.PackageStatus;
import com.yanyun.music.workflow.SongProductionWorkflow;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.SongProductionWorkflowResult;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MockSongProductionWorkflow implements SongProductionWorkflow {

  private static final String ASSET_BASE_URL = "http://localhost:9000/yanyun-works-local/";

  private final WorkRepository workRepository;
  private final QuotaAdapter quotaAdapter;
  private final ModerationAdapter moderationAdapter;
  private final PublishAdapter publishAdapter;
  private final MusicProviderRegistry musicProviderRegistry;
  private final MusicProviderSelection musicProviderSelection;
  private final ObjectStorageClient objectStorageClient;
  private final ObjectMapper objectMapper;

  public MockSongProductionWorkflow(
      WorkRepository workRepository,
      QuotaAdapter quotaAdapter,
      ModerationAdapter moderationAdapter,
      PublishAdapter publishAdapter,
      MusicProviderRegistry musicProviderRegistry,
      MusicProviderSelection musicProviderSelection,
      ObjectStorageClient objectStorageClient,
      ObjectMapper objectMapper) {
    this.workRepository = workRepository;
    this.quotaAdapter = quotaAdapter;
    this.moderationAdapter = moderationAdapter;
    this.publishAdapter = publishAdapter;
    this.musicProviderRegistry = musicProviderRegistry;
    this.musicProviderSelection = musicProviderSelection;
    this.objectStorageClient = objectStorageClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public SongProductionWorkflowResult produce(SongProductionWorkflowInput input) {
    UUID workId = UUID.fromString(input.workId());
    UUID jobId =
        workRepository.insertGenerationJob(
            workId,
            "SONG_PRODUCTION",
            "RUNNING",
            GenerationStage.QUOTA_LOCKING,
            OffsetDateTime.now(),
            null);

    QuotaLock lock = quotaAdapter.lockGenerateQuota(input.userId(), input.workId());
    if (!lock.locked()) {
      workRepository.insertQuotaTransaction(
          workId, input.userId(), lock.lockId(), "LOCK_GENERATE", "FAILED", lock.message());
      return fail(
          workId,
          jobId,
          FailureCode.QUOTA_LOCK_FAILED,
          firstNonBlank(lock.message(), "Quota lock failed"),
          false,
          input.userId(),
          null);
    }
    workRepository.insertQuotaTransaction(
        workId, input.userId(), lock.lockId(), "LOCK_GENERATE", "LOCKED", lock.message());

    MusicGenerationResult musicResult;
    try {
      musicResult =
          musicProviderRegistry
              .require(selectedProvider(input).providerType())
              .submit(
                  new MusicGenerationRequest(
                      input.workId(),
                      input.lyricsText(),
                      input.musicPrompt(),
                      input.vocalPreference(),
                      Map.of()));
    } catch (RuntimeException exception) {
      return fail(
          workId,
          jobId,
          FailureCode.MUSIC_GENERATION_FAILED,
          firstNonBlank(exception.getMessage(), "Music generation failed"),
          input.musicRetryAllowedAfterFailure(),
          input.userId(),
          lock.lockId());
    }
    if (musicResult.status() != MusicGenerationStatus.SUCCEEDED) {
      return fail(
          workId,
          jobId,
          FailureCode.MUSIC_GENERATION_FAILED,
          firstNonBlank(musicResult.failureMessage(), "Music generation failed"),
          input.musicRetryAllowedAfterFailure(),
          input.userId(),
          lock.lockId());
    }

    createMockMediaAssets(workId, musicResult);

    PublishHandoff handoff;
    try {
      handoff = publishAdapter.preparePackage(input.workId());
    } catch (RuntimeException exception) {
      return fail(
          workId,
          jobId,
          FailureCode.PACKAGE_BUILD_FAILED,
          "Publish package preparation failed",
          true,
          input.userId(),
          lock.lockId());
    }
    ModerationDecision packageDecision =
        moderationAdapter.preCheckPublishPackage(input.userId(), input.workId());
    if (!packageDecision.allowed()) {
      return fail(
          workId,
          jobId,
          FailureCode.PACKAGE_BLOCKED,
          firstNonBlank(packageDecision.message(), "Package moderation blocked"),
          false,
          input.userId(),
          lock.lockId());
    }

    String packageJson;
    StoredObject storedPackage;
    try {
      packageJson = writeJson(buildPackageJson(workId, input));
      storedPackage =
          objectStorageClient.putObject(
              new ObjectStoragePutRequest(
                  handoff.packageObjectKey(),
                  "application/json",
                  packageJson.getBytes(StandardCharsets.UTF_8)));
    } catch (RuntimeException exception) {
      return fail(
          workId,
          jobId,
          FailureCode.PACKAGE_BUILD_FAILED,
          firstNonBlank(exception.getMessage(), "Package build failed"),
          true,
          input.userId(),
          lock.lockId());
    }

    workRepository.upsertPublishPackage(
        new PublishPackageRow(
            UUID.randomUUID(),
            workId,
            PackageStatus.PACKAGE_READY,
            packageJson,
            storedPackage.objectKey(),
            storedPackage.url(),
            handoff.expiresAt(),
            null,
            null,
            null));

    QuotaCommit commit = quotaAdapter.commitGenerateQuota(input.userId(), lock.lockId());
    workRepository.insertQuotaTransaction(
        workId, input.userId(), lock.lockId(), "COMMIT_GENERATE", "COMMITTED", commit.message());
    workRepository.markPackageReady(
        workId, input.songTitle(), input.songSummary(), true, commit.committed());
    workRepository.completeGenerationJob(
        jobId, "SUCCEEDED", GenerationStage.PACKAGE_READY, null, null);

    return SongProductionWorkflowResult.packageReady(
        jobId.toString(), PackageStatus.PACKAGE_READY.name());
  }

  private SongProductionWorkflowResult fail(
      UUID workId,
      UUID jobId,
      FailureCode failureCode,
      String failureMessage,
      boolean retryable,
      String userId,
      String lockId) {
    if (lockId != null) {
      QuotaRelease release = quotaAdapter.releaseGenerateQuota(userId, lockId, failureCode.name());
      workRepository.insertQuotaTransaction(
          workId,
          userId,
          lockId,
          "RELEASE_GENERATE",
          release.released() ? "RELEASED" : "FAILED",
          release.message());
    }
    workRepository.markFailure(workId, failureCode, failureMessage, retryable);
    workRepository.completeGenerationJob(
        jobId, "FAILED", GenerationStage.FAILED, failureCode, failureMessage);
    return SongProductionWorkflowResult.failed(
        jobId.toString(),
        PackageStatus.PACKAGE_NOT_READY.name(),
        failureCode.name(),
        failureMessage);
  }

  private void createMockMediaAssets(UUID workId, MusicGenerationResult musicResult) {
    workRepository.upsertMediaAsset(
        new MediaAssetRow(
            workId,
            "AUDIO",
            firstNonBlank(musicResult.audioObjectKey(), "audio/" + workId + ".mp3"),
            "audio/mpeg",
            4_800_000L,
            "mock-audio",
            null,
            null,
            musicResult.durationMs() == null ? 180_000 : musicResult.durationMs(),
            "{}"));
    workRepository.upsertMediaAsset(
        new MediaAssetRow(
            workId,
            "COVER",
            "covers/" + workId + ".png",
            "image/png",
            512_000L,
            "mock-cover",
            1080,
            1920,
            null,
            "{}"));
    workRepository.upsertMediaAsset(
        new MediaAssetRow(
            workId,
            "VIDEO",
            "videos/" + workId + ".mp4",
            "video/mp4",
            12_000_000L,
            "mock-video",
            1080,
            1920,
            180_000,
            "{}"));
    workRepository.upsertMediaAsset(
        new MediaAssetRow(
            workId,
            "TIMELINE",
            "timelines/" + workId + ".json",
            "application/json",
            64_000L,
            "mock-timeline",
            null,
            null,
            180_000,
            "{}"));
  }

  private MusicProviderSelection selectedProvider(SongProductionWorkflowInput input) {
    return input.musicProvider() == null || input.musicProvider().isBlank()
        ? musicProviderSelection
        : MusicProviderSelection.fromConfig(input.musicProvider());
  }

  private Map<String, Object> buildPackageJson(UUID workId, SongProductionWorkflowInput input) {
    return Map.of(
        "work_id",
        workId.toString(),
        "video",
        Map.of(
            "url",
            ASSET_BASE_URL + "videos/" + workId + ".mp4",
            "mime_type",
            "video/mp4",
            "file_size_bytes",
            12_000_000,
            "checksum",
            "mock-video"),
        "cover",
        Map.of(
            "url",
            ASSET_BASE_URL + "covers/" + workId + ".png",
            "mime_type",
            "image/png",
            "file_size_bytes",
            512_000,
            "checksum",
            "mock-cover"),
        "lyrics",
        Map.of(
            "text",
            input.lyricsText(),
            "timeline_url",
            ASSET_BASE_URL + "timelines/" + workId + ".json"),
        "metadata",
        Map.of(
            "song_title",
            input.songTitle(),
            "song_summary",
            input.songSummary(),
            "source",
            "mock-local"));
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to write JSON", exception);
    }
  }
}
