package com.yanyun.music.image2;

import com.yanyun.music.dreammaker.DreamMakerClient;
import com.yanyun.music.dreammaker.DreamMakerClientException;
import com.yanyun.music.dreammaker.DreamMakerOutputFile;
import com.yanyun.music.dreammaker.DreamMakerRunRequest;
import com.yanyun.music.dreammaker.DreamMakerStatusRequest;
import com.yanyun.music.dreammaker.DreamMakerStatusResponse;
import com.yanyun.music.dreammaker.DreamMakerSubmitResponse;
import com.yanyun.music.dreammaker.DreamMakerTaskStatus;
import com.yanyun.music.media.MediaAssetDescriptor;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DreamMakerImage2CoverGenerationService implements CoverGenerationService {

  public static final String SOURCE_URL_METADATA_KEY = "source_url";

  private final DreamMakerClient dreamMakerClient;
  private final Image2Properties properties;

  public DreamMakerImage2CoverGenerationService(
      DreamMakerClient dreamMakerClient, Image2Properties properties) {
    if (dreamMakerClient == null) {
      throw new IllegalArgumentException("dreamMakerClient is required");
    }
    this.dreamMakerClient = dreamMakerClient;
    this.properties = properties == null ? new Image2Properties() : properties;
  }

  @Override
  public CoverGenerationResult generateCover(CoverGenerationRequest request) {
    ensureConfigured();
    try {
      DreamMakerSubmitResponse submitResponse =
          dreamMakerClient.submit(
              new DreamMakerRunRequest(
                  properties.getAppName(), properties.getSubAppName(), params(request)));
      if (!submitResponse.accepted()) {
        throw new IllegalStateException(
            firstNonBlank(submitResponse.message(), "Image2 task submission failed"));
      }
      return pollUntilTerminal(request, submitResponse.taskId());
    } catch (DreamMakerClientException exception) {
      throw new IllegalStateException(
          firstNonBlank(exception.getMessage(), "Image2 DreamMaker request failed"), exception);
    }
  }

  private Map<String, Object> params(CoverGenerationRequest request) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("model", properties.getModelName());
    params.put(
        "prompt", trimToLength(firstNonBlank(request.visualPrompt(), request.songSummary()), 4000));
    params.put("n", 1);
    params.put("size", outputSize(request));
    params.put("quality", properties.getQuality());
    params.put("output_format", properties.getOutputFormat());
    params.put("background", properties.getBackground());
    if (request.negativePrompt() != null && !request.negativePrompt().isBlank()) {
      params.put("negative_prompt", trimToLength(request.negativePrompt(), 1000));
    }
    return params;
  }

  private CoverGenerationResult pollUntilTerminal(CoverGenerationRequest request, String taskId) {
    DreamMakerStatusResponse latest = null;
    for (int attempt = 0; attempt < properties.getMaxPollAttempts(); attempt++) {
      latest =
          dreamMakerClient.status(
              new DreamMakerStatusRequest(
                  properties.getAppName(), properties.getSubAppName(), taskId));
      if (!latest.successfulCode()) {
        throw new IllegalStateException(
            firstNonBlank(latest.message(), "Image2 status polling failed"));
      }
      if (latest.status() == DreamMakerTaskStatus.SUCCEEDED) {
        return successFromStatus(request, taskId, latest);
      }
      if (latest.status() == DreamMakerTaskStatus.FAILED) {
        throw new IllegalStateException(
            firstNonBlank(latest.message(), "Image2 generation failed"));
      }
      sleepBeforeNextPoll();
    }
    String providerStatus = latest == null ? null : latest.providerStatus();
    throw new IllegalStateException(
        providerStatus == null
            ? "Image2 provider did not finish before timeout"
            : "Image2 provider did not finish before timeout: " + providerStatus);
  }

  private CoverGenerationResult successFromStatus(
      CoverGenerationRequest request, String taskId, DreamMakerStatusResponse statusResponse) {
    Optional<DreamMakerOutputFile> image =
        statusResponse.outputFiles().stream()
            .filter(file -> hasText(file.url()))
            .filter(file -> isImage(file.fileType(), file.url()))
            .findFirst();
    if (image.isEmpty()) {
      throw new IllegalStateException("Image2 provider returned success without image output");
    }
    String size = outputSize(request);
    int width = width(size);
    int height = height(size);
    String format = normalizedFormat();
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("provider", "dreammaker-image2");
    metadata.put("model", properties.getModelName());
    metadata.put("app_name", properties.getAppName());
    metadata.put("sub_app_name", properties.getSubAppName());
    metadata.put("provider_task_id", taskId);
    metadata.put("size", size);
    metadata.put("quality", properties.getQuality());
    metadata.put("output_format", format);
    metadata.put(SOURCE_URL_METADATA_KEY, image.get().url());
    metadata.putAll(request.providerOptions());
    return new CoverGenerationResult(
        new MediaAssetDescriptor(
            "COVER",
            "covers/" + request.workId() + "." + format,
            mimeType(format),
            0L,
            "dreammaker-image2",
            width,
            height,
            null,
            metadata));
  }

  private boolean isImage(String fileType, String url) {
    if (fileType != null && !fileType.isBlank()) {
      String normalized = fileType.trim().toLowerCase(java.util.Locale.ROOT);
      return normalized.contains("image")
          || normalized.equals("png")
          || normalized.equals("jpeg")
          || normalized.equals("jpg")
          || normalized.equals("webp");
    }
    String normalizedUrl = url.toLowerCase(java.util.Locale.ROOT);
    return normalizedUrl.contains(".png")
        || normalizedUrl.contains(".jpg")
        || normalizedUrl.contains(".jpeg")
        || normalizedUrl.contains(".webp");
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

  private String normalizedFormat() {
    String format = properties.getOutputFormat().trim().toLowerCase(java.util.Locale.ROOT);
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

  private void sleepBeforeNextPoll() {
    Duration interval = properties.getPollInterval();
    if (interval == null || interval.isZero()) {
      return;
    }
    try {
      Thread.sleep(interval.toMillis());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Image2 polling was interrupted", exception);
    }
  }

  private void ensureConfigured() {
    if (!properties.isRealCallsEnabled()) {
      throw new IllegalStateException(
          "Image2 real calls are disabled; set IMAGE_REAL_CALLS_ENABLED=true for manual integration");
    }
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
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
