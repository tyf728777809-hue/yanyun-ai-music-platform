package com.yanyun.music.suno;

import com.yanyun.music.dreammaker.DreamMakerClient;
import com.yanyun.music.dreammaker.DreamMakerClientException;
import com.yanyun.music.dreammaker.DreamMakerFailureMapper;
import com.yanyun.music.dreammaker.DreamMakerOutputFile;
import com.yanyun.music.dreammaker.DreamMakerRunRequest;
import com.yanyun.music.dreammaker.DreamMakerStatusRequest;
import com.yanyun.music.dreammaker.DreamMakerStatusResponse;
import com.yanyun.music.dreammaker.DreamMakerSubmitResponse;
import com.yanyun.music.dreammaker.DreamMakerTaskStatus;
import com.yanyun.music.musicprovider.MusicGenerationRequest;
import com.yanyun.music.musicprovider.MusicGenerationResult;
import com.yanyun.music.musicprovider.MusicProvider;
import com.yanyun.music.musicprovider.MusicProviderType;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class SunoMusicProvider implements MusicProvider {

  private static final String APP_NAME = "suno";
  private static final String SUB_APP_NAME = "music-gen";
  private static final String CONTENT_TYPE = "audio/mpeg";

  private final DreamMakerClient dreamMakerClient;
  private final SunoMusicProviderOptions options;

  public SunoMusicProvider() {
    this(null, SunoMusicProviderOptions.defaults());
  }

  public SunoMusicProvider(DreamMakerClient dreamMakerClient, SunoMusicProviderOptions options) {
    this.dreamMakerClient = dreamMakerClient;
    this.options = options == null ? SunoMusicProviderOptions.defaults() : options;
  }

  @Override
  public MusicProviderType providerType() {
    return MusicProviderType.SUNO;
  }

  @Override
  public MusicGenerationResult submit(MusicGenerationRequest request) {
    if (dreamMakerClient == null) {
      throw new UnsupportedOperationException("Suno DreamMaker client is not configured");
    }
    try {
      DreamMakerSubmitResponse submitResponse =
          dreamMakerClient.submit(
              new DreamMakerRunRequest(APP_NAME, SUB_APP_NAME, params(request)));
      if (!submitResponse.accepted()) {
        return providerFailure(
            null,
            DreamMakerFailureMapper.fromProviderError(
                submitResponse.code(), submitResponse.message()),
            DreamMakerFailureMapper.sanitizedMessage(
                "Suno", submitResponse.message(), "Suno task submission failed"));
      }
      return pollUntilTerminal(submitResponse.taskId());
    } catch (DreamMakerClientException exception) {
      return providerFailure(null, exception.failureCode(), exception.getMessage());
    }
  }

  private Map<String, Object> params(MusicGenerationRequest request) {
    Map<String, Object> params = new LinkedHashMap<>();
    if (hasText(request.lyricsText())) {
      params.put("prompt", trimToLength(request.lyricsText(), 5000));
    } else {
      params.put("gpt_description_prompt", trimToLength(request.musicPrompt(), 500));
    }
    if (hasText(request.musicPrompt())) {
      params.put("tags", trimToLength(request.musicPrompt(), 1000));
    }
    params.put("mv", options.model());
    params.put("make_instrumental", false);

    String vocalGender = vocalGender(request.vocalPreference());
    if (vocalGender != null) {
      params.put("metadata", Map.of("vocal_gender", vocalGender));
    }
    return params;
  }

  private MusicGenerationResult pollUntilTerminal(String taskId) {
    DreamMakerStatusResponse latest = null;
    for (int attempt = 0; attempt < options.maxPollAttempts(); attempt++) {
      latest = dreamMakerClient.status(new DreamMakerStatusRequest(APP_NAME, SUB_APP_NAME, taskId));
      if (!latest.successfulCode()) {
        return providerFailure(
            taskId,
            DreamMakerFailureMapper.fromProviderError(latest.code(), latest.message()),
            DreamMakerFailureMapper.sanitizedMessage(
                "Suno", latest.message(), "Suno status polling failed"));
      }
      if (latest.status() == DreamMakerTaskStatus.SUCCEEDED) {
        return successFromStatus(taskId, latest);
      }
      if (latest.status() == DreamMakerTaskStatus.FAILED) {
        return providerFailure(
            taskId,
            DreamMakerFailureMapper.fromProviderError(latest.code(), latest.message()),
            DreamMakerFailureMapper.sanitizedMessage(
                "Suno", latest.message(), "Suno music generation failed"));
      }
      sleepBeforeNextPoll();
    }
    String providerStatus = latest == null ? null : latest.providerStatus();
    String message =
        providerStatus == null
            ? "Suno provider did not finish before timeout"
            : "Suno provider did not finish before timeout: " + providerStatus;
    return providerFailure(taskId, DreamMakerFailureMapper.PROVIDER_TIMEOUT, message);
  }

  private MusicGenerationResult successFromStatus(
      String taskId, DreamMakerStatusResponse statusResponse) {
    Optional<DreamMakerOutputFile> audio =
        statusResponse.outputFiles().stream()
            .filter(DreamMakerOutputFile::isAudio)
            .filter(file -> hasText(file.url()))
            .findFirst();
    if (audio.isEmpty()) {
      return providerFailure(
          taskId,
          DreamMakerFailureMapper.MUSIC_QUALITY_FAILED,
          "Suno provider returned success without audio output");
    }
    return MusicGenerationResult.succeededFromSource(
        providerType(),
        taskId,
        audio.get().url(),
        CONTENT_TYPE,
        audio.get().durationMs(),
        "Suno music generation succeeded");
  }

  private MusicGenerationResult providerFailure(
      String taskId, String failureCode, String failureMessage) {
    return MusicGenerationResult.failed(providerType(), taskId, failureCode, failureMessage);
  }

  private void sleepBeforeNextPoll() {
    Duration interval = options.pollInterval();
    if (interval.isZero()) {
      return;
    }
    try {
      Thread.sleep(interval.toMillis());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new DreamMakerClientException(
          DreamMakerFailureMapper.PROVIDER_TIMEOUT, "Suno polling was interrupted", exception);
    }
  }

  private String vocalGender(String value) {
    if (value == null) {
      return null;
    }
    return switch (value.trim().toUpperCase(java.util.Locale.ROOT)) {
      case "FEMALE" -> "f";
      case "MALE" -> "m";
      default -> null;
    };
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
