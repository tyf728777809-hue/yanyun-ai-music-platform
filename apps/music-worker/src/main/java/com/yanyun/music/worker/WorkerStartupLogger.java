package com.yanyun.music.worker;

import com.yanyun.music.creativeagent.CoverPromptAgent;
import com.yanyun.music.creativeagent.MusicPromptAgent;
import com.yanyun.music.creativeagent.QualityEvaluationAgent;
import com.yanyun.music.deepseek.DeepSeekProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class WorkerStartupLogger implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(WorkerStartupLogger.class);

  private final TemporalWorkerProperties properties;
  private final DeepSeekProperties deepSeekProperties;
  private final MusicPromptAgent musicPromptAgent;
  private final CoverPromptAgent coverPromptAgent;
  private final QualityEvaluationAgent qualityEvaluationAgent;

  public WorkerStartupLogger(
      TemporalWorkerProperties properties,
      DeepSeekProperties deepSeekProperties,
      MusicPromptAgent musicPromptAgent,
      CoverPromptAgent coverPromptAgent,
      QualityEvaluationAgent qualityEvaluationAgent) {
    this.properties = properties;
    this.deepSeekProperties = deepSeekProperties;
    this.musicPromptAgent = musicPromptAgent;
    this.coverPromptAgent = coverPromptAgent;
    this.qualityEvaluationAgent = qualityEvaluationAgent;
  }

  @Override
  public void run(ApplicationArguments args) {
    log.info(
        "music-worker ready. temporalTarget={}, namespace={}, taskQueue={}",
        properties.target(),
        properties.namespace(),
        properties.taskQueue());
    log.info(
        "music-worker agent config. deepseekRealCallsEnabled={}, deepseekAgentRealCallsEnabled={}, musicPromptAgent={}, coverPromptAgent={}, qualityEvaluationAgent={}",
        deepSeekProperties.isRealCallsEnabled(),
        deepSeekProperties.isAgentRealCallsEnabled(),
        musicPromptAgent.getClass().getSimpleName(),
        coverPromptAgent.getClass().getSimpleName(),
        qualityEvaluationAgent.getClass().getSimpleName());
  }
}
