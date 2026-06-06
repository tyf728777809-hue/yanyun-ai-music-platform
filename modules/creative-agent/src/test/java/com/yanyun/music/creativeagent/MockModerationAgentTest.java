package com.yanyun.music.creativeagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MockModerationAgentTest {

  @Test
  void passesCleanMusicPromptAndRecordsAgentRun() {
    List<AgentRunRecord> records = new ArrayList<>();
    MockModerationAgent agent = new MockModerationAgent(records::add);

    ModerationAgentResult result =
        agent.preCheck(
            new ModerationAgentRequest(
                "work-1", ModerationTarget.MUSIC_PROMPT, "国风摇滚 cinematic", Map.of()));

    assertTrue(result.allowed());
    assertEquals(ModerationAgentDecision.PASS, result.decision());
    assertEquals("PASS", result.recommendedAction());
    assertEquals("ModerationAgent", result.metadata().get("agent"));

    assertEquals(1, records.size());
    AgentRunRecord record = records.getFirst();
    assertEquals("work-1", record.workId());
    assertEquals("ModerationAgent", record.agentName());
    assertEquals("v0.1", record.agentVersion());
    assertEquals("MUSIC_PROMPT_PRECHECK", record.operation());
    assertEquals("mock-moderation-agent", record.modelName());
    assertEquals("moderation.agent.v1", record.promptTemplateKey());
    assertEquals(1, record.promptTemplateVersion());
    assertEquals(AgentRunStatus.SUCCEEDED, record.status());
    assertNotNull(record.inputHash());
    assertNotNull(record.outputHash());
    assertTrue(record.latencyMs() >= 0);
  }

  @Test
  void blocksMarkerContent() {
    ModerationAgentResult result =
        new MockModerationAgent()
            .preCheck(
                new ModerationAgentRequest(
                    "work-1", ModerationTarget.MUSIC_PROMPT, "music [BLOCK] prompt", Map.of()));

    assertFalse(result.allowed());
    assertEquals(ModerationAgentDecision.BLOCK, result.decision());
    assertEquals("RETURN_TO_EDIT", result.recommendedAction());
    assertEquals(List.of("MOCK_AGENT_BLOCKED"), result.riskCodes());
    assertEquals("Mock moderation agent blocked content.", result.message());
  }
}
