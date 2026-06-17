package com.yanyun.music.creativeagent;

import java.util.List;

public record LyricsCraftPlanResult(
    String songThesisGuard,
    String selectedDevice,
    String selectedAngle,
    String chorusMechanism,
    String yanyunBoundaryGuard,
    List<String> rejectedAlternatives) {

  public LyricsCraftPlanResult {
    songThesisGuard = nullToEmpty(songThesisGuard);
    selectedDevice = nullToEmpty(selectedDevice);
    selectedAngle = nullToEmpty(selectedAngle);
    chorusMechanism = nullToEmpty(chorusMechanism);
    yanyunBoundaryGuard = nullToEmpty(yanyunBoundaryGuard);
    rejectedAlternatives =
        rejectedAlternatives == null ? List.of() : List.copyOf(rejectedAlternatives);
  }

  public boolean usable() {
    return !selectedDevice.isBlank() || !selectedAngle.isBlank() || !chorusMechanism.isBlank();
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value.trim();
  }
}
