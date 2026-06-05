package com.yanyun.music.dreammaker;

import java.util.Map;

public record DreamMakerRunRequest(String appName, String subAppName, Map<String, Object> params) {

  public DreamMakerRunRequest {
    appName = requireText(appName, "appName");
    subAppName = requireText(subAppName, "subAppName");
    params = params == null ? Map.of() : Map.copyOf(params);
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    return value.trim();
  }
}
