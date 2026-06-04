package com.yanyun.music.api.me;

import com.yanyun.music.auth.AccountAdapter;
import com.yanyun.music.auth.UserProfile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

  private static final String DEFAULT_MOCK_USER_ID = "mock_user_001";

  private final AccountAdapter accountAdapter;

  public MeController(AccountAdapter accountAdapter) {
    this.accountAdapter = accountAdapter;
  }

  @GetMapping("/api/v1/me")
  public UserProfile getCurrentUser(
      @RequestHeader(
              value = "X-Mock-User-Id",
              required = false,
              defaultValue = DEFAULT_MOCK_USER_ID)
          String mockUserId) {
    return accountAdapter.getCurrentUser(mockUserId);
  }
}
