package com.yanyun.music.creativeagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MockQualityEvaluationAgentTest {

  @Test
  void passesCompletePublishPackageAndRecordsAgentRun() {
    List<AgentRunRecord> records = new ArrayList<>();
    MockQualityEvaluationAgent agent = new MockQualityEvaluationAgent(records::add);

    QualityEvaluationResult result = agent.evaluate(validRequest());

    assertEquals(QualityGate.PUBLISH_PACKAGE, result.gate());
    assertEquals(QualityDecision.PASS, result.decision());
    assertEquals(100, result.score());
    assertEquals("PASS", result.recommendedAction());
    assertEquals("QualityEvaluationAgent", result.metadata().get("agent"));

    assertEquals(1, records.size());
    AgentRunRecord record = records.getFirst();
    assertEquals("work-1", record.workId());
    assertEquals("QualityEvaluationAgent", record.agentName());
    assertEquals("v0.1", record.agentVersion());
    assertEquals("PACKAGE_QUALITY_GATE", record.operation());
    assertEquals("mock-quality-evaluation", record.modelName());
    assertEquals("quality.evaluation.v1", record.promptTemplateKey());
    assertEquals(1, record.promptTemplateVersion());
    assertEquals(AgentRunStatus.SUCCEEDED, record.status());
    assertNotNull(record.inputHash());
    assertNotNull(record.outputHash());
    assertTrue(record.latencyMs() >= 0);
  }

  @Test
  void retriesWhenRequiredAssetsAreMissing() {
    QualityEvaluationResult result =
        new MockQualityEvaluationAgent()
            .evaluate(
                new QualityEvaluationRequest(
                    "work-1",
                    QualityGate.PUBLISH_PACKAGE,
                    "title",
                    "",
                    "MOCK",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of()));

    assertEquals(QualityDecision.RETRY, result.decision());
    assertTrue(result.retryable());
    assertEquals("RETRY_PACKAGE_BUILD", result.recommendedAction());
    assertTrue(result.score() < 100);
    assertTrue(result.reasons().contains("audio asset is missing"));
    assertTrue(result.reasons().contains("timeline asset is missing"));
  }

  private QualityEvaluationRequest validRequest() {
    return new QualityEvaluationRequest(
        "work-1",
        QualityGate.PUBLISH_PACKAGE,
        "雁门归人",
        "lyrics",
        "MOCK",
        "audio/work-1.mp3",
        180_000L,
        "covers/work-1.png",
        1920,
        1080,
        "videos/work-1.mp4",
        1920,
        1080,
        180_000L,
        "timelines/work-1.json",
        Map.of("source", "unit-test"));
  }
}
