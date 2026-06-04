package com.yanyun.music.minimax;

import com.yanyun.music.musicprovider.MusicGenerationRequest;
import com.yanyun.music.musicprovider.MusicGenerationResult;
import com.yanyun.music.musicprovider.MusicProvider;
import com.yanyun.music.musicprovider.MusicProviderType;

public final class MiniMaxMusicProvider implements MusicProvider {

  @Override
  public MusicProviderType providerType() {
    return MusicProviderType.MINIMAX;
  }

  @Override
  public MusicGenerationResult submit(MusicGenerationRequest request) {
    throw new UnsupportedOperationException(
        "MiniMax real music generation is not implemented in local mock stage");
  }
}
