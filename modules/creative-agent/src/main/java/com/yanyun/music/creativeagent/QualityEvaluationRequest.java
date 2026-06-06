package com.yanyun.music.creativeagent;

import java.util.Map;

public record QualityEvaluationRequest(
    String workId,
    QualityGate gate,
    String songTitle,
    String lyricsText,
    String musicProvider,
    String audioObjectKey,
    Long audioDurationMs,
    String coverObjectKey,
    Integer coverWidth,
    Integer coverHeight,
    String videoObjectKey,
    Integer videoWidth,
    Integer videoHeight,
    Long videoDurationMs,
    String timelineObjectKey,
    Map<String, Object> context) {

  public QualityEvaluationRequest {
    gate = gate == null ? QualityGate.PUBLISH_PACKAGE : gate;
    context = context == null ? Map.of() : Map.copyOf(context);
  }
}
