package com.yanyun.music.production.lyrics;

import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.LyricsEditJobRow;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
    prefix = "yanyun.lyrics-edit",
    name = "dispatcher-enabled",
    havingValue = "true",
    matchIfMissing = false)
public class LyricsEditJobDispatcher implements DisposableBean {

  private static final Logger log = LoggerFactory.getLogger(LyricsEditJobDispatcher.class);

  private final WorkRepository workRepository;
  private final LyricsEditJobProcessor processor;
  private final int batchSize;
  private final Duration lockTimeout;
  private final String workerId;
  private final ThreadPoolExecutor executor;

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
    this.executor =
        new ThreadPoolExecutor(
            this.batchSize,
            this.batchSize,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(this.batchSize),
            threadFactory(this.workerId),
            new ThreadPoolExecutor.AbortPolicy());
  }

  @Scheduled(fixedDelayString = "${yanyun.lyrics-edit.poll-interval-ms:2000}")
  public void scheduledDrain() {
    drainOnce();
  }

  public int drainOnce() {
    int capacity = availableCapacity();
    if (capacity <= 0) {
      return 0;
    }
    List<LyricsEditJobRow> jobs =
        workRepository.claimDueLyricsEditJobs(capacity, workerId, lockTimeout);
    for (LyricsEditJobRow job : jobs) {
      executor.execute(() -> processJob(job));
    }
    return jobs.size();
  }

  private int availableCapacity() {
    return batchSize - executor.getActiveCount() - executor.getQueue().size();
  }

  private void processJob(LyricsEditJobRow job) {
    try {
      processor.process(job);
    } catch (RuntimeException exception) {
      log.warn("Unexpected lyrics edit dispatcher failure. jobId={}", job.id(), exception);
      workRepository.markLyricsEditJobFailed(
          job.id(), "LYRICS_EDIT_DISPATCH_FAILED", "AI 改词暂时失败，原歌词已保留。", true);
    }
  }

  @Override
  public void destroy() {
    executor.shutdownNow();
  }

  private String workerId() {
    try {
      return InetAddress.getLocalHost().getHostName() + "-lyrics-edit-" + UUID.randomUUID();
    } catch (UnknownHostException exception) {
      return "lyrics-edit-" + UUID.randomUUID();
    }
  }

  private static ThreadFactory threadFactory(String workerId) {
    AtomicInteger counter = new AtomicInteger();
    return runnable -> {
      Thread thread = new Thread(runnable);
      thread.setName(workerId + "-" + counter.incrementAndGet());
      thread.setDaemon(true);
      return thread;
    };
  }
}
