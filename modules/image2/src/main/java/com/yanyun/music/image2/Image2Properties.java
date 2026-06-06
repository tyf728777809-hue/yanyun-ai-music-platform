package com.yanyun.music.image2;

import java.time.Duration;

public class Image2Properties {

  private boolean realCallsEnabled;
  private String appName = "gpt-image-2";
  private String subAppName = "gpt-image-2";
  private String modelName = "gpt-image-2";
  private String size = "2048x1152";
  private String quality = "medium";
  private String outputFormat = "png";
  private String background = "opaque";
  private int maxPollAttempts = 60;
  private Duration pollInterval = Duration.ofSeconds(2);

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
        outputFormat == null || outputFormat.isBlank() ? "png" : outputFormat.trim();
  }

  public String getBackground() {
    return background;
  }

  public void setBackground(String background) {
    this.background = background == null || background.isBlank() ? "opaque" : background.trim();
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
