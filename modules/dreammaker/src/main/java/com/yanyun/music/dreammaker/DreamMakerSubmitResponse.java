package com.yanyun.music.dreammaker;

public record DreamMakerSubmitResponse(int code, String message, String taskId) {

  public boolean accepted() {
    return code == 0 && taskId != null && !taskId.isBlank();
  }
}
