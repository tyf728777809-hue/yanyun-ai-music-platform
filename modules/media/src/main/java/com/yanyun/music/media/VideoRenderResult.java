package com.yanyun.music.media;

public record VideoRenderResult(
    MediaAssetDescriptor videoAsset, MediaAssetDescriptor timelineAsset) {

  public VideoRenderResult {
    if (videoAsset == null) {
      throw new IllegalArgumentException("videoAsset is required");
    }
    if (timelineAsset == null) {
      throw new IllegalArgumentException("timelineAsset is required");
    }
  }
}
