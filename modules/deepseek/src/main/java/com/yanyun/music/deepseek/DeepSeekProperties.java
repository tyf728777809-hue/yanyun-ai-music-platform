package com.yanyun.music.deepseek;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;

public class DeepSeekProperties {

  private boolean agentRealCallsEnabled;
  private boolean realCallsEnabled;
  private URI baseUrl = URI.create("https://api.deepseek.com");
  private String apiKey = "";
  private String modelName = "deepseek-v4-pro";
  private Duration requestTimeout = Duration.ofSeconds(30);
  private Duration creativeBriefRequestTimeout = Duration.ofSeconds(20);
  private int maxAttempts = 1;
  private int responseMaxTokens = 4096;
  private int creativeBriefResponseMaxTokens = 900;
  private int creativeBriefSemanticAttempts = 1;
  private int lyricsSemanticAttempts = 2;
  private BigDecimal temperature = BigDecimal.valueOf(0.7);

  public boolean isAgentRealCallsEnabled() {
    return agentRealCallsEnabled;
  }

  public void setAgentRealCallsEnabled(boolean agentRealCallsEnabled) {
    this.agentRealCallsEnabled = agentRealCallsEnabled;
  }

  public boolean isRealCallsEnabled() {
    return realCallsEnabled;
  }

  public void setRealCallsEnabled(boolean realCallsEnabled) {
    this.realCallsEnabled = realCallsEnabled;
  }

  public URI getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(URI baseUrl) {
    this.baseUrl = baseUrl == null ? URI.create("https://api.deepseek.com") : baseUrl;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey == null ? "" : apiKey.trim();
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName =
        modelName == null || modelName.isBlank() ? "deepseek-v4-pro" : modelName.trim();
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(Duration requestTimeout) {
    this.requestTimeout =
        requestTimeout == null || requestTimeout.isNegative()
            ? Duration.ofSeconds(30)
            : requestTimeout;
  }

  public Duration getCreativeBriefRequestTimeout() {
    return creativeBriefRequestTimeout;
  }

  public void setCreativeBriefRequestTimeout(Duration creativeBriefRequestTimeout) {
    this.creativeBriefRequestTimeout =
        creativeBriefRequestTimeout == null || creativeBriefRequestTimeout.isNegative()
            ? Duration.ofSeconds(20)
            : creativeBriefRequestTimeout;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = Math.max(1, maxAttempts);
  }

  public int getResponseMaxTokens() {
    return responseMaxTokens;
  }

  public void setResponseMaxTokens(int responseMaxTokens) {
    this.responseMaxTokens = Math.max(256, responseMaxTokens);
  }

  public int getCreativeBriefResponseMaxTokens() {
    return creativeBriefResponseMaxTokens;
  }

  public void setCreativeBriefResponseMaxTokens(int creativeBriefResponseMaxTokens) {
    this.creativeBriefResponseMaxTokens = Math.max(256, creativeBriefResponseMaxTokens);
  }

  public int getCreativeBriefSemanticAttempts() {
    return creativeBriefSemanticAttempts;
  }

  public void setCreativeBriefSemanticAttempts(int creativeBriefSemanticAttempts) {
    this.creativeBriefSemanticAttempts = Math.max(1, creativeBriefSemanticAttempts);
  }

  public int getLyricsSemanticAttempts() {
    return lyricsSemanticAttempts;
  }

  public void setLyricsSemanticAttempts(int lyricsSemanticAttempts) {
    this.lyricsSemanticAttempts = Math.max(1, lyricsSemanticAttempts);
  }

  public BigDecimal getTemperature() {
    return temperature;
  }

  public void setTemperature(BigDecimal temperature) {
    this.temperature =
        temperature == null || temperature.signum() < 0 ? BigDecimal.ZERO : temperature;
  }
}
