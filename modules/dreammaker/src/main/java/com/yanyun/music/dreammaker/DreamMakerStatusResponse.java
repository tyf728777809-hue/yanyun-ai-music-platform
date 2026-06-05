package com.yanyun.music.dreammaker;

import java.util.List;

public record DreamMakerStatusResponse(
    int code,
    String message,
    DreamMakerTaskStatus status,
    String providerStatus,
    List<DreamMakerOutputFile> outputFiles) {

  public DreamMakerStatusResponse {
    status = status == null ? DreamMakerTaskStatus.FAILED : status;
    outputFiles = outputFiles == null ? List.of() : List.copyOf(outputFiles);
  }

  public boolean successfulCode() {
    return code == 0;
  }
}
