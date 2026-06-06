package com.yanyun.music.image2;

public interface CoverGenerationService {

  String SOURCE_URL_METADATA_KEY = "source_url";
  String INLINE_BASE64_METADATA_KEY = "inline_base64";

  CoverGenerationResult generateCover(CoverGenerationRequest request);
}
