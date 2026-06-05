package com.yanyun.music.minimax;

import java.time.Duration;

public record MiniMaxMusicProviderOptions(
    String model,
    int maxPollAttempts,
    Duration pollInterval,
    String audioFormat,
    int sampleRate,
    int bitrate) {

  public static MiniMaxMusicProviderOptions defaults() {
    return new MiniMaxMusicProviderOptions(
        "minimax-music-2.6", 60, Duration.ofSeconds(2), "mp3", 44100, 256000);
  }

  public MiniMaxMusicProviderOptions {
    model = model == null || model.isBlank() ? "minimax-music-2.6" : model.trim();
    maxPollAttempts = Math.max(1, maxPollAttempts);
    pollInterval = pollInterval == null || pollInterval.isNegative() ? Duration.ZERO : pollInterval;
    audioFormat = audioFormat == null || audioFormat.isBlank() ? "mp3" : audioFormat.trim();
    sampleRate = sampleRate <= 0 ? 44100 : sampleRate;
    bitrate = bitrate <= 0 ? 256000 : bitrate;
  }
}
