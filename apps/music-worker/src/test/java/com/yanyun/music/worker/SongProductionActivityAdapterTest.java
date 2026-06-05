package com.yanyun.music.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanyun.music.workflow.SongProductionWorkflow;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import com.yanyun.music.workflow.SongProductionWorkflowResult;
import org.junit.jupiter.api.Test;

class SongProductionActivityAdapterTest {

  @Test
  void delegatesProductionToSpringWorkflowBean() {
    SongProductionWorkflow delegate = mock(SongProductionWorkflow.class);
    SongProductionWorkflowInput input =
        new SongProductionWorkflowInput(
            "019754b2-78da-7a92-a937-a0e51bcb55d5",
            "local-user",
            "019754b2-78da-7a92-a937-a0e51bcb55d6",
            "Local Night Song",
            "local summary",
            "lyrics",
            "music prompt",
            "female",
            "mock",
            true,
            "019754b2-78da-7a92-a937-a0e51bcb55d7");
    SongProductionWorkflowResult expected =
        SongProductionWorkflowResult.packageReady(input.jobId(), "PACKAGE_READY");
    SongProductionActivityAdapter adapter = new SongProductionActivityAdapter(delegate);

    when(delegate.produce(input)).thenReturn(expected);

    SongProductionWorkflowResult actual = adapter.produce(input);

    assertThat(actual).isSameAs(expected);
    verify(delegate).produce(input);
  }
}
