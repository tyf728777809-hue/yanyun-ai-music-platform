package com.yanyun.music.moderation;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public final class MockModerationAdapter implements ModerationAdapter {

  private final Set<String> publishPackageBlockedUserIds;
  private final Set<String> publishPackageBlockedWorkIds;

  public MockModerationAdapter() {
    this(Set.of(), Set.of());
  }

  public MockModerationAdapter(
      Collection<String> publishPackageBlockedUserIds,
      Collection<String> publishPackageBlockedWorkIds) {
    this.publishPackageBlockedUserIds = normalized(publishPackageBlockedUserIds);
    this.publishPackageBlockedWorkIds = normalized(publishPackageBlockedWorkIds);
  }

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
    if (publishPackageBlockedUserIds.contains(userId)
        || publishPackageBlockedWorkIds.contains(workId)) {
      return ModerationDecision.blocked("MOCK_PACKAGE_BLOCKED", "作品暂不能交给社区发布。");
    }
    return ModerationDecision.allow();
  }

  private ModerationDecision decide(String text) {
    if (text != null && text.contains("[BLOCK]")) {
      return ModerationDecision.blocked("MOCK_BLOCKED", "Mock moderation blocked content.");
    }
    return ModerationDecision.allow();
  }

  private Set<String> normalized(Collection<String> values) {
    if (values == null || values.isEmpty()) {
      return Set.of();
    }
    return values.stream()
        .filter(value -> value != null && !value.isBlank())
        .map(String::trim)
        .collect(Collectors.toUnmodifiableSet());
  }
}
