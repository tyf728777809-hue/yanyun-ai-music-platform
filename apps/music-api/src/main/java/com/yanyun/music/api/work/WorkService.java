package com.yanyun.music.api.work;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.api.work.WorkDtos.ConfirmWorkRequest;
import com.yanyun.music.api.work.WorkDtos.CreateWorkResponse;
import com.yanyun.music.api.work.WorkDtos.FailureInfo;
import com.yanyun.music.api.work.WorkDtos.InspirationCreateRequest;
import com.yanyun.music.api.work.WorkDtos.JobAcceptedResponse;
import com.yanyun.music.api.work.WorkDtos.LyricsContinueRequest;
import com.yanyun.music.api.work.WorkDtos.LyricsCreateRequest;
import com.yanyun.music.api.work.WorkDtos.LyricsDraft;
import com.yanyun.music.api.work.WorkDtos.LyricsPolishRequest;
import com.yanyun.music.api.work.WorkDtos.MediaAssets;
import com.yanyun.music.api.work.WorkDtos.Pagination;
import com.yanyun.music.api.work.WorkDtos.PublishHandoffHint;
import com.yanyun.music.api.work.WorkDtos.PublishPackage;
import com.yanyun.music.api.work.WorkDtos.QuotaHint;
import com.yanyun.music.api.work.WorkDtos.RetryMusicRequest;
import com.yanyun.music.api.work.WorkDtos.WorkDetail;
import com.yanyun.music.api.work.WorkDtos.WorkListResponse;
import com.yanyun.music.api.work.WorkDtos.WorkSummary;
import com.yanyun.music.api.workflow.WorkflowDispatchProperties;
import com.yanyun.music.api.workflow.WorkflowOutboxService;
import com.yanyun.music.dreammaker.DreamMakerProperties;
import com.yanyun.music.lyrics.LyricsGenerationRequest;
import com.yanyun.music.lyrics.LyricsGenerationResult;
import com.yanyun.music.lyrics.LyricsGenerationService;
import com.yanyun.music.lyrics.LyricsOperation;
import com.yanyun.music.moderation.ModerationAdapter;
import com.yanyun.music.moderation.ModerationDecision;
import com.yanyun.music.musicprovider.MusicProviderSelection;
import com.yanyun.music.musicprovider.MusicProviderType;
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
import com.yanyun.music.workdomain.WorkSnapshot;
import com.yanyun.music.workdomain.WorkStateMachine;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkService {

  private static final int POLISH_LIMIT = 2;
  private static final int MUSIC_RETRY_LIMIT = 2;
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
  private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};

  private final WorkRepository workRepository;
  private final QuotaAdapter quotaAdapter;
  private final ModerationAdapter moderationAdapter;
  private final ObjectStorageClient objectStorageClient;
  private final LyricsGenerationService lyricsGenerationService;
  private final SongProductionWorkflow songProductionWorkflow;
  private final WorkflowDispatchProperties workflowDispatchProperties;
  private final WorkflowOutboxService workflowOutboxService;
  private final MusicProviderSelection configuredMusicProvider;
  private final DreamMakerProperties dreamMakerProperties;
  private final YunwuProperties yunwuProperties;
  private final String sunoBackend;
  private final ObjectMapper objectMapper;

  public WorkService(
      WorkRepository workRepository,
      QuotaAdapter quotaAdapter,
      ModerationAdapter moderationAdapter,
      ObjectStorageClient objectStorageClient,
      LyricsGenerationService lyricsGenerationService,
      SongProductionWorkflow songProductionWorkflow,
      WorkflowDispatchProperties workflowDispatchProperties,
      WorkflowOutboxService workflowOutboxService,
      MusicProviderSelection configuredMusicProvider,
      DreamMakerProperties dreamMakerProperties,
      YunwuProperties yunwuProperties,
      @Value("${yanyun.suno.backend:yunwu}") String sunoBackend,
      ObjectMapper objectMapper) {
    this.workRepository = workRepository;
    this.quotaAdapter = quotaAdapter;
    this.moderationAdapter = moderationAdapter;
    this.objectStorageClient = objectStorageClient;
    this.lyricsGenerationService = lyricsGenerationService;
    this.songProductionWorkflow = songProductionWorkflow;
    this.workflowDispatchProperties = workflowDispatchProperties;
    this.workflowOutboxService = workflowOutboxService;
    this.configuredMusicProvider = configuredMusicProvider;
    this.dreamMakerProperties = dreamMakerProperties;
    this.yunwuProperties = yunwuProperties == null ? new YunwuProperties() : yunwuProperties;
    this.sunoBackend = sunoBackend == null || sunoBackend.isBlank() ? "yunwu" : sunoBackend.trim();
    this.objectMapper = objectMapper;
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public CreateWorkResponse createFromInspiration(String userId, InspirationCreateRequest request) {
    requireText(request == null ? null : request.storyInput(), "story_input is required");
    ModerationDecision decision = moderationAdapter.preCheckUserInput(userId, request.storyInput());
    requireAllowed(decision, FailureCode.USER_INPUT_BLOCKED);

    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    LyricsGenerationResult lyrics =
        lyricsGenerationService.generate(
            new LyricsGenerationRequest(
                userId,
                workId.toString(),
                LyricsOperation.INSPIRATION,
                request.storyInput(),
                null,
                null,
                null,
                request.musicStyle(),
                request.vocalPreference()));
    UUID jobId =
        createWorkWithDraft(
            workId,
            draftId,
            userId,
            CreationMode.INSPIRATION,
            lyrics,
            request.storyInput(),
            null,
            request.mood(),
            request.scene(),
            request.relationship(),
            request.musicStyle(),
            request.vocalPreference());

    WorkRow work = getRequiredWork(workId, userId);
    return new CreateWorkResponse(
        work.id(),
        work.workCode(),
        work.status(),
        work.generationStage(),
        jobId,
        quotaHint(work),
        availableActions(work));
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public CreateWorkResponse createFromLyrics(String userId, LyricsCreateRequest request) {
    requireText(request == null ? null : request.lyricsInput(), "lyrics_input is required");
    ModerationDecision decision = moderationAdapter.preCheckLyrics(userId, request.lyricsInput());
    requireAllowed(decision, FailureCode.LYRICS_PRECHECK_FAILED);

    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    LyricsGenerationResult lyrics =
        lyricsGenerationService.generate(
            new LyricsGenerationRequest(
                userId,
                workId.toString(),
                LyricsOperation.LYRICS,
                request.lyricsInput(),
                null,
                null,
                request.songTitle(),
                request.musicStyle(),
                request.vocalPreference()));
    UUID jobId =
        createWorkWithDraft(
            workId,
            draftId,
            userId,
            CreationMode.LYRICS,
            lyrics,
            null,
            request.lyricsInput(),
            null,
            null,
            null,
            request.musicStyle(),
            request.vocalPreference());

    WorkRow work = getRequiredWork(workId, userId);
    return new CreateWorkResponse(
        work.id(),
        work.workCode(),
        work.status(),
        work.generationStage(),
        jobId,
        quotaHint(work),
        availableActions(work));
  }

  @Transactional(readOnly = true)
  public WorkListResponse listWorks(String userId, WorkStatus status, int page, int pageSize) {
    int safePage = Math.max(1, page);
    int safePageSize = Math.min(Math.max(1, pageSize), 50);
    int offset = (safePage - 1) * safePageSize;
    long totalItems = workRepository.countWorks(userId, status);
    int totalPages = (int) Math.ceil(totalItems / (double) safePageSize);
    List<WorkSummary> items =
        workRepository.listWorks(userId, status, safePageSize, offset).stream()
            .map(this::toSummary)
            .toList();
    return new WorkListResponse(
        items, new Pagination(safePage, safePageSize, totalItems, Math.max(totalPages, 1)));
  }

  @Transactional(readOnly = true)
  public WorkDetail getWork(String userId, UUID workId) {
    return toDetail(getRequiredWork(workId, userId));
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public JobAcceptedResponse polishLyrics(String userId, UUID workId, LyricsPolishRequest request) {
    requireText(request == null ? null : request.instruction(), "instruction is required");
    WorkRow work = getRequiredWork(workId, userId);
    if (!WorkStateMachine.canEditLyrics(work.status())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Current work cannot edit lyrics");
    }
    if (work.polishUsedCount() >= POLISH_LIMIT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "No remaining polish attempts");
    }

    LyricsDraftRow latest = getRequiredLyricsDraft(workId);
    LyricsGenerationResult lyrics =
        lyricsGenerationService.generate(
            new LyricsGenerationRequest(
                userId,
                workId.toString(),
                LyricsOperation.POLISH,
                null,
                latest.lyricsText(),
                request.instruction(),
                latest.songTitle(),
                latest.musicPrompt(),
                null));
    UUID jobId = replaceLyricsDraft(work, lyrics, "LYRICS_POLISH", true);
    WorkRow updated = getRequiredWork(workId, userId);
    return accepted(updated, jobId);
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public JobAcceptedResponse continueLyrics(
      String userId, UUID workId, LyricsContinueRequest request) {
    WorkRow work = getRequiredWork(workId, userId);
    if (!WorkStateMachine.canEditLyrics(work.status())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Current work cannot edit lyrics");
    }
    if (work.polishUsedCount() >= POLISH_LIMIT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "No remaining AI edit attempts");
    }

    LyricsDraftRow latest = getRequiredLyricsDraft(workId);
    String instruction =
        request == null || request.instruction() == null || request.instruction().isBlank()
            ? "Continue current lyrics."
            : request.instruction().trim();
    LyricsGenerationResult lyrics =
        lyricsGenerationService.generate(
            new LyricsGenerationRequest(
                userId,
                workId.toString(),
                LyricsOperation.CONTINUE,
                null,
                latest.lyricsText(),
                instruction,
                latest.songTitle(),
                latest.musicPrompt(),
                null));
    UUID jobId = replaceLyricsDraft(work, lyrics, "LYRICS_CONTINUE", true);
    WorkRow updated = getRequiredWork(workId, userId);
    return accepted(updated, jobId);
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public JobAcceptedResponse confirmWork(String userId, UUID workId, ConfirmWorkRequest request) {
    WorkRow work = getRequiredWork(workId, userId);
    if (!WorkStateMachine.canConfirm(work.status())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only lyrics-ready works can be confirmed");
    }
    LyricsDraftRow draft = getRequiredLyricsDraft(workId);

    String selectedMusicProvider = musicProvider(request == null ? null : request.musicProvider());
    requireSafeRealMusicDispatch(selectedMusicProvider);

    if (workflowDispatchProperties.outboxMode()) {
      return enqueueSongProduction(work, draft, selectedMusicProvider, true);
    }

    SongProductionWorkflowResult workflowResult =
        songProductionWorkflow.produce(
            workflowInput(workId, userId, draft, selectedMusicProvider, true));
    if (!workflowResult.packageReady()) {
      throw workflowFailure(workflowResult);
    }

    WorkRow updated = getRequiredWork(workId, userId);
    return accepted(updated, UUID.fromString(workflowResult.jobId()));
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public JobAcceptedResponse retryMusic(String userId, UUID workId, RetryMusicRequest request) {
    WorkRow work = getRequiredWork(workId, userId);
    if (!WorkStateMachine.canRetryMusic(workSnapshot(work))) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Current work cannot retry music generation");
    }

    LyricsDraftRow draft = getRequiredLyricsDraft(workId);
    String selectedMusicProvider = musicProvider(request == null ? null : request.musicProvider());
    requireSafeRealMusicDispatch(selectedMusicProvider);
    if (!workRepository.reserveMusicRetry(workId, userId, work.version(), MUSIC_RETRY_LIMIT)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Music retry is already running or retry limit reached");
    }
    boolean retryAllowedAfterFailure = work.musicRetryCount() + 1 < MUSIC_RETRY_LIMIT;
    if (workflowDispatchProperties.outboxMode()) {
      return enqueueReservedSongProduction(
          workId, userId, draft, selectedMusicProvider, retryAllowedAfterFailure);
    }
    SongProductionWorkflowResult workflowResult =
        songProductionWorkflow.produce(
            workflowInput(workId, userId, draft, selectedMusicProvider, retryAllowedAfterFailure));
    if (!workflowResult.packageReady()) {
      throw workflowFailure(workflowResult);
    }

    WorkRow updated = getRequiredWork(workId, userId);
    return accepted(updated, UUID.fromString(workflowResult.jobId()));
  }

  private JobAcceptedResponse enqueueSongProduction(
      WorkRow work,
      LyricsDraftRow draft,
      String selectedMusicProvider,
      boolean musicRetryAllowedAfterFailure) {
    if (!workRepository.reserveSongProduction(work.id(), work.userId(), work.version())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Song production is already running or work changed");
    }
    return enqueueReservedSongProduction(
        work.id(), work.userId(), draft, selectedMusicProvider, musicRetryAllowedAfterFailure);
  }

  private JobAcceptedResponse enqueueReservedSongProduction(
      UUID workId,
      String userId,
      LyricsDraftRow draft,
      String selectedMusicProvider,
      boolean musicRetryAllowedAfterFailure) {
    UUID jobId = UUID.randomUUID();
    workRepository.insertGenerationJob(
        jobId,
        workId,
        "SONG_PRODUCTION",
        "RUNNING",
        GenerationStage.QUOTA_LOCKING,
        OffsetDateTime.now(),
        null);
    workflowOutboxService.enqueueSongProduction(
        workId,
        workflowInput(
            workId, userId, draft, selectedMusicProvider, musicRetryAllowedAfterFailure, jobId));
    return accepted(getRequiredWork(workId, userId), jobId);
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public JobAcceptedResponse regenerateCover(String userId, UUID workId) {
    WorkRow work = getRequiredWork(workId, userId);
    if (work.status() != WorkStatus.GENERATED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only generated works can regenerate cover");
    }
    workRepository.upsertMediaAsset(
        new MediaAssetRow(
            workId,
            "COVER",
            "covers/" + workId + "-regen.png",
            "image/png",
            512_000L,
            "mock-cover-regen",
            1080,
            1920,
            null,
            "{}"));
    UUID jobId =
        workRepository.insertGenerationJob(
            workId,
            "COVER_REGENERATION",
            "SUCCEEDED",
            GenerationStage.PACKAGE_READY,
            OffsetDateTime.now(),
            OffsetDateTime.now());
    return accepted(getRequiredWork(workId, userId), jobId);
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public JobAcceptedResponse rerenderVideo(String userId, UUID workId) {
    WorkRow work = getRequiredWork(workId, userId);
    if (work.status() != WorkStatus.GENERATED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only generated works can rerender video");
    }
    workRepository.upsertMediaAsset(
        new MediaAssetRow(
            workId,
            "VIDEO",
            "videos/" + workId + "-rerender.mp4",
            "video/mp4",
            12_800_000L,
            "mock-video-rerender",
            1080,
            1920,
            180_000,
            "{}"));
    UUID jobId =
        workRepository.insertGenerationJob(
            workId,
            "VIDEO_RERENDER",
            "SUCCEEDED",
            GenerationStage.PACKAGE_READY,
            OffsetDateTime.now(),
            OffsetDateTime.now());
    return accepted(getRequiredWork(workId, userId), jobId);
  }

  @Transactional(readOnly = true)
  public PublishPackage getPublishPackage(String userId, UUID workId) {
    WorkRow work = getRequiredWork(workId, userId);
    Optional<PublishPackageRow> publishPackage = workRepository.findPublishPackage(workId);
    return publishPackage
        .map(row -> toPublishPackage(work, row))
        .orElseGet(() -> emptyPackage(work));
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public PublishPackage markPublishPackageFetched(String userId, UUID workId) {
    WorkRow work = getRequiredWork(workId, userId);
    if (work.packageStatus() != PackageStatus.PACKAGE_READY) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "作品尚未准备好，暂不能交给社区发布。");
    }
    workRepository.markPackageFetched(workId);
    return getPublishPackage(userId, workId);
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public PublishPackage refreshPublishPackageUrl(String userId, UUID workId) {
    WorkRow work = getRequiredWork(workId, userId);
    if (work.status() != WorkStatus.GENERATED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only generated works can refresh package URL");
    }
    PublishPackageRow packageRow =
        workRepository
            .findPublishPackage(workId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Publish package not found"));
    if (packageRow.packageObjectKey() == null || packageRow.packageObjectKey().isBlank()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Publish package not found");
    }
    String refreshedPackageJson = refreshedPackageJson(workId, packageRow);
    objectStorageClient.putObject(
        new ObjectStoragePutRequest(
            packageRow.packageObjectKey(),
            "application/json",
            refreshedPackageJson.getBytes(StandardCharsets.UTF_8)));
    ObjectStorageDownloadUrl downloadUrl =
        objectStorageClient.createDownloadUrl(packageRow.packageObjectKey());
    workRepository.updatePublishPackageUrl(
        workId, refreshedPackageJson, downloadUrl.url(), downloadUrl.expiresAt());
    return getPublishPackage(userId, workId);
  }

  private SongProductionWorkflowInput workflowInput(
      UUID workId,
      String userId,
      LyricsDraftRow draft,
      String musicProvider,
      boolean musicRetryAllowedAfterFailure) {
    return workflowInput(workId, userId, draft, musicProvider, musicRetryAllowedAfterFailure, null);
  }

  private SongProductionWorkflowInput workflowInput(
      UUID workId,
      String userId,
      LyricsDraftRow draft,
      String musicProvider,
      boolean musicRetryAllowedAfterFailure,
      UUID jobId) {
    return new SongProductionWorkflowInput(
        workId.toString(),
        userId,
        draft.id().toString(),
        draft.songTitle(),
        draft.songSummary(),
        draft.lyricsText(),
        draft.musicPrompt(),
        draft.coverPromptSeed(),
        "AUTO",
        musicProvider,
        musicRetryAllowedAfterFailure,
        jobId == null ? null : jobId.toString());
  }

  private String musicProvider(String requestedProvider) {
    if (requestedProvider == null || requestedProvider.isBlank()) {
      return null;
    }
    try {
      return MusicProviderSelection.fromConfig(requestedProvider)
          .providerType()
          .name()
          .toLowerCase(Locale.ROOT);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
    }
  }

  private void requireSafeRealMusicDispatch(String selectedMusicProvider) {
    MusicProviderType effectiveProvider = effectiveMusicProvider(selectedMusicProvider);
    if (workflowDispatchProperties.outboxMode()
        && workflowDispatchProperties.getOutbox().getDispatchTarget()
            == WorkflowDispatchProperties.DispatchTarget.LOCAL
        && effectiveProvider != MusicProviderType.MOCK) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "非 Mock 音乐供应商不能使用 outbox + local 调度，请切换到 outbox + Temporal。");
    }
    if (effectiveProvider == MusicProviderType.MOCK || !realMusicCallsEnabled(effectiveProvider)) {
      return;
    }
    boolean temporalOutbox =
        workflowDispatchProperties.outboxMode()
            && workflowDispatchProperties.getOutbox().getDispatchTarget()
                == WorkflowDispatchProperties.DispatchTarget.TEMPORAL;
    if (!temporalOutbox) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "真实音乐联调必须使用 outbox + Temporal worker，不能在同步 API 线程中执行。");
    }
  }

  private boolean realMusicCallsEnabled(MusicProviderType providerType) {
    return switch (providerType) {
      case SUNO ->
          "dreammaker".equals(normalize(sunoBackend))
              ? dreamMakerProperties.isRealCallsEnabled()
              : yunwuProperties.isRealCallsEnabled();
      case MINIMAX -> dreamMakerProperties.isRealCallsEnabled();
      case MOCK -> false;
    };
  }

  private MusicProviderType effectiveMusicProvider(String selectedMusicProvider) {
    if (selectedMusicProvider == null || selectedMusicProvider.isBlank()) {
      return configuredMusicProvider.providerType();
    }
    return MusicProviderSelection.fromConfig(selectedMusicProvider).providerType();
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private UUID createWorkWithDraft(
      UUID workId,
      UUID draftId,
      String userId,
      CreationMode creationMode,
      LyricsGenerationResult lyrics,
      String storyInput,
      String lyricsInput,
      String mood,
      String scene,
      String relationship,
      String musicStyle,
      String vocalPreference) {
    WorkRow work =
        new WorkRow(
            workId,
            generateWorkCode(),
            userId,
            creationMode,
            WorkStatus.LYRICS_READY,
            GenerationStage.WAITING_CONFIRM,
            PackageStatus.PACKAGE_NOT_READY,
            firstNonBlank(lyrics.songTitle(), "Yanyun Lyrics"),
            firstNonBlank(lyrics.songSummary(), "Yanyun lyrics draft."),
            0,
            0,
            null,
            null,
            null,
            null,
            false,
            false,
            0,
            null,
            null,
            null,
            0);
    workRepository.insertWork(work, "MockUser", false, false);
    Map<String, Object> inputSnapshot = new HashMap<>();
    inputSnapshot.put("creation_mode", creationMode.name());
    inputSnapshot.put("story_input", storyInput);
    inputSnapshot.put("lyrics_input", lyricsInput);
    inputSnapshot.put("music_style", musicStyle);
    inputSnapshot.put("vocal_preference", vocalPreference);

    workRepository.insertWorkInput(
        workId,
        storyInput,
        lyricsInput,
        mood,
        scene,
        relationship,
        musicStyle,
        vocalPreference,
        writeJson(inputSnapshot));
    workRepository.insertLyricsDraft(
        new LyricsDraftRow(
            draftId,
            workId,
            1,
            firstNonBlank(lyrics.songTitle(), "Yanyun Lyrics"),
            firstNonBlank(lyrics.songSummary(), "Yanyun lyrics draft."),
            lyrics.lyricsText(),
            lyrics.musicPrompt(),
            writeJson(lyrics.riskNotes()),
            writeJson(lyrics.yanyunReferences()),
            lyrics.coverPromptSeed(),
            lyrics.qualityScore(),
            lyrics.knowledgeBaseVersion(),
            writeJson(lyrics.promptTemplateVersions()),
            null));
    return workRepository.insertGenerationJob(
        workId,
        creationMode == CreationMode.INSPIRATION ? "LYRICS_GENERATION" : "LYRICS_PROCESSING",
        "SUCCEEDED",
        GenerationStage.WAITING_CONFIRM,
        OffsetDateTime.now(),
        OffsetDateTime.now());
  }

  private UUID replaceLyricsDraft(
      WorkRow work, LyricsGenerationResult lyrics, String jobType, boolean incrementEditCount) {
    UUID draftId = UUID.randomUUID();
    String title = firstNonBlank(lyrics.songTitle(), work.songTitle());
    String summary = firstNonBlank(lyrics.songSummary(), work.songSummary());
    workRepository.insertLyricsDraft(
        new LyricsDraftRow(
            draftId,
            work.id(),
            workRepository.nextLyricsVersion(work.id()),
            title,
            summary,
            lyrics.lyricsText(),
            lyrics.musicPrompt(),
            writeJson(lyrics.riskNotes()),
            writeJson(lyrics.yanyunReferences()),
            lyrics.coverPromptSeed(),
            lyrics.qualityScore(),
            lyrics.knowledgeBaseVersion(),
            writeJson(lyrics.promptTemplateVersions()),
            null));
    workRepository.markLyricsReady(work.id(), title, summary, incrementEditCount);
    return workRepository.insertGenerationJob(
        work.id(),
        jobType,
        "SUCCEEDED",
        GenerationStage.WAITING_CONFIRM,
        OffsetDateTime.now(),
        OffsetDateTime.now());
  }

  private WorkSummary toSummary(WorkRow work) {
    MediaAssets mediaAssets = mediaAssets(work.id());
    return new WorkSummary(
        work.id(),
        work.workCode(),
        work.songTitle(),
        work.status(),
        work.generationStage(),
        work.packageStatus(),
        mediaAssets.coverUrl(),
        mediaAssets.videoUrl(),
        work.updatedAt());
  }

  private WorkDetail toDetail(WorkRow work) {
    LyricsDraft lyricsDraft =
        workRepository.findLatestLyricsDraft(work.id()).map(this::toLyricsDraft).orElse(null);
    return new WorkDetail(
        work.id(),
        work.workCode(),
        work.creationMode(),
        work.status(),
        work.generationStage(),
        work.packageStatus(),
        work.songTitle(),
        work.songSummary(),
        lyricsDraft,
        mediaAssets(work.id()),
        work.polishUsedCount(),
        Math.max(0, POLISH_LIMIT - work.polishUsedCount()),
        quotaHint(work),
        failureInfo(work),
        availableActions(work),
        publishHandoffHint(work),
        work.createdAt(),
        work.updatedAt(),
        work.generatedAt());
  }

  private LyricsDraft toLyricsDraft(LyricsDraftRow draft) {
    return new LyricsDraft(
        draft.id(),
        draft.versionNo(),
        draft.songTitle(),
        draft.songSummary(),
        draft.lyricsText(),
        draft.musicPrompt(),
        readStringList(draft.riskNotesJson()),
        readStringList(draft.yanyunReferencesJson()));
  }

  private MediaAssets mediaAssets(UUID workId) {
    Map<String, MediaAssetRow> assets = new HashMap<>();
    for (MediaAssetRow asset : workRepository.findMediaAssets(workId)) {
      assets.put(asset.assetType(), asset);
    }
    MediaAssetRow audio = assets.get("AUDIO");
    MediaAssetRow cover = assets.get("COVER");
    MediaAssetRow video = assets.get("VIDEO");
    return new MediaAssets(
        audio == null ? null : assetUrl(audio.objectKey()),
        cover == null ? null : assetUrl(cover.objectKey()),
        video == null ? null : assetUrl(video.objectKey()),
        video == null ? null : video.durationMs(),
        video == null ? null : video.fileSizeBytes());
  }

  private PublishPackage toPublishPackage(WorkRow work, PublishPackageRow row) {
    return new PublishPackage(
        work.id(),
        row.packageStatus(),
        row.packageUrl(),
        row.packageUrlExpiresAt(),
        readJsonObject(row.packageJson()),
        availableActions(work),
        row.packageStatus() == PackageStatus.PACKAGE_BLOCKED ? "作品暂不能交给社区发布。" : null);
  }

  private String refreshedPackageJson(UUID workId, PublishPackageRow packageRow) {
    Map<String, Object> packageJson = new HashMap<>(readJsonObject(packageRow.packageJson()));
    Map<String, MediaAssetRow> assets = new HashMap<>();
    for (MediaAssetRow asset : workRepository.findMediaAssets(workId)) {
      assets.put(asset.assetType(), asset);
    }
    replaceMediaUrl(packageJson, "audio", assets.get("AUDIO"));
    replaceMediaUrl(packageJson, "cover", assets.get("COVER"));
    replaceMediaUrl(packageJson, "video", assets.get("VIDEO"));
    replaceLyricsTimelineUrl(packageJson, assets.get("TIMELINE"));
    return writeJson(packageJson);
  }

  @SuppressWarnings("unchecked")
  private void replaceMediaUrl(Map<String, Object> packageJson, String field, MediaAssetRow asset) {
    if (asset == null) {
      return;
    }
    Object existing = packageJson.get(field);
    Map<String, Object> media =
        existing instanceof Map<?, ?> existingMap
            ? new HashMap<>((Map<String, Object>) existingMap)
            : new HashMap<>();
    media.put("url", assetUrl(asset.objectKey()));
    packageJson.put(field, media);
  }

  @SuppressWarnings("unchecked")
  private void replaceLyricsTimelineUrl(Map<String, Object> packageJson, MediaAssetRow asset) {
    if (asset == null) {
      return;
    }
    Object existing = packageJson.get("lyrics");
    Map<String, Object> lyrics =
        existing instanceof Map<?, ?> existingMap
            ? new HashMap<>((Map<String, Object>) existingMap)
            : new HashMap<>();
    lyrics.put("timeline_url", assetUrl(asset.objectKey()));
    packageJson.put("lyrics", lyrics);
  }

  private PublishPackage emptyPackage(WorkRow work) {
    PackageStatus packageStatus =
        work.packageStatus() == PackageStatus.PACKAGE_BLOCKED
            ? PackageStatus.PACKAGE_BLOCKED
            : PackageStatus.PACKAGE_NOT_READY;
    return new PublishPackage(
        work.id(),
        packageStatus,
        null,
        null,
        null,
        availableActions(work),
        packageStatus == PackageStatus.PACKAGE_BLOCKED ? "作品暂不能交给社区发布。" : null);
  }

  private QuotaHint quotaHint(WorkRow work) {
    QuotaDecision decision = quotaAdapter.getHint(work.userId(), work.polishUsedCount());
    return new QuotaHint(
        work.quotaLocked() || decision.locked(),
        decision.commitTiming(),
        decision.remainingGenerateCount(),
        Math.max(0, POLISH_LIMIT - work.polishUsedCount()),
        decision.message());
  }

  private FailureInfo failureInfo(WorkRow work) {
    if (work.failureCode() == null) {
      return null;
    }
    return new FailureInfo(
        work.failureCode(),
        work.failureMessage(),
        effectiveRetryable(work),
        work.failedAt(),
        isMusicRetryFailure(work.failureCode()) ? work.musicRetryCount() : null,
        isMusicRetryFailure(work.failureCode()) ? MUSIC_RETRY_LIMIT : null,
        isMusicRetryFailure(work.failureCode()) ? remainingMusicRetries(work) : null,
        recommendedAction(work));
  }

  private PublishHandoffHint publishHandoffHint(WorkRow work) {
    boolean ready = work.packageStatus() == PackageStatus.PACKAGE_READY;
    return new PublishHandoffHint(ready, ready ? "作品已准备好，可交给社区发布。" : "作品尚未准备好，暂不能交给社区发布。");
  }

  private List<AvailableAction> availableActions(WorkRow work) {
    return WorkStateMachine.availableActions(workSnapshot(work));
  }

  private WorkSnapshot workSnapshot(WorkRow work) {
    return new WorkSnapshot(
        work.status(),
        work.generationStage(),
        work.packageStatus(),
        work.failureCode(),
        Boolean.TRUE.equals(work.retryable()),
        remainingMusicRetries(work));
  }

  private boolean effectiveRetryable(WorkRow work) {
    if (!Boolean.TRUE.equals(work.retryable())) {
      return false;
    }
    return !isMusicRetryFailure(work.failureCode()) || remainingMusicRetries(work) > 0;
  }

  private int remainingMusicRetries(WorkRow work) {
    return Math.max(0, MUSIC_RETRY_LIMIT - work.musicRetryCount());
  }

  private boolean isMusicRetryFailure(FailureCode failureCode) {
    if (failureCode == null) {
      return false;
    }
    return switch (failureCode) {
      case MUSIC_GENERATION_FAILED, MUSIC_QUALITY_FAILED, PROVIDER_TIMEOUT, RATE_LIMITED -> true;
      default -> false;
    };
  }

  private AvailableAction recommendedAction(WorkRow work) {
    return availableActions(work).stream().findFirst().orElse(null);
  }

  private JobAcceptedResponse accepted(WorkRow work, UUID jobId) {
    return new JobAcceptedResponse(
        work.id(), work.status(), work.generationStage(), jobId, availableActions(work));
  }

  private ResponseStatusException workflowFailure(SongProductionWorkflowResult workflowResult) {
    String failureCode = workflowResult.failureCode();
    HttpStatus status =
        FailureCode.PACKAGE_BLOCKED.name().equals(failureCode)
            ? HttpStatus.FORBIDDEN
            : HttpStatus.CONFLICT;
    return new ResponseStatusException(
        status, firstNonBlank(workflowResult.failureMessage(), "Song production failed"));
  }

  private WorkRow getRequiredWork(UUID workId, String userId) {
    return workRepository
        .findWorkForUser(workId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work not found"));
  }

  private LyricsDraftRow getRequiredLyricsDraft(UUID workId) {
    return workRepository
        .findLatestLyricsDraft(workId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.CONFLICT, "Lyrics draft not found"));
  }

  private void requireAllowed(ModerationDecision decision, FailureCode failureCode) {
    if (!decision.allowed()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, firstNonBlank(decision.message(), failureCode.name()));
    }
  }

  private void requireText(String text, String message) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException(message);
    }
  }

  private String generateWorkCode() {
    String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    return "YYM-" + date + "-" + suffix;
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String assetUrl(String objectKey) {
    return objectStorageClient.createDownloadUrl(objectKey).url();
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to write JSON", exception);
    }
  }

  private List<String> readStringList(String json) {
    try {
      return objectMapper.readValue(json == null ? "[]" : json, STRING_LIST);
    } catch (JsonProcessingException exception) {
      return List.of();
    }
  }

  private Map<String, Object> readJsonObject(String json) {
    try {
      return objectMapper.readValue(json == null ? "{}" : json, JSON_OBJECT);
    } catch (JsonProcessingException exception) {
      return Map.of();
    }
  }
}
