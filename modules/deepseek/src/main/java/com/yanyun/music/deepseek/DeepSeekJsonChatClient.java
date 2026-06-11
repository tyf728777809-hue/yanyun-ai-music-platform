package com.yanyun.music.deepseek;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class DeepSeekJsonChatClient {

  private static final Pattern BEARER_TOKEN_PATTERN =
      Pattern.compile("Bearer\\s+[A-Za-z0-9._~+/=-]+", Pattern.CASE_INSENSITIVE);
  private static final Pattern API_KEY_PATTERN =
      Pattern.compile("sk-[A-Za-z0-9_-]{8,}", Pattern.CASE_INSENSITIVE);

  private final DeepSeekProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public DeepSeekJsonChatClient(DeepSeekProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, HttpClient.newHttpClient());
  }

  DeepSeekJsonChatClient(
      DeepSeekProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
    if (properties == null) {
      throw new IllegalArgumentException("properties is required");
    }
    if (objectMapper == null) {
      throw new IllegalArgumentException("objectMapper is required");
    }
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient == null ? HttpClient.newHttpClient() : httpClient;
  }

  public JsonNode completeJson(
      String systemPrompt, String userPrompt, BigDecimal temperature, int responseMaxTokens) {
    ensureConfigured();
    HttpRequest httpRequest =
        HttpRequest.newBuilder(chatCompletionsUri())
            .timeout(requestTimeout())
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + properties.getApiKey())
            .POST(
                HttpRequest.BodyPublishers.ofByteArray(
                    writeJson(
                        requestBody(systemPrompt, userPrompt, temperature, responseMaxTokens))))
            .build();
    JsonNode root = sendWithRetry(httpRequest);
    String content = firstChoiceContent(root);
    return parseContentJson(content);
  }

  JsonNode parseContentJson(String content) {
    try {
      return objectMapper.readTree(content);
    } catch (JsonProcessingException exception) {
      String jsonObject = extractFirstJsonObject(content);
      if (jsonObject != null) {
        try {
          return objectMapper.readTree(jsonObject);
        } catch (JsonProcessingException ignored) {
          throw new IllegalStateException("DeepSeek response content JSON is invalid", exception);
        }
      }
      throw new IllegalStateException("DeepSeek response content JSON is invalid", exception);
    }
  }

  public String modelName() {
    return properties.getModelName();
  }

  private Map<String, Object> requestBody(
      String systemPrompt, String userPrompt, BigDecimal temperature, int responseMaxTokens) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", properties.getModelName());
    body.put(
        "messages",
        List.of(
            Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
            Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt)));
    body.put("response_format", Map.of("type", "json_object"));
    body.put("temperature", temperature == null ? properties.getTemperature() : temperature);
    body.put(
        "max_tokens",
        responseMaxTokens <= 0 ? properties.getResponseMaxTokens() : responseMaxTokens);
    return body;
  }

  private JsonNode sendWithRetry(HttpRequest request) {
    RuntimeException lastFailure = null;
    for (int attempt = 1; attempt <= properties.getMaxAttempts(); attempt++) {
      try {
        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= HttpURLConnection.HTTP_OK
            && response.statusCode() < HttpURLConnection.HTTP_MULT_CHOICE) {
          return objectMapper.readTree(response.body());
        }
        RuntimeException failure = httpFailure(response.statusCode(), response.body());
        if (response.statusCode() < 500 || attempt == properties.getMaxAttempts()) {
          throw failure;
        }
        lastFailure = failure;
      } catch (HttpTimeoutException exception) {
        lastFailure = new IllegalStateException("DeepSeek request timed out", exception);
      } catch (JsonProcessingException exception) {
        throw new IllegalStateException("DeepSeek response JSON is invalid", exception);
      } catch (IOException exception) {
        lastFailure = new IllegalStateException("DeepSeek request failed", exception);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("DeepSeek request interrupted", exception);
      }
    }
    throw lastFailure == null ? new IllegalStateException("DeepSeek request failed") : lastFailure;
  }

  private RuntimeException httpFailure(int statusCode, String body) {
    String fallback = "DeepSeek request failed with HTTP " + statusCode;
    if (body == null || body.isBlank()) {
      return new IllegalStateException(fallback);
    }
    try {
      JsonNode root = objectMapper.readTree(body);
      String providerMessage =
          DeepSeekAgentJson.firstNonBlank(
              DeepSeekAgentJson.text(root.path("error"), "message", "msg", "detail"),
              DeepSeekAgentJson.text(root, "message", "msg", "error"));
      return new IllegalStateException(
          sanitize(DeepSeekAgentJson.firstNonBlank(providerMessage, fallback)));
    } catch (JsonProcessingException exception) {
      return new IllegalStateException(fallback);
    }
  }

  private String firstChoiceContent(JsonNode root) {
    JsonNode choices = root.path("choices");
    if (!choices.isArray() || choices.isEmpty()) {
      throw new IllegalStateException("DeepSeek response did not include choices");
    }
    String content = choices.get(0).path("message").path("content").asText("");
    if (content.isBlank()) {
      throw new IllegalStateException("DeepSeek response content is empty");
    }
    return content;
  }

  private String extractFirstJsonObject(String content) {
    if (content == null || content.isBlank()) {
      return null;
    }
    int start = content.indexOf('{');
    if (start < 0) {
      return null;
    }
    boolean inString = false;
    boolean escaped = false;
    int depth = 0;
    for (int index = start; index < content.length(); index++) {
      char current = content.charAt(index);
      if (escaped) {
        escaped = false;
        continue;
      }
      if (current == '\\' && inString) {
        escaped = true;
        continue;
      }
      if (current == '"') {
        inString = !inString;
        continue;
      }
      if (inString) {
        continue;
      }
      if (current == '{') {
        depth++;
      } else if (current == '}') {
        depth--;
        if (depth == 0) {
          return content.substring(start, index + 1);
        }
      }
    }
    return null;
  }

  private void ensureConfigured() {
    if (!properties.isRealCallsEnabled()) {
      throw new IllegalStateException(
          "DeepSeek real calls are disabled; set DEEPSEEK_REAL_CALLS_ENABLED=true for manual integration");
    }
    if (!properties.isAgentRealCallsEnabled()) {
      throw new IllegalStateException(
          "AGENT_REAL_CALLS_ENABLED=true is required before DeepSeek real calls");
    }
    if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
      throw new IllegalStateException("DEEPSEEK_API_KEY is required for DeepSeek calls");
    }
    if (properties.getBaseUrl() == null) {
      throw new IllegalStateException("DEEPSEEK_BASE_URL is required for DeepSeek calls");
    }
    if (properties.getModelName() == null || properties.getModelName().isBlank()) {
      throw new IllegalStateException("DEEPSEEK_MODEL_NAME is required for DeepSeek calls");
    }
  }

  private URI chatCompletionsUri() {
    String base = properties.getBaseUrl().toString();
    if (!base.endsWith("/")) {
      base = base + "/";
    }
    return URI.create(base).resolve("chat/completions");
  }

  private Duration requestTimeout() {
    Duration timeout = properties.getRequestTimeout();
    return timeout == null || timeout.isNegative() ? Duration.ofSeconds(30) : timeout;
  }

  private byte[] writeJson(Object value) {
    try {
      return objectMapper.writeValueAsBytes(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("DeepSeek request JSON cannot be written", exception);
    }
  }

  private String sanitize(String value) {
    String sanitized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
    sanitized = BEARER_TOKEN_PATTERN.matcher(sanitized).replaceAll("Bearer <redacted>");
    sanitized = API_KEY_PATTERN.matcher(sanitized).replaceAll("<api-key-redacted>");
    return sanitized.length() <= 240 ? sanitized : sanitized.substring(0, 240);
  }
}
