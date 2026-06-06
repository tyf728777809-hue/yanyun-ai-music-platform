package com.yanyun.music.workdomain;

import java.util.ArrayList;
import java.util.List;

public final class WorkStateMachine {

  private WorkStateMachine() {}

  public static List<AvailableAction> availableActions(WorkSnapshot work) {
    List<AvailableAction> actions = new ArrayList<>();

    switch (work.status()) {
      case DRAFT -> actions.add(AvailableAction.RETURN_TO_EDIT);
      case LYRICS_READY -> {
        actions.add(AvailableAction.POLISH_LYRICS);
        actions.add(AvailableAction.CONTINUE_LYRICS);
        actions.add(AvailableAction.CONFIRM_WORK);
        actions.add(AvailableAction.RETURN_TO_EDIT);
      }
      case LYRICS_FAILED -> {
        actions.add(AvailableAction.RETRY_LYRICS);
        actions.add(AvailableAction.RETURN_TO_EDIT);
      }
      case GENERATED -> appendGeneratedActions(actions, work.packageStatus());
      case FAILED ->
          appendFailureActions(
              actions, work.failureCode(), work.retryable(), work.remainingMusicRetryCount());
      case CANCELLED -> actions.add(AvailableAction.RETURN_TO_EDIT);
      case LYRICS_GENERATING, GENERATING -> {
        // In-flight states intentionally expose no mutating action in v0.1.
      }
    }

    return List.copyOf(actions);
  }

  public static boolean canConfirm(WorkStatus status) {
    return status == WorkStatus.LYRICS_READY;
  }

  public static boolean canRetryMusic(WorkSnapshot work) {
    if (work.status() != WorkStatus.FAILED
        || !work.retryable()
        || work.failureCode() == null
        || work.remainingMusicRetryCount() <= 0) {
      return false;
    }
    return switch (work.failureCode()) {
      case MUSIC_GENERATION_FAILED, MUSIC_QUALITY_FAILED, PROVIDER_TIMEOUT, RATE_LIMITED -> true;
      default -> false;
    };
  }

  public static boolean canEditLyrics(WorkStatus status) {
    return status == WorkStatus.DRAFT || status == WorkStatus.LYRICS_READY;
  }

  public static WorkSnapshot confirmLyrics() {
    return new WorkSnapshot(
        WorkStatus.GENERATING,
        GenerationStage.QUOTA_LOCKING,
        PackageStatus.PACKAGE_NOT_READY,
        null,
        true,
        0);
  }

  public static WorkSnapshot packageReady() {
    return new WorkSnapshot(
        WorkStatus.GENERATED,
        GenerationStage.PACKAGE_READY,
        PackageStatus.PACKAGE_READY,
        null,
        true,
        0);
  }

  public static WorkSnapshot failed(FailureCode failureCode, boolean retryable) {
    return new WorkSnapshot(
        WorkStatus.FAILED,
        GenerationStage.FAILED,
        PackageStatus.PACKAGE_NOT_READY,
        failureCode,
        retryable,
        retryable ? 1 : 0);
  }

  private static void appendGeneratedActions(
      List<AvailableAction> actions, PackageStatus packageStatus) {
    actions.add(AvailableAction.RERENDER_VIDEO);

    switch (packageStatus) {
      case PACKAGE_READY -> {
        actions.add(AvailableAction.REFRESH_PACKAGE_URL);
        actions.add(AvailableAction.MARK_PACKAGE_FETCHED);
      }
      case PACKAGE_FETCHED, PACKAGE_EXPIRED -> actions.add(AvailableAction.REFRESH_PACKAGE_URL);
      case PACKAGE_BLOCKED -> actions.add(AvailableAction.CONTACT_SUPPORT);
      case PACKAGE_NOT_READY -> {
        // No publish handoff action until the package is ready.
      }
    }
  }

  private static void appendFailureActions(
      List<AvailableAction> actions,
      FailureCode failureCode,
      boolean retryable,
      int remainingMusicRetryCount) {
    if (!retryable || failureCode == null) {
      actions.add(AvailableAction.CONTACT_SUPPORT);
      actions.add(AvailableAction.RETURN_TO_EDIT);
      return;
    }

    switch (failureCode) {
      case LYRICS_GENERATION_FAILED, LYRICS_PRECHECK_FAILED ->
          actions.add(AvailableAction.RETRY_LYRICS);
      case MUSIC_GENERATION_FAILED, MUSIC_QUALITY_FAILED, PROVIDER_TIMEOUT, RATE_LIMITED -> {
        if (remainingMusicRetryCount > 0) {
          actions.add(AvailableAction.RETRY_MUSIC);
        } else {
          actions.add(AvailableAction.CONTACT_SUPPORT);
        }
      }
      case COVER_GENERATION_FAILED -> actions.add(AvailableAction.RETRY_COVER);
      case VIDEO_RENDER_FAILED, PACKAGE_BUILD_FAILED -> actions.add(AvailableAction.RERENDER_VIDEO);
      case USER_INPUT_BLOCKED -> actions.add(AvailableAction.RETURN_TO_EDIT);
      case PACKAGE_BLOCKED, PROVIDER_AUTH_FAILED, QUOTA_LOCK_FAILED, UNKNOWN_ERROR ->
          actions.add(AvailableAction.CONTACT_SUPPORT);
    }

    actions.add(AvailableAction.RETURN_TO_EDIT);
  }
}
