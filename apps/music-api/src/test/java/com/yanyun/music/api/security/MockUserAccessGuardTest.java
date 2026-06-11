package com.yanyun.music.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yanyun.music.configcenter.CompanyIntegrationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class MockUserAccessGuardTest {

  @Test
  void allowsMockUserInLocalMockEnvironment() {
    MockUserAccessGuard guard = new MockUserAccessGuard(new CompanyIntegrationProperties());

    assertEquals("mock_user_001", guard.requireAllowed(" mock_user_001 "));
  }

  @Test
  void rejectsMockUserOutsideLocalEnvironment() {
    CompanyIntegrationProperties properties = new CompanyIntegrationProperties();
    properties.setEnvironment("production");

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> new MockUserAccessGuard(properties).requireAllowed("mock_user_001"));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
  }

  @Test
  void rejectsMockUserWhenAnyCompanyAdapterIsNotMock() {
    CompanyIntegrationProperties properties = new CompanyIntegrationProperties();
    properties.setAccountAdapterMode("company");

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> new MockUserAccessGuard(properties).requireAllowed("mock_user_001"));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
  }
}
