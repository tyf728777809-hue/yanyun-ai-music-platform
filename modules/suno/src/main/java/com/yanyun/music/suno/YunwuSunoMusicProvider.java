package com.yanyun.music.suno;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.dreammaker.DreamMakerFailureMapper;
import com.yanyun.music.musicprovider.MusicGenerationRequest;
import com.yanyun.music.musicprovider.MusicGenerationResult;
import com.yanyun.music.musicprovider.MusicProvider;
import com.yanyun.music.musicprovider.MusicProviderType;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class YunwuSunoMusicProvider implements MusicProvider {

  private static final String CONTENT_TYPE = "audio/mpeg";

  private final YunwuProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public YunwuSunoMusicProvider(YunwuProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, HttpClient.newHttpClient());
  }

  YunwuSunoMusicProvider(
      YunwuProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
    this.properties = properties == null ? new YunwuProperties() : properties;
    this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    this.httpClient = httpClient == null ? HttpClient.newHttpClient() : httpClient;
  }

  @Override
  public MusicProviderType providerType() {
    return MusicProviderType.SUNO;
  }

  @Override
  public MusicGenerationResult submit(MusicGenerationRequest request) {
    try {
      ensureConfigured();
      JsonNode submitRoot = postJson(apiUri("/suno/submit/music"), requestBody(request));
      if (!accepted(submitRoot)) {
        return providerFailure(
            null,
            DreamMakerFailureMapper.fromProviderError(code(submitRoot), message(submitRoot)),
            DreamMakerFailureMapper.sanitizedMessage(
                "Yunwu Suno", message(submitRoot), "Yunwu Suno task submission failed"));
      }
      String taskId = taskId(submitRoot);
      if (!hasText(taskId)) {
        return providerFailure(
            null,
            DreamMakerFailureMapper.MUSIC_GENERATION_FAILED,
            "Yunwu Suno task submission succeeded without task id");
      }
      return pollUntilTerminal(taskId);
    } catch (YunwuSunoProviderException exception) {
      return providerFailure(null, exception.failureCode(), exception.getMessage());
    }
  }

  private Map<String, Object> requestBody(MusicGenerationRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("mv", properties.getSunoModel());
    body.put("make_instrumental", false);
    if (hasText(request.lyricsText())) {
      body.put("prompt", trimToLength(request.lyricsText(), 3000));
      body.put("tags", trimToLength(firstNonBlank(request.musicPrompt(), "国风,古风,民谣"), 200));
      body.put("title", trimToLength(providerOption(request, "title", "燕云原创歌曲"), 80));
    } else {
      body.put(
          "gpt_description_prompt",
          trimToLength(firstNonBlank(request.musicPrompt(), "写一首燕云主题原创歌曲"), 500));
    }
    return body;
  }

  private MusicGenerationResult pollUntilTerminal(String taskId) {
    JsonNode latest = null;
    for (int attempt = 0; attempt < properties.getMaxPollAttempts(); attempt++) {
      latest = getJson(apiUri("/suno/fetch/" + encodePath(taskId)));
      if (!accepted(latest)) {
        return providerFailure(
            taskId,
            DreamMakerFailureMapper.fromProviderError(code(latest), message(latest)),
            DreamMakerFailureMapper.sanitizedMessage(
                "Yunwu Suno", message(latest), "Yunwu Suno status polling failed"));
      }
      String status = status(latest);
      if (terminalSucceeded(status)) {
        return successFromStatus(taskId, latest);
      }
      if (terminalFailed(status)) {
        return providerFailure(
            taskId,
            DreamMakerFailureMapper.fromProviderError(code(latest), message(latest)),
            DreamMakerFailureMapper.sanitizedMessage(
                "Yunwu Suno",
                firstNonBlank(message(latest), status),
                "Yunwu Suno generation failed"));
      }
      sleepBeforeNextPoll();
    }
    String providerStatus = latest == null ? null : status(latest);
    String message =
        providerStatus == null
            ? "Yunwu Suno provider did not finish before timeout"
            : "Yunwu Suno provider did not finish before timeout: " + providerStatus;
    return providerFailure(taskId, DreamMakerFailureMapper.PROVIDER_TIMEOUT, message);
  }

  private MusicGenerationResult successFromStatus(String taskId, JsonNode root) {
    Optional<AudioOutput> audioOutput = firstAudioOutput(root);
    if (audioOutput.isEmpty()) {
      return providerFailure(
          taskId,
          DreamMakerFailureMapper.MUSIC_QUALITY_FAILED,
          "Yunwu Suno provider returned success without audio output");
    }
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("provider", "yunwu-suno");
    if (hasText(audioOutput.get().audioId())) {
      metadata.put("provider_audio_id", audioOutput.get().audioId());
      metadata.put("timestamped_lyrics_lookup", "task_id_and_audio_id");
    } else {
      metadata.put("timestamped_lyrics_lookup", "missing_audio_id");
    }
    return MusicGenerationResult.succeededFromSource(
        providerType(),
        taskId,
        modelName(),
        audioOutput.get().audioUrl(),
        CONTENT_TYPE,
        firstNonNull(audioOutput.get().durationMs(), durationMs(root)),
        "Yunwu Suno music generation succeeded",
        metadata);
  }

  private JsonNode postJson(URI uri, Map<String, Object> body) {
    byte[] bytes = writeJson(body);
    HttpRequest request =
        baseRequest(uri)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
            .build();
    return send(request);
  }

  private JsonNode getJson(URI uri) {
    return send(baseRequest(uri).GET().build());
  }

  private HttpRequest.Builder baseRequest(URI uri) {
    return HttpRequest.newBuilder(uri)
        .timeout(requestTimeout())
        .header("Accept", "application/json")
        .header("Authorization", "Bearer " + properties.getApiKey());
  }

  private JsonNode send(HttpRequest request) {
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < HttpURLConnection.HTTP_OK
          || response.statusCode() >= HttpURLConnection.HTTP_MULT_CHOICE) {
        throw new YunwuSunoProviderException(
            DreamMakerFailureMapper.fromHttpStatus(response.statusCode()),
            responseFailureMessage(response.statusCode(), response.body()));
      }
      return objectMapper.readTree(response.body());
    } catch (YunwuSunoProviderException exception) {
      throw exception;
    } catch (HttpTimeoutException exception) {
      throw new YunwuSunoProviderException(
          DreamMakerFailureMapper.PROVIDER_TIMEOUT, "Yunwu Suno request timed out", exception);
    } catch (JsonProcessingException exception) {
      throw new YunwuSunoProviderException(
          DreamMakerFailureMapper.MUSIC_GENERATION_FAILED,
          "Yunwu Suno response JSON is invalid",
          exception);
    } catch (IOException exception) {
      throw new YunwuSunoProviderException(
          DreamMakerFailureMapper.MUSIC_GENERATION_FAILED, "Yunwu Suno request failed", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new YunwuSunoProviderException(
          DreamMakerFailureMapper.PROVIDER_TIMEOUT, "Yunwu Suno request interrupted", exception);
    }
  }

  private String responseFailureMessage(int statusCode, String body) {
    String fallback = "Yunwu Suno request failed with HTTP " + statusCode;
    if (body == null || body.isBlank()) {
      return fallback;
    }
    try {
      JsonNode root = objectMapper.readTree(body);
      return DreamMakerFailureMapper.sanitizedMessage("Yunwu Suno", message(root), fallback);
    } catch (JsonProcessingException exception) {
      return fallback;
    }
  }

  private boolean accepted(JsonNode root) {
    String providerCode = text(root, "code");
    if (hasText(providerCode)) {
      if (!successCode(providerCode)) {
        return false;
      }
    } else {
      int code = code(root);
      if (code > 0) {
        return false;
      }
    }
    String status = status(root);
    if (terminalFailed(status)) {
      return false;
    }
    return true;
  }

  private int code(JsonNode root) {
    JsonNode node = firstPresent(root, "code", "err_code", "error_code", "status_code");
    return node == null || !node.canConvertToInt() ? 0 : node.asInt(0);
  }

  private String message(JsonNode root) {
    String direct = text(root, "message", "msg", "error", "detail");
    if (hasText(direct)) {
      return direct;
    }
    return text(root.path("data"), "message", "msg", "error", "detail");
  }

  private String status(JsonNode root) {
    String direct = text(root, "status", "state", "task_status", "taskStatus");
    if (hasText(direct)) {
      return direct;
    }
    JsonNode data = root.path("data");
    String dataStatus = text(data, "status", "state", "task_status", "taskStatus");
    if (hasText(dataStatus)) {
      return dataStatus;
    }
    return text(data.path("task"), "status", "state", "task_status", "taskStatus");
  }

  private String taskId(JsonNode root) {
    String direct = text(root, "task_id", "taskId", "id");
    if (hasText(direct)) {
      return direct;
    }
    JsonNode data = root.path("data");
    if (data.isTextual() && hasText(data.asText())) {
      return data.asText().trim();
    }
    String fromData = text(data, "task_id", "taskId", "id");
    if (hasText(fromData)) {
      return fromData;
    }
    return text(data.path("task"), "task_id", "taskId", "id");
  }

  private boolean terminalSucceeded(String status) {
    String normalized = normalize(status);
    return normalized.equals("success")
        || normalized.equals("succeeded")
        || normalized.equals("complete")
        || normalized.equals("completed")
        || normalized.equals("done")
        || normalized.equals("finished");
  }

  private boolean terminalFailed(String status) {
    String normalized = normalize(status);
    return normalized.equals("fail")
        || normalized.equals("failed")
        || normalized.equals("error")
        || normalized.equals("rejected")
        || normalized.equals("cancelled")
        || normalized.equals("canceled");
  }

  private boolean successCode(String value) {
    String normalized = normalize(value);
    return normalized.equals("0")
        || normalized.equals("200")
        || normalized.equals("ok")
        || normalized.equals("success")
        || normalized.equals("succeeded");
  }

  private Optional<String> firstUrlByField(JsonNode root, boolean requireAudioFieldName) {
    List<String> urls = new ArrayList<>();
    collectUrls(root, requireAudioFieldName, urls);
    return urls.stream().findFirst();
  }

  private Optional<String> firstAudioLikeUrl(JsonNode root) {
    List<String> urls = new ArrayList<>();
    collectUrls(root, false, urls);
    return urls.stream().filter(this::isAudioUrl).findFirst().or(() -> urls.stream().findFirst());
  }

  private Optional<AudioOutput> firstAudioOutput(JsonNode root) {
    List<AudioOutput> outputs = new ArrayList<>();
    collectAudioOutputs(root, outputs);
    return outputs.stream()
        .filter(output -> isAudioUrl(output.audioUrl()))
        .findFirst()
        .or(() -> outputs.stream().findFirst())
        .or(
            () ->
                firstUrlByField(root, true)
                    .or(() -> firstAudioLikeUrl(root))
                    .map(url -> new AudioOutput(url, null, durationMs(root))));
  }

  private void collectAudioOutputs(JsonNode node, List<AudioOutput> outputs) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return;
    }
    if (node.isObject()) {
      String audioUrl = directAudioUrl(node);
      if (hasText(audioUrl)) {
        outputs.add(new AudioOutput(audioUrl, directAudioId(node), durationMs(node)));
      }
      node.fields().forEachRemaining(entry -> collectAudioOutputs(entry.getValue(), outputs));
      return;
    }
    if (node.isArray()) {
      node.forEach(value -> collectAudioOutputs(value, outputs));
    }
  }

  private String directAudioUrl(JsonNode node) {
    var fields = node.fields();
    while (fields.hasNext()) {
      var entry = fields.next();
      String fieldName = entry.getKey().toLowerCase(Locale.ROOT);
      JsonNode value = entry.getValue();
      if (!value.isTextual()) {
        continue;
      }
      String text = value.asText("");
      boolean urlField = fieldName.contains("url");
      boolean audioField = fieldName.contains("audio") || fieldName.contains("song");
      if (hasText(text) && isHttpUrl(text) && urlField && audioField) {
        return text.trim();
      }
    }
    return null;
  }

  private String directAudioId(JsonNode node) {
    String explicit = text(node, "audio_id", "audioId", "clip_id", "clipId");
    if (hasText(explicit)) {
      return explicit.trim();
    }
    String id = text(node, "id");
    return hasText(id) ? id.trim() : null;
  }

  private void collectUrls(JsonNode node, boolean requireAudioFieldName, List<String> urls) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return;
    }
    if (node.isObject()) {
      node.fields()
          .forEachRemaining(
              entry -> {
                String fieldName = entry.getKey().toLowerCase(Locale.ROOT);
                JsonNode value = entry.getValue();
                if (value.isTextual()) {
                  String text = value.asText("");
                  boolean urlField = fieldName.contains("url");
                  boolean audioField = fieldName.contains("audio") || fieldName.contains("song");
                  if (hasText(text)
                      && isHttpUrl(text)
                      && urlField
                      && (!requireAudioFieldName || audioField)) {
                    urls.add(text.trim());
                  }
                }
                collectUrls(value, requireAudioFieldName, urls);
              });
      return;
    }
    if (node.isArray()) {
      node.forEach(value -> collectUrls(value, requireAudioFieldName, urls));
    }
  }

  private Integer durationMs(JsonNode root) {
    JsonNode node = firstPresentDeep(root, "duration_ms", "durationMs");
    if (node != null && node.isNumber()) {
      return node.asInt();
    }
    JsonNode seconds = firstPresentDeep(root, "duration");
    if (seconds == null || seconds.isMissingNode() || seconds.isNull()) {
      return null;
    }
    String value = seconds.isNumber() ? seconds.asText() : seconds.asText("");
    if (value.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(value.trim()).multiply(BigDecimal.valueOf(1000L)).intValue();
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private JsonNode firstPresent(JsonNode node, String... names) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    for (String name : names) {
      JsonNode value = node.path(name);
      if (!value.isMissingNode() && !value.isNull()) {
        return value;
      }
    }
    return null;
  }

  private JsonNode firstPresentDeep(JsonNode node, String... names) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    JsonNode current = firstPresent(node, names);
    if (current != null) {
      return current;
    }
    if (node.isObject()) {
      var fields = node.fields();
      while (fields.hasNext()) {
        JsonNode found = firstPresentDeep(fields.next().getValue(), names);
        if (found != null) {
          return found;
        }
      }
      return null;
    }
    if (node.isArray()) {
      for (JsonNode item : node) {
        JsonNode found = firstPresentDeep(item, names);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  private String text(JsonNode node, String... fieldNames) {
    JsonNode value = firstPresent(node, fieldNames);
    if (value == null || value.isMissingNode() || value.isNull() || value.asText("").isBlank()) {
      return null;
    }
    return value.asText();
  }

  private byte[] writeJson(Object value) {
    try {
      return objectMapper.writeValueAsBytes(value);
    } catch (JsonProcessingException exception) {
      throw new YunwuSunoProviderException(
          DreamMakerFailureMapper.MUSIC_GENERATION_FAILED,
          "Yunwu Suno request JSON cannot be written",
          exception);
    }
  }

  private URI apiUri(String path) {
    String normalizedBase = properties.getBaseUrl().toString();
    if (!normalizedBase.endsWith("/")) {
      normalizedBase += "/";
    }
    String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
    return URI.create(normalizedBase).resolve(normalizedPath);
  }

  private String encodePath(String value) {
    return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private String providerOption(MusicGenerationRequest request, String key, String fallback) {
    Object value = request.providerOptions().get(key);
    if (value instanceof String text && hasText(text)) {
      return text.trim();
    }
    return fallback;
  }

  private void ensureConfigured() {
    if (!properties.isRealCallsEnabled()) {
      throw new YunwuSunoProviderException(
          DreamMakerFailureMapper.MUSIC_GENERATION_FAILED,
          "Yunwu Suno real calls are disabled; set YUNWU_REAL_CALLS_ENABLED=true for manual integration");
    }
    if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
      throw new YunwuSunoProviderException(
          DreamMakerFailureMapper.PROVIDER_AUTH_FAILED,
          "YUNWU_API_KEY is required for Yunwu Suno calls");
    }
  }

  private Duration requestTimeout() {
    Duration timeout = properties.getRequestTimeout();
    return timeout == null || timeout.isNegative() ? Duration.ofSeconds(30) : timeout;
  }

  private MusicGenerationResult providerFailure(
      String taskId, String failureCode, String failureMessage) {
    return MusicGenerationResult.failed(
        providerType(), taskId, modelName(), failureCode, failureMessage);
  }

  private String modelName() {
    return "yunwu:suno:" + properties.getSunoModel();
  }

  private void sleepBeforeNextPoll() {
    Duration interval = properties.getPollInterval();
    if (interval == null || interval.isZero()) {
      return;
    }
    try {
      Thread.sleep(interval.toMillis());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new YunwuSunoProviderException(
          DreamMakerFailureMapper.PROVIDER_TIMEOUT,
          "Yunwu Suno polling was interrupted",
          exception);
    }
  }

  private boolean isHttpUrl(String value) {
    return value.startsWith("http://") || value.startsWith("https://");
  }

  private boolean isAudioUrl(String value) {
    String normalized = value.toLowerCase(Locale.ROOT);
    return normalized.contains(".mp3")
        || normalized.contains(".wav")
        || normalized.contains(".m4a")
        || normalized.contains(".aac")
        || normalized.contains(".flac")
        || normalized.contains(".ogg");
  }

  private String firstNonBlank(String value, String fallback) {
    return hasText(value) ? value.trim() : fallback;
  }

  private Integer firstNonNull(Integer value, Integer fallback) {
    return value == null ? fallback : value;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String trimToLength(String value, int maxLength) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private static final class YunwuSunoProviderException extends RuntimeException {

    private final String failureCode;

    private YunwuSunoProviderException(String failureCode, String message) {
      super(message);
      this.failureCode = failureCode;
    }

    private YunwuSunoProviderException(String failureCode, String message, Throwable cause) {
      super(message, cause);
      this.failureCode = failureCode;
    }

    private String failureCode() {
      return failureCode;
    }
  }

  private record AudioOutput(String audioUrl, String audioId, Integer durationMs) {}
}
