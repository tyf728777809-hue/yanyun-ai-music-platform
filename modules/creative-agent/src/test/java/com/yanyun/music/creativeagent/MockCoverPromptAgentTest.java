package com.yanyun.music.creativeagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunStatus;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockCoverPromptAgentTest {

  @Test
  void generatesVisualPromptAndRecordsAgentRun() {
    List<AgentRunRecord> records = new ArrayList<>();
    MockCoverPromptAgent agent = new MockCoverPromptAgent(records::add);

    CoverPromptResult result =
        agent.generate(
            new CoverPromptRequest(
                "work-1",
                "雁门归人",
                "frontier reunion",
                "lyrics",
                "国风摇滚",
                "moonlit frontier",
                1920,
                1080));

    assertTrue(result.visualPrompt().contains("雁门归人"));
    assertTrue(result.visualPrompt().contains("moonlit frontier"));
    assertEquals(1920, result.width());
    assertEquals(1080, result.height());
    assertEquals("CoverPromptAgent", result.providerOptions().get("agent"));

    assertEquals(1, records.size());
    AgentRunRecord record = records.getFirst();
    assertEquals("work-1", record.workId());
    assertEquals("CoverPromptAgent", record.agentName());
    assertEquals("v0.1", record.agentVersion());
    assertEquals("COVER_PROMPT", record.operation());
    assertEquals("mock-cover-prompt", record.modelName());
    assertEquals("cover.prompt.v1", record.promptTemplateKey());
    assertEquals(1, record.promptTemplateVersion());
    assertEquals(AgentRunStatus.SUCCEEDED, record.status());
    assertNotNull(record.inputHash());
    assertNotNull(record.outputHash());
    assertTrue(record.latencyMs() >= 0);
  }

  @Test
  void usesSafeDefaults() {
    CoverPromptResult result =
        new MockCoverPromptAgent()
            .generate(new CoverPromptRequest("work-1", null, null, null, null, null, null, null));

    assertTrue(result.visualPrompt().contains("Yanyun AI Music Cover"));
    assertEquals(1920, result.width());
    assertEquals(1080, result.height());
  }
}
