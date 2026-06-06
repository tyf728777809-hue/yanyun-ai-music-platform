package com.yanyun.music.configcenter;

import java.util.List;

public record IntegrationComponentReadiness(
    String component,
    String configuredMode,
    String implementation,
    IntegrationReadinessStatus status,
    boolean blocksCompanyDeployment,
    List<String> requiredEnvVars,
    String handoffNote) {}
