package com.yanyun.music.creativeagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yanyun.music.agentruntime.AgentRunRecord;
import com.yanyun.music.agentruntime.AgentRunStatus;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockMusicPromptAgentTest {

  @Test
  void generatesProviderAwarePromptAndRecordsAgentRun() {
    List<AgentRunRecord> records = new ArrayList<>();
    MockMusicPromptAgent agent = new MockMusicPromptAgent(records::add);

    MusicPromptResult result =
        agent.generate(
            new MusicPromptRequest(
                "work-1", "雁门归人", "frontier reunion", "lyrics", "国风摇滚, cinematic", "AUTO", "SUNO"));

    assertTrue(result.musicPrompt().contains("国风摇滚"));
    assertTrue(result.musicPrompt().contains("provider profile: suno"));
    assertEquals("SUNO", result.providerOptions().get("provider_profile"));
    assertEquals("MusicPromptAgent", result.providerOptions().get("agent"));

    assertEquals(1, records.size());
    AgentRunRecord record = records.getFirst();
    assertEquals("work-1", record.workId());
    assertEquals("MusicPromptAgent", record.agentName());
    assertEquals("v0.1", record.agentVersion());
    assertEquals("MUSIC_PROMPT", record.operation());
    assertEquals("mock-music-prompt", record.modelName());
    assertEquals("music.prompt.v1", record.promptTemplateKey());
    assertEquals(1, record.promptTemplateVersion());
    assertEquals(AgentRunStatus.SUCCEEDED, record.status());
    assertNotNull(record.inputHash());
    assertNotNull(record.outputHash());
    assertTrue(record.latencyMs() >= 0);
  }

  @Test
  void usesSafeDefaults() {
    MusicPromptResult result =
        new MockMusicPromptAgent()
            .generate(new MusicPromptRequest("work-1", null, null, "", null, null, null));

    assertTrue(result.musicPrompt().contains("ancient chinese folk pop"));
    assertTrue(result.musicPrompt().contains("provider profile: mock"));
    assertTrue(result.musicPrompt().contains("vocal: auto"));
  }
}
