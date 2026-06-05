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
    boolean musicRetryAllowedAfterFailure) {}
