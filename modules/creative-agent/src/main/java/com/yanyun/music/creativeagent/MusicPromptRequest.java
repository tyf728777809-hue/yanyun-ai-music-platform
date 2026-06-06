package com.yanyun.music.creativeagent;

public record MusicPromptRequest(
    String workId,
    String songTitle,
    String songSummary,
    String lyricsText,
    String musicPromptSeed,
    String vocalPreference,
    String musicProvider) {}
