package com.yanyun.music.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SongProductionStepActivitiesContractTest {

  @Test
  void contextBuildsDeterministicStepIdempotencyKey() {
    SongProductionActivityContext context =
        new SongProductionActivityContext("work-1", "job-1", "user-1");

    assertEquals(
        "work-1:job-1:SUBMIT_MUSIC", context.idempotencyKey(SongProductionStepName.SUBMIT_MUSIC));
  }

  @Test
  void contextCanBeDerivedFromWorkflowInputWithJobId() {
    SongProductionWorkflowInput input =
        new SongProductionWorkflowInput(
            "work-1", "user-1", "draft-1", "title", "summary", "lyrics", "prompt", "AUTO", "mock",
            true, "job-1");

    SongProductionActivityContext context = SongProductionActivityContext.from(input);

    assertEquals("work-1", context.workId());
    assertEquals("job-1", context.jobId());
    assertEquals("user-1", context.userId());
  }

  @Test
  void contextRejectsMissingJobIdBecauseStepRetriesNeedStableKeys() {
    SongProductionWorkflowInput input =
        new SongProductionWorkflowInput(
            "work-1", "user-1", "draft-1", "title", "summary", "lyrics", "prompt", "AUTO", "mock",
            true);

    assertThrows(IllegalArgumentException.class, () -> SongProductionActivityContext.from(input));
  }

  @Test
  void stepResultsCopyReferencesAndPreserveFailureSummary() {
    SongProductionActivityContext context =
        new SongProductionActivityContext("work-1", "job-1", "user-1");
    Map<String, String> references = new java.util.LinkedHashMap<>();
    references.put("provider_trace_id", "task-1");

    SongProductionStepResult result =
        SongProductionStepResult.failed(
            context,
            SongProductionStepName.POLL_MUSIC,
            "MUSIC_GENERATION_FAILED",
            "provider timed out",
            references);
    references.put("provider_trace_id", "task-2");

    assertEquals("work-1:job-1:POLL_MUSIC", result.idempotencyKey());
    assertEquals("MUSIC_GENERATION_FAILED", result.failureCode());
    assertEquals("task-1", result.references().get("provider_trace_id"));
    assertThrows(
        UnsupportedOperationException.class,
        () -> result.references().put("new_reference", "not-allowed"));
  }

  @Test
  void stepActivitiesExposeThePlannedPhaseOneSkeleton() {
    Set<String> methodNames =
        Arrays.stream(SongProductionStepActivities.class.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());

    assertEquals(14, methodNames.size());
    assertTrue(
        methodNames.containsAll(
            Set.of(
                "lockQuota",
                "generateMusicPrompt",
                "preCheckMusicPrompt",
                "submitMusic",
                "pollMusic",
                "importAudio",
                "generateCoverPrompt",
                "generateCover",
                "renderVideo",
                "evaluatePackage",
                "preCheckPublishPackage",
                "assemblePublishPackage",
                "commitQuota",
                "releaseQuota")));
  }
}
