package com.yanyun.music.api.work;

import com.yanyun.music.workdomain.AvailableAction;
import com.yanyun.music.workdomain.CreationMode;
import com.yanyun.music.workdomain.FailureCode;
import com.yanyun.music.workdomain.GenerationStage;
import com.yanyun.music.workdomain.PackageStatus;
import com.yanyun.music.workdomain.WorkStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WorkDtos {

  private WorkDtos() {}

  public record InspirationCreateRequest(
      String storyInput,
      String mood,
      String scene,
      String relationship,
      String musicStyle,
      String vocalPreference) {}

  public record LyricsCreateRequest(
      String lyricsInput, String songTitle, String musicStyle, String vocalPreference) {}

  public record LyricsPolishRequest(String instruction) {}

  public record LyricsContinueRequest(String instruction) {}

  public record ConfirmWorkRequest(UUID lyricsDraftId, OffsetDateTime userConfirmedAt) {}

  public record CreateWorkResponse(
      UUID workId,
      String workCode,
      WorkStatus status,
      GenerationStage generationStage,
      UUID jobId,
      QuotaHint quotaHint,
      List<AvailableAction> availableActions) {}

  public record JobAcceptedResponse(
      UUID workId,
      WorkStatus status,
      GenerationStage generationStage,
      UUID jobId,
      List<AvailableAction> availableActions) {}

  public record WorkListResponse(List<WorkSummary> items, Pagination pagination) {}

  public record WorkSummary(
      UUID workId,
      String workCode,
      String songTitle,
      WorkStatus status,
      GenerationStage generationStage,
      PackageStatus packageStatus,
      String coverUrl,
      String videoPreviewUrl,
      OffsetDateTime updatedAt) {}

  public record WorkDetail(
      UUID workId,
      String workCode,
      CreationMode creationMode,
      WorkStatus status,
      GenerationStage generationStage,
      PackageStatus packageStatus,
      String songTitle,
      String songSummary,
      LyricsDraft lyricsDraft,
      MediaAssets mediaAssets,
      int polishUsedCount,
      int polishRemainingCount,
      QuotaHint quotaHint,
      FailureInfo failure,
      List<AvailableAction> availableActions,
      PublishHandoffHint publishHandoffHint,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt,
      OffsetDateTime generatedAt) {}

  public record LyricsDraft(
      UUID lyricsDraftId,
      int versionNo,
      String songTitle,
      String songSummary,
      String lyricsText,
      String musicPrompt,
      List<String> riskNotes,
      List<String> yanyunReferences) {}

  public record MediaAssets(
      String audioUrl,
      String coverUrl,
      String videoUrl,
      Integer videoDurationMs,
      Long videoFileSizeBytes) {}

  public record PublishPackage(
      UUID workId,
      PackageStatus packageStatus,
      String packageUrl,
      OffsetDateTime packageUrlExpiresAt,
      Map<String, Object> packageJson,
      List<AvailableAction> availableActions,
      String blockedReason) {}

  public record QuotaHint(
      boolean locked,
      String commitTiming,
      int remainingGenerateCount,
      int remainingPolishCount,
      String message) {}

  public record FailureInfo(
      FailureCode failureCode, String failureMessage, boolean retryable, OffsetDateTime failedAt) {}

  public record PublishHandoffHint(boolean readyForHandoff, String message) {}

  public record Pagination(int page, int pageSize, long totalItems, int totalPages) {}
}
