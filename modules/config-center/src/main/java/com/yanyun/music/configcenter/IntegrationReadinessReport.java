package com.yanyun.music.configcenter;

import java.time.OffsetDateTime;
import java.util.List;

public record IntegrationReadinessReport(
    String environment,
    String service,
    OffsetDateTime generatedAt,
    IntegrationOverallStatus overallStatus,
    List<IntegrationComponentReadiness> components,
    List<String> notes) {}
