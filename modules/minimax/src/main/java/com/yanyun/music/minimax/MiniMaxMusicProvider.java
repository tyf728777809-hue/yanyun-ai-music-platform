package com.yanyun.music.minimax;

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

public final class MiniMaxMusicProvider implements MusicProvider {

  private static final String APP_NAME = "music-minimax";
  private static final String SUB_APP_NAME = "text-to-music";
  private static final String CONTENT_TYPE = "audio/mpeg";

  private final DreamMakerClient dreamMakerClient;
  private final MiniMaxMusicProviderOptions options;

  public MiniMaxMusicProvider() {
    this(null, MiniMaxMusicProviderOptions.defaults());
  }

  public MiniMaxMusicProvider(
      DreamMakerClient dreamMakerClient, MiniMaxMusicProviderOptions options) {
    this.dreamMakerClient = dreamMakerClient;
    this.options = options == null ? MiniMaxMusicProviderOptions.defaults() : options;
  }

  @Override
  public MusicProviderType providerType() {
    return MusicProviderType.MINIMAX;
  }

  @Override
  public MusicGenerationResult submit(MusicGenerationRequest request) {
    if (dreamMakerClient == null) {
      throw new UnsupportedOperationException("MiniMax DreamMaker client is not configured");
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
                "MiniMax", submitResponse.message(), "MiniMax task submission failed"));
      }
      return pollUntilTerminal(submitResponse.taskId());
    } catch (DreamMakerClientException exception) {
      return providerFailure(null, exception.failureCode(), exception.getMessage());
    }
  }

  private Map<String, Object> params(MusicGenerationRequest request) {
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("model", options.model());
    params.put("prompt", trimToLength(firstNonBlank(request.musicPrompt(), "燕云主题原创歌曲"), 2000));
    boolean hasLyrics = hasText(request.lyricsText());
    params.put("is_instrumental", !hasLyrics);
    if (hasLyrics) {
      params.put("lyrics", trimToLength(request.lyricsText(), 3500));
      params.put("lyrics_optimizer", false);
    } else {
      params.put("lyrics_optimizer", true);
    }
    params.put("audio_format", options.audioFormat());
    params.put("sample_rate", options.sampleRate());
    params.put("bitrate", options.bitrate());
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
                "MiniMax", latest.message(), "MiniMax status polling failed"));
      }
      if (latest.status() == DreamMakerTaskStatus.SUCCEEDED) {
        return successFromStatus(taskId, latest);
      }
      if (latest.status() == DreamMakerTaskStatus.FAILED) {
        return providerFailure(
            taskId,
            DreamMakerFailureMapper.fromProviderError(latest.code(), latest.message()),
            DreamMakerFailureMapper.sanitizedMessage(
                "MiniMax", latest.message(), "MiniMax music generation failed"));
      }
      sleepBeforeNextPoll();
    }
    String providerStatus = latest == null ? null : latest.providerStatus();
    String message =
        providerStatus == null
            ? "MiniMax provider did not finish before timeout"
            : "MiniMax provider did not finish before timeout: " + providerStatus;
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
          "MiniMax provider returned success without audio output");
    }
    return MusicGenerationResult.succeededFromSource(
        providerType(),
        taskId,
        modelName(),
        audio.get().url(),
        CONTENT_TYPE,
        audio.get().durationMs(),
        "MiniMax music generation succeeded");
  }

  private MusicGenerationResult providerFailure(
      String taskId, String failureCode, String failureMessage) {
    return MusicGenerationResult.failed(
        providerType(), taskId, modelName(), failureCode, failureMessage);
  }

  private String modelName() {
    return APP_NAME + ":" + SUB_APP_NAME + ":" + options.model();
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
          DreamMakerFailureMapper.PROVIDER_TIMEOUT, "MiniMax polling was interrupted", exception);
    }
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
