package com.yanyun.music.moderation;

public final class MockModerationAdapter implements ModerationAdapter {

  @Override
  public ModerationDecision preCheckUserInput(String userId, String text) {
    return decide(text);
  }

  @Override
  public ModerationDecision preCheckLyrics(String userId, String lyricsText) {
    return decide(lyricsText);
  }

  @Override
  public ModerationDecision preCheckPublishPackage(String userId, String workId) {
    return ModerationDecision.allow();
  }

  private ModerationDecision decide(String text) {
    if (text != null && text.contains("[BLOCK]")) {
      return ModerationDecision.blocked("MOCK_BLOCKED", "Mock moderation blocked content.");
    }
    return ModerationDecision.allow();
  }
}
