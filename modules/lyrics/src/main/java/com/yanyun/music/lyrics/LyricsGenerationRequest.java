package com.yanyun.music.lyrics;

public record LyricsGenerationRequest(
    String userId,
    String workId,
    LyricsOperation operation,
    String userInput,
    String currentLyrics,
    String instruction,
    String requestedTitle,
    String mood,
    String musicStyle,
    String vocalPreference) {}
