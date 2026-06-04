package com.yanyun.music.moderation;

public interface ModerationAdapter {

  ModerationDecision preCheckUserInput(String userId, String text);

  ModerationDecision preCheckLyrics(String userId, String lyricsText);

  ModerationDecision preCheckPublishPackage(String userId, String workId);
}
