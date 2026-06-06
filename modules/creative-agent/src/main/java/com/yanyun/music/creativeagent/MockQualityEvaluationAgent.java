package com.yanyun.music.creativeagent;

import com.yanyun.music.agentruntime.AgentRunHashing;
import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunRecorder;
import com.yanyun.music.agentruntime.AgentRunStatus;
import com.yanyun.music.agentruntime.NoopAgentRunRecorder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MockQualityEvaluationAgent implements QualityEvaluationAgent {

  private static final String AGENT_NAME = "QualityEvaluationAgent";
  private static final String AGENT_VERSION = "v0.1";
  private static final String MODEL_NAME = "mock-quality-evaluation";
  private static final String TEMPLATE_KEY = "quality.evaluation.v1";
  private static final int TEMPLATE_VERSION = 1;

  private final AgentRunRecorder agentRunRecorder;

  public MockQualityEvaluationAgent() {
    this(NoopAgentRunRecorder.INSTANCE);
  }

  public MockQualityEvaluationAgent(AgentRunRecorder agentRunRecorder) {
    this.agentRunRecorder =
        agentRunRecorder == null ? NoopAgentRunRecorder.INSTANCE : agentRunRecorder;
  }

  @Override
  public QualityEvaluationResult evaluate(QualityEvaluationRequest request) {
    long startedAt = System.nanoTime();
    try {
      QualityEvaluationResult result = evaluateResult(request);
      record(request, result, startedAt, null);
      return result;
    } catch (RuntimeException exception) {
      record(request, null, startedAt, exception);
      throw exception;
    }
  }

  private QualityEvaluationResult evaluateResult(QualityEvaluationRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    List<String> reasons = new ArrayList<>();
    int score = 100;
    if (isBlank(request.lyricsText())) {
      reasons.add("lyrics text is missing");
      score -= 15;
    }
    if (isBlank(request.audioObjectKey())) {
      reasons.add("audio asset is missing");
      score -= 25;
    }
    if (request.audioDurationMs() == null || request.audioDurationMs() <= 0) {
      reasons.add("audio duration is invalid");
      score -= 15;
    }
    if (isBlank(request.coverObjectKey())) {
      reasons.add("cover asset is missing");
      score -= 20;
    }
    if (!isSixteenByNine(request.coverWidth(), request.coverHeight())) {
      reasons.add("cover asset is not 16:9");
      score -= 10;
    }
    if (isBlank(request.videoObjectKey())) {
      reasons.add("video asset is missing");
      score -= 25;
    }
    if (!isSixteenByNine(request.videoWidth(), request.videoHeight())) {
      reasons.add("video asset is not 16:9");
      score -= 10;
    }
    if (request.videoDurationMs() == null || request.videoDurationMs() <= 0) {
      reasons.add("video duration is invalid");
      score -= 10;
    }
    if (isBlank(request.timelineObjectKey())) {
      reasons.add("timeline asset is missing");
      score -= 10;
    }

    QualityDecision decision = reasons.isEmpty() ? QualityDecision.PASS : QualityDecision.RETRY;
    return new QualityEvaluationResult(
        request.gate(),
        decision,
        score,
        reasons,
        decision == QualityDecision.PASS ? "PASS" : "RETRY_PACKAGE_BUILD",
        decision != QualityDecision.PASS,
        Map.of(
            "agent",
            AGENT_NAME,
            "agent_version",
            AGENT_VERSION,
            "gate",
            request.gate().name(),
            "prompt_template_key",
            TEMPLATE_KEY,
            "prompt_template_version",
            TEMPLATE_VERSION));
  }

  private void record(
      QualityEvaluationRequest request,
      QualityEvaluationResult result,
      long startedAt,
      RuntimeException exception) {
    agentRunRecorder.record(
        new AgentRunRecord(
            request == null ? null : request.workId(),
            null,
            AGENT_NAME,
            AGENT_VERSION,
            request == null ? "UNKNOWN" : request.gate().operationName(),
            MODEL_NAME,
            TEMPLATE_KEY,
            TEMPLATE_VERSION,
            request == null ? null : AgentRunHashing.sha256(inputFingerprint(request)),
            result == null ? null : AgentRunHashing.sha256(outputFingerprint(result)),
            exception == null ? AgentRunStatus.SUCCEEDED : AgentRunStatus.FAILED,
            elapsedMs(startedAt),
            null,
            null,
            null,
            exception == null ? null : "QUALITY_EVALUATION_AGENT_FAILED",
            exception == null ? null : exception.getMessage()));
  }

  private String inputFingerprint(QualityEvaluationRequest request) {
    return String.join(
        "\n",
        nullToEmpty(request.workId()),
        request.gate().name(),
        nullToEmpty(request.songTitle()),
        nullToEmpty(request.lyricsText()),
        nullToEmpty(request.musicProvider()),
        nullToEmpty(request.audioObjectKey()),
        request.audioDurationMs() == null ? "" : request.audioDurationMs().toString(),
        nullToEmpty(request.coverObjectKey()),
        request.coverWidth() == null ? "" : request.coverWidth().toString(),
        request.coverHeight() == null ? "" : request.coverHeight().toString(),
        nullToEmpty(request.videoObjectKey()),
        request.videoWidth() == null ? "" : request.videoWidth().toString(),
        request.videoHeight() == null ? "" : request.videoHeight().toString(),
        request.videoDurationMs() == null ? "" : request.videoDurationMs().toString(),
        nullToEmpty(request.timelineObjectKey()),
        request.context().toString());
  }

  private String outputFingerprint(QualityEvaluationResult result) {
    return String.join(
        "\n",
        result.gate().name(),
        result.decision().name(),
        Integer.toString(result.score()),
        result.reasons().toString(),
        nullToEmpty(result.recommendedAction()),
        Boolean.toString(result.retryable()),
        result.metadata().toString());
  }

  private boolean isSixteenByNine(Integer width, Integer height) {
    if (width == null || height == null || width <= 0 || height <= 0) {
      return false;
    }
    double actual = (double) width / (double) height;
    return Math.abs(actual - (16.0 / 9.0)) < 0.01;
  }

  private int elapsedMs(long startedAt) {
    long elapsed = (System.nanoTime() - startedAt) / 1_000_000L;
    return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, elapsed);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
