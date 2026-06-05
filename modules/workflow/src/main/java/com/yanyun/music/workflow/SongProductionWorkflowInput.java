package com.yanyun.music.workflow;

public record SongProductionWorkflowInput(
    String workId,
    String userId,
    String lyricsDraftId,
    String songTitle,
    String songSummary,
    String lyricsText,
    String musicPrompt,
    String vocalPreference,
    String musicProvider,
    boolean musicRetryAllowedAfterFailure,
    String jobId) {

  public SongProductionWorkflowInput(
      String workId,
      String userId,
      String lyricsDraftId,
      String songTitle,
      String songSummary,
      String lyricsText,
      String musicPrompt,
      String vocalPreference,
      String musicProvider,
      boolean musicRetryAllowedAfterFailure) {
    this(
        workId,
        userId,
        lyricsDraftId,
        songTitle,
        songSummary,
        lyricsText,
        musicPrompt,
        vocalPreference,
        musicProvider,
        musicRetryAllowedAfterFailure,
        null);
  }
}
