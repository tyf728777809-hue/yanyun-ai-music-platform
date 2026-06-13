package com.yanyun.music.knowledge;

public class KnowledgeProperties {

  private static final String DEFAULT_KB_VERSION = "yanyun-commercial-kb-2026-06-13-v1";

  private String retrievalMode = "disabled";
  private String kbVersion = DEFAULT_KB_VERSION;
  private int maxReferences = 6;
  private int entityLimit = 4;
  private int semanticLimit = 6;

  public KnowledgeRetrievalMode mode() {
    return KnowledgeRetrievalMode.from(retrievalMode);
  }

  public String getRetrievalMode() {
    return retrievalMode;
  }

  public void setRetrievalMode(String retrievalMode) {
    this.retrievalMode = retrievalMode == null ? "disabled" : retrievalMode;
  }

  public String getKbVersion() {
    return kbVersion;
  }

  public void setKbVersion(String kbVersion) {
    this.kbVersion =
        kbVersion == null || kbVersion.isBlank() ? DEFAULT_KB_VERSION : kbVersion.trim();
  }

  public int getMaxReferences() {
    return maxReferences;
  }

  public void setMaxReferences(int maxReferences) {
    this.maxReferences = Math.max(1, maxReferences);
  }

  public int getEntityLimit() {
    return entityLimit;
  }

  public void setEntityLimit(int entityLimit) {
    this.entityLimit = Math.max(1, entityLimit);
  }

  public int getSemanticLimit() {
    return semanticLimit;
  }

  public void setSemanticLimit(int semanticLimit) {
    this.semanticLimit = Math.max(1, semanticLimit);
  }
}
