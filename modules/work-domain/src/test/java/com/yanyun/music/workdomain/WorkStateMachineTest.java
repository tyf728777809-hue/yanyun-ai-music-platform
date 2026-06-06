package com.yanyun.music.workdomain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WorkStateMachineTest {

  @Test
  void lyricsReadyWorkCanBeConfirmedAndEdited() {
    List<AvailableAction> actions =
        WorkStateMachine.availableActions(
            new WorkSnapshot(
                WorkStatus.LYRICS_READY,
                GenerationStage.WAITING_CONFIRM,
                PackageStatus.PACKAGE_NOT_READY,
                null,
                true,
                0));

    assertTrue(actions.contains(AvailableAction.CONFIRM_WORK));
    assertTrue(actions.contains(AvailableAction.POLISH_LYRICS));
    assertTrue(actions.contains(AvailableAction.CONTINUE_LYRICS));
    assertTrue(WorkStateMachine.canConfirm(WorkStatus.LYRICS_READY));
  }

  @Test
  void generatedWorkWithReadyPackageCanBeHandedOff() {
    List<AvailableAction> actions =
        WorkStateMachine.availableActions(
            new WorkSnapshot(
                WorkStatus.GENERATED,
                GenerationStage.PACKAGE_READY,
                PackageStatus.PACKAGE_READY,
                null,
                true,
                0));

    assertTrue(actions.contains(AvailableAction.REFRESH_PACKAGE_URL));
    assertTrue(actions.contains(AvailableAction.MARK_PACKAGE_FETCHED));
    assertTrue(actions.contains(AvailableAction.RERENDER_VIDEO));
  }

  @Test
  void nonRetryableFailureOnlyAllowsSupportOrEdit() {
    List<AvailableAction> actions =
        WorkStateMachine.availableActions(
            new WorkSnapshot(
                WorkStatus.FAILED,
                GenerationStage.FAILED,
                PackageStatus.PACKAGE_NOT_READY,
                FailureCode.PACKAGE_BLOCKED,
                false,
                0));

    assertTrue(actions.contains(AvailableAction.CONTACT_SUPPORT));
    assertTrue(actions.contains(AvailableAction.RETURN_TO_EDIT));
    assertFalse(actions.contains(AvailableAction.RETRY_MUSIC));
  }

  @Test
  void retryableMusicFailureAllowsRetryMusic() {
    WorkSnapshot work =
        new WorkSnapshot(
            WorkStatus.FAILED,
            GenerationStage.FAILED,
            PackageStatus.PACKAGE_NOT_READY,
            FailureCode.MUSIC_GENERATION_FAILED,
            true,
            2);

    List<AvailableAction> actions = WorkStateMachine.availableActions(work);

    assertTrue(actions.contains(AvailableAction.RETRY_MUSIC));
    assertTrue(actions.contains(AvailableAction.RETURN_TO_EDIT));
    assertTrue(WorkStateMachine.canRetryMusic(work));
  }

  @Test
  void retryMusicRejectsRetryableNonMusicFailure() {
    WorkSnapshot work =
        new WorkSnapshot(
            WorkStatus.FAILED,
            GenerationStage.FAILED,
            PackageStatus.PACKAGE_NOT_READY,
            FailureCode.PACKAGE_BUILD_FAILED,
            true,
            2);

    assertFalse(WorkStateMachine.canRetryMusic(work));
  }

  @Test
  void providerAuthFailureNeverAllowsMusicRetryEvenIfMarkedRetryable() {
    WorkSnapshot work =
        new WorkSnapshot(
            WorkStatus.FAILED,
            GenerationStage.FAILED,
            PackageStatus.PACKAGE_NOT_READY,
            FailureCode.PROVIDER_AUTH_FAILED,
            true,
            2);

    List<AvailableAction> actions = WorkStateMachine.availableActions(work);

    assertFalse(actions.contains(AvailableAction.RETRY_MUSIC));
    assertTrue(actions.contains(AvailableAction.CONTACT_SUPPORT));
    assertTrue(actions.contains(AvailableAction.RETURN_TO_EDIT));
    assertFalse(WorkStateMachine.canRetryMusic(work));
  }

  @Test
  void retryMusicRejectsExhaustedMusicFailure() {
    WorkSnapshot work =
        new WorkSnapshot(
            WorkStatus.FAILED,
            GenerationStage.FAILED,
            PackageStatus.PACKAGE_NOT_READY,
            FailureCode.MUSIC_GENERATION_FAILED,
            true,
            0);

    List<AvailableAction> actions = WorkStateMachine.availableActions(work);

    assertFalse(actions.contains(AvailableAction.RETRY_MUSIC));
    assertTrue(actions.contains(AvailableAction.CONTACT_SUPPORT));
    assertFalse(WorkStateMachine.canRetryMusic(work));
  }
}
