package com.yanyun.music.configcenter;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class IntegrationReadinessService {

  private final CompanyIntegrationProperties properties;
  private final Clock clock;

  public IntegrationReadinessService(CompanyIntegrationProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public IntegrationReadinessReport buildReport() {
    List<IntegrationComponentReadiness> components = new ArrayList<>();
    components.add(
        companyAdapter(
            "company_account",
            properties.getAccountAdapterMode(),
            "MockAccountAdapter",
            List.of("COMPANY_ACCOUNT_ADAPTER_MODE", "COMPANY_ACCOUNT_BASE_URL"),
            "正式接入时由公司账号态或网关注入真实用户身份，不能信任用户自传 Mock Header。"));
    components.add(
        companyAdapter(
            "company_moderation",
            properties.getModerationAdapterMode(),
            "MockModerationAdapter",
            List.of("COMPANY_MODERATION_ADAPTER_MODE", "COMPANY_MODERATION_BASE_URL"),
            "正式接入时需覆盖输入、歌词和发布包交接前审核，并返回可映射的阻断原因。"));
    components.add(
        companyAdapter(
            "company_quota",
            properties.getQuotaAdapterMode(),
            "MockQuotaAdapter",
            List.of("COMPANY_QUOTA_ADAPTER_MODE", "COMPANY_QUOTA_BASE_URL"),
            "正式接入时 lock / commit / release 必须幂等，并能与公司权益流水对账。"));
    components.add(
        companyAdapter(
            "company_publish",
            properties.getPublishAdapterMode(),
            "MockPublishAdapter",
            List.of("COMPANY_PUBLISH_ADAPTER_MODE", "COMPANY_PUBLISH_BASE_URL"),
            "本平台只准备发布包和交接状态；社区发布、审核流转和互动由公司系统承接。"));
    components.add(
        companyAdapter(
            "company_share",
            properties.getShareAdapterMode(),
            "NotImplementedShareBoundary",
            List.of("COMPANY_SHARE_ADAPTER_MODE", "COMPANY_SHARE_BASE_URL"),
            "分享系统当前没有本平台业务调用点，但 PRD 边界要求由公司既有分享系统承接。"));

    components.add(musicProviderComponent());
    components.add(objectStorageComponent());
    components.add(workflowComponent());
    components.add(dreamMakerComponent());

    boolean productionLike = !"local".equals(normalize(properties.getEnvironment()));
    boolean blocked =
        components.stream()
            .anyMatch(
                component ->
                    component.status() == IntegrationReadinessStatus.BLOCKED
                        || (productionLike && component.blocksCompanyDeployment()));
    IntegrationOverallStatus overallStatus =
        blocked
            ? IntegrationOverallStatus.BLOCKED_FOR_COMPANY_DEPLOYMENT
            : IntegrationOverallStatus.READY_FOR_LOCAL;

    return new IntegrationReadinessReport(
        safeValue(properties.getEnvironment(), "local"),
        "music-api",
        OffsetDateTime.now(clock),
        overallStatus,
        List.copyOf(components),
        List.of(
            "本报告只读取配置和静态边界，不调用真实公司系统或真实模型供应商。",
            "required_env_vars 只列变量名，不代表这些变量已安全配置。",
            "公司部署前必须把所有 blocks_company_deployment=true 的 Mock 边界替换或由公司确认豁免。"));
  }

  private IntegrationComponentReadiness companyAdapter(
      String component,
      String configuredMode,
      String implementation,
      List<String> requiredEnvVars,
      String handoffNote) {
    String mode = normalize(configuredMode);
    if ("mock".equals(mode)) {
      return new IntegrationComponentReadiness(
          component,
          "mock",
          implementation,
          IntegrationReadinessStatus.MOCK_ONLY,
          true,
          requiredEnvVars,
          handoffNote);
    }
    return new IntegrationComponentReadiness(
        component,
        mode,
        implementation,
        IntegrationReadinessStatus.BLOCKED,
        true,
        requiredEnvVars,
        "已配置非 Mock 模式，但仓库内尚未提供真实公司 Adapter 实现。" + handoffNote);
  }

  private IntegrationComponentReadiness musicProviderComponent() {
    String provider = normalize(properties.getMusicProvider());
    boolean mock = "mock".equals(provider);
    return new IntegrationComponentReadiness(
        "music_provider",
        provider,
        mock ? "MockMusicProvider" : provider + " DreamMaker provider",
        mock ? IntegrationReadinessStatus.MOCK_ONLY : IntegrationReadinessStatus.READY_FOR_LOCAL,
        mock,
        List.of(
            "MUSIC_PROVIDER",
            "DREAMMAKER_REAL_CALLS_ENABLED",
            "DREAMMAKER_ACCESS_KEY",
            "DREAMMAKER_SECRET_KEY"),
        mock
            ? "本地 Mock 可跑通；真实出歌联调需显式选择 suno 或 minimax。"
            : "已选择真实音乐 Provider 边界；真实调用仍受 DreamMaker 硬开关保护。");
  }

  private IntegrationComponentReadiness objectStorageComponent() {
    String provider = normalize(properties.getObjectStorageProvider());
    boolean local = "local".equals(provider);
    return new IntegrationComponentReadiness(
        "object_storage",
        provider,
        local ? "LocalObjectStorageClient" : "S3ObjectStorageClient",
        local ? IntegrationReadinessStatus.MOCK_ONLY : IntegrationReadinessStatus.READY_FOR_LOCAL,
        local,
        List.of(
            "OBJECT_STORAGE_PROVIDER",
            "S3_ENDPOINT",
            "S3_PUBLIC_ENDPOINT",
            "S3_BUCKET_YANYUN_WORKS",
            "S3_ACCESS_KEY",
            "S3_SECRET_KEY"),
        local ? "本地文件存储只适合开发；公司部署需切换到 S3/MinIO 兼容对象存储。" : "已选择 S3/MinIO 兼容对象存储模式。");
  }

  private IntegrationComponentReadiness workflowComponent() {
    String mode = normalize(properties.getWorkflowDispatchMode());
    String target = normalize(properties.getWorkflowDispatchTarget());
    boolean temporal = "outbox".equals(mode) && "temporal".equals(target);
    return new IntegrationComponentReadiness(
        "workflow_dispatch",
        mode + "/" + target,
        temporal ? "TemporalSongProductionWorkflowStarter" : "LocalSongProductionWorkflowStarter",
        temporal
            ? IntegrationReadinessStatus.READY_FOR_LOCAL
            : IntegrationReadinessStatus.MOCK_ONLY,
        !temporal,
        List.of(
            "MUSIC_WORKFLOW_DISPATCH_MODE",
            "WORKFLOW_OUTBOX_DISPATCH_TARGET",
            "TEMPORAL_TARGET",
            "TEMPORAL_NAMESPACE",
            "TEMPORAL_TASK_QUEUE"),
        temporal
            ? "已配置 API outbox 到 Temporal worker 的启动边界。"
            : "本地 sync/local 模式可开发；公司部署建议使用 outbox + temporal。");
  }

  private IntegrationComponentReadiness dreamMakerComponent() {
    boolean enabled = properties.isDreammakerRealCallsEnabled();
    return new IntegrationComponentReadiness(
        "dreammaker_guard",
        enabled ? "real-calls-enabled" : "real-calls-disabled",
        "DreamMakerHttpClient",
        enabled ? IntegrationReadinessStatus.READY_FOR_LOCAL : IntegrationReadinessStatus.MOCK_ONLY,
        !enabled,
        List.of(
            "DREAMMAKER_REAL_CALLS_ENABLED",
            "DREAMMAKER_API_BASE_URL",
            "DREAMMAKER_ACCESS_KEY",
            "DREAMMAKER_SECRET_KEY"),
        enabled ? "真实 DreamMaker 调用硬开关已打开；必须确认密钥通过安全注入。" : "默认保护状态，不会触发真实 Suno/MiniMax 调用。");
  }

  private static String normalize(String value) {
    return safeValue(value, "mock").trim().toLowerCase(Locale.ROOT);
  }

  private static String safeValue(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
