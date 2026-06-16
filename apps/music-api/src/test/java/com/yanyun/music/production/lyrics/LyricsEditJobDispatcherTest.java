package com.yanyun.music.production.lyrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.LyricsEditJobRow;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class LyricsEditJobDispatcherTest {

  @Test
  void drainOnceSubmitsSlowJobsWithoutBlockingSchedulerThread() throws Exception {
    WorkRepository workRepository = mock(WorkRepository.class);
    LyricsEditJobProcessor processor = mock(LyricsEditJobProcessor.class);
    LyricsEditJobRow job = job();
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(workRepository.claimDueLyricsEditJobs(anyInt(), any(), any())).thenReturn(List.of(job));
    org.mockito.Mockito.doAnswer(
            invocation -> {
              started.countDown();
              assertTrue(release.await(5, TimeUnit.SECONDS));
              return null;
            })
        .when(processor)
        .process(job);

    LyricsEditJobDispatcher dispatcher =
        new LyricsEditJobDispatcher(workRepository, processor, 1, Duration.ofMinutes(10));
    try {
      assertTimeoutPreemptively(
          Duration.ofMillis(500), () -> assertEquals(1, dispatcher.drainOnce()));
      assertTrue(started.await(1, TimeUnit.SECONDS));
      assertEquals(0, dispatcher.drainOnce());
      verify(workRepository, times(1)).claimDueLyricsEditJobs(eq(1), any(), any());
    } finally {
      release.countDown();
      dispatcher.destroy();
    }
  }

  private LyricsEditJobRow job() {
    UUID workId = UUID.randomUUID();
    return new LyricsEditJobRow(
        UUID.randomUUID(),
        workId,
        "user-1",
        "CREATE_INSPIRATION",
        null,
        null,
        null,
        "RUNNING",
        null,
        null,
        false,
        1,
        2,
        OffsetDateTime.now(),
        "test",
        OffsetDateTime.now(),
        null,
        OffsetDateTime.now(),
        OffsetDateTime.now(),
        "{\"story_input\":\"清河小路\"}");
  }
}
