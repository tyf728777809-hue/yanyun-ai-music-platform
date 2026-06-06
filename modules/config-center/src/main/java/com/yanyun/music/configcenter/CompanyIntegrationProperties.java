package com.yanyun.music.configcenter;

public class CompanyIntegrationProperties {

  private String environment = "local";
  private String accountAdapterMode = "mock";
  private String moderationAdapterMode = "mock";
  private String quotaAdapterMode = "mock";
  private String publishAdapterMode = "mock";
  private String shareAdapterMode = "mock";
  private String musicProvider = "mock";
  private String renderWorkerMode = "mock";
  private String objectStorageProvider = "local";
  private String workflowDispatchMode = "sync";
  private String workflowDispatchTarget = "local";
  private String temporalTarget = "localhost:7233";
  private boolean agentRealCallsEnabled;
  private boolean deepseekRealCallsEnabled;
  private String deepseekBaseUrl = "";
  private String deepseekModelName = "";
  private String imageProvider = "mock";
  private boolean imageRealCallsEnabled;
  private String image2BaseUrl = "";
  private String image2ModelName = "";
  private boolean dreammakerRealCallsEnabled;

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public String getAccountAdapterMode() {
    return accountAdapterMode;
  }

  public void setAccountAdapterMode(String accountAdapterMode) {
    this.accountAdapterMode = accountAdapterMode;
  }

  public String getModerationAdapterMode() {
    return moderationAdapterMode;
  }

  public void setModerationAdapterMode(String moderationAdapterMode) {
    this.moderationAdapterMode = moderationAdapterMode;
  }

  public String getQuotaAdapterMode() {
    return quotaAdapterMode;
  }

  public void setQuotaAdapterMode(String quotaAdapterMode) {
    this.quotaAdapterMode = quotaAdapterMode;
  }

  public String getPublishAdapterMode() {
    return publishAdapterMode;
  }

  public void setPublishAdapterMode(String publishAdapterMode) {
    this.publishAdapterMode = publishAdapterMode;
  }

  public String getShareAdapterMode() {
    return shareAdapterMode;
  }

  public void setShareAdapterMode(String shareAdapterMode) {
    this.shareAdapterMode = shareAdapterMode;
  }

  public String getMusicProvider() {
    return musicProvider;
  }

  public void setMusicProvider(String musicProvider) {
    this.musicProvider = musicProvider;
  }

  public String getRenderWorkerMode() {
    return renderWorkerMode;
  }

  public void setRenderWorkerMode(String renderWorkerMode) {
    this.renderWorkerMode = renderWorkerMode;
  }

  public String getObjectStorageProvider() {
    return objectStorageProvider;
  }

  public void setObjectStorageProvider(String objectStorageProvider) {
    this.objectStorageProvider = objectStorageProvider;
  }

  public String getWorkflowDispatchMode() {
    return workflowDispatchMode;
  }

  public void setWorkflowDispatchMode(String workflowDispatchMode) {
    this.workflowDispatchMode = workflowDispatchMode;
  }

  public String getWorkflowDispatchTarget() {
    return workflowDispatchTarget;
  }

  public void setWorkflowDispatchTarget(String workflowDispatchTarget) {
    this.workflowDispatchTarget = workflowDispatchTarget;
  }

  public String getTemporalTarget() {
    return temporalTarget;
  }

  public void setTemporalTarget(String temporalTarget) {
    this.temporalTarget = temporalTarget;
  }

  public boolean isAgentRealCallsEnabled() {
    return agentRealCallsEnabled;
  }

  public void setAgentRealCallsEnabled(boolean agentRealCallsEnabled) {
    this.agentRealCallsEnabled = agentRealCallsEnabled;
  }

  public boolean isDeepseekRealCallsEnabled() {
    return deepseekRealCallsEnabled;
  }

  public void setDeepseekRealCallsEnabled(boolean deepseekRealCallsEnabled) {
    this.deepseekRealCallsEnabled = deepseekRealCallsEnabled;
  }

  public String getDeepseekBaseUrl() {
    return deepseekBaseUrl;
  }

  public void setDeepseekBaseUrl(String deepseekBaseUrl) {
    this.deepseekBaseUrl = deepseekBaseUrl;
  }

  public String getDeepseekModelName() {
    return deepseekModelName;
  }

  public void setDeepseekModelName(String deepseekModelName) {
    this.deepseekModelName = deepseekModelName;
  }

  public String getImageProvider() {
    return imageProvider;
  }

  public void setImageProvider(String imageProvider) {
    this.imageProvider = imageProvider;
  }

  public boolean isImageRealCallsEnabled() {
    return imageRealCallsEnabled;
  }

  public void setImageRealCallsEnabled(boolean imageRealCallsEnabled) {
    this.imageRealCallsEnabled = imageRealCallsEnabled;
  }

  public String getImage2BaseUrl() {
    return image2BaseUrl;
  }

  public void setImage2BaseUrl(String image2BaseUrl) {
    this.image2BaseUrl = image2BaseUrl;
  }

  public String getImage2ModelName() {
    return image2ModelName;
  }

  public void setImage2ModelName(String image2ModelName) {
    this.image2ModelName = image2ModelName;
  }

  public boolean isDreammakerRealCallsEnabled() {
    return dreammakerRealCallsEnabled;
  }

  public void setDreammakerRealCallsEnabled(boolean dreammakerRealCallsEnabled) {
    this.dreammakerRealCallsEnabled = dreammakerRealCallsEnabled;
  }
}
