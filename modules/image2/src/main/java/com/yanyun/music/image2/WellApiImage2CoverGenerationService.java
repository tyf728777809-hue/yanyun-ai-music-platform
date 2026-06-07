package com.yanyun.music.image2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.media.MediaAssetDescriptor;
import java.io.IOException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WellApiImage2CoverGenerationService implements CoverGenerationService {

  private static final Pattern HTTP_URL_PATTERN = Pattern.compile("https?://[^\\s\"'<>]+");

  private final Image2Properties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public WellApiImage2CoverGenerationService(
      Image2Properties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, HttpClient.newHttpClient());
  }

  WellApiImage2CoverGenerationService(
      Image2Properties properties, ObjectMapper objectMapper, HttpClient httpClient) {
    this.properties = properties == null ? new Image2Properties() : properties;
    this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    this.httpClient = httpClient == null ? HttpClient.newHttpClient() : httpClient;
  }

  @Override
  public CoverGenerationResult generateCover(CoverGenerationRequest request) {
    ensureConfigured();
    JsonNode root = postJson(apiUri("/v1/images/generations"), requestBody(request));
    Optional<String> imageUrl = firstImageUrl(root);
    if (imageUrl.isPresent()) {
      return success(request, imageUrl.get(), null, providerTaskId(root));
    }
    Optional<String> inlineBase64 = firstTextByField(root, "b64_json", "b64Json");
    if (inlineBase64.isPresent()) {
      return success(request, null, inlineBase64.get(), providerTaskId(root));
    }
    throw new IllegalStateException("WellAPI gpt-image-2 returned success without image output");
  }

  private Map<String, Object> requestBody(CoverGenerationRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", properties.getModelName());
    body.put(
        "prompt", trimToLength(firstNonBlank(request.visualPrompt(), request.songSummary()), 1000));
    body.put("n", 1);
    body.put("size", outputSize(request));
    body.put("quality", properties.getQuality());
    body.put("format", normalizedFormat());
    return body;
  }

  private CoverGenerationResult success(
      CoverGenerationRequest request, String imageUrl, String inlineBase64, String providerTaskId) {
    String size = outputSize(request);
    int width = width(size);
    int height = height(size);
    String format =
        imageUrl == null ? normalizedFormat() : formatFromUrl(imageUrl).orElse(normalizedFormat());
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("provider", "wellapi-image2");
    metadata.put("model", properties.getModelName());
    if (hasText(providerTaskId)) {
      metadata.put("provider_task_id", providerTaskId);
    }
    metadata.put("base_url", properties.getBaseUrl().toString());
    metadata.put("size", size);
    metadata.put("quality", properties.getQuality());
    metadata.put("output_format", format);
    if (imageUrl != null) {
      metadata.put(SOURCE_URL_METADATA_KEY, imageUrl);
    }
    if (inlineBase64 != null) {
      metadata.put(INLINE_BASE64_METADATA_KEY, inlineBase64);
    }
    metadata.putAll(request.providerOptions());
    return new CoverGenerationResult(
        new MediaAssetDescriptor(
            "COVER",
            "covers/" + request.workId() + "." + format,
            mimeType(format),
            0L,
            "wellapi-image2",
            width,
            height,
            null,
            metadata));
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
        throw new IllegalStateException(
            responseFailureMessage(response.statusCode(), response.body()));
      }
      return objectMapper.readTree(response.body());
    } catch (HttpTimeoutException exception) {
      throw new IllegalStateException("WellAPI gpt-image-2 request timed out", exception);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("WellAPI gpt-image-2 response JSON is invalid", exception);
    } catch (IOException exception) {
      throw new IllegalStateException("WellAPI gpt-image-2 request failed", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("WellAPI gpt-image-2 request interrupted", exception);
    }
  }

  private Optional<String> firstImageUrl(JsonNode root) {
    List<String> urls = new ArrayList<>();
    collectUrls(root, urls);
    return urls.stream()
        .filter(this::looksLikeImageUrl)
        .findFirst()
        .or(() -> urls.stream().findFirst());
  }

  private void collectUrls(JsonNode node, List<String> urls) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return;
    }
    if (node.isTextual()) {
      Matcher matcher = HTTP_URL_PATTERN.matcher(node.asText(""));
      while (matcher.find()) {
        urls.add(matcher.group());
      }
      return;
    }
    if (node.isObject()) {
      node.fields()
          .forEachRemaining(
              entry -> {
                String fieldName = entry.getKey().toLowerCase(Locale.ROOT);
                JsonNode value = entry.getValue();
                if (value.isTextual()
                    && fieldName.contains("url")
                    && value.asText("").startsWith("http")) {
                  urls.add(value.asText("").trim());
                }
                collectUrls(value, urls);
              });
      return;
    }
    if (node.isArray()) {
      node.forEach(value -> collectUrls(value, urls));
    }
  }

  private Optional<String> firstTextByField(JsonNode node, String... fieldNames) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return Optional.empty();
    }
    if (node.isObject()) {
      for (String fieldName : fieldNames) {
        JsonNode value = node.path(fieldName);
        if (!value.isMissingNode()
            && !value.isNull()
            && value.isTextual()
            && hasText(value.asText())) {
          return Optional.of(value.asText().trim());
        }
      }
      var fields = node.fields();
      while (fields.hasNext()) {
        Optional<String> found = firstTextByField(fields.next().getValue(), fieldNames);
        if (found.isPresent()) {
          return found;
        }
      }
      return Optional.empty();
    }
    if (node.isArray()) {
      for (JsonNode value : node) {
        Optional<String> found = firstTextByField(value, fieldNames);
        if (found.isPresent()) {
          return found;
        }
      }
    }
    return Optional.empty();
  }

  private String providerTaskId(JsonNode root) {
    String direct = text(root, "id", "task_id", "taskId");
    if (hasText(direct)) {
      return direct;
    }
    return text(root.path("data"), "id", "task_id", "taskId");
  }

  private String outputSize(CoverGenerationRequest request) {
    if (validImage2Size(request.width(), request.height())) {
      return request.width() + "x" + request.height();
    }
    return properties.getSize();
  }

  private boolean validImage2Size(int width, int height) {
    if (width <= 0 || height <= 0) {
      return false;
    }
    long pixels = (long) width * (long) height;
    int longest = Math.max(width, height);
    int shortest = Math.min(width, height);
    return width % 16 == 0
        && height % 16 == 0
        && longest <= 3840
        && pixels >= 655_360L
        && pixels <= 8_294_400L
        && longest <= shortest * 3L;
  }

  private int width(String size) {
    int separator = size.indexOf('x');
    if (separator < 0) {
      return 2048;
    }
    return parsePositiveInt(size.substring(0, separator), 2048);
  }

  private int height(String size) {
    int separator = size.indexOf('x');
    if (separator < 0) {
      return 1152;
    }
    return parsePositiveInt(size.substring(separator + 1), 1152);
  }

  private int parsePositiveInt(String value, int fallback) {
    try {
      int parsed = Integer.parseInt(value.trim());
      return parsed > 0 ? parsed : fallback;
    } catch (NumberFormatException exception) {
      return fallback;
    }
  }

  private Optional<String> formatFromUrl(String imageUrl) {
    String normalized = imageUrl.toLowerCase(Locale.ROOT);
    if (normalized.contains(".jpg") || normalized.contains(".jpeg")) {
      return Optional.of("jpeg");
    }
    if (normalized.contains(".webp")) {
      return Optional.of("webp");
    }
    if (normalized.contains(".png")) {
      return Optional.of("png");
    }
    return Optional.empty();
  }

  private boolean looksLikeImageUrl(String imageUrl) {
    return formatFromUrl(imageUrl).isPresent();
  }

  private String normalizedFormat() {
    String format = properties.getOutputFormat().trim().toLowerCase(Locale.ROOT);
    return switch (format) {
      case "jpg", "jpeg" -> "jpeg";
      case "webp" -> "webp";
      default -> "png";
    };
  }

  private String mimeType(String format) {
    return switch (format) {
      case "jpeg" -> "image/jpeg";
      case "webp" -> "image/webp";
      default -> "image/png";
    };
  }

  private String text(JsonNode node, String... fieldNames) {
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

  private String responseFailureMessage(int statusCode, String body) {
    String fallback = "WellAPI gpt-image-2 request failed with HTTP " + statusCode;
    if (body == null || body.isBlank()) {
      return fallback;
    }
    try {
      JsonNode root = objectMapper.readTree(body);
      String message = firstNonBlank(text(root, "message", "msg", "error", "detail"), fallback);
      String sanitized = message.replaceAll("[\\r\\n\\t]+", " ").trim();
      sanitized = sanitized.replaceAll("(?i)bearer\\s+[a-z0-9._~+/=-]+", "Bearer <redacted>");
      sanitized =
          sanitized.replaceAll("(?i)(api[_ -]?key|token)\\s*[:=]\\s*[^,;\\s]+", "$1=<redacted>");
      return sanitized.length() <= 240 ? sanitized : sanitized.substring(0, 240);
    } catch (JsonProcessingException exception) {
      return fallback;
    }
  }

  private byte[] writeJson(Object value) {
    try {
      return objectMapper.writeValueAsBytes(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "WellAPI gpt-image-2 request JSON cannot be written", exception);
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

  private void ensureConfigured() {
    if (!properties.isRealCallsEnabled()) {
      throw new IllegalStateException(
          "Image2 real calls are disabled; set IMAGE_REAL_CALLS_ENABLED=true for manual integration");
    }
    if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
      throw new IllegalStateException("WELLAPI_API_KEY is required for WellAPI gpt-image-2 calls");
    }
  }

  private Duration requestTimeout() {
    Duration timeout = properties.getRequestTimeout();
    return timeout == null || timeout.isNegative() ? Duration.ofSeconds(30) : timeout;
  }

  private String firstNonBlank(String value, String fallback) {
    return hasText(value) ? value.trim() : fallback;
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
}
