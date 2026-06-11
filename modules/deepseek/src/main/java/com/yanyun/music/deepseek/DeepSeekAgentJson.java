package com.yanyun.music.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.creativeagent.CreativeDomainDecision;
import com.yanyun.music.creativeagent.QualityDecision;
import com.yanyun.music.creativeagent.QualityGate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class DeepSeekAgentJson {

  private DeepSeekAgentJson() {}

  static String text(JsonNode node, String... fieldNames) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    for (String fieldName : fieldNames) {
      JsonNode value = node.path(fieldName);
      if (!value.isMissingNode() && !value.isNull() && !value.asText("").isBlank()) {
        return value.asText();
      }
    }
    return null;
  }

  static String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  static List<String> stringList(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return List.of();
    }
    if (!node.isArray()) {
      String value = node.asText("");
      return value.isBlank() ? List.of() : List.of(value);
    }
    List<String> values = new ArrayList<>();
    for (JsonNode item : node) {
      String value = item.asText("");
      if (!value.isBlank()) {
        values.add(value);
      }
    }
    return values;
  }

  static Map<String, Object> objectMap(ObjectMapper objectMapper, JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull() || !node.isObject()) {
      return Map.of();
    }
    return objectMapper.convertValue(
        node, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
  }

  static CreativeDomainDecision creativeDomainDecision(String value) {
    if (value == null || value.isBlank()) {
      return CreativeDomainDecision.PASS;
    }
    try {
      return CreativeDomainDecision.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      return CreativeDomainDecision.PASS;
    }
  }

  static QualityGate qualityGate(String value, QualityGate fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    try {
      return QualityGate.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      return fallback;
    }
  }

  static QualityDecision qualityDecision(String value) {
    if (value == null || value.isBlank()) {
      return QualityDecision.PASS;
    }
    try {
      return QualityDecision.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException exception) {
      return QualityDecision.MANUAL_REVIEW;
    }
  }
}
