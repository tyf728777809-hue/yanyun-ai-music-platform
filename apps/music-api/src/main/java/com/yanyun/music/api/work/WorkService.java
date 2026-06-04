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
import com.yanyun.music.api.work.WorkRepository.LyricsDraftRow;
import com.yanyun.music.api.work.WorkRepository.MediaAssetRow;
import com.yanyun.music.api.work.WorkRepository.PublishPackageRow;
import com.yanyun.music.api.work.WorkRepository.WorkRow;
import com.yanyun.music.moderation.ModerationAdapter;
import com.yanyun.music.moderation.ModerationDecision;
import com.yanyun.music.musicprovider.MusicProviderSelection;
import com.yanyun.music.publish.PublishAdapter;
import com.yanyun.music.publish.PublishHandoff;
import com.yanyun.music.quota.QuotaAdapter;
import com.yanyun.music.quota.QuotaDecision;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkService {

  private static final int POLISH_LIMIT = 2;
  private static final String ASSET_BASE_URL = "http://localhost:9000/yanyun-works-local/";
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
  private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};

  private final WorkRepository workRepository;
  private final QuotaAdapter quotaAdapter;
  private final ModerationAdapter moderationAdapter;
  private final PublishAdapter publishAdapter;
  private final SongProductionWorkflow songProductionWorkflow;
  private final ObjectMapper objectMapper;

  public WorkService(
      WorkRepository workRepository,
      QuotaAdapter quotaAdapter,
      ModerationAdapter moderationAdapter,
      PublishAdapter publishAdapter,
      SongProductionWorkflow songProductionWorkflow,
      ObjectMapper objectMapper) {
    this.workRepository = workRepository;
    this.quotaAdapter = quotaAdapter;
    this.moderationAdapter = moderationAdapter;
    this.publishAdapter = publishAdapter;
    this.songProductionWorkflow = songProductionWorkflow;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public CreateWorkResponse createFromInspiration(String userId, InspirationCreateRequest request) {
    requireText(request == null ? null : request.storyInput(), "story_input is required");
    ModerationDecision decision = moderationAdapter.preCheckUserInput(userId, request.storyInput());
    requireAllowed(decision, FailureCode.USER_INPUT_BLOCKED);

    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    String title = "Yanyun Mock Song";
    String summary = compactSummary(request.storyInput());
    String lyricsText = buildLyricsFromStory(request.storyInput());
    String musicPrompt = buildMusicPrompt(request.musicStyle(), request.vocalPreference());
    UUID jobId =
        createWorkWithDraft(
            workId,
            draftId,
            userId,
            CreationMode.INSPIRATION,
            title,
            summary,
            lyricsText,
            musicPrompt,
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

  @Transactional
  public CreateWorkResponse createFromLyrics(String userId, LyricsCreateRequest request) {
    requireText(request == null ? null : request.lyricsInput(), "lyrics_input is required");
    ModerationDecision decision = moderationAdapter.preCheckLyrics(userId, request.lyricsInput());
    requireAllowed(decision, FailureCode.LYRICS_PRECHECK_FAILED);

    UUID workId = UUID.randomUUID();
    UUID draftId = UUID.randomUUID();
    String title = firstNonBlank(request.songTitle(), "Yanyun Lyrics Song");
    String summary = "Created from user-provided lyrics.";
    String musicPrompt = buildMusicPrompt(request.musicStyle(), request.vocalPreference());
    UUID jobId =
        createWorkWithDraft(
            workId,
            draftId,
            userId,
            CreationMode.LYRICS,
            title,
            summary,
            request.lyricsInput(),
            musicPrompt,
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

  @Transactional
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
    String nextLyrics =
        latest.lyricsText() + "\n\n[Mock polish instruction]\n" + request.instruction().trim();
    UUID jobId =
        replaceLyricsDraft(
            work, latest.songTitle(), latest.songSummary(), nextLyrics, latest.musicPrompt(), true);
    WorkRow updated = getRequiredWork(workId, userId);
    return accepted(updated, jobId);
  }

  @Transactional
  public JobAcceptedResponse continueLyrics(
      String userId, UUID workId, LyricsContinueRequest request) {
    WorkRow work = getRequiredWork(workId, userId);
    if (!WorkStateMachine.canEditLyrics(work.status())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Current work cannot edit lyrics");
    }

    LyricsDraftRow latest = getRequiredLyricsDraft(workId);
    String instruction =
        request == null || request.instruction() == null || request.instruction().isBlank()
            ? "Continue current lyrics."
            : request.instruction().trim();
    String nextLyrics = latest.lyricsText() + "\n\n[Mock continued section]\n" + instruction;
    UUID jobId =
        replaceLyricsDraft(
            work,
            latest.songTitle(),
            latest.songSummary(),
            nextLyrics,
            latest.musicPrompt(),
            false);
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

    SongProductionWorkflowResult workflowResult =
        songProductionWorkflow.produce(
            workflowInput(
                workId,
                userId,
                draft,
                musicProvider(request == null ? null : request.musicProvider())));
    if (!workflowResult.packageReady()) {
      throw workflowFailure(workflowResult);
    }

    WorkRow updated = getRequiredWork(workId, userId);
    return accepted(updated, UUID.fromString(workflowResult.jobId()));
  }

  @Transactional(noRollbackFor = ResponseStatusException.class)
  public JobAcceptedResponse retryMusic(String userId, UUID workId, RetryMusicRequest request) {
    WorkRow work = getRequiredWork(workId, userId);
    WorkSnapshot snapshot =
        new WorkSnapshot(
            work.status(),
            work.generationStage(),
            work.packageStatus(),
            work.failureCode(),
            Boolean.TRUE.equals(work.retryable()));
    if (!WorkStateMachine.canRetryMusic(snapshot)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Current work cannot retry music generation");
    }

    LyricsDraftRow draft = getRequiredLyricsDraft(workId);
    SongProductionWorkflowResult workflowResult =
        songProductionWorkflow.produce(
            workflowInput(
                workId,
                userId,
                draft,
                musicProvider(request == null ? null : request.musicProvider())));
    if (!workflowResult.packageReady()) {
      throw workflowFailure(workflowResult);
    }

    WorkRow updated = getRequiredWork(workId, userId);
    return accepted(updated, UUID.fromString(workflowResult.jobId()));
  }

  @Transactional
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

  @Transactional
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

  @Transactional
  public PublishPackage markPublishPackageFetched(String userId, UUID workId) {
    WorkRow work = getRequiredWork(workId, userId);
    if (work.packageStatus() != PackageStatus.PACKAGE_READY) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Package is not ready for handoff");
    }
    workRepository.markPackageFetched(workId);
    return getPublishPackage(userId, workId);
  }

  @Transactional
  public PublishPackage refreshPublishPackageUrl(String userId, UUID workId) {
    WorkRow work = getRequiredWork(workId, userId);
    if (work.status() != WorkStatus.GENERATED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only generated works can refresh package URL");
    }
    if (workRepository.findPublishPackage(workId).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Publish package not found");
    }
    PublishHandoff handoff = publishAdapter.refreshPackageUrl(workId.toString());
    workRepository.updatePublishPackageUrl(workId, handoff.packageUrl(), handoff.expiresAt());
    return getPublishPackage(userId, workId);
  }

  private SongProductionWorkflowInput workflowInput(
      UUID workId, String userId, LyricsDraftRow draft, String musicProvider) {
    return new SongProductionWorkflowInput(
        workId.toString(),
        userId,
        draft.id().toString(),
        draft.songTitle(),
        draft.songSummary(),
        draft.lyricsText(),
        draft.musicPrompt(),
        "AUTO",
        musicProvider);
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

  private UUID createWorkWithDraft(
      UUID workId,
      UUID draftId,
      String userId,
      CreationMode creationMode,
      String title,
      String summary,
      String lyricsText,
      String musicPrompt,
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
            title,
            summary,
            0,
            0,
            null,
            null,
            null,
            null,
            false,
            false,
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
            title,
            summary,
            lyricsText,
            musicPrompt,
            writeJson(List.of()),
            writeJson(List.of("mock-yanyun-kb-v0")),
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
      WorkRow work,
      String title,
      String summary,
      String lyricsText,
      String musicPrompt,
      boolean incrementPolish) {
    UUID draftId = UUID.randomUUID();
    workRepository.insertLyricsDraft(
        new LyricsDraftRow(
            draftId,
            work.id(),
            workRepository.nextLyricsVersion(work.id()),
            title,
            summary,
            lyricsText,
            musicPrompt,
            writeJson(List.of()),
            writeJson(List.of("mock-yanyun-kb-v0")),
            null));
    workRepository.markLyricsReady(work.id(), title, summary, incrementPolish);
    return workRepository.insertGenerationJob(
        work.id(),
        incrementPolish ? "LYRICS_POLISH" : "LYRICS_CONTINUE",
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
        row.packageStatus() == PackageStatus.PACKAGE_BLOCKED ? "Package moderation blocked" : null);
  }

  private PublishPackage emptyPackage(WorkRow work) {
    return new PublishPackage(
        work.id(), PackageStatus.PACKAGE_NOT_READY, null, null, null, availableActions(work), null);
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
        Boolean.TRUE.equals(work.retryable()),
        work.failedAt());
  }

  private PublishHandoffHint publishHandoffHint(WorkRow work) {
    boolean ready = work.packageStatus() == PackageStatus.PACKAGE_READY;
    return new PublishHandoffHint(
        ready,
        ready
            ? "Package is ready for company-side publish handoff."
            : "Package is not ready for handoff.");
  }

  private List<AvailableAction> availableActions(WorkRow work) {
    return WorkStateMachine.availableActions(
        new WorkSnapshot(
            work.status(),
            work.generationStage(),
            work.packageStatus(),
            work.failureCode(),
            Boolean.TRUE.equals(work.retryable())));
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

  private String buildLyricsFromStory(String storyInput) {
    String summary = compactSummary(storyInput);
    return "[Verse]\n"
        + summary
        + "\n\n[Chorus]\nThe wind carries the name across Yanyun.\nThe old vow becomes a new song.";
  }

  private String buildMusicPrompt(String musicStyle, String vocalPreference) {
    return "Commercial Chinese game OST ballad, style="
        + firstNonBlank(musicStyle, "cinematic folk pop")
        + ", vocal="
        + firstNonBlank(vocalPreference, "AUTO")
        + ", vertical MP4 ready.";
  }

  private String compactSummary(String value) {
    String trimmed = value == null ? "" : value.trim().replaceAll("\\s+", " ");
    if (trimmed.length() <= 96) {
      return trimmed;
    }
    return trimmed.substring(0, 96);
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String assetUrl(String objectKey) {
    return ASSET_BASE_URL + objectKey;
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
