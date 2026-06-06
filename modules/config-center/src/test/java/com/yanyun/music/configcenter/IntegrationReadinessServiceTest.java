package com.yanyun.music.configcenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class IntegrationReadinessServiceTest {

  private final Clock fixedClock =
      Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneOffset.UTC);

  @Test
  void defaultLocalConfigurationIsReadyForLocalButMarksCompanyAdaptersMockOnly() {
    IntegrationReadinessReport report =
        new IntegrationReadinessService(new CompanyIntegrationProperties(), fixedClock)
            .buildReport();

    assertEquals("local", report.environment());
    assertEquals(IntegrationOverallStatus.READY_FOR_LOCAL, report.overallStatus());

    IntegrationComponentReadiness account =
        report.components().stream()
            .filter(component -> component.component().equals("company_account"))
            .findFirst()
            .orElseThrow();
    assertEquals(IntegrationReadinessStatus.MOCK_ONLY, account.status());
    assertTrue(account.blocksCompanyDeployment());
    assertEquals("MockAccountAdapter", account.implementation());

    IntegrationComponentReadiness renderWorker =
        report.components().stream()
            .filter(component -> component.component().equals("render_worker"))
            .findFirst()
            .orElseThrow();
    assertEquals(IntegrationReadinessStatus.MOCK_ONLY, renderWorker.status());
    assertTrue(renderWorker.blocksCompanyDeployment());
  }

  @Test
  void nonMockCompanyAdapterModeIsBlockedUntilRealImplementationExists() {
    CompanyIntegrationProperties properties = new CompanyIntegrationProperties();
    properties.setEnvironment("prod");
    properties.setAccountAdapterMode("company");

    IntegrationReadinessReport report =
        new IntegrationReadinessService(properties, fixedClock).buildReport();

    assertEquals(IntegrationOverallStatus.BLOCKED_FOR_COMPANY_DEPLOYMENT, report.overallStatus());
    IntegrationComponentReadiness account =
        report.components().stream()
            .filter(component -> component.component().equals("company_account"))
            .findFirst()
            .orElseThrow();
    assertEquals("company", account.configuredMode());
    assertEquals(IntegrationReadinessStatus.BLOCKED, account.status());
    assertTrue(account.blocksCompanyDeployment());
  }

  @Test
  void reportDoesNotContainSecretValues() {
    CompanyIntegrationProperties properties = new CompanyIntegrationProperties();
    properties.setDreammakerRealCallsEnabled(true);

    String report =
        new IntegrationReadinessService(properties, fixedClock).buildReport().toString();

    assertFalse(report.contains("Bearer "));
    assertFalse(report.contains("sk-"));
    assertFalse(report.contains("real-secret-value"));
  }

  @Test
  void localProcessRenderWorkerIsReadyForLocal() {
    CompanyIntegrationProperties properties = new CompanyIntegrationProperties();
    properties.setRenderWorkerMode("local-process");

    IntegrationReadinessReport report =
        new IntegrationReadinessService(properties, fixedClock).buildReport();

    IntegrationComponentReadiness renderWorker =
        report.components().stream()
            .filter(component -> component.component().equals("render_worker"))
            .findFirst()
            .orElseThrow();
    assertEquals(IntegrationReadinessStatus.READY_FOR_LOCAL, renderWorker.status());
    assertEquals("LocalProcessVideoRenderService", renderWorker.implementation());
    assertTrue(renderWorker.blocksCompanyDeployment());
  }
}
