package com.yanyun.music.knowledge;

public record KnowledgeReference(
    String chunkId, String title, String fileName, String headingPath, String content) {

  public String displayName() {
    if (title != null && !title.isBlank()) {
      return title.trim();
    }
    if (fileName != null && !fileName.isBlank()) {
      return fileName.trim();
    }
    return chunkId;
  }
}
