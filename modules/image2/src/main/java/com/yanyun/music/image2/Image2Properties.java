package com.yanyun.music.image2;

import java.net.URI;
import java.time.Duration;

public class Image2Properties {

  private URI baseUrl = URI.create("https://wellapi.ai");
  private String apiKey = "";
  private boolean realCallsEnabled;
  private String appName = "gpt-image-2";
  private String subAppName = "gpt-image-2";
  private String modelName = "gpt-image-2";
  private String size = "2048x1152";
  private String quality = "medium";
  private String outputFormat = "jpeg";
  private String background = "opaque";
  private Duration requestTimeout = Duration.ofSeconds(30);
  private int maxPollAttempts = 60;
  private Duration pollInterval = Duration.ofSeconds(2);

  public URI getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(URI baseUrl) {
    this.baseUrl = baseUrl == null ? URI.create("https://wellapi.ai") : baseUrl;
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

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName == null || appName.isBlank() ? "gpt-image-2" : appName.trim();
  }

  public String getSubAppName() {
    return subAppName;
  }

  public void setSubAppName(String subAppName) {
    this.subAppName =
        subAppName == null || subAppName.isBlank() ? "gpt-image-2" : subAppName.trim();
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName == null || modelName.isBlank() ? "gpt-image-2" : modelName.trim();
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size == null || size.isBlank() ? "2048x1152" : size.trim();
  }

  public String getQuality() {
    return quality;
  }

  public void setQuality(String quality) {
    this.quality = quality == null || quality.isBlank() ? "medium" : quality.trim();
  }

  public String getOutputFormat() {
    return outputFormat;
  }

  public void setOutputFormat(String outputFormat) {
    this.outputFormat =
        outputFormat == null || outputFormat.isBlank() ? "jpeg" : outputFormat.trim();
  }

  public String getBackground() {
    return background;
  }

  public void setBackground(String background) {
    this.background = background == null || background.isBlank() ? "opaque" : background.trim();
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
}
