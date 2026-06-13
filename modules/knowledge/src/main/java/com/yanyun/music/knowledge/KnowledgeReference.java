package com.yanyun.music.knowledge;

public record KnowledgeReference(
    String chunkId,
    String title,
    String fileName,
    String headingPath,
    String content,
    String factLevel,
    KnowledgeSourceClass sourceClass) {

  public KnowledgeReference(
      String chunkId, String title, String fileName, String headingPath, String content) {
    this(
        chunkId,
        title,
        fileName,
        headingPath,
        content,
        "creative_guidance",
        KnowledgeSourceClass.CREATIVE_MATERIALS);
  }

  public KnowledgeReference {
    sourceClass = sourceClass == null ? KnowledgeSourceClass.fromFactLevel(factLevel) : sourceClass;
  }

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
