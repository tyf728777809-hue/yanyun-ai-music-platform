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

    IntegrationComponentReadiness deepseek =
        report.components().stream()
            .filter(component -> component.component().equals("deepseek_guard"))
            .findFirst()
            .orElseThrow();
    assertEquals(IntegrationReadinessStatus.MOCK_ONLY, deepseek.status());
    assertEquals("MockDeepSeekLyricsClient", deepseek.implementation());

    IntegrationComponentReadiness image2 =
        report.components().stream()
            .filter(component -> component.component().equals("image2_guard"))
            .findFirst()
            .orElseThrow();
    assertEquals(IntegrationReadinessStatus.MOCK_ONLY, image2.status());
    assertEquals("MockCoverGenerationService", image2.implementation());
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
    properties.setDeepseekRealCallsEnabled(true);
    properties.setDeepseekBaseUrl("https://deepseek.example.test");
    properties.setImageProvider("image2");
    properties.setImageRealCallsEnabled(true);
    properties.setImage2BaseUrl("https://image2.example.test");

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

  @Test
  void deepseekRealCallsAreBlockedWhenApiKeyIsMissing() {
    CompanyIntegrationProperties properties = new CompanyIntegrationProperties();
    properties.setAgentRealCallsEnabled(true);
    properties.setDeepseekRealCallsEnabled(true);
    properties.setDeepseekBaseUrl("https://deepseek.example.test");
    properties.setDeepseekModelName("deepseek-test-model");

    IntegrationReadinessReport report =
        new IntegrationReadinessService(properties, fixedClock).buildReport();

    IntegrationComponentReadiness deepseek =
        report.components().stream()
            .filter(component -> component.component().equals("deepseek_guard"))
            .findFirst()
            .orElseThrow();
    assertEquals(IntegrationReadinessStatus.BLOCKED, deepseek.status());
    assertEquals("real-calls-missing-api-key", deepseek.configuredMode());
    assertTrue(deepseek.blocksCompanyDeployment());
  }

  @Test
  void deepseekRealCallsAreReadyWhenClientAndRequiredConfigExist() {
    CompanyIntegrationProperties properties = new CompanyIntegrationProperties();
    properties.setAgentRealCallsEnabled(true);
    properties.setDeepseekRealCallsEnabled(true);
    properties.setDeepseekBaseUrl("https://deepseek.example.test");
    properties.setDeepseekModelName("deepseek-test-model");

    IntegrationReadinessReport report =
        new IntegrationReadinessService(properties, fixedClock, false, false, true).buildReport();

    IntegrationComponentReadiness deepseek =
        report.components().stream()
            .filter(component -> component.component().equals("deepseek_guard"))
            .findFirst()
            .orElseThrow();
    assertEquals(IntegrationReadinessStatus.READY_FOR_LOCAL, deepseek.status());
    assertEquals("real-calls-enabled", deepseek.configuredMode());
    assertEquals("RealDeepSeekLyricsClient", deepseek.implementation());
    assertFalse(deepseek.blocksCompanyDeployment());
  }

  @Test
  void image2WellApiRealCallsAreBlockedWhenApiKeyIsMissing() {
    CompanyIntegrationProperties properties = new CompanyIntegrationProperties();
    properties.setImageProvider("image2");
    properties.setImage2Backend("wellapi");
    properties.setImageRealCallsEnabled(true);
    properties.setImage2BaseUrl("https://wellapi.example.test");
    properties.setImage2ModelName("image2-test-model");

    IntegrationReadinessReport report =
        new IntegrationReadinessService(properties, fixedClock).buildReport();

    IntegrationComponentReadiness image2 =
        report.components().stream()
            .filter(component -> component.component().equals("image2_guard"))
            .findFirst()
            .orElseThrow();
    assertEquals(IntegrationReadinessStatus.BLOCKED, image2.status());
    assertEquals("real-calls-missing-wellapi-api-key", image2.configuredMode());
    assertTrue(image2.blocksCompanyDeployment());
  }

  @Test
  void image2WellApiRealCallsAreReadyWhenApiKeyExists() {
    CompanyIntegrationProperties properties = new CompanyIntegrationProperties();
    properties.setImageProvider("image2");
    properties.setImage2Backend("wellapi");
    properties.setImageRealCallsEnabled(true);
    properties.setImage2BaseUrl("https://wellapi.example.test");
    properties.setImage2ModelName("gpt-image-2");

    IntegrationReadinessReport report =
        new IntegrationReadinessService(properties, fixedClock, false, false, false, false, true)
            .buildReport();

    IntegrationComponentReadiness image2 =
        report.components().stream()
            .filter(component -> component.component().equals("image2_guard"))
            .findFirst()
            .orElseThrow();
    assertEquals(IntegrationReadinessStatus.READY_FOR_LOCAL, image2.status());
    assertEquals("real-calls-enabled/wellapi", image2.configuredMode());
    assertEquals("WellApiImage2CoverGenerationService", image2.implementation());
    assertFalse(image2.blocksCompanyDeployment());
  }

  @Test
  void image2DreamMakerBackendStillRequiresDreamMakerGuard() {
    CompanyIntegrationProperties properties = new CompanyIntegrationProperties();
    properties.setImageProvider("image2");
    properties.setImage2Backend("dreammaker");
    properties.setImageRealCallsEnabled(true);
    properties.setImage2ModelName("gpt-image-2");

    IntegrationReadinessReport report =
        new IntegrationReadinessService(properties, fixedClock).buildReport();

    IntegrationComponentReadiness image2 =
        report.components().stream()
            .filter(component -> component.component().equals("image2_guard"))
            .findFirst()
            .orElseThrow();
    assertEquals(IntegrationReadinessStatus.BLOCKED, image2.status());
    assertEquals("image2-enabled-dreammaker-disabled", image2.configuredMode());
  }

  @Test
  void yunwuSunoRealCallsAreReadyWhenApiKeyExists() {
    CompanyIntegrationProperties properties = new CompanyIntegrationProperties();
    properties.setMusicProvider("suno");
    properties.setSunoBackend("yunwu");
    properties.setYunwuRealCallsEnabled(true);
    properties.setYunwuBaseUrl("https://yunwu.example.test");

    IntegrationReadinessReport report =
        new IntegrationReadinessService(properties, fixedClock, false, false, false, true, false)
            .buildReport();

    IntegrationComponentReadiness music =
        report.components().stream()
            .filter(component -> component.component().equals("music_provider"))
            .findFirst()
            .orElseThrow();
    assertEquals("suno/yunwu", music.configuredMode());
    assertEquals("YunwuSunoMusicProvider", music.implementation());

    IntegrationComponentReadiness yunwu =
        report.components().stream()
            .filter(component -> component.component().equals("yunwu_suno_guard"))
            .findFirst()
            .orElseThrow();
    assertEquals(IntegrationReadinessStatus.READY_FOR_LOCAL, yunwu.status());
    assertEquals("real-calls-enabled", yunwu.configuredMode());
  }

  @Test
  void dreamMakerRealCallsAreBlockedWhenCredentialsAreMissing() {
    CompanyIntegrationProperties properties = new CompanyIntegrationProperties();
    properties.setDreammakerRealCallsEnabled(true);

    IntegrationReadinessReport report =
        new IntegrationReadinessService(properties, fixedClock, false, true).buildReport();

    IntegrationComponentReadiness dreamMaker =
        report.components().stream()
            .filter(component -> component.component().equals("dreammaker_guard"))
            .findFirst()
            .orElseThrow();
    assertEquals(IntegrationReadinessStatus.BLOCKED, dreamMaker.status());
    assertEquals("real-calls-missing-credentials", dreamMaker.configuredMode());
    assertTrue(dreamMaker.blocksCompanyDeployment());
  }

  @Test
  void dreamMakerRealCallsAreReadyOnlyWhenCredentialsAreConfigured() {
    CompanyIntegrationProperties properties = new CompanyIntegrationProperties();
    properties.setDreammakerRealCallsEnabled(true);

    IntegrationReadinessReport report =
        new IntegrationReadinessService(properties, fixedClock, true, true).buildReport();

    IntegrationComponentReadiness dreamMaker =
        report.components().stream()
            .filter(component -> component.component().equals("dreammaker_guard"))
            .findFirst()
            .orElseThrow();
    assertEquals(IntegrationReadinessStatus.READY_FOR_LOCAL, dreamMaker.status());
    assertEquals("real-calls-enabled", dreamMaker.configuredMode());
    assertFalse(dreamMaker.blocksCompanyDeployment());
  }
}
