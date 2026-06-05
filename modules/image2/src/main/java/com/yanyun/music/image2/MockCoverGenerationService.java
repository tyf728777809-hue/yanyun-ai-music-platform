package com.yanyun.music.image2;

import com.yanyun.music.media.MediaAssetDescriptor;
import java.util.Map;

public final class MockCoverGenerationService implements CoverGenerationService {

  @Override
  public CoverGenerationResult generateCover(CoverGenerationRequest request) {
    return new CoverGenerationResult(
        new MediaAssetDescriptor(
            "COVER",
            "covers/" + request.workId() + ".png",
            "image/png",
            512_000L,
            "mock-cover",
            1920,
            1080,
            null,
            Map.of(
                "provider",
                "mock-image2",
                "profile",
                "cover-16x9",
                "prompt_source",
                "lyrics-and-music-prompt")));
  }
}
