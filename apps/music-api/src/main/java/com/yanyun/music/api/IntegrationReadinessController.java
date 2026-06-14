package com.yanyun.music.api;

import com.yanyun.music.configcenter.IntegrationReadinessReport;
import com.yanyun.music.configcenter.IntegrationReadinessService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class IntegrationReadinessController {

  private final IntegrationReadinessService integrationReadinessService;
  private final boolean allowPublic;

  public IntegrationReadinessController(
      IntegrationReadinessService integrationReadinessService,
      @Value("${yanyun.integration-readiness.allow-public:false}") boolean allowPublic) {
    this.integrationReadinessService = integrationReadinessService;
    this.allowPublic = allowPublic;
  }

  @GetMapping("/internal/integration-readiness")
  public IntegrationReadinessReport readiness(HttpServletRequest request) {
    if (!allowPublic && !isLocalRequest(request)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
    }
    return integrationReadinessService.buildReport();
  }

  IntegrationReadinessReport readiness() {
    return integrationReadinessService.buildReport();
  }

  private boolean isLocalRequest(HttpServletRequest request) {
    if (request == null) {
      return false;
    }
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null
        && !forwardedFor.isBlank()
        && !isLoopback(forwardedFor.split(",")[0])) {
      return false;
    }
    return isLoopback(request.getRemoteAddr());
  }

  private boolean isLoopback(String host) {
    if (host == null || host.isBlank()) {
      return false;
    }
    String value = host.trim();
    return "localhost".equalsIgnoreCase(value)
        || "127.0.0.1".equals(value)
        || "::1".equals(value)
        || "0:0:0:0:0:0:0:1".equals(value)
        || resolveLoopback(value);
  }

  private boolean resolveLoopback(String host) {
    try {
      return InetAddress.getByName(host).isLoopbackAddress();
    } catch (UnknownHostException exception) {
      return false;
    }
  }
}
