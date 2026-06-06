package com.yanyun.music.deepseek;

public interface DeepSeekLyricsClient {

  DeepSeekLyricsResponse generate(DeepSeekLyricsRequest request);

  default String modelName() {
    return "mock-deepseek-lyrics";
  }
}
