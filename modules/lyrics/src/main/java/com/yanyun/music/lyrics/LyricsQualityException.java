package com.yanyun.music.lyrics;

public final class LyricsQualityException extends RuntimeException {

  public LyricsQualityException(String message) {
    super(message == null || message.isBlank() ? "歌词不够贴合燕云十六声，请调整灵感后重试。" : message);
  }
}
