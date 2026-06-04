package com.yanyun.music.auth;

import java.util.List;

public final class MockAccountAdapter implements AccountAdapter {

  @Override
  public UserProfile getCurrentUser(String userId) {
    String resolvedUserId = userId == null || userId.isBlank() ? "mock_user_001" : userId;
    return new UserProfile(resolvedUserId, "MockUser", null, List.of("USER"));
  }
}
