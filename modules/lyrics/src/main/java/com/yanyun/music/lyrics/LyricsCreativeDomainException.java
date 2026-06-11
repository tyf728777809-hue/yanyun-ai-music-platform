package com.yanyun.music.lyrics;

public final class LyricsCreativeDomainException extends RuntimeException {

  private final String recommendedRewrite;

  public LyricsCreativeDomainException(String message, String recommendedRewrite) {
    super(
        message == null || message.isBlank()
            ? "Creative request is outside Yanyun scope"
            : message);
    this.recommendedRewrite =
        recommendedRewrite == null || recommendedRewrite.isBlank()
            ? null
            : recommendedRewrite.trim();
  }

  public String recommendedRewrite() {
    return recommendedRewrite;
  }
}
