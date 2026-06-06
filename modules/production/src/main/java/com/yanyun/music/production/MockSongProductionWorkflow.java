package com.yanyun.music.production;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.creativeagent.CoverPromptAgent;
import com.yanyun.music.creativeagent.CoverPromptRequest;
import com.yanyun.music.creativeagent.CoverPromptResult;
import com.yanyun.music.creativeagent.MusicPromptAgent;
import com.yanyun.music.creativeagent.MusicPromptRequest;
import com.yanyun.music.creativeagent.MusicPromptResult;
import com.yanyun.music.image2.CoverGenerationRequest;
import com.yanyun.music.image2.CoverGenerationResult;
import com.yanyun.music.image2.CoverGenerationService;
import com.yanyun.music.media.MediaAssetDescriptor;
import com.yanyun.music.media.VideoRenderRequest;
import com.yanyun.music.media.VideoRenderResult;
import com.yanyun.music.media.VideoRenderService;
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
import com.yanyun.music.storage.ObjectStorageDownloadUrl;
import com.yanyun.music.storage.ObjectStoragePutRequest;
import com.yanyun.music.storage.RemoteObjectImportRequest;
import com.yanyun.music.storage.RemoteObjectImporter;
import com.yanyun.music.storage.StoredObject;
import com.yanyun.music.workdomain.FailureCode;
import com.yanyun.music.workdomain.GenerationStage;
import com.yanyun.music.workdomain.PackageStatus;
import com.yanyun.music.workflow.SongProductionWorkflow;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.SongProductionWorkflowResult;
import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.MediaAssetRow;
import com.yanyun.music.workpersistence.WorkRepository.ProviderCallRow;
import com.yanyun.music.workpersistence.WorkRepository.PublishPackageRow;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class MockSongProductionWorkflow implements SongProductionWorkflow {

  private static final Pattern BEARER_TOKEN_PATTERN =
      Pattern.compile("(?i)bearer\\s+[a-z0-9._~+/=-]+");
  private static final Pattern JWT_PATTERN =
      Pattern.compile("\\b[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b");
  private static final Pattern SECRET_FIELD_PATTERN =
      Pattern.compile("(?i)(access[_ -]?key|secret[_ -]?key|token)\\s*[:=]\\s*[^,;\\s]+");

  private final WorkRepository workRepository;
  private final QuotaAdapter quotaAdapter;
  private final ModerationAdapter moderationAdapter;
  private final PublishAdapter publishAdapter;
  private final MusicProviderRegistry musicProviderRegistry;
  private final MusicProviderSelection musicProviderSelection;
  private final ObjectStorageClient objectStorageClient;
  private final RemoteObjectImporter remoteObjectImporter;
  private final MusicPromptAgent musicPromptAgent;
  private final CoverPromptAgent coverPromptAgent;
  private final CoverGenerationService coverGenerationService;
  private final VideoRenderService videoRenderService;
  private final ObjectMapper objectMapper;

  public MockSongProductionWorkflow(
      WorkRepository workRepository,
      QuotaAdapter quotaAdapter,
      ModerationAdapter moderationAdapter,
      PublishAdapter publishAdapter,
      MusicProviderRegistry musicProviderRegistry,
      MusicProviderSelection musicProviderSelection,
      ObjectStorageClient objectStorageClient,
      RemoteObjectImporter remoteObjectImporter,
      MusicPromptAgent musicPromptAgent,
      CoverPromptAgent coverPromptAgent,
      CoverGenerationService coverGenerationService,
      VideoRenderService videoRenderService,
      ObjectMapper objectMapper) {
    this.workRepository = workRepository;
    this.quotaAdapter = quotaAdapter;
    this.moderationAdapter = moderationAdapter;
    this.publishAdapter = publishAdapter;
    this.musicProviderRegistry = musicProviderRegistry;
    this.musicProviderSelection = musicProviderSelection;
    this.objectStorageClient = objectStorageClient;
    this.remoteObjectImporter = remoteObjectImporter;
    this.musicPromptAgent = musicPromptAgent;
    this.coverPromptAgent = coverPromptAgent;
    this.coverGenerationService = coverGenerationService;
    this.videoRenderService = videoRenderService;
    this.objectMapper = objectMapper;
  }

  @Override
  public SongProductionWorkflowResult produce(SongProductionWorkflowInput input) {
    UUID workId = UUID.fromString(input.workId());
    UUID jobId = resolveJobId(workId, input);

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
    MusicProviderSelection selectedProvider = selectedProvider(input);
    MusicPromptResult musicPrompt;
    try {
      musicPrompt =
          musicPromptAgent.generate(
              new MusicPromptRequest(
                  input.workId(),
                  input.songTitle(),
                  input.songSummary(),
                  input.lyricsText(),
                  input.musicPrompt(),
                  input.vocalPreference(),
                  selectedProvider.providerType().name()));
    } catch (RuntimeException exception) {
      return fail(
          workId,
          jobId,
          FailureCode.MUSIC_GENERATION_FAILED,
          firstNonBlank(exception.getMessage(), "Music prompt generation failed"),
          input.musicRetryAllowedAfterFailure(),
          input.userId(),
          lock.lockId());
    }
    MusicGenerationRequest musicRequest =
        new MusicGenerationRequest(
            input.workId(),
            input.lyricsText(),
            musicPrompt.musicPrompt(),
            input.vocalPreference(),
            musicPrompt.providerOptions());
    long providerStartedAt = System.nanoTime();
    try {
      musicResult =
          musicProviderRegistry.require(selectedProvider.providerType()).submit(musicRequest);
    } catch (RuntimeException exception) {
      recordProviderCall(
          workId,
          jobId,
          selectedProvider,
          musicRequest,
          null,
          "FAILED",
          elapsedMillis(providerStartedAt),
          "PROVIDER_EXCEPTION",
          firstNonBlank(exception.getMessage(), "Music provider failed"));
      return fail(
          workId,
          jobId,
          FailureCode.MUSIC_GENERATION_FAILED,
          firstNonBlank(exception.getMessage(), "Music generation failed"),
          input.musicRetryAllowedAfterFailure(),
          input.userId(),
          lock.lockId());
    }
    recordProviderCall(
        workId,
        jobId,
        selectedProvider,
        musicRequest,
        musicResult,
        musicResult.status().name(),
        elapsedMillis(providerStartedAt),
        musicResult.failureCode(),
        musicResult.failureMessage());
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

    GeneratedMediaAssets mediaAssets;
    try {
      mediaAssets = createMediaAssets(workId, input, musicResult);
    } catch (RuntimeException exception) {
      return fail(
          workId,
          jobId,
          FailureCode.PACKAGE_BUILD_FAILED,
          firstNonBlank(exception.getMessage(), "Media asset generation failed"),
          true,
          input.userId(),
          lock.lockId());
    }

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
    ObjectStorageDownloadUrl packageDownloadUrl;
    try {
      packageJson = writeJson(buildPackageJson(workId, input, mediaAssets));
      storedPackage =
          objectStorageClient.putObject(
              new ObjectStoragePutRequest(
                  handoff.packageObjectKey(),
                  "application/json",
                  packageJson.getBytes(StandardCharsets.UTF_8)));
      packageDownloadUrl = objectStorageClient.createDownloadUrl(storedPackage.objectKey());
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
            packageDownloadUrl.url(),
            packageDownloadUrl.expiresAt(),
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
    String safeFailureMessage =
        firstNonBlank(sanitizeProviderError(failureMessage), failureCode.name());
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
    workRepository.markFailure(workId, failureCode, safeFailureMessage, retryable);
    workRepository.completeGenerationJob(
        jobId, "FAILED", GenerationStage.FAILED, failureCode, safeFailureMessage);
    return SongProductionWorkflowResult.failed(
        jobId.toString(),
        PackageStatus.PACKAGE_NOT_READY.name(),
        failureCode.name(),
        safeFailureMessage);
  }

  private UUID resolveJobId(UUID workId, SongProductionWorkflowInput input) {
    if (input.jobId() != null && !input.jobId().isBlank()) {
      return UUID.fromString(input.jobId());
    }
    return workRepository.insertGenerationJob(
        workId,
        "SONG_PRODUCTION",
        "RUNNING",
        GenerationStage.QUOTA_LOCKING,
        OffsetDateTime.now(),
        null);
  }

  private GeneratedMediaAssets createMediaAssets(
      UUID workId, SongProductionWorkflowInput input, MusicGenerationResult musicResult) {
    StoredObject importedAudio = importProviderAudio(workId, musicResult);
    String audioObjectKey =
        firstNonBlank(
            musicResult.audioObjectKey(),
            importedAudio == null ? "audio/" + workId + ".mp3" : importedAudio.objectKey());
    MediaAssetRow audioAsset =
        new MediaAssetRow(
            workId,
            "AUDIO",
            audioObjectKey,
            firstNonBlank(musicResult.audioContentType(), "audio/mpeg"),
            importedAudio == null ? 4_800_000L : importedAudio.sizeBytes(),
            importedAudio == null ? "mock-audio" : "provider-audio",
            null,
            null,
            musicResult.durationMs() == null ? 180_000 : musicResult.durationMs(),
            "{}");
    workRepository.upsertMediaAsset(audioAsset);

    CoverPromptResult coverPrompt =
        coverPromptAgent.generate(
            new CoverPromptRequest(
                workId.toString(),
                input.songTitle(),
                input.songSummary(),
                input.lyricsText(),
                input.musicPrompt(),
                input.coverPromptSeed(),
                1920,
                1080));
    CoverGenerationResult coverResult =
        coverGenerationService.generateCover(
            new CoverGenerationRequest(
                workId.toString(),
                input.songTitle(),
                input.songSummary(),
                input.lyricsText(),
                input.musicPrompt(),
                coverPrompt.visualPrompt(),
                coverPrompt.negativePrompt(),
                coverPrompt.width(),
                coverPrompt.height(),
                coverPrompt.providerOptions()));
    MediaAssetRow coverAsset = toMediaAssetRow(workId, coverResult.asset());
    workRepository.upsertMediaAsset(coverAsset);

    VideoRenderResult videoResult =
        videoRenderService.renderVideo(
            new VideoRenderRequest(
                workId.toString(),
                input.songTitle(),
                input.songSummary(),
                input.lyricsText(),
                audioAsset.objectKey(),
                audioAsset.mimeType(),
                coverAsset.objectKey(),
                audioAsset.durationMs()));
    MediaAssetRow videoAsset = toMediaAssetRow(workId, videoResult.videoAsset());
    MediaAssetRow timelineAsset = toMediaAssetRow(workId, videoResult.timelineAsset());
    workRepository.upsertMediaAsset(videoAsset);
    workRepository.upsertMediaAsset(timelineAsset);
    return new GeneratedMediaAssets(audioAsset, coverAsset, videoAsset, timelineAsset);
  }

  private MediaAssetRow toMediaAssetRow(UUID workId, MediaAssetDescriptor asset) {
    return new MediaAssetRow(
        workId,
        asset.assetType(),
        asset.objectKey(),
        asset.mimeType(),
        asset.fileSizeBytes(),
        asset.checksum(),
        asset.width(),
        asset.height(),
        asset.durationMs(),
        writeJson(asset.metadata()));
  }

  private StoredObject importProviderAudio(UUID workId, MusicGenerationResult musicResult) {
    if (musicResult.audioObjectKey() != null && !musicResult.audioObjectKey().isBlank()) {
      return null;
    }
    if (musicResult.audioSourceUrl() == null || musicResult.audioSourceUrl().isBlank()) {
      return null;
    }
    return remoteObjectImporter.importObject(
        new RemoteObjectImportRequest(
            musicResult.audioSourceUrl(),
            "audio/" + workId + ".mp3",
            firstNonBlank(musicResult.audioContentType(), "audio/mpeg")));
  }

  private MusicProviderSelection selectedProvider(SongProductionWorkflowInput input) {
    return input.musicProvider() == null || input.musicProvider().isBlank()
        ? musicProviderSelection
        : MusicProviderSelection.fromConfig(input.musicProvider());
  }

  private void recordProviderCall(
      UUID workId,
      UUID jobId,
      MusicProviderSelection selectedProvider,
      MusicGenerationRequest request,
      MusicGenerationResult result,
      String status,
      int latencyMs,
      String errorCode,
      String errorMessage) {
    workRepository.insertProviderCall(
        new ProviderCallRow(
            workId,
            jobId,
            selectedProvider.providerType().name(),
            "MUSIC_GENERATION",
            modelName(selectedProvider, result),
            hashRequest(request),
            sha256(request.musicPrompt()),
            result == null ? null : result.providerTaskId(),
            status,
            latencyMs,
            null,
            errorCode,
            sanitizeProviderError(errorMessage)));
  }

  private String modelName(MusicProviderSelection selectedProvider, MusicGenerationResult result) {
    if (result != null && result.modelName() != null && !result.modelName().isBlank()) {
      return result.modelName();
    }
    return selectedProvider.providerType().name().toLowerCase(java.util.Locale.ROOT);
  }

  private String sanitizeProviderError(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String sanitized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
    sanitized = BEARER_TOKEN_PATTERN.matcher(sanitized).replaceAll("Bearer <redacted>");
    sanitized = JWT_PATTERN.matcher(sanitized).replaceAll("<jwt-redacted>");
    sanitized = SECRET_FIELD_PATTERN.matcher(sanitized).replaceAll("$1=<redacted>");
    return sanitized.length() <= 240 ? sanitized : sanitized.substring(0, 240);
  }

  private int elapsedMillis(long startedAtNanos) {
    return Math.toIntExact(
        Math.max(0L, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos)));
  }

  private String hashRequest(MusicGenerationRequest request) {
    return sha256(
        request.workId()
            + "\n"
            + request.vocalPreference()
            + "\n"
            + request.musicPrompt()
            + "\n"
            + request.lyricsText());
  }

  private Map<String, Object> buildPackageJson(
      UUID workId, SongProductionWorkflowInput input, GeneratedMediaAssets mediaAssets) {
    return Map.of(
        "work_id",
        workId.toString(),
        "video",
        Map.of(
            "url",
            assetUrl(mediaAssets.videoAsset()),
            "mime_type",
            mediaAssets.videoAsset().mimeType(),
            "file_size_bytes",
            mediaAssets.videoAsset().fileSizeBytes(),
            "checksum",
            mediaAssets.videoAsset().checksum()),
        "cover",
        Map.of(
            "url",
            assetUrl(mediaAssets.coverAsset()),
            "mime_type",
            mediaAssets.coverAsset().mimeType(),
            "file_size_bytes",
            mediaAssets.coverAsset().fileSizeBytes(),
            "checksum",
            mediaAssets.coverAsset().checksum()),
        "lyrics",
        Map.of("text", input.lyricsText(), "timeline_url", assetUrl(mediaAssets.timelineAsset())),
        "metadata",
        Map.of(
            "song_title",
            input.songTitle(),
            "song_summary",
            input.songSummary(),
            "source",
            "mock-local"));
  }

  private String assetUrl(MediaAssetRow asset) {
    return objectStorageClient.createDownloadUrl(asset.objectKey()).url();
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

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of()
          .formatHex(digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 digest is unavailable", exception);
    }
  }

  private record GeneratedMediaAssets(
      MediaAssetRow audioAsset,
      MediaAssetRow coverAsset,
      MediaAssetRow videoAsset,
      MediaAssetRow timelineAsset) {}
}
