package com.yanyun.music.production;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.creativeagent.CoverPromptAgent;
import com.yanyun.music.creativeagent.CoverPromptRequest;
import com.yanyun.music.creativeagent.CoverPromptResult;
import com.yanyun.music.creativeagent.ModerationAgent;
import com.yanyun.music.creativeagent.ModerationAgentRequest;
import com.yanyun.music.creativeagent.ModerationAgentResult;
import com.yanyun.music.creativeagent.ModerationTarget;
import com.yanyun.music.creativeagent.MusicPromptAgent;
import com.yanyun.music.creativeagent.MusicPromptRequest;
import com.yanyun.music.creativeagent.MusicPromptResult;
import com.yanyun.music.creativeagent.QualityDecision;
import com.yanyun.music.creativeagent.QualityEvaluationAgent;
import com.yanyun.music.creativeagent.QualityEvaluationRequest;
import com.yanyun.music.creativeagent.QualityEvaluationResult;
import com.yanyun.music.creativeagent.QualityGate;
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
import com.yanyun.music.workflow.SongProductionWorkflow;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.SongProductionWorkflowResult;
import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.MediaAssetRow;
import com.yanyun.music.workpersistence.WorkRepository.ProviderCallRow;
import com.yanyun.music.workpersistence.WorkRepository.PublishPackageRow;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

@Service
public class MockSongProductionWorkflow implements SongProductionWorkflow {

  private static final Pattern BEARER_TOKEN_PATTERN =
      Pattern.compile("(?i)bearer\\s+[a-z0-9._~+/=-]+");
  private static final Pattern JWT_PATTERN =
      Pattern.compile("\\b[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b");
  private static final Pattern SECRET_FIELD_PATTERN =
      Pattern.compile(
          "(?i)(access[_ -]?key|secret[_ -]?key|api[_ -]?key|token)\\s*[:=]\\s*[^,;\\s]+");
  private static final Pattern URL_PATTERN = Pattern.compile("https?://[^,;\\s]+");
  private static final int MOCK_AUDIO_SAMPLE_RATE = 8_000;
  private static final short MOCK_AUDIO_CHANNELS = 1;
  private static final short MOCK_AUDIO_BITS_PER_SAMPLE = 16;

  private final WorkRepository workRepository;
  private final QuotaAdapter quotaAdapter;
  private final ModerationAdapter moderationAdapter;
  private final PublishAdapter publishAdapter;
  private final MusicProviderRegistry musicProviderRegistry;
  private final MusicProviderSelection musicProviderSelection;
  private final ObjectStorageClient objectStorageClient;
  private final RemoteObjectImporter remoteObjectImporter;
  private final MusicPromptAgent musicPromptAgent;
  private final ModerationAgent moderationAgent;
  private final CoverPromptAgent coverPromptAgent;
  private final QualityEvaluationAgent qualityEvaluationAgent;
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
      ModerationAgent moderationAgent,
      CoverPromptAgent coverPromptAgent,
      QualityEvaluationAgent qualityEvaluationAgent,
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
    this.moderationAgent = moderationAgent;
    this.coverPromptAgent = coverPromptAgent;
    this.qualityEvaluationAgent = qualityEvaluationAgent;
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
    if (!markStage(workId, jobId, GenerationStage.MUSIC_GENERATING)) {
      releaseStaleQuota(workId, input.userId(), lock.lockId());
      return stale(workId, jobId, GenerationStage.MUSIC_GENERATING);
    }

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
    QualityEvaluationResult musicPromptQuality;
    try {
      musicPromptQuality = evaluateMusicPromptQuality(input, selectedProvider, musicPrompt);
    } catch (RuntimeException exception) {
      return fail(
          workId,
          jobId,
          FailureCode.MUSIC_QUALITY_FAILED,
          firstNonBlank(exception.getMessage(), "Music prompt quality evaluation failed"),
          input.musicRetryAllowedAfterFailure(),
          input.userId(),
          lock.lockId());
    }
    if (musicPromptQuality.decision() != QualityDecision.PASS) {
      return fail(
          workId,
          jobId,
          FailureCode.MUSIC_QUALITY_FAILED,
          qualityFailureMessage("Music prompt quality gate failed", musicPromptQuality),
          musicPromptQuality.retryable(),
          input.userId(),
          lock.lockId());
    }
    ModerationAgentResult musicPromptModeration;
    try {
      musicPromptModeration = preCheckMusicPrompt(input, selectedProvider, musicPrompt);
    } catch (RuntimeException exception) {
      return fail(
          workId,
          jobId,
          FailureCode.MUSIC_GENERATION_FAILED,
          firstNonBlank(exception.getMessage(), "Music prompt moderation failed"),
          input.musicRetryAllowedAfterFailure(),
          input.userId(),
          lock.lockId());
    }
    if (!musicPromptModeration.allowed()) {
      return fail(
          workId,
          jobId,
          FailureCode.MUSIC_GENERATION_FAILED,
          firstNonBlank(musicPromptModeration.message(), "Music prompt moderation blocked"),
          false,
          input.userId(),
          lock.lockId());
    }
    MusicGenerationRequest musicRequest =
        new MusicGenerationRequest(
            input.workId(),
            firstNonBlank(musicPrompt.lyricsWithStructureTags(), input.lyricsText()),
            musicPrompt.musicPrompt(),
            input.vocalPreference(),
            musicProviderOptions(input, musicPrompt));
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
      FailureCode musicFailureCode = musicFailureCode(musicResult);
      return fail(
          workId,
          jobId,
          musicFailureCode,
          firstNonBlank(musicResult.failureMessage(), "Music generation failed"),
          retryableMusicFailure(musicFailureCode, input),
          input.userId(),
          lock.lockId());
    }

    GeneratedMediaAssets mediaAssets;
    try {
      mediaAssets = createMediaAssets(workId, jobId, input, musicResult);
    } catch (StaleWorkflowException exception) {
      releaseStaleQuota(workId, input.userId(), lock.lockId());
      return stale(workId, jobId, exception.stage());
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

    QualityEvaluationResult packageQuality;
    try {
      if (!markStage(workId, jobId, GenerationStage.PACKAGE_BUILDING)) {
        releaseStaleQuota(workId, input.userId(), lock.lockId());
        return stale(workId, jobId, GenerationStage.PACKAGE_BUILDING);
      }
      packageQuality = evaluatePackageQuality(input, selectedProvider, mediaAssets);
    } catch (RuntimeException exception) {
      return fail(
          workId,
          jobId,
          FailureCode.PACKAGE_BUILD_FAILED,
          firstNonBlank(exception.getMessage(), "Package quality evaluation failed"),
          true,
          input.userId(),
          lock.lockId());
    }
    if (packageQuality.decision() != QualityDecision.PASS) {
      return fail(
          workId,
          jobId,
          FailureCode.PACKAGE_BUILD_FAILED,
          packageQualityFailureMessage(packageQuality),
          packageQuality.retryable(),
          input.userId(),
          lock.lockId());
    }

    PublishHandoff handoff;
    try {
      if (!markStage(workId, jobId, GenerationStage.PACKAGE_PRECHECK)) {
        releaseStaleQuota(workId, input.userId(), lock.lockId());
        return stale(workId, jobId, GenerationStage.PACKAGE_PRECHECK);
      }
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

  private boolean markStage(UUID workId, UUID jobId, GenerationStage stage) {
    return workRepository.markGenerationStage(workId, jobId, stage);
  }

  private SongProductionWorkflowResult stale(UUID workId, UUID jobId, GenerationStage stage) {
    workRepository.completeGenerationJob(
        jobId,
        "CANCELLED",
        stage,
        FailureCode.PROVIDER_TIMEOUT,
        "Song production stopped because the work changed");
    return SongProductionWorkflowResult.failed(
        jobId.toString(),
        PackageStatus.PACKAGE_NOT_READY.name(),
        FailureCode.PROVIDER_TIMEOUT.name(),
        "Song production stopped because the work changed");
  }

  private void releaseStaleQuota(UUID workId, String userId, String lockId) {
    if (lockId == null) {
      return;
    }
    QuotaRelease release = quotaAdapter.releaseGenerateQuota(userId, lockId, "WORK_CHANGED");
    workRepository.insertQuotaTransaction(
        workId,
        userId,
        lockId,
        "RELEASE_GENERATE",
        release.released() ? "RELEASED" : "FAILED",
        release.message());
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
        failurePackageStatus(failureCode).name(),
        failureCode.name(),
        safeFailureMessage);
  }

  private PackageStatus failurePackageStatus(FailureCode failureCode) {
    return failureCode == FailureCode.PACKAGE_BLOCKED
        ? PackageStatus.PACKAGE_BLOCKED
        : PackageStatus.PACKAGE_NOT_READY;
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

  private FailureCode musicFailureCode(MusicGenerationResult musicResult) {
    if (musicResult.failureCode() == null || musicResult.failureCode().isBlank()) {
      return FailureCode.MUSIC_GENERATION_FAILED;
    }
    try {
      return FailureCode.valueOf(musicResult.failureCode());
    } catch (IllegalArgumentException exception) {
      return FailureCode.MUSIC_GENERATION_FAILED;
    }
  }

  private boolean retryableMusicFailure(
      FailureCode failureCode, SongProductionWorkflowInput input) {
    return input.musicRetryAllowedAfterFailure()
        && switch (failureCode) {
          case MUSIC_GENERATION_FAILED, MUSIC_QUALITY_FAILED, PROVIDER_TIMEOUT, RATE_LIMITED ->
              true;
          default -> false;
        };
  }

  private GeneratedMediaAssets createMediaAssets(
      UUID workId,
      UUID jobId,
      SongProductionWorkflowInput input,
      MusicGenerationResult musicResult) {
    AudioObject audioObject = resolveAudioObject(workId, musicResult);
    MediaAssetRow audioAsset =
        new MediaAssetRow(
            workId,
            "AUDIO",
            audioObject.objectKey(),
            audioObject.contentType(),
            audioObject.sizeBytes(),
            audioObject.checksum(),
            null,
            null,
            musicResult.durationMs() == null ? 180_000 : musicResult.durationMs(),
            writeJson(audioObject.metadata()));
    workRepository.upsertMediaAsset(audioAsset);

    if (!markStage(workId, jobId, GenerationStage.COVER_GENERATING)) {
      throw new StaleWorkflowException(GenerationStage.COVER_GENERATING);
    }
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
    QualityEvaluationResult coverPromptQuality = evaluateCoverPromptQuality(input, coverPrompt);
    if (coverPromptQuality.decision() != QualityDecision.PASS) {
      throw new IllegalStateException(
          qualityFailureMessage("Cover prompt quality gate failed", coverPromptQuality));
    }
    MediaAssetDescriptor coverDescriptor = generateCoverOrDefault(workId, input, coverPrompt);
    MediaAssetRow coverAsset = toMediaAssetRow(workId, coverDescriptor);
    workRepository.upsertMediaAsset(coverAsset);

    if (!markStage(workId, jobId, GenerationStage.VIDEO_RENDERING)) {
      throw new StaleWorkflowException(GenerationStage.VIDEO_RENDERING);
    }
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

  private MediaAssetDescriptor generateCoverOrDefault(
      UUID workId, SongProductionWorkflowInput input, CoverPromptResult coverPrompt) {
    try {
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
                  coverProviderOptions(coverPrompt)));
      return materializeMockCoverIfNeeded(importRemoteCoverIfNeeded(coverResult.asset()));
    } catch (RuntimeException exception) {
      return defaultCoverAsset(workId, coverPrompt, exception);
    }
  }

  private QualityEvaluationResult evaluatePackageQuality(
      SongProductionWorkflowInput input,
      MusicProviderSelection selectedProvider,
      GeneratedMediaAssets mediaAssets) {
    return qualityEvaluationAgent.evaluate(
        new QualityEvaluationRequest(
            input.workId(),
            QualityGate.PUBLISH_PACKAGE,
            input.songTitle(),
            input.lyricsText(),
            selectedProvider.providerType().name(),
            mediaAssets.audioAsset().objectKey(),
            toLong(mediaAssets.audioAsset().durationMs()),
            mediaAssets.coverAsset().objectKey(),
            mediaAssets.coverAsset().width(),
            mediaAssets.coverAsset().height(),
            mediaAssets.videoAsset().objectKey(),
            mediaAssets.videoAsset().width(),
            mediaAssets.videoAsset().height(),
            toLong(mediaAssets.videoAsset().durationMs()),
            mediaAssets.timelineAsset().objectKey(),
            Map.of("workflow", "SongProductionWorkflow", "quality_gate", "publish_package")));
  }

  private QualityEvaluationResult evaluateMusicPromptQuality(
      SongProductionWorkflowInput input,
      MusicProviderSelection selectedProvider,
      MusicPromptResult musicPrompt) {
    return qualityEvaluationAgent.evaluate(
        new QualityEvaluationRequest(
            input.workId(),
            QualityGate.MUSIC,
            input.songTitle(),
            firstNonBlank(musicPrompt.lyricsWithStructureTags(), input.lyricsText()),
            selectedProvider.providerType().name(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Map.of(
                "workflow",
                "SongProductionWorkflow",
                "quality_gate",
                "music_prompt",
                "music_prompt",
                musicPrompt.musicPrompt(),
                "style_prompt",
                firstNonBlank(musicPrompt.stylePrompt(), ""),
                "exclude_prompt",
                firstNonBlank(musicPrompt.excludePrompt(), ""),
                "provider_options",
                musicPrompt.providerOptions(),
                "advanced_options",
                musicPrompt.advancedOptions())));
  }

  private QualityEvaluationResult evaluateCoverPromptQuality(
      SongProductionWorkflowInput input, CoverPromptResult coverPrompt) {
    return qualityEvaluationAgent.evaluate(
        new QualityEvaluationRequest(
            input.workId(),
            QualityGate.COVER,
            input.songTitle(),
            input.lyricsText(),
            null,
            null,
            null,
            null,
            coverPrompt.width(),
            coverPrompt.height(),
            null,
            null,
            null,
            null,
            null,
            Map.of(
                "workflow",
                "SongProductionWorkflow",
                "quality_gate",
                "cover_prompt",
                "visual_prompt",
                coverPrompt.visualPrompt(),
                "negative_prompt",
                coverPrompt.negativePrompt(),
                "text_prompt",
                firstNonBlank(coverPrompt.textPrompt(), ""),
                "style_constraints",
                coverPrompt.styleConstraints(),
                "typography_requirements",
                coverPrompt.typographyRequirements(),
                "provider_options",
                coverPrompt.providerOptions())));
  }

  private Map<String, Object> coverProviderOptions(CoverPromptResult coverPrompt) {
    Map<String, Object> options = new LinkedHashMap<>(coverPrompt.providerOptions());
    if (coverPrompt.textPrompt() != null && !coverPrompt.textPrompt().isBlank()) {
      options.putIfAbsent("text_prompt", coverPrompt.textPrompt());
    }
    if (!coverPrompt.typographyRequirements().isEmpty()) {
      options.putIfAbsent("typography_requirements", coverPrompt.typographyRequirements());
    }
    return options;
  }

  private ModerationAgentResult preCheckMusicPrompt(
      SongProductionWorkflowInput input,
      MusicProviderSelection selectedProvider,
      MusicPromptResult musicPrompt) {
    return moderationAgent.preCheck(
        new ModerationAgentRequest(
            input.workId(),
            ModerationTarget.MUSIC_PROMPT,
            musicPrompt.musicPrompt(),
            Map.of(
                "workflow",
                "SongProductionWorkflow",
                "music_provider",
                selectedProvider.providerType().name(),
                "stage",
                "before_music_provider")));
  }

  private String packageQualityFailureMessage(QualityEvaluationResult result) {
    return qualityFailureMessage("Package quality gate failed", result);
  }

  private String qualityFailureMessage(String prefix, QualityEvaluationResult result) {
    if (result != null && !result.reasons().isEmpty()) {
      return prefix + ": " + String.join("; ", result.reasons());
    }
    return prefix + ": " + (result == null ? "UNKNOWN" : result.decision().name());
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

  private AudioObject resolveAudioObject(UUID workId, MusicGenerationResult musicResult) {
    StoredObject importedAudio = importProviderAudio(workId, musicResult);
    if (importedAudio != null) {
      return new AudioObject(
          importedAudio.objectKey(),
          firstNonBlank(importedAudio.contentType(), "audio/mpeg"),
          importedAudio.sizeBytes(),
          "provider-audio",
          audioMetadata(musicResult, "provider-audio"));
    }
    if (musicResult.providerType() == MusicProviderType.MOCK) {
      String objectKey = firstNonBlank(musicResult.audioObjectKey(), "audio/" + workId + ".wav");
      String contentType =
          firstNonBlank(musicResult.audioContentType(), mockAudioContentType(objectKey));
      byte[] content =
          silentWav(musicResult.durationMs() == null ? 180_000 : musicResult.durationMs());
      StoredObject storedAudio =
          objectStorageClient.putObject(
              new ObjectStoragePutRequest(objectKey, contentType, content));
      long sizeBytes = storedAudio == null ? content.length : storedAudio.sizeBytes();
      return new AudioObject(
          objectKey,
          contentType,
          sizeBytes,
          "mock-audio",
          audioMetadata(musicResult, "mock-audio"));
    }
    return new AudioObject(
        firstNonBlank(musicResult.audioObjectKey(), "audio/" + workId + ".mp3"),
        firstNonBlank(musicResult.audioContentType(), "audio/mpeg"),
        4_800_000L,
        "provider-audio",
        audioMetadata(musicResult, "provider-audio"));
  }

  private Map<String, Object> audioMetadata(MusicGenerationResult musicResult, String source) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("source", source);
    metadata.put("provider", musicResult.providerType().name().toLowerCase(java.util.Locale.ROOT));
    metadata.put("model_name", firstNonBlank(musicResult.modelName(), "unknown"));
    if (musicResult.providerTaskId() != null && !musicResult.providerTaskId().isBlank()) {
      metadata.put("provider_task_id", musicResult.providerTaskId());
    }
    putSafeProviderMetadata(metadata, musicResult.metadata(), "provider_audio_id");
    putSafeProviderMetadata(metadata, musicResult.metadata(), "timestamped_lyrics_lookup");
    return metadata;
  }

  private void putSafeProviderMetadata(
      Map<String, Object> metadata, Map<String, Object> providerMetadata, String key) {
    Object value = providerMetadata.get(key);
    if (value != null) {
      metadata.put(key, value);
    }
  }

  private String mockAudioContentType(String objectKey) {
    return objectKey != null && objectKey.toLowerCase(java.util.Locale.ROOT).endsWith(".wav")
        ? "audio/wav"
        : "audio/mpeg";
  }

  private byte[] silentWav(int durationMs) {
    int safeDurationMs = Math.max(durationMs, 1);
    int sampleCount = Math.max(1, (int) (((long) MOCK_AUDIO_SAMPLE_RATE * safeDurationMs) / 1000L));
    int bytesPerSample = MOCK_AUDIO_BITS_PER_SAMPLE / 8;
    int dataSize = sampleCount * MOCK_AUDIO_CHANNELS * bytesPerSample;
    int totalSize = 44 + dataSize;
    byte[] wav = new byte[totalSize];
    writeAscii(wav, 0, "RIFF");
    writeLittleEndianInt(wav, 4, totalSize - 8);
    writeAscii(wav, 8, "WAVE");
    writeAscii(wav, 12, "fmt ");
    writeLittleEndianInt(wav, 16, 16);
    writeLittleEndianShort(wav, 20, (short) 1);
    writeLittleEndianShort(wav, 22, MOCK_AUDIO_CHANNELS);
    writeLittleEndianInt(wav, 24, MOCK_AUDIO_SAMPLE_RATE);
    writeLittleEndianInt(wav, 28, MOCK_AUDIO_SAMPLE_RATE * MOCK_AUDIO_CHANNELS * bytesPerSample);
    writeLittleEndianShort(wav, 32, (short) (MOCK_AUDIO_CHANNELS * bytesPerSample));
    writeLittleEndianShort(wav, 34, MOCK_AUDIO_BITS_PER_SAMPLE);
    writeAscii(wav, 36, "data");
    writeLittleEndianInt(wav, 40, dataSize);
    return wav;
  }

  private void writeAscii(byte[] target, int offset, String value) {
    byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
    System.arraycopy(bytes, 0, target, offset, bytes.length);
  }

  private void writeLittleEndianInt(byte[] target, int offset, int value) {
    target[offset] = (byte) (value & 0xff);
    target[offset + 1] = (byte) ((value >>> 8) & 0xff);
    target[offset + 2] = (byte) ((value >>> 16) & 0xff);
    target[offset + 3] = (byte) ((value >>> 24) & 0xff);
  }

  private void writeLittleEndianShort(byte[] target, int offset, short value) {
    target[offset] = (byte) (value & 0xff);
    target[offset + 1] = (byte) ((value >>> 8) & 0xff);
  }

  private MediaAssetDescriptor importRemoteCoverIfNeeded(MediaAssetDescriptor asset) {
    Object sourceUrl = asset.metadata().get(CoverGenerationService.SOURCE_URL_METADATA_KEY);
    if (sourceUrl instanceof String value && !value.isBlank()) {
      StoredObject importedCover =
          remoteObjectImporter.importObject(
              new RemoteObjectImportRequest(value, asset.objectKey(), asset.mimeType()));
      Map<String, Object> metadata = new LinkedHashMap<>(asset.metadata());
      metadata.remove(CoverGenerationService.SOURCE_URL_METADATA_KEY);
      metadata.put("object_storage_imported", true);
      return new MediaAssetDescriptor(
          asset.assetType(),
          importedCover.objectKey(),
          importedCover.contentType(),
          importedCover.sizeBytes(),
          asset.checksum(),
          asset.width(),
          asset.height(),
          asset.durationMs(),
          metadata);
    }
    Object inlineBase64 = asset.metadata().get(CoverGenerationService.INLINE_BASE64_METADATA_KEY);
    if (!(inlineBase64 instanceof String value) || value.isBlank()) {
      return asset;
    }
    StoredObject importedCover =
        objectStorageClient.putObject(
            new ObjectStoragePutRequest(
                asset.objectKey(), asset.mimeType(), Base64.getDecoder().decode(value)));
    Map<String, Object> metadata = new LinkedHashMap<>(asset.metadata());
    metadata.remove(CoverGenerationService.INLINE_BASE64_METADATA_KEY);
    metadata.put("object_storage_imported", true);
    metadata.put("object_storage_import_source", "inline_base64");
    return new MediaAssetDescriptor(
        asset.assetType(),
        importedCover.objectKey(),
        importedCover.contentType(),
        importedCover.sizeBytes(),
        asset.checksum(),
        asset.width(),
        asset.height(),
        asset.durationMs(),
        metadata);
  }

  private MediaAssetDescriptor materializeMockCoverIfNeeded(MediaAssetDescriptor asset) {
    if (!"mock-image2".equals(asset.metadata().get("provider"))) {
      return asset;
    }
    byte[] content = mockCoverPng(asset.width(), asset.height());
    StoredObject storedCover =
        objectStorageClient.putObject(
            new ObjectStoragePutRequest(asset.objectKey(), asset.mimeType(), content));
    Map<String, Object> metadata = new LinkedHashMap<>(asset.metadata());
    metadata.put("object_storage_imported", true);
    metadata.put("object_storage_import_source", "mock_cover");
    long sizeBytes = storedCover == null ? content.length : storedCover.sizeBytes();
    return new MediaAssetDescriptor(
        asset.assetType(),
        asset.objectKey(),
        asset.mimeType(),
        sizeBytes,
        sha256(content),
        asset.width(),
        asset.height(),
        asset.durationMs(),
        metadata);
  }

  private byte[] mockCoverPng(Integer width, Integer height) {
    int safeWidth = width == null || width <= 0 ? 1920 : width;
    int safeHeight = height == null || height <= 0 ? 1080 : height;
    BufferedImage image = new BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setPaint(
          new GradientPaint(
              0, 0, new Color(18, 20, 27), safeWidth, safeHeight, new Color(45, 42, 37)));
      graphics.fillRect(0, 0, safeWidth, safeHeight);

      graphics.setColor(new Color(198, 163, 91, 150));
      graphics.fillOval(
          (int) (safeWidth * 0.68),
          (int) (safeHeight * 0.16),
          (int) (safeWidth * 0.12),
          (int) (safeWidth * 0.12));

      Path2D.Double ridge = new Path2D.Double();
      ridge.moveTo(safeWidth * 0.08, safeHeight * 0.72);
      ridge.curveTo(
          safeWidth * 0.24,
          safeHeight * 0.57,
          safeWidth * 0.35,
          safeHeight * 0.64,
          safeWidth * 0.47,
          safeHeight * 0.49);
      ridge.curveTo(
          safeWidth * 0.60,
          safeHeight * 0.34,
          safeWidth * 0.76,
          safeHeight * 0.40,
          safeWidth * 0.91,
          safeHeight * 0.25);
      graphics.setStroke(
          new BasicStroke(
              Math.max(8f, safeWidth / 120f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      graphics.setColor(new Color(129, 143, 153, 190));
      graphics.draw(ridge);

      graphics.setStroke(new BasicStroke(Math.max(2f, safeWidth / 480f)));
      graphics.setColor(new Color(231, 218, 184, 85));
      graphics.drawRect(
          (int) (safeWidth * 0.055),
          (int) (safeHeight * 0.09),
          (int) (safeWidth * 0.89),
          (int) (safeHeight * 0.82));
    } finally {
      graphics.dispose();
    }
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      ImageIO.write(image, "png", output);
      return output.toByteArray();
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Failed to write mock cover PNG", exception);
    }
  }

  private MediaAssetDescriptor defaultCoverAsset(
      UUID workId, CoverPromptResult coverPrompt, RuntimeException cause) {
    byte[] content = defaultCoverSvg().getBytes(StandardCharsets.UTF_8);
    String objectKey = "covers/" + workId + "-default.svg";
    StoredObject storedCover =
        objectStorageClient.putObject(
            new ObjectStoragePutRequest(objectKey, "image/svg+xml", content));
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("provider", "default-cover");
    metadata.put("fallback", true);
    metadata.put(
        "fallback_reason",
        firstNonBlank(sanitizeProviderError(cause.getMessage()), "cover generation unavailable"));
    metadata.put("fallback_error_type", cause.getClass().getSimpleName());
    metadata.put("fallback_error_location", firstStackFrame(cause));
    metadata.put("visual_prompt_hash", sha256(coverPrompt.visualPrompt()));
    metadata.put("negative_prompt_hash", sha256(coverPrompt.negativePrompt()));
    metadata.put("width", coverPrompt.width());
    metadata.put("height", coverPrompt.height());
    metadata.put("object_storage_imported", true);
    metadata.put("object_storage_import_source", "default_cover");
    return new MediaAssetDescriptor(
        "COVER",
        storedCover.objectKey(),
        storedCover.contentType(),
        storedCover.sizeBytes(),
        sha256(content),
        coverPrompt.width(),
        coverPrompt.height(),
        null,
        metadata);
  }

  private String defaultCoverSvg() {
    return """
        <svg xmlns="http://www.w3.org/2000/svg" width="1920" height="1080" viewBox="0 0 1920 1080">
          <rect width="1920" height="1080" fill="#16181f"/>
          <rect x="96" y="96" width="1728" height="888" fill="none" stroke="#c6a35b" stroke-width="8"/>
          <path d="M240 760 C520 560 720 620 960 460 C1210 295 1430 350 1680 220" fill="none" stroke="#7f8f99" stroke-width="18" opacity="0.72"/>
          <circle cx="1480" cy="260" r="88" fill="#c6a35b" opacity="0.78"/>
          <text x="160" y="205" font-family="serif" font-size="72" fill="#e9dfc3">Yanyun AI Music</text>
          <text x="160" y="300" font-family="serif" font-size="40" fill="#aeb7bb">Default cover fallback</text>
        </svg>
        """;
  }

  private String firstStackFrame(RuntimeException cause) {
    if (cause.getStackTrace().length == 0) {
      return "unknown";
    }
    StackTraceElement frame = cause.getStackTrace()[0];
    return frame.getClassName() + "#" + frame.getMethodName();
  }

  private MusicProviderSelection selectedProvider(SongProductionWorkflowInput input) {
    return input.musicProvider() == null || input.musicProvider().isBlank()
        ? musicProviderSelection
        : MusicProviderSelection.fromConfig(input.musicProvider());
  }

  private Map<String, Object> musicProviderOptions(
      SongProductionWorkflowInput input, MusicPromptResult musicPrompt) {
    Map<String, Object> options = new LinkedHashMap<>(musicPrompt.providerOptions());
    if (input.songTitle() != null && !input.songTitle().isBlank()) {
      options.putIfAbsent("title", input.songTitle());
    }
    if (input.songSummary() != null && !input.songSummary().isBlank()) {
      options.putIfAbsent("song_summary", input.songSummary());
    }
    if (musicPrompt.excludePrompt() != null && !musicPrompt.excludePrompt().isBlank()) {
      options.putIfAbsent("exclude_prompt", musicPrompt.excludePrompt());
    }
    if (!musicPrompt.advancedOptions().isEmpty()) {
      options.putIfAbsent("advanced_options", musicPrompt.advancedOptions());
    }
    return options;
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
    sanitized = URL_PATTERN.matcher(sanitized).replaceAll("<url-redacted>");
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
        "audio",
        Map.of(
            "url",
            assetUrl(mediaAssets.audioAsset()),
            "mime_type",
            mediaAssets.audioAsset().mimeType(),
            "file_size_bytes",
            mediaAssets.audioAsset().fileSizeBytes(),
            "checksum",
            mediaAssets.audioAsset().checksum()),
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

  private Long toLong(Integer value) {
    return value == null ? null : value.longValue();
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

  private String sha256(byte[] value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value == null ? new byte[0] : value));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 digest is unavailable", exception);
    }
  }

  private record GeneratedMediaAssets(
      MediaAssetRow audioAsset,
      MediaAssetRow coverAsset,
      MediaAssetRow videoAsset,
      MediaAssetRow timelineAsset) {}

  private static final class StaleWorkflowException extends RuntimeException {
    private final GenerationStage stage;

    private StaleWorkflowException(GenerationStage stage) {
      super("Song production stopped because the work changed");
      this.stage = stage;
    }

    private GenerationStage stage() {
      return stage;
    }
  }

  private record AudioObject(
      String objectKey,
      String contentType,
      long sizeBytes,
      String checksum,
      Map<String, Object> metadata) {}
}
