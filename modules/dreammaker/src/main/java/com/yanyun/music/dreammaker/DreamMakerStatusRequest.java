package com.yanyun.music.dreammaker;

public record DreamMakerStatusRequest(String appName, String subAppName, String taskId) {

  public DreamMakerStatusRequest {
    appName = requireText(appName, "appName");
    subAppName = requireText(subAppName, "subAppName");
    taskId = requireText(taskId, "taskId");
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }
}
