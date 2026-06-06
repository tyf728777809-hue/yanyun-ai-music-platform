package com.yanyun.music.production;

import com.yanyun.music.agentruntime.AgentRunSanitizer;
import com.yanyun.music.workflow.SongProductionActivityContext;
import com.yanyun.music.workflow.SongProductionStepActivities;
import com.yanyun.music.workflow.SongProductionStepName;
import com.yanyun.music.workflow.SongProductionStepResult;
import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.GenerationJobStepRow;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class RecordingSongProductionStepActivities implements SongProductionStepActivities {

  private static final String EXTERNAL_TRACE_ID_KEY = "external_trace_id";

  private final Consumer<GenerationJobStepRow> stepRecorder;
  private final Clock clock;

  public RecordingSongProductionStepActivities(WorkRepository workRepository) {
    this(workRepository::upsertGenerationJobStep, Clock.systemUTC());
  }

  RecordingSongProductionStepActivities(Consumer<GenerationJobStepRow> stepRecorder, Clock clock) {
    this.stepRecorder = stepRecorder;
    this.clock = clock == null ? Clock.systemUTC() : clock;
  }

  @Override
  public SongProductionStepResult lockQuota(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.LOCK_QUOTA);
  }

  @Override
  public SongProductionStepResult generateMusicPrompt(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.GENERATE_MUSIC_PROMPT);
  }

  @Override
  public SongProductionStepResult preCheckMusicPrompt(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.PRE_CHECK_MUSIC_PROMPT);
  }

  @Override
  public SongProductionStepResult submitMusic(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.SUBMIT_MUSIC);
  }

  @Override
  public SongProductionStepResult pollMusic(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.POLL_MUSIC);
  }

  @Override
  public SongProductionStepResult importAudio(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.IMPORT_AUDIO);
  }

  @Override
  public SongProductionStepResult generateCoverPrompt(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.GENERATE_COVER_PROMPT);
  }

  @Override
  public SongProductionStepResult generateCover(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.GENERATE_COVER);
  }

  @Override
  public SongProductionStepResult renderVideo(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.RENDER_VIDEO);
  }

  @Override
  public SongProductionStepResult evaluatePackage(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.EVALUATE_PACKAGE);
  }

  @Override
  public SongProductionStepResult preCheckPublishPackage(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.PRE_CHECK_PUBLISH_PACKAGE);
  }

  @Override
  public SongProductionStepResult assemblePublishPackage(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.ASSEMBLE_PUBLISH_PACKAGE);
  }

  @Override
  public SongProductionStepResult commitQuota(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.COMMIT_QUOTA);
  }

  @Override
  public SongProductionStepResult releaseQuota(SongProductionActivityContext context) {
    return recordSucceeded(context, SongProductionStepName.RELEASE_QUOTA);
  }

  SongProductionStepResult recordFailed(
      SongProductionActivityContext context,
      SongProductionStepName stepName,
      String failureCode,
      String failureMessage,
      Map<String, String> references) {
    Map<String, String> safeReferences = references == null ? Map.of() : Map.copyOf(references);
    String sanitizedMessage = AgentRunSanitizer.sanitizeFailureMessage(failureMessage);
    OffsetDateTime now = OffsetDateTime.now(clock);
    SongProductionStepResult result =
        SongProductionStepResult.failed(
            context, stepName, failureCode, sanitizedMessage, safeReferences);
    record(context, result, safeReferences.get(EXTERNAL_TRACE_ID_KEY), now);
    return result;
  }

  private SongProductionStepResult recordSucceeded(
      SongProductionActivityContext context, SongProductionStepName stepName) {
    Map<String, String> references =
        Map.of("activity_mode", "mock-step-recording", "step_name", stepName.name());
    OffsetDateTime now = OffsetDateTime.now(clock);
    SongProductionStepResult result =
        SongProductionStepResult.succeeded(context, stepName, references);
    record(context, result, null, now);
    return result;
  }

  private void record(
      SongProductionActivityContext context,
      SongProductionStepResult result,
      String externalTraceId,
      OffsetDateTime timestamp) {
    stepRecorder.accept(
        new GenerationJobStepRow(
            UUID.randomUUID(),
            parseUuid(context.jobId(), "jobId"),
            parseUuid(context.workId(), "workId"),
            result.stepName().name(),
            result.idempotencyKey(),
            result.status(),
            1,
            externalTraceId,
            result.failureCode(),
            result.failureMessage(),
            timestamp,
            timestamp,
            null,
            null));
  }

  private UUID parseUuid(String value, String fieldName) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException(fieldName + " must be a UUID", exception);
    }
  }
}
