package com.yanyun.music.musicprovider;

import java.util.Map;
import java.util.UUID;

public final class MockMusicProvider implements MusicProvider {

  private static final int DEFAULT_DURATION_MS = 180_000;

  private final int durationMs;

  public MockMusicProvider() {
    this(DEFAULT_DURATION_MS);
  }

  public MockMusicProvider(int durationMs) {
    if (durationMs <= 0) {
      throw new IllegalArgumentException("durationMs must be positive");
    }
    this.durationMs = durationMs;
  }

  @Override
  public MusicProviderType providerType() {
    return MusicProviderType.MOCK;
  }

  @Override
  public MusicGenerationResult submit(MusicGenerationRequest request) {
    String taskId = "mock-music-" + UUID.randomUUID();
    String audioObjectKey = "audio/" + request.workId() + ".wav";
    return new MusicGenerationResult(
        providerType(),
        taskId,
        "mock",
        MusicGenerationStatus.SUCCEEDED,
        audioObjectKey,
        null,
        "audio/wav",
        durationMs,
        null,
        null,
        "Mock music generation succeeded",
        Map.of("provider", "mock-audio"));
  }
}
