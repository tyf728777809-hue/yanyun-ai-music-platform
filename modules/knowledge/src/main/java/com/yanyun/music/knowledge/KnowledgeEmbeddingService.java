package com.yanyun.music.knowledge;

public interface KnowledgeEmbeddingService {

  int dimensions();

  double[] embed(String text);
}
