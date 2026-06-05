package com.yanyun.music.dreammaker;

import java.net.URI;
import java.time.Duration;

public class DreamMakerProperties {

  private URI baseUrl = URI.create("https://api-all.dreammaker.netease.com");
  private String accessKey = "";
  private String secretKey = "";
  private String userAccessToken = "";
  private boolean realCallsEnabled;
  private Duration requestTimeout = Duration.ofSeconds(30);
  private int maxPollAttempts = 60;
  private Duration pollInterval = Duration.ofSeconds(2);
  private String sunoModel = "chirp-crow";
  private String minimaxModel = "minimax-music-2.6";

  public URI getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(URI baseUrl) {
    this.baseUrl = baseUrl == null ? URI.create("https://api-all.dreammaker.netease.com") : baseUrl;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey == null ? "" : accessKey.trim();
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey == null ? "" : secretKey.trim();
  }

  public String getUserAccessToken() {
    return userAccessToken;
  }

  public void setUserAccessToken(String userAccessToken) {
    this.userAccessToken = userAccessToken == null ? "" : userAccessToken.trim();
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
    this.sunoModel = sunoModel == null || sunoModel.isBlank() ? "chirp-crow" : sunoModel.trim();
  }

  public String getMinimaxModel() {
    return minimaxModel;
  }

  public void setMinimaxModel(String minimaxModel) {
    this.minimaxModel =
        minimaxModel == null || minimaxModel.isBlank() ? "minimax-music-2.6" : minimaxModel.trim();
  }
}
