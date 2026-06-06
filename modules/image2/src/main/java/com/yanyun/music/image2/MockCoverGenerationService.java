package com.yanyun.music.image2;

import com.yanyun.music.media.MediaAssetDescriptor;
import java.util.Map;

public final class MockCoverGenerationService implements CoverGenerationService {

  @Override
  public CoverGenerationResult generateCover(CoverGenerationRequest request) {
    Map<String, Object> metadata =
        new java.util.LinkedHashMap<>(
            Map.of(
                "provider",
                "mock-image2",
                "profile",
                "cover-16x9",
                "prompt_source",
                "cover-prompt-agent"));
    metadata.put("visual_prompt", request.visualPrompt());
    metadata.put("negative_prompt", request.negativePrompt());
    metadata.putAll(request.providerOptions());
    return new CoverGenerationResult(
        new MediaAssetDescriptor(
            "COVER",
            "covers/" + request.workId() + ".png",
            "image/png",
            512_000L,
            "mock-cover",
            request.width(),
            request.height(),
            null,
            metadata));
  }
}
