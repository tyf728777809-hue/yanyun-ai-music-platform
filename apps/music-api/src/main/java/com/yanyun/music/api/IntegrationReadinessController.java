package com.yanyun.music.api;

import com.yanyun.music.configcenter.IntegrationReadinessReport;
import com.yanyun.music.configcenter.IntegrationReadinessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IntegrationReadinessController {

  private final IntegrationReadinessService integrationReadinessService;

  public IntegrationReadinessController(IntegrationReadinessService integrationReadinessService) {
    this.integrationReadinessService = integrationReadinessService;
  }

  @GetMapping("/internal/integration-readiness")
  public IntegrationReadinessReport readiness() {
    return integrationReadinessService.buildReport();
  }
}
