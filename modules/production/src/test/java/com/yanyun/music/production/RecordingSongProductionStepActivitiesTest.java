package com.yanyun.music.production;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yanyun.music.workflow.SongProductionActivityContext;
import com.yanyun.music.workflow.SongProductionStepName;
import com.yanyun.music.workflow.SongProductionStepResult;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.SongProductionWorkflowResult;
import com.yanyun.music.workflow.StepwiseSongProductionWorkflow;
import com.yanyun.music.workpersistence.WorkRepository.GenerationJobStepRow;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecordingSongProductionStepActivitiesTest {

  private final Clock fixedClock =
      Clock.fixed(Instant.parse("2026-06-06T13:18:00Z"), ZoneOffset.UTC);

  @Test
  void recordsSuccessfulStepWithStableIdempotencyKey() {
    List<GenerationJobStepRow> rows = new ArrayList<>();
    RecordingSongProductionStepActivities activities =
        new RecordingSongProductionStepActivities(rows::add, fixedClock);
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    SongProductionActivityContext context =
        new SongProductionActivityContext(workId.toString(), jobId.toString(), "user-1");

    SongProductionStepResult result = activities.submitMusic(context);

    assertTrue(result.success());
    assertEquals(SongProductionStepName.SUBMIT_MUSIC, result.stepName());
    assertEquals(workId + ":" + jobId + ":SUBMIT_MUSIC", result.idempotencyKey());
    assertEquals("mock-step-recording", result.references().get("activity_mode"));
    assertEquals(1, rows.size());

    GenerationJobStepRow row = rows.getFirst();
    assertNotNull(row.id());
    assertEquals(workId, row.workId());
    assertEquals(jobId, row.jobId());
    assertEquals("SUBMIT_MUSIC", row.stepName());
    assertEquals(result.idempotencyKey(), row.idempotencyKey());
    assertEquals("SUCCEEDED", row.status());
    assertEquals(1, row.attemptCount());
    assertEquals(OffsetDateTime.parse("2026-06-06T13:18:00Z"), row.startedAt());
    assertEquals(OffsetDateTime.parse("2026-06-06T13:18:00Z"), row.completedAt());
  }

  @Test
  void recordsFailedStepWithSanitizedMessageAndExternalTrace() {
    List<GenerationJobStepRow> rows = new ArrayList<>();
    RecordingSongProductionStepActivities activities =
        new RecordingSongProductionStepActivities(rows::add, fixedClock);
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    SongProductionActivityContext context =
        new SongProductionActivityContext(workId.toString(), jobId.toString(), "user-1");

    SongProductionStepResult result =
        activities.recordFailed(
            context,
            SongProductionStepName.RENDER_VIDEO,
            "VIDEO_RENDER_FAILED",
            "failed Authorization Bearer abc.def.ghi token=raw-token",
            Map.of("external_trace_id", "render-task-1"));

    assertFalse(result.success());
    assertEquals("VIDEO_RENDER_FAILED", result.failureCode());
    assertTrue(result.failureMessage().contains("Bearer [REDACTED]"));
    assertTrue(result.failureMessage().contains("token=[REDACTED]"));
    assertFalse(result.failureMessage().contains("abc.def.ghi"));
    assertFalse(result.failureMessage().contains("raw-token"));

    GenerationJobStepRow row = rows.getFirst();
    assertEquals("RENDER_VIDEO", row.stepName());
    assertEquals("FAILED", row.status());
    assertEquals("render-task-1", row.externalTraceId());
    assertEquals("VIDEO_RENDER_FAILED", row.failureCode());
    assertEquals(result.failureMessage(), row.failureMessage());
  }

  @Test
  void recordsAllSuccessfulStepsWhenUsedByStepwiseWorkflow() {
    List<GenerationJobStepRow> rows = new ArrayList<>();
    RecordingSongProductionStepActivities activities =
        new RecordingSongProductionStepActivities(rows::add, fixedClock);
    UUID workId = UUID.randomUUID();
    UUID jobId = UUID.randomUUID();
    SongProductionWorkflowInput input =
        new SongProductionWorkflowInput(
            workId.toString(),
            "user-1",
            UUID.randomUUID().toString(),
            "title",
            "summary",
            "lyrics",
            "music prompt",
            "AUTO",
            "mock",
            true,
            jobId.toString());

    SongProductionWorkflowResult result =
        new StepwiseSongProductionWorkflow(activities).produce(input);

    assertTrue(result.packageReady());
    assertEquals("PACKAGE_READY", result.packageStatus());
    assertEquals(13, rows.size());
    assertEquals("LOCK_QUOTA", rows.getFirst().stepName());
    assertEquals("COMMIT_QUOTA", rows.getLast().stepName());
    assertTrue(rows.stream().allMatch(row -> workId.equals(row.workId())));
    assertTrue(rows.stream().allMatch(row -> jobId.equals(row.jobId())));
    assertTrue(rows.stream().allMatch(row -> "SUCCEEDED".equals(row.status())));
    assertTrue(
        rows.stream()
            .noneMatch(row -> SongProductionStepName.RELEASE_QUOTA.name().equals(row.stepName())));
  }
}
