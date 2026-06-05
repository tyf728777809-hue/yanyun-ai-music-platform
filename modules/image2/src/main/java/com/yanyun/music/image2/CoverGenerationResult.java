package com.yanyun.music.image2;

import com.yanyun.music.media.MediaAssetDescriptor;

public record CoverGenerationResult(MediaAssetDescriptor asset) {

  public CoverGenerationResult {
    if (asset == null) {
      throw new IllegalArgumentException("asset is required");
    }
  }
}
