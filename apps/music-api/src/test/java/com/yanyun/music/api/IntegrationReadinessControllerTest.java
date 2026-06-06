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

class IntegrationReadinessControllerTest {

  @Test
  void readinessReturnsIntegrationReport() {
    Clock clock = Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneOffset.UTC);
    IntegrationReadinessController controller =
        new IntegrationReadinessController(
            new IntegrationReadinessService(new CompanyIntegrationProperties(), clock));

    IntegrationReadinessReport report = controller.readiness();

    assertThat(report.service()).isEqualTo("music-api");
    assertThat(report.overallStatus()).isEqualTo(IntegrationOverallStatus.READY_FOR_LOCAL);
    assertThat(report.components())
        .extracting("component")
        .contains("company_account", "company_quota", "deepseek_guard", "image2_guard");
  }
}
