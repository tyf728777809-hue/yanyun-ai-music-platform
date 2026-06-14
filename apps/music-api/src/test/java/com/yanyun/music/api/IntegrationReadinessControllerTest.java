package com.yanyun.music.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.yanyun.music.configcenter.CompanyIntegrationProperties;
import com.yanyun.music.configcenter.IntegrationOverallStatus;
import com.yanyun.music.configcenter.IntegrationReadinessReport;
import com.yanyun.music.configcenter.IntegrationReadinessService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class IntegrationReadinessControllerTest {

  @Test
  void readinessReturnsIntegrationReport() {
    Clock clock = Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneOffset.UTC);
    IntegrationReadinessController controller =
        new IntegrationReadinessController(
            new IntegrationReadinessService(new CompanyIntegrationProperties(), clock), false);

    IntegrationReadinessReport report = controller.readiness();

    assertThat(report.service()).isEqualTo("music-api");
    assertThat(report.overallStatus()).isEqualTo(IntegrationOverallStatus.READY_FOR_LOCAL);
    assertThat(report.components())
        .extracting("component")
        .contains("company_account", "company_quota", "deepseek_guard", "image2_guard");
  }

  @Test
  void readinessRejectsNonLocalRequestsByDefault() {
    Clock clock = Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneOffset.UTC);
    IntegrationReadinessController controller =
        new IntegrationReadinessController(
            new IntegrationReadinessService(new CompanyIntegrationProperties(), clock), false);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("203.0.113.10");

    org.junit.jupiter.api.Assertions.assertThrows(
        ResponseStatusException.class, () -> controller.readiness(request));
  }

  @Test
  void readinessAllowsLocalRequests() {
    Clock clock = Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneOffset.UTC);
    IntegrationReadinessController controller =
        new IntegrationReadinessController(
            new IntegrationReadinessService(new CompanyIntegrationProperties(), clock), false);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");

    assertThat(controller.readiness(request).service()).isEqualTo("music-api");
  }
}
