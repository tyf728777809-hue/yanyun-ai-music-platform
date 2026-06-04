package com.yanyun.music.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class WorkerStartupLogger implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(WorkerStartupLogger.class);

  private final TemporalWorkerProperties properties;

  public WorkerStartupLogger(TemporalWorkerProperties properties) {
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    log.info(
        "music-worker ready. temporalTarget={}, namespace={}, taskQueue={}",
        properties.target(),
        properties.namespace(),
        properties.taskQueue());
  }
}
