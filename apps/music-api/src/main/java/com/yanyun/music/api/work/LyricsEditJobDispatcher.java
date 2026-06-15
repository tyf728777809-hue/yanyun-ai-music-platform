package com.yanyun.music.api.work;

import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.LyricsEditJobRow;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
    prefix = "yanyun.lyrics-edit",
    name = "dispatcher-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class LyricsEditJobDispatcher {

  private static final Logger log = LoggerFactory.getLogger(LyricsEditJobDispatcher.class);

  private final WorkRepository workRepository;
  private final LyricsEditJobProcessor processor;
  private final int batchSize;
  private final Duration lockTimeout;
  private final String workerId;

  public LyricsEditJobDispatcher(
      WorkRepository workRepository,
      LyricsEditJobProcessor processor,
      @Value("${yanyun.lyrics-edit.batch-size:2}") int batchSize,
      @Value("${yanyun.lyrics-edit.lock-timeout:10m}") Duration lockTimeout) {
    this.workRepository = workRepository;
    this.processor = processor;
    this.batchSize = Math.max(1, batchSize);
    this.lockTimeout = lockTimeout == null ? Duration.ofMinutes(10) : lockTimeout;
    this.workerId = workerId();
  }

  @Scheduled(fixedDelayString = "${yanyun.lyrics-edit.poll-interval-ms:2000}")
  public void scheduledDrain() {
    drainOnce();
  }

  public int drainOnce() {
    List<LyricsEditJobRow> jobs =
        workRepository.claimDueLyricsEditJobs(batchSize, workerId, lockTimeout);
    for (LyricsEditJobRow job : jobs) {
      try {
        processor.process(job);
      } catch (RuntimeException exception) {
        log.warn("Unexpected lyrics edit dispatcher failure. jobId={}", job.id(), exception);
        workRepository.markLyricsEditJobFailed(
            job.id(), "LYRICS_EDIT_DISPATCH_FAILED", "AI 改词暂时失败，原歌词已保留。", true);
      }
    }
    return jobs.size();
  }

  private String workerId() {
    try {
      return InetAddress.getLocalHost().getHostName() + "-lyrics-edit-" + UUID.randomUUID();
    } catch (UnknownHostException exception) {
      return "lyrics-edit-" + UUID.randomUUID();
    }
  }
}
