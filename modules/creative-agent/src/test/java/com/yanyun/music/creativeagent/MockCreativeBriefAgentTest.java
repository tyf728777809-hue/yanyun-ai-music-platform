package com.yanyun.music.creativeagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunStatus;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockCreativeBriefAgentTest {

  @Test
  void generatesBriefAndRecordsAgentRun() {
    List<AgentRunRecord> records = new ArrayList<>();
    MockCreativeBriefAgent agent = new MockCreativeBriefAgent(records::add);

    CreativeBriefResult result =
        agent.generate(
            new CreativeBriefRequest(
                "user-1",
                "work-1",
                "INSPIRATION",
                "边关旧友重逢",
                null,
                null,
                null,
                "国风摇滚",
                "AUTO",
                List.of("雁门关")));

    assertTrue(result.userIntentSummary().contains("边关旧友"));
    assertEquals(List.of("heroic", "driving"), result.moodTags());
    assertEquals(List.of("雁门关"), result.yanyunReferences());

    assertEquals(1, records.size());
    AgentRunRecord record = records.getFirst();
    assertEquals("work-1", record.workId());
    assertEquals("CreativeBriefAgent", record.agentName());
    assertEquals("v0.1", record.agentVersion());
    assertEquals("INSPIRATION", record.operation());
    assertEquals("mock-creative-brief", record.modelName());
    assertEquals("creative.brief.v1", record.promptTemplateKey());
    assertEquals(1, record.promptTemplateVersion());
    assertEquals(AgentRunStatus.SUCCEEDED, record.status());
    assertNotNull(record.inputHash());
    assertNotNull(record.outputHash());
    assertTrue(record.latencyMs() >= 0);
  }

  @Test
  void usesSafeDefaultsWhenReferencesAreEmpty() {
    CreativeBriefResult result =
        new MockCreativeBriefAgent()
            .generate(
                new CreativeBriefRequest(
                    "user-1", "work-1", "LYRICS", "", "旧词", "", null, null, null, List.of()));

    assertEquals("ancient chinese folk pop", result.musicDirection());
    assertEquals(List.of("Mock Yanyun Reference"), result.yanyunReferences());
    assertEquals("user-lyrics-centered", result.narrativeViewpoint());
  }
}
