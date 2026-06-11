package com.yanyun.music.api.security;

import com.yanyun.music.configcenter.CompanyIntegrationProperties;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MockUserAccessGuard {

  private final CompanyIntegrationProperties properties;

  public MockUserAccessGuard(CompanyIntegrationProperties properties) {
    this.properties = properties;
  }

  public String requireAllowed(String mockUserId) {
    if (!mockUserAccessAllowed()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Mock user header is only allowed in local mock environments");
    }
    if (mockUserId == null || mockUserId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mock user id is required");
    }
    return mockUserId.trim();
  }

  private boolean mockUserAccessAllowed() {
    return localEnvironment(properties.getEnvironment())
        && mockMode(properties.getAccountAdapterMode())
        && mockMode(properties.getModerationAdapterMode())
        && mockMode(properties.getQuotaAdapterMode())
        && mockMode(properties.getPublishAdapterMode())
        && mockMode(properties.getShareAdapterMode());
  }

  private boolean localEnvironment(String environment) {
    String normalized = normalize(environment);
    return normalized.equals("local")
        || normalized.equals("mock")
        || normalized.equals("dev")
        || normalized.equals("development")
        || normalized.equals("test");
  }

  private boolean mockMode(String mode) {
    return normalize(mode).equals("mock");
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
