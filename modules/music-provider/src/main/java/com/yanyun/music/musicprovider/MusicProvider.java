package com.yanyun.music.musicprovider;

import java.util.Optional;

public interface MusicProvider {

  MusicProviderType providerType();

  MusicGenerationResult submit(MusicGenerationRequest request);

  default Optional<MusicGenerationResult> refreshAudio(MusicAudioRefreshRequest request) {
    return Optional.empty();
  }
}
