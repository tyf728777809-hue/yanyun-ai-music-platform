package com.yanyun.music.creativeagent;

public record CoverPromptRequest(
    String workId,
    String songTitle,
    String songSummary,
    String lyricsText,
    String musicPrompt,
    String coverPromptSeed,
    Integer width,
    Integer height) {}
