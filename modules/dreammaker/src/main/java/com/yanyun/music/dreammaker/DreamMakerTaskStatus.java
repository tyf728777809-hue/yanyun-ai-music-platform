package com.yanyun.music.dreammaker;

import java.util.Locale;

public enum DreamMakerTaskStatus {
  QUEUED,
  RUNNING,
  UNKNOWN,
  SUCCEEDED,
  FAILED;

  public static DreamMakerTaskStatus fromProviderStatus(String value) {
    if (value == null || value.isBlank()) {
      return UNKNOWN;
    }
    return switch (value.trim().toLowerCase(Locale.ROOT)) {
      case "queued", "queue", "pending", "created" -> QUEUED;
      case "running", "processing", "in_progress" -> RUNNING;
      case "success", "succeeded", "completed", "complete" -> SUCCEEDED;
      case "failed", "failure", "error", "cancelled", "canceled" -> FAILED;
      default -> UNKNOWN;
    };
  }
}
