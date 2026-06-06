package com.yanyun.music.configcenter;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class IntegrationReadinessService {

  private final CompanyIntegrationProperties properties;
  private final Clock clock;
  private final boolean dreamMakerAccessKeyConfigured;
  private final boolean dreamMakerSecretKeyConfigured;
  private final boolean deepSeekApiKeyConfigured;
  private final boolean yunwuApiKeyConfigured;
  private final boolean image2ApiKeyConfigured;

  public IntegrationReadinessService(CompanyIntegrationProperties properties, Clock clock) {
    this(properties, clock, false, false, false);
  }

  public IntegrationReadinessService(
      CompanyIntegrationProperties properties,
      Clock clock,
      boolean dreamMakerAccessKeyConfigured,
      boolean dreamMakerSecretKeyConfigured) {
    this(properties, clock, dreamMakerAccessKeyConfigured, dreamMakerSecretKeyConfigured, false);
  }

  public IntegrationReadinessService(
      CompanyIntegrationProperties properties,
      Clock clock,
      boolean dreamMakerAccessKeyConfigured,
      boolean dreamMakerSecretKeyConfigured,
      boolean deepSeekApiKeyConfigured) {
    this(
        properties,
        clock,
        dreamMakerAccessKeyConfigured,
        dreamMakerSecretKeyConfigured,
        deepSeekApiKeyConfigured,
        false,
        false);
  }

  public IntegrationReadinessService(
      CompanyIntegrationProperties properties,
      Clock clock,
      boolean dreamMakerAccessKeyConfigured,
      boolean dreamMakerSecretKeyConfigured,
      boolean deepSeekApiKeyConfigured,
      boolean yunwuApiKeyConfigured,
      boolean image2ApiKeyConfigured) {
    this.properties = properties;
    this.clock = clock;
    this.dreamMakerAccessKeyConfigured = dreamMakerAccessKeyConfigured;
    this.dreamMakerSecretKeyConfigured = dreamMakerSecretKeyConfigured;
    this.deepSeekApiKeyConfigured = deepSeekApiKeyConfigured;
    this.yunwuApiKeyConfigured = yunwuApiKeyConfigured;
    this.image2ApiKeyConfigured = image2ApiKeyConfigured;
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
    components.add(renderWorkerComponent());
    components.add(objectStorageComponent());
    components.add(workflowComponent());
    components.add(deepSeekComponent());
    components.add(image2Component());
    components.add(yunwuComponent());
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
    if ("suno".equals(provider)) {
      String backend = normalizeOr(properties.getSunoBackend(), "yunwu");
      boolean dreamMaker = "dreammaker".equals(backend);
      return new IntegrationComponentReadiness(
          "music_provider",
          provider + "/" + backend,
          dreamMaker ? "SunoMusicProvider" : "YunwuSunoMusicProvider",
          IntegrationReadinessStatus.READY_FOR_LOCAL,
          false,
          dreamMaker
              ? List.of(
                  "MUSIC_PROVIDER",
                  "SUNO_BACKEND",
                  "DREAMMAKER_REAL_CALLS_ENABLED",
                  "DREAMMAKER_ACCESS_KEY",
                  "DREAMMAKER_SECRET_KEY",
                  "DREAMMAKER_SUNO_MODEL")
              : List.of(
                  "MUSIC_PROVIDER",
                  "SUNO_BACKEND",
                  "YUNWU_REAL_CALLS_ENABLED",
                  "YUNWU_BASE_URL",
                  "YUNWU_API_KEY",
                  "YUNWU_SUNO_MODEL"),
          dreamMaker
              ? "已选择 Suno 业务 Provider，后端使用 DreamMaker；这是正式生产目标路径。"
              : "已选择 Suno 业务 Provider，后端使用 Yunwu；这是当前非内网公网联调路径，生产仍需保留 DreamMaker 切换。");
    }
    if ("minimax".equals(provider)) {
      return new IntegrationComponentReadiness(
          "music_provider",
          provider + "/dreammaker",
          "MiniMaxMusicProvider",
          IntegrationReadinessStatus.READY_FOR_LOCAL,
          false,
          List.of(
              "MUSIC_PROVIDER",
              "DREAMMAKER_REAL_CALLS_ENABLED",
              "DREAMMAKER_ACCESS_KEY",
              "DREAMMAKER_SECRET_KEY",
              "MINIMAX_MODEL"),
          "已选择 MiniMax 业务 Provider，当前仍通过 DreamMaker 任务式客户端接入。");
    }
    return new IntegrationComponentReadiness(
        "music_provider",
        provider,
        mock ? "MockMusicProvider" : provider + " provider",
        mock ? IntegrationReadinessStatus.MOCK_ONLY : IntegrationReadinessStatus.READY_FOR_LOCAL,
        mock,
        List.of("MUSIC_PROVIDER"),
        mock ? "本地 Mock 可跑通；真实出歌联调需显式选择 suno 或 minimax。" : "已选择真实音乐 Provider 边界。");
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

  private IntegrationComponentReadiness renderWorkerComponent() {
    String mode = normalize(properties.getRenderWorkerMode());
    boolean localProcess = "local-process".equals(mode);
    return new IntegrationComponentReadiness(
        "render_worker",
        mode,
        localProcess ? "LocalProcessVideoRenderService" : "MockVideoRenderService",
        localProcess
            ? IntegrationReadinessStatus.READY_FOR_LOCAL
            : IntegrationReadinessStatus.MOCK_ONLY,
        true,
        List.of(
            "RENDER_WORKER_MODE",
            "RENDER_WORKER_WORKING_DIRECTORY",
            "RENDER_WORKER_COMMAND",
            "RENDER_WORKER_ARGUMENTS",
            "RENDER_WORKER_TIMEOUT"),
        localProcess
            ? "已选择本地进程 render-worker，可用于本地真实 MP4 成片 smoke。"
            : "默认 Mock 视频资产可跑通；真实 MP4 成片 smoke 需显式切换 local-process。");
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
    List<String> requiredEnvVars =
        List.of(
            "DREAMMAKER_REAL_CALLS_ENABLED",
            "DREAMMAKER_API_BASE_URL",
            "DREAMMAKER_ACCESS_KEY",
            "DREAMMAKER_SECRET_KEY");
    if (!enabled) {
      return new IntegrationComponentReadiness(
          "dreammaker_guard",
          "real-calls-disabled",
          "DreamMakerHttpClient",
          IntegrationReadinessStatus.MOCK_ONLY,
          true,
          requiredEnvVars,
          "默认保护状态，不会触发真实 Suno/MiniMax 调用。");
    }
    if (!dreamMakerAccessKeyConfigured || !dreamMakerSecretKeyConfigured) {
      return new IntegrationComponentReadiness(
          "dreammaker_guard",
          "real-calls-missing-credentials",
          "DreamMakerHttpClient",
          IntegrationReadinessStatus.BLOCKED,
          true,
          requiredEnvVars,
          "DreamMaker 真实调用硬开关已打开，但缺少 AccessKey 或 SecretKey；不得发出真实音乐请求。");
    }
    return new IntegrationComponentReadiness(
        "dreammaker_guard",
        "real-calls-enabled",
        "DreamMakerHttpClient",
        IntegrationReadinessStatus.READY_FOR_LOCAL,
        false,
        requiredEnvVars,
        "真实 DreamMaker 调用硬开关已打开，且 AK/SK 已通过当前运行环境配置；报告不会输出密钥值。");
  }

  private IntegrationComponentReadiness deepSeekComponent() {
    boolean enabled = properties.isDeepseekRealCallsEnabled();
    List<String> requiredEnvVars =
        List.of(
            "AGENT_REAL_CALLS_ENABLED",
            "DEEPSEEK_REAL_CALLS_ENABLED",
            "DEEPSEEK_BASE_URL",
            "DEEPSEEK_API_KEY",
            "DEEPSEEK_MODEL_NAME",
            "DEEPSEEK_TIMEOUT_MS",
            "DEEPSEEK_MAX_ATTEMPTS");
    if (!enabled) {
      return new IntegrationComponentReadiness(
          "deepseek_guard",
          "real-calls-disabled",
          "MockDeepSeekLyricsClient",
          IntegrationReadinessStatus.MOCK_ONLY,
          true,
          requiredEnvVars,
          "默认保护状态，写词、润色和续写仍使用 Mock DeepSeek，不会触发真实 LLM 调用。");
    }
    if (!properties.isAgentRealCallsEnabled()) {
      return new IntegrationComponentReadiness(
          "deepseek_guard",
          "deepseek-enabled-agent-disabled",
          "MockDeepSeekLyricsClient",
          IntegrationReadinessStatus.BLOCKED,
          true,
          requiredEnvVars,
          "DeepSeek 真实调用开关已打开，但 AGENT_REAL_CALLS_ENABLED=false；必须同时打开总 Agent 开关。");
    }
    if (properties.getDeepseekBaseUrl() == null || properties.getDeepseekBaseUrl().isBlank()) {
      return new IntegrationComponentReadiness(
          "deepseek_guard",
          "real-calls-missing-base-url",
          "MockDeepSeekLyricsClient",
          IntegrationReadinessStatus.BLOCKED,
          true,
          requiredEnvVars,
          "DeepSeek 真实调用开关已打开，但缺少 DEEPSEEK_BASE_URL；不得发出外部请求。");
    }
    if (properties.getDeepseekModelName() == null || properties.getDeepseekModelName().isBlank()) {
      return new IntegrationComponentReadiness(
          "deepseek_guard",
          "real-calls-missing-model",
          "RealDeepSeekLyricsClient",
          IntegrationReadinessStatus.BLOCKED,
          true,
          requiredEnvVars,
          "DeepSeek 真实调用开关已打开，但缺少 DEEPSEEK_MODEL_NAME；不得发出外部请求。");
    }
    if (!deepSeekApiKeyConfigured) {
      return new IntegrationComponentReadiness(
          "deepseek_guard",
          "real-calls-missing-api-key",
          "RealDeepSeekLyricsClient",
          IntegrationReadinessStatus.BLOCKED,
          true,
          requiredEnvVars,
          "DeepSeek 真实调用开关已打开，但缺少 DEEPSEEK_API_KEY；不得发出外部请求。");
    }
    return new IntegrationComponentReadiness(
        "deepseek_guard",
        "real-calls-enabled",
        "RealDeepSeekLyricsClient",
        IntegrationReadinessStatus.READY_FOR_LOCAL,
        false,
        requiredEnvVars,
        "DeepSeek OpenAI 兼容真实客户端已可用于受控本地联调；报告不会输出 API Key。");
  }

  private IntegrationComponentReadiness yunwuComponent() {
    boolean selectedForSuno =
        "suno".equals(normalize(properties.getMusicProvider()))
            && "yunwu".equals(normalizeOr(properties.getSunoBackend(), "dreammaker"));
    boolean enabled = selectedForSuno && properties.isYunwuRealCallsEnabled();
    List<String> requiredEnvVars =
        List.of(
            "SUNO_BACKEND",
            "YUNWU_REAL_CALLS_ENABLED",
            "YUNWU_BASE_URL",
            "YUNWU_API_KEY",
            "YUNWU_SUNO_MODEL",
            "YUNWU_MAX_POLL_ATTEMPTS",
            "YUNWU_POLL_INTERVAL");
    if (!selectedForSuno) {
      return new IntegrationComponentReadiness(
          "yunwu_suno_guard",
          "not-selected",
          "YunwuSunoMusicProvider",
          IntegrationReadinessStatus.MOCK_ONLY,
          false,
          requiredEnvVars,
          "Yunwu 当前未作为 Suno 后端启用；DreamMaker Suno 接口仍保留用于生产切换。");
    }
    if (!enabled) {
      return new IntegrationComponentReadiness(
          "yunwu_suno_guard",
          "real-calls-disabled",
          "YunwuSunoMusicProvider",
          IntegrationReadinessStatus.MOCK_ONLY,
          true,
          requiredEnvVars,
          "已选择 Yunwu 作为当前公网联调后端，但真实调用硬开关未打开，不会发出外部音乐请求。");
    }
    if (properties.getYunwuBaseUrl() == null || properties.getYunwuBaseUrl().isBlank()) {
      return new IntegrationComponentReadiness(
          "yunwu_suno_guard",
          "real-calls-missing-base-url",
          "YunwuSunoMusicProvider",
          IntegrationReadinessStatus.BLOCKED,
          true,
          requiredEnvVars,
          "Yunwu 真实调用开关已打开，但缺少 YUNWU_BASE_URL；不得发出外部请求。");
    }
    if (!yunwuApiKeyConfigured) {
      return new IntegrationComponentReadiness(
          "yunwu_suno_guard",
          "real-calls-missing-api-key",
          "YunwuSunoMusicProvider",
          IntegrationReadinessStatus.BLOCKED,
          true,
          requiredEnvVars,
          "Yunwu 真实调用开关已打开，但缺少 YUNWU_API_KEY；不得发出外部请求。");
    }
    return new IntegrationComponentReadiness(
        "yunwu_suno_guard",
        "real-calls-enabled",
        "YunwuSunoMusicProvider",
        IntegrationReadinessStatus.READY_FOR_LOCAL,
        false,
        requiredEnvVars,
        "Yunwu Suno 可用于当前非公司内网环境的受控公网联调；报告不会输出 API Key。");
  }

  private IntegrationComponentReadiness image2Component() {
    String provider = normalize(properties.getImageProvider());
    String backend = normalizeOr(properties.getImage2Backend(), "dreammaker");
    boolean enabled = properties.isImageRealCallsEnabled() || !"mock".equals(provider);
    List<String> requiredEnvVars =
        "dreammaker".equals(backend)
            ? List.of(
                "IMAGE_PROVIDER",
                "IMAGE2_BACKEND",
                "IMAGE_REAL_CALLS_ENABLED",
                "DREAMMAKER_REAL_CALLS_ENABLED",
                "DREAMMAKER_API_BASE_URL",
                "DREAMMAKER_ACCESS_KEY",
                "DREAMMAKER_SECRET_KEY",
                "IMAGE2_MODEL_NAME",
                "IMAGE2_SIZE",
                "IMAGE2_QUALITY",
                "IMAGE2_MAX_POLL_ATTEMPTS")
            : List.of(
                "IMAGE_PROVIDER",
                "IMAGE2_BACKEND",
                "IMAGE_REAL_CALLS_ENABLED",
                "WELLAPI_BASE_URL",
                "WELLAPI_API_KEY",
                "IMAGE2_MODEL_NAME",
                "IMAGE2_SIZE",
                "IMAGE2_QUALITY",
                "IMAGE2_OUTPUT_FORMAT");
    if (!enabled) {
      return new IntegrationComponentReadiness(
          "image2_guard",
          "mock",
          "MockCoverGenerationService",
          IntegrationReadinessStatus.MOCK_ONLY,
          true,
          requiredEnvVars,
          "默认保护状态，封面生成仍使用 MockCoverGenerationService，不会触发真实 Image 2 调用。");
    }
    if (!properties.isImageRealCallsEnabled()) {
      return new IntegrationComponentReadiness(
          "image2_guard",
          provider + "-provider-real-calls-disabled",
          "MockCoverGenerationService",
          IntegrationReadinessStatus.BLOCKED,
          true,
          requiredEnvVars,
          "已选择非 Mock 图片 Provider，但 IMAGE_REAL_CALLS_ENABLED=false；不得发出真实 Image 2 请求。");
    }
    if ("dreammaker".equals(backend) && !properties.isDreammakerRealCallsEnabled()) {
      return new IntegrationComponentReadiness(
          "image2_guard",
          "image2-enabled-dreammaker-disabled",
          "DreamMakerImage2CoverGenerationService",
          IntegrationReadinessStatus.BLOCKED,
          true,
          requiredEnvVars,
          "Image 2 选择 DreamMaker 后端；IMAGE_REAL_CALLS_ENABLED=true 时也必须打开 DREAMMAKER_REAL_CALLS_ENABLED。");
    }
    if ("dreammaker".equals(backend)
        && (!dreamMakerAccessKeyConfigured || !dreamMakerSecretKeyConfigured)) {
      return new IntegrationComponentReadiness(
          "image2_guard",
          "real-calls-missing-dreammaker-credentials",
          "DreamMakerImage2CoverGenerationService",
          IntegrationReadinessStatus.BLOCKED,
          true,
          requiredEnvVars,
          "Image 2 真实调用开关已打开，但缺少 DreamMaker AccessKey 或 SecretKey；不得发出外部请求。");
    }
    if ("wellapi".equals(backend)
        && (properties.getImage2BaseUrl() == null || properties.getImage2BaseUrl().isBlank())) {
      return new IntegrationComponentReadiness(
          "image2_guard",
          "real-calls-missing-wellapi-base-url",
          "WellApiImage2CoverGenerationService",
          IntegrationReadinessStatus.BLOCKED,
          true,
          requiredEnvVars,
          "Image 2 真实调用开关已打开，且选择 WellAPI 后端，但缺少 WELLAPI_BASE_URL；不得发出外部请求。");
    }
    if ("wellapi".equals(backend) && !image2ApiKeyConfigured) {
      return new IntegrationComponentReadiness(
          "image2_guard",
          "real-calls-missing-wellapi-api-key",
          "WellApiImage2CoverGenerationService",
          IntegrationReadinessStatus.BLOCKED,
          true,
          requiredEnvVars,
          "Image 2 真实调用开关已打开，且选择 WellAPI 后端，但缺少 WELLAPI_API_KEY；不得发出外部请求。");
    }
    if (properties.getImage2ModelName() == null || properties.getImage2ModelName().isBlank()) {
      return new IntegrationComponentReadiness(
          "image2_guard",
          "real-calls-missing-model",
          "dreammaker".equals(backend)
              ? "DreamMakerImage2CoverGenerationService"
              : "WellApiImage2CoverGenerationService",
          IntegrationReadinessStatus.BLOCKED,
          true,
          requiredEnvVars,
          "Image 2 真实调用开关已打开，但缺少 IMAGE2_MODEL_NAME；不得发出外部请求。");
    }
    return new IntegrationComponentReadiness(
        "image2_guard",
        "real-calls-enabled/" + backend,
        "dreammaker".equals(backend)
            ? "DreamMakerImage2CoverGenerationService"
            : "WellApiImage2CoverGenerationService",
        IntegrationReadinessStatus.READY_FOR_LOCAL,
        false,
        requiredEnvVars,
        "dreammaker".equals(backend)
            ? "Image 2 通过 DreamMaker 任务式客户端接入，这是正式生产目标路径。"
            : "Image 2 通过 WellAPI OpenAI 兼容图片接口接入，这是当前公网联调路径；供应商图片会导入平台对象存储后再进入发布包。");
  }

  private static String normalize(String value) {
    return safeValue(value, "mock").trim().toLowerCase(Locale.ROOT);
  }

  private static String normalizeOr(String value, String fallback) {
    return safeValue(value, fallback).trim().toLowerCase(Locale.ROOT);
  }

  private static String safeValue(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
