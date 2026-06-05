package com.yanyun.music.api.integration.dreammaker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.dreammaker.DreamMakerClient;
import com.yanyun.music.dreammaker.DreamMakerClientException;
import com.yanyun.music.dreammaker.DreamMakerFailureMapper;
import com.yanyun.music.dreammaker.DreamMakerOutputFile;
import com.yanyun.music.dreammaker.DreamMakerRunRequest;
import com.yanyun.music.dreammaker.DreamMakerStatusRequest;
import com.yanyun.music.dreammaker.DreamMakerStatusResponse;
import com.yanyun.music.dreammaker.DreamMakerSubmitResponse;
import com.yanyun.music.dreammaker.DreamMakerTaskStatus;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DreamMakerHttpClient implements DreamMakerClient {

  private final DreamMakerProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public DreamMakerHttpClient(DreamMakerProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, HttpClient.newHttpClient());
  }

  DreamMakerHttpClient(
      DreamMakerProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
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

  @Override
  public DreamMakerSubmitResponse submit(DreamMakerRunRequest request) {
    ensureConfigured();
    URI uri =
        apiUri(
            "/api/v1/apps/" + request.appName() + "/run",
            Map.of("sub_app_name", request.subAppName()));
    byte[] body = writeJson(Map.of("params", request.params()));
    HttpRequest httpRequest =
        baseRequest(uri)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
    JsonNode root = send(httpRequest);
    JsonNode data = root.path("data");
    return new DreamMakerSubmitResponse(
        root.path("code").asInt(-1), text(root, "message", "msg"), text(data, "task_id", "taskId"));
  }

  @Override
  public DreamMakerStatusResponse status(DreamMakerStatusRequest request) {
    ensureConfigured();
    URI uri =
        apiUri(
            "/api/v1/apps/" + request.appName() + "/status",
            Map.of("sub_app_name", request.subAppName(), "task_id", request.taskId()));
    HttpRequest httpRequest = baseRequest(uri).GET().build();
    JsonNode root = send(httpRequest);
    JsonNode data = root.path("data");
    String providerStatus = text(data, "status");
    return new DreamMakerStatusResponse(
        root.path("code").asInt(-1),
        text(root, "message", "msg"),
        DreamMakerTaskStatus.fromProviderStatus(providerStatus),
        providerStatus,
        outputFiles(data));
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
        throw new DreamMakerClientException(
            DreamMakerFailureMapper.fromHttpStatus(response.statusCode()),
            "DreamMaker request failed with HTTP " + response.statusCode());
      }
      return objectMapper.readTree(response.body());
    } catch (HttpTimeoutException exception) {
      throw new DreamMakerClientException(
          DreamMakerFailureMapper.PROVIDER_TIMEOUT, "DreamMaker request timed out", exception);
    } catch (JsonProcessingException exception) {
      throw new DreamMakerClientException(
          DreamMakerFailureMapper.MUSIC_GENERATION_FAILED,
          "DreamMaker response JSON is invalid",
          exception);
    } catch (IOException exception) {
      throw new DreamMakerClientException(
          DreamMakerFailureMapper.MUSIC_GENERATION_FAILED, "DreamMaker request failed", exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new DreamMakerClientException(
          DreamMakerFailureMapper.PROVIDER_TIMEOUT, "DreamMaker request interrupted", exception);
    }
  }

  private List<DreamMakerOutputFile> outputFiles(JsonNode data) {
    JsonNode output = data.path("output");
    JsonNode files = output.path("output");
    if (!files.isArray() && output.isArray()) {
      files = output;
    }
    if (!files.isArray()) {
      return List.of();
    }
    List<DreamMakerOutputFile> result = new ArrayList<>();
    for (JsonNode file : files) {
      result.add(
          new DreamMakerOutputFile(
              text(file, "name"),
              resolveOutputUrl(text(file, "url")),
              resolveOutputUrl(text(file, "cover")),
              text(file, "file_type", "fileType"),
              durationMs(file.path("duration"))));
    }
    return result;
  }

  private URI apiUri(String path, Map<String, String> queryParams) {
    StringBuilder query = new StringBuilder();
    for (Map.Entry<String, String> entry : queryParams.entrySet()) {
      if (!query.isEmpty()) {
        query.append('&');
      }
      query.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
    }
    return properties.getBaseUrl().resolve(path + "?" + query);
  }

  private String resolveOutputUrl(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.startsWith("//")) {
      return properties.getBaseUrl().getScheme() + ":" + trimmed;
    }
    URI uri = URI.create(trimmed);
    if (uri.isAbsolute()) {
      return trimmed;
    }
    return properties.getBaseUrl().resolve(trimmed).toString();
  }

  private Integer durationMs(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    String value = node.isNumber() ? node.asText() : node.asText("");
    if (value.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(value.trim()).multiply(BigDecimal.valueOf(1000L)).intValue();
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private byte[] writeJson(Object value) {
    try {
      return objectMapper.writeValueAsBytes(value);
    } catch (JsonProcessingException exception) {
      throw new DreamMakerClientException(
          DreamMakerFailureMapper.MUSIC_GENERATION_FAILED,
          "DreamMaker request JSON cannot be written",
          exception);
    }
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

  private void ensureConfigured() {
    if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
      throw new DreamMakerClientException(
          DreamMakerFailureMapper.MUSIC_GENERATION_FAILED,
          "DREAMMAKER_API_KEY is required for DreamMaker calls");
    }
  }

  private Duration requestTimeout() {
    Duration timeout = properties.getRequestTimeout();
    return timeout == null || timeout.isNegative() ? Duration.ofSeconds(30) : timeout;
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
