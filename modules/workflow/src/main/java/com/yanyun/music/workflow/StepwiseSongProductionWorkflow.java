package com.yanyun.music.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.Objects;

public class StepwiseSongProductionWorkflow implements SongProductionWorkflow {

  private static final Duration ACTIVITY_START_TO_CLOSE_TIMEOUT = Duration.ofMinutes(30);

  private final SongProductionStepActivities activities;

  public StepwiseSongProductionWorkflow() {
    this(
        Workflow.newActivityStub(
            SongProductionStepActivities.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(ACTIVITY_START_TO_CLOSE_TIMEOUT)
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(1).build())
                .build()));
  }

  public StepwiseSongProductionWorkflow(SongProductionStepActivities activities) {
    this.activities = Objects.requireNonNull(activities, "activities is required");
  }

  @Override
  public SongProductionWorkflowResult produce(SongProductionWorkflowInput input) {
    SongProductionActivityContext context = SongProductionActivityContext.from(input);

    SongProductionStepResult lockQuota =
        call(context, SongProductionStepName.LOCK_QUOTA, activities::lockQuota);
    if (!lockQuota.success()) {
      return failed(context, lockQuota);
    }

    SongProductionStepResult result =
        firstFailure(
            context,
            new StepPlan[] {
              new StepPlan(
                  SongProductionStepName.GENERATE_MUSIC_PROMPT, activities::generateMusicPrompt),
              new StepPlan(
                  SongProductionStepName.PRE_CHECK_MUSIC_PROMPT, activities::preCheckMusicPrompt),
              new StepPlan(SongProductionStepName.SUBMIT_MUSIC, activities::submitMusic),
              new StepPlan(SongProductionStepName.POLL_MUSIC, activities::pollMusic),
              new StepPlan(SongProductionStepName.IMPORT_AUDIO, activities::importAudio),
              new StepPlan(
                  SongProductionStepName.GENERATE_COVER_PROMPT, activities::generateCoverPrompt),
              new StepPlan(SongProductionStepName.GENERATE_COVER, activities::generateCover),
              new StepPlan(SongProductionStepName.RENDER_VIDEO, activities::renderVideo),
              new StepPlan(SongProductionStepName.EVALUATE_PACKAGE, activities::evaluatePackage),
              new StepPlan(
                  SongProductionStepName.PRE_CHECK_PUBLISH_PACKAGE,
                  activities::preCheckPublishPackage),
              new StepPlan(
                  SongProductionStepName.ASSEMBLE_PUBLISH_PACKAGE,
                  activities::assemblePublishPackage),
              new StepPlan(SongProductionStepName.COMMIT_QUOTA, activities::commitQuota)
            });
    if (!result.success()) {
      releaseQuota(context);
      return failed(context, result);
    }

    return SongProductionWorkflowResult.packageReady(context.jobId(), "PACKAGE_READY");
  }

  private SongProductionStepResult firstFailure(
      SongProductionActivityContext context, StepPlan[] steps) {
    SongProductionStepResult last = null;
    for (StepPlan step : steps) {
      last = call(context, step.stepName(), step.stepCall());
      if (!last.success()) {
        return last;
      }
    }
    return last;
  }

  private SongProductionStepResult call(
      SongProductionActivityContext context, SongProductionStepName stepName, StepCall stepCall) {
    try {
      SongProductionStepResult result = stepCall.apply(context);
      return normalize(context, stepName, result);
    } catch (RuntimeException exception) {
      return SongProductionStepResult.failed(
          context,
          stepName,
          stepName.name() + "_FAILED",
          "Step " + stepName.name() + " failed.",
          null);
    }
  }

  private SongProductionStepResult normalize(
      SongProductionActivityContext context,
      SongProductionStepName expectedStepName,
      SongProductionStepResult result) {
    if (result == null) {
      return SongProductionStepResult.failed(
          context,
          expectedStepName,
          expectedStepName.name() + "_FAILED",
          "Step " + expectedStepName.name() + " returned no result.",
          null);
    }
    if (result.stepName() != expectedStepName) {
      return SongProductionStepResult.failed(
          context,
          expectedStepName,
          "WORKFLOW_STEP_MISMATCH",
          "Step " + expectedStepName.name() + " returned an unexpected result.",
          result.references());
    }
    return result;
  }

  private void releaseQuota(SongProductionActivityContext context) {
    call(context, SongProductionStepName.RELEASE_QUOTA, activities::releaseQuota);
  }

  private SongProductionWorkflowResult failed(
      SongProductionActivityContext context, SongProductionStepResult result) {
    return SongProductionWorkflowResult.failed(
        context.jobId(),
        packageStatusForFailure(result),
        failureCode(result),
        failureMessage(result));
  }

  private String packageStatusForFailure(SongProductionStepResult result) {
    return "PACKAGE_BLOCKED".equals(result.failureCode()) ? "PACKAGE_BLOCKED" : "FAILED";
  }

  private String failureCode(SongProductionStepResult result) {
    return result.failureCode() == null || result.failureCode().isBlank()
        ? result.stepName().name() + "_FAILED"
        : result.failureCode();
  }

  private String failureMessage(SongProductionStepResult result) {
    return result.failureMessage() == null || result.failureMessage().isBlank()
        ? "Step " + result.stepName().name() + " failed."
        : result.failureMessage();
  }

  private interface StepCall {
    SongProductionStepResult apply(SongProductionActivityContext context);
  }

  private record StepPlan(SongProductionStepName stepName, StepCall stepCall) {}
}
