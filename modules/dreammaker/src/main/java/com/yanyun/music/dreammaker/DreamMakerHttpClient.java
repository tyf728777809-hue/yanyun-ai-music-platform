package com.yanyun.music.dreammaker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class DreamMakerHttpClient implements DreamMakerClient {

  private static final String HMAC_SHA256 = "HmacSHA256";
  private static final long TOKEN_TTL_SECONDS = 1800L;
  private static final long TOKEN_NOT_BEFORE_SKEW_SECONDS = 5L;

  private final DreamMakerProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final Clock clock;

  public DreamMakerHttpClient(DreamMakerProperties properties, ObjectMapper objectMapper) {
    this(properties, objectMapper, HttpClient.newHttpClient(), Clock.systemUTC());
  }

  DreamMakerHttpClient(
      DreamMakerProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
    this(properties, objectMapper, httpClient, Clock.systemUTC());
  }

  DreamMakerHttpClient(
      DreamMakerProperties properties,
      ObjectMapper objectMapper,
      HttpClient httpClient,
      Clock clock) {
    if (properties == null) {
      throw new IllegalArgumentException("properties is required");
    }
    if (objectMapper == null) {
      throw new IllegalArgumentException("objectMapper is required");
    }
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = httpClient == null ? HttpClient.newHttpClient() : httpClient;
    this.clock = clock == null ? Clock.systemUTC() : clock;
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
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(uri)
            .timeout(requestTimeout())
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + jwtToken());
    if (properties.getUserAccessToken() != null && !properties.getUserAccessToken().isBlank()) {
      builder.header("X-Access-Token", properties.getUserAccessToken());
    }
    return builder;
  }

  private JsonNode send(HttpRequest request) {
    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() < HttpURLConnection.HTTP_OK
          || response.statusCode() >= HttpURLConnection.HTTP_MULT_CHOICE) {
        throw new DreamMakerClientException(
            DreamMakerFailureMapper.fromHttpStatus(response.statusCode()),
            responseFailureMessage(response.statusCode(), response.body()));
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

  private String responseFailureMessage(int statusCode, String body) {
    String fallback = "DreamMaker request failed with HTTP " + statusCode;
    if (body == null || body.isBlank()) {
      return fallback;
    }
    try {
      JsonNode root = objectMapper.readTree(body);
      String providerMessage = text(root, "message", "msg", "error");
      return DreamMakerFailureMapper.sanitizedMessage("DreamMaker", providerMessage, fallback);
    } catch (JsonProcessingException exception) {
      return fallback;
    }
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
    if (!properties.isRealCallsEnabled()) {
      throw new DreamMakerClientException(
          DreamMakerFailureMapper.MUSIC_GENERATION_FAILED,
          "DreamMaker real calls are disabled; set DREAMMAKER_REAL_CALLS_ENABLED=true for manual integration");
    }
    if (properties.getAccessKey() == null
        || properties.getAccessKey().isBlank()
        || properties.getSecretKey() == null
        || properties.getSecretKey().isBlank()) {
      throw new DreamMakerClientException(
          DreamMakerFailureMapper.MUSIC_GENERATION_FAILED,
          "DREAMMAKER_ACCESS_KEY and DREAMMAKER_SECRET_KEY are required for DreamMaker calls");
    }
  }

  private Duration requestTimeout() {
    Duration timeout = properties.getRequestTimeout();
    return timeout == null || timeout.isNegative() ? Duration.ofSeconds(30) : timeout;
  }

  private String jwtToken() {
    Instant now = clock.instant();
    String header = base64Url(writeJson(Map.of("alg", "HS256", "typ", "JWT")));
    String payload =
        base64Url(
            writeJson(
                Map.of(
                    "iss",
                    properties.getAccessKey(),
                    "exp",
                    now.plusSeconds(TOKEN_TTL_SECONDS).getEpochSecond(),
                    "nbf",
                    now.minusSeconds(TOKEN_NOT_BEFORE_SKEW_SECONDS).getEpochSecond())));
    String signingInput = header + "." + payload;
    return signingInput + "." + base64Url(hmacSha256(signingInput));
  }

  private byte[] hmacSha256(String value) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(
          new SecretKeySpec(
              properties.getSecretKey().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
      return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    } catch (java.security.InvalidKeyException | java.security.NoSuchAlgorithmException exception) {
      throw new DreamMakerClientException(
          DreamMakerFailureMapper.MUSIC_GENERATION_FAILED,
          "DreamMaker JWT signature cannot be generated",
          exception);
    }
  }

  private String base64Url(byte[] value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
