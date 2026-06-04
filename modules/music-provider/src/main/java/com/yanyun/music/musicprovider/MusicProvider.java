package com.yanyun.music.musicprovider;

public interface MusicProvider {

  MusicProviderType providerType();

  MusicGenerationResult submit(MusicGenerationRequest request);
}
