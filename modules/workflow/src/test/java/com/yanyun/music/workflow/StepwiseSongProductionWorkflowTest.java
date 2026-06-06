package com.yanyun.music.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StepwiseSongProductionWorkflowTest {

  @Test
  void runsPlannedStepsInOrderAndReturnsPackageReady() {
    FakeStepActivities activities = new FakeStepActivities();
    SongProductionWorkflowInput input = input();

    SongProductionWorkflowResult result =
        new StepwiseSongProductionWorkflow(activities).produce(input);

    assertTrue(result.packageReady());
    assertEquals(input.jobId(), result.jobId());
    assertEquals("PACKAGE_READY", result.packageStatus());
    assertEquals(
        List.of(
            SongProductionStepName.LOCK_QUOTA,
            SongProductionStepName.GENERATE_MUSIC_PROMPT,
            SongProductionStepName.PRE_CHECK_MUSIC_PROMPT,
            SongProductionStepName.SUBMIT_MUSIC,
            SongProductionStepName.POLL_MUSIC,
            SongProductionStepName.IMPORT_AUDIO,
            SongProductionStepName.GENERATE_COVER_PROMPT,
            SongProductionStepName.GENERATE_COVER,
            SongProductionStepName.RENDER_VIDEO,
            SongProductionStepName.EVALUATE_PACKAGE,
            SongProductionStepName.PRE_CHECK_PUBLISH_PACKAGE,
            SongProductionStepName.ASSEMBLE_PUBLISH_PACKAGE,
            SongProductionStepName.COMMIT_QUOTA),
        activities.calls());
  }

  @Test
  void doesNotReleaseQuotaWhenLockQuotaFails() {
    FakeStepActivities activities = new FakeStepActivities();
    activities.fail(
        SongProductionStepName.LOCK_QUOTA, "QUOTA_LOCK_FAILED", "quota cannot be locked");

    SongProductionWorkflowResult result =
        new StepwiseSongProductionWorkflow(activities).produce(input());

    assertFalse(result.packageReady());
    assertEquals("FAILED", result.packageStatus());
    assertEquals("QUOTA_LOCK_FAILED", result.failureCode());
    assertEquals(List.of(SongProductionStepName.LOCK_QUOTA), activities.calls());
  }

  @Test
  void releasesQuotaAndReturnsOriginalFailureWhenLaterStepFails() {
    FakeStepActivities activities = new FakeStepActivities();
    activities.fail(
        SongProductionStepName.POLL_MUSIC, "MUSIC_GENERATION_FAILED", "provider task timed out");

    SongProductionWorkflowResult result =
        new StepwiseSongProductionWorkflow(activities).produce(input());

    assertFalse(result.packageReady());
    assertEquals("FAILED", result.packageStatus());
    assertEquals("MUSIC_GENERATION_FAILED", result.failureCode());
    assertEquals("provider task timed out", result.failureMessage());
    assertEquals(
        List.of(
            SongProductionStepName.LOCK_QUOTA,
            SongProductionStepName.GENERATE_MUSIC_PROMPT,
            SongProductionStepName.PRE_CHECK_MUSIC_PROMPT,
            SongProductionStepName.SUBMIT_MUSIC,
            SongProductionStepName.POLL_MUSIC,
            SongProductionStepName.RELEASE_QUOTA),
        activities.calls());
  }

  @Test
  void mapsPublishPrecheckFailureToPackageBlocked() {
    FakeStepActivities activities = new FakeStepActivities();
    activities.fail(
        SongProductionStepName.PRE_CHECK_PUBLISH_PACKAGE,
        "PACKAGE_BLOCKED",
        "publish precheck blocked the work");

    SongProductionWorkflowResult result =
        new StepwiseSongProductionWorkflow(activities).produce(input());

    assertFalse(result.packageReady());
    assertEquals("PACKAGE_BLOCKED", result.packageStatus());
    assertEquals("PACKAGE_BLOCKED", result.failureCode());
    assertTrue(activities.calls().contains(SongProductionStepName.RELEASE_QUOTA));
  }

  private SongProductionWorkflowInput input() {
    return new SongProductionWorkflowInput(
        UUID.randomUUID().toString(),
        "user-1",
        UUID.randomUUID().toString(),
        "title",
        "summary",
        "lyrics",
        "music prompt",
        "AUTO",
        "mock",
        true,
        UUID.randomUUID().toString());
  }

  private static final class FakeStepActivities implements SongProductionStepActivities {

    private final List<SongProductionStepName> calls = new ArrayList<>();
    private final Map<SongProductionStepName, Failure> failures =
        new EnumMap<>(SongProductionStepName.class);

    List<SongProductionStepName> calls() {
      return List.copyOf(calls);
    }

    void fail(SongProductionStepName stepName, String failureCode, String failureMessage) {
      failures.put(stepName, new Failure(failureCode, failureMessage));
    }

    @Override
    public SongProductionStepResult lockQuota(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.LOCK_QUOTA);
    }

    @Override
    public SongProductionStepResult generateMusicPrompt(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.GENERATE_MUSIC_PROMPT);
    }

    @Override
    public SongProductionStepResult preCheckMusicPrompt(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.PRE_CHECK_MUSIC_PROMPT);
    }

    @Override
    public SongProductionStepResult submitMusic(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.SUBMIT_MUSIC);
    }

    @Override
    public SongProductionStepResult pollMusic(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.POLL_MUSIC);
    }

    @Override
    public SongProductionStepResult importAudio(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.IMPORT_AUDIO);
    }

    @Override
    public SongProductionStepResult generateCoverPrompt(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.GENERATE_COVER_PROMPT);
    }

    @Override
    public SongProductionStepResult generateCover(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.GENERATE_COVER);
    }

    @Override
    public SongProductionStepResult renderVideo(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.RENDER_VIDEO);
    }

    @Override
    public SongProductionStepResult evaluatePackage(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.EVALUATE_PACKAGE);
    }

    @Override
    public SongProductionStepResult preCheckPublishPackage(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.PRE_CHECK_PUBLISH_PACKAGE);
    }

    @Override
    public SongProductionStepResult assemblePublishPackage(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.ASSEMBLE_PUBLISH_PACKAGE);
    }

    @Override
    public SongProductionStepResult commitQuota(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.COMMIT_QUOTA);
    }

    @Override
    public SongProductionStepResult releaseQuota(SongProductionActivityContext context) {
      return record(context, SongProductionStepName.RELEASE_QUOTA);
    }

    private SongProductionStepResult record(
        SongProductionActivityContext context, SongProductionStepName stepName) {
      calls.add(stepName);
      Failure failure = failures.get(stepName);
      if (failure != null) {
        return SongProductionStepResult.failed(
            context, stepName, failure.failureCode(), failure.failureMessage(), Map.of());
      }
      return SongProductionStepResult.succeeded(context, stepName, Map.of("mode", "fake"));
    }
  }

  private record Failure(String failureCode, String failureMessage) {}
}
