package com.yanyun.music.production;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class RenderWorkerProperties {

  private String mode = "mock";
  private String workingDirectory = "apps/render-worker";
  private String command = "npm";
  private List<String> arguments = new ArrayList<>(List.of("run", "render:job", "--"));
  private Duration timeout = Duration.ofMinutes(10);
  private String compositionId = "LyricVideo16x9";
  private String videoObjectKeyPrefix = "videos";
  private String timelineObjectKeyPrefix = "timelines";

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getWorkingDirectory() {
    return workingDirectory;
  }

  public void setWorkingDirectory(String workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  public List<String> getArguments() {
    return arguments;
  }

  public void setArguments(List<String> arguments) {
    this.arguments = arguments == null ? new ArrayList<>() : new ArrayList<>(arguments);
  }

  public Duration getTimeout() {
    return timeout;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }

  public String getCompositionId() {
    return compositionId;
  }

  public void setCompositionId(String compositionId) {
    this.compositionId = compositionId;
  }

  public String getVideoObjectKeyPrefix() {
    return videoObjectKeyPrefix;
  }

  public void setVideoObjectKeyPrefix(String videoObjectKeyPrefix) {
    this.videoObjectKeyPrefix = videoObjectKeyPrefix;
  }

  public String getTimelineObjectKeyPrefix() {
    return timelineObjectKeyPrefix;
  }

  public void setTimelineObjectKeyPrefix(String timelineObjectKeyPrefix) {
    this.timelineObjectKeyPrefix = timelineObjectKeyPrefix;
  }

  String normalizedMode() {
    return mode == null || mode.isBlank() ? "mock" : mode.trim().toLowerCase(java.util.Locale.ROOT);
  }
}
