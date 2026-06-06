package com.yanyun.music.musicprovider;

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
    String audioObjectKey = "audio/" + request.workId() + ".mp3";
    return MusicGenerationResult.succeeded(
        providerType(),
        taskId,
        "mock",
        audioObjectKey,
        durationMs,
        "Mock music generation succeeded");
  }
}
