package com.yanyun.music.suno;

import java.time.Duration;

public record SunoMusicProviderOptions(String model, int maxPollAttempts, Duration pollInterval) {

  public static SunoMusicProviderOptions defaults() {
    return new SunoMusicProviderOptions("chirp-crow", 60, Duration.ofSeconds(2));
  }

  public SunoMusicProviderOptions {
    model = model == null || model.isBlank() ? "chirp-crow" : model.trim();
    maxPollAttempts = Math.max(1, maxPollAttempts);
    pollInterval = pollInterval == null || pollInterval.isNegative() ? Duration.ZERO : pollInterval;
  }
}
