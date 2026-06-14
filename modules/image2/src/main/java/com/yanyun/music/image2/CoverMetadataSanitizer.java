package com.yanyun.music.image2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class CoverMetadataSanitizer {

  private static final Set<String> SAFE_PROVIDER_OPTION_KEYS =
      Set.of(
          "text_prompt",
          "typography_requirements",
          "text_policy",
          "title_text_policy",
          "composition",
          "color_palette",
          "subject",
          "lighting",
          "style",
          "quality");

  private CoverMetadataSanitizer() {}

  static Map<String, Object> safeProviderOptions(Map<String, Object> providerOptions) {
    if (providerOptions == null || providerOptions.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> safe = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : providerOptions.entrySet()) {
      if (SAFE_PROVIDER_OPTION_KEYS.contains(entry.getKey()) && entry.getValue() != null) {
        safe.put(entry.getKey(), entry.getValue());
      }
    }
    return safe;
  }
}
