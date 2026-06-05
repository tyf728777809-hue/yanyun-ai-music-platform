package com.yanyun.music.musicprovider;

import java.util.UUID;

public final class MockMusicProvider implements MusicProvider {

  @Override
  public MusicProviderType providerType() {
    return MusicProviderType.MOCK;
  }

  @Override
  public MusicGenerationResult submit(MusicGenerationRequest request) {
    String taskId = "mock-music-" + UUID.randomUUID();
    String audioObjectKey = "audio/" + request.workId() + ".mp3";
    return MusicGenerationResult.succeeded(
        providerType(), taskId, "mock", audioObjectKey, 180_000, "Mock music generation succeeded");
  }
}
