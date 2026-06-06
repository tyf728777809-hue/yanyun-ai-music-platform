package com.yanyun.music.suno;

import java.net.URI;
import java.time.Duration;

public class YunwuProperties {

  private URI baseUrl = URI.create("https://yunwu.ai");
  private String apiKey = "";
  private boolean realCallsEnabled;
  private Duration requestTimeout = Duration.ofSeconds(30);
  private int maxPollAttempts = 60;
  private Duration pollInterval = Duration.ofSeconds(2);
  private String sunoModel = "chirp-v5";

  public URI getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(URI baseUrl) {
    this.baseUrl = baseUrl == null ? URI.create("https://yunwu.ai") : baseUrl;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey == null ? "" : apiKey.trim();
  }

  public boolean isRealCallsEnabled() {
    return realCallsEnabled;
  }

  public void setRealCallsEnabled(boolean realCallsEnabled) {
    this.realCallsEnabled = realCallsEnabled;
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

  public int getMaxPollAttempts() {
    return maxPollAttempts;
  }

  public void setMaxPollAttempts(int maxPollAttempts) {
    this.maxPollAttempts = Math.max(1, maxPollAttempts);
  }

  public Duration getPollInterval() {
    return pollInterval;
  }

  public void setPollInterval(Duration pollInterval) {
    this.pollInterval =
        pollInterval == null || pollInterval.isNegative() ? Duration.ZERO : pollInterval;
  }

  public String getSunoModel() {
    return sunoModel;
  }

  public void setSunoModel(String sunoModel) {
    this.sunoModel = sunoModel == null || sunoModel.isBlank() ? "chirp-v5" : sunoModel.trim();
  }
}
