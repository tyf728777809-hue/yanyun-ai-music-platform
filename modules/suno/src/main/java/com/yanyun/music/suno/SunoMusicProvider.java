package com.yanyun.music.suno;

import com.yanyun.music.musicprovider.MusicGenerationRequest;
import com.yanyun.music.musicprovider.MusicGenerationResult;
import com.yanyun.music.musicprovider.MusicProvider;
import com.yanyun.music.musicprovider.MusicProviderType;

public final class SunoMusicProvider implements MusicProvider {

  @Override
  public MusicProviderType providerType() {
    return MusicProviderType.SUNO;
  }

  @Override
  public MusicGenerationResult submit(MusicGenerationRequest request) {
    throw new UnsupportedOperationException(
        "Suno real music generation is not implemented in local mock stage");
  }
}
