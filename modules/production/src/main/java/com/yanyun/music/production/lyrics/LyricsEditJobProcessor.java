package com.yanyun.music.production.lyrics;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.lyrics.LyricsCreativeDomainException;
import com.yanyun.music.lyrics.LyricsGenerationRequest;
import com.yanyun.music.lyrics.LyricsGenerationResult;
import com.yanyun.music.lyrics.LyricsGenerationService;
import com.yanyun.music.lyrics.LyricsGenerationTransientException;
import com.yanyun.music.lyrics.LyricsOperation;
import com.yanyun.music.lyrics.LyricsQualityException;
import com.yanyun.music.workdomain.FailureCode;
import com.yanyun.music.workdomain.GenerationStage;
import com.yanyun.music.workdomain.WorkStateMachine;
import com.yanyun.music.workdomain.WorkStatus;
import com.yanyun.music.workpersistence.WorkRepository;
import com.yanyun.music.workpersistence.WorkRepository.LyricsDraftRow;
import com.yanyun.music.workpersistence.WorkRepository.LyricsEditJobRow;
import com.yanyun.music.workpersistence.WorkRepository.WorkRow;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LyricsEditJobProcessor {

  private static final Logger log = LoggerFactory.getLogger(LyricsEditJobProcessor.class);

  private final WorkRepository workRepository;
  private final LyricsGenerationService lyricsGenerationService;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transactionTemplate;

  public LyricsEditJobProcessor(
      WorkRepository workRepository,
      LyricsGenerationService lyricsGenerationService,
      ObjectMapper objectMapper,
      PlatformTransactionManager transactionManager) {
    this.workRepository = workRepository;
    this.lyricsGenerationService = lyricsGenerationService;
    this.objectMapper = objectMapper;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public void process(LyricsEditJobRow job) {
    if (initialCreation(job)) {
      processInitialCreation(job);
      return;
    }
    processEdit(job);
  }

  private void processEdit(LyricsEditJobRow job) {
    LyricsDraftRow sourceDraft = preflight(job);
    if (sourceDraft == null) {
      return;
    }
    try {
      LyricsGenerationResult lyrics =
          lyricsGenerationService.generate(
              new LyricsGenerationRequest(
                  job.userId(),
                  job.workId().toString(),
                  operation(job),
                  null,
                  sourceDraft.lyricsText(),
                  job.instruction(),
                  sourceDraft.songTitle(),
                  null,
                  sourceDraft.musicPrompt(),
                  null));
      transactionTemplate.executeWithoutResult(status -> completeSuccess(job, lyrics));
    } catch (LyricsCreativeDomainException exception) {
      fail(job, "LYRICS_CREATIVE_DOMAIN_REJECTED", exception.getMessage(), false);
    } catch (LyricsQualityException exception) {
      fail(job, "LYRICS_QUALITY_FAILED", exception.getMessage(), false);
    } catch (LyricsGenerationTransientException exception) {
      fail(job, "LYRICS_GENERATION_TRANSIENT", exception.getMessage(), true);
    } catch (RuntimeException exception) {
      log.warn("Lyrics edit job failed. jobId={}, workId={}", job.id(), job.workId(), exception);
      fail(job, "LYRICS_GENERATION_FAILED", "AI 改词暂时失败，原歌词已保留。", true);
    }
  }

  private void processInitialCreation(LyricsEditJobRow job) {
    LyricsTaskPayload payload;
    try {
      payload = readPayload(job);
    } catch (RuntimeException exception) {
      fail(job, "LYRICS_GENERATION_FAILED", "创作输入读取失败，请返回重新创作。", false);
      return;
    }
    WorkRow work = findWork(job);
    if (work == null) {
      workRepository.markLyricsEditJobCancelled(job.id(), "WORK_NOT_FOUND", "Work not found");
      return;
    }
    if (work.status() != WorkStatus.LYRICS_GENERATING) {
      workRepository.markLyricsEditJobCancelled(
          job.id(), "WORK_NOT_LYRICS_GENERATING", "Work is no longer generating lyrics");
      return;
    }
    workRepository.markGenerationJobRunning(
        job.id(), job.workId(), GenerationStage.LYRICS_GENERATING);
    try {
      String userInput =
          "CREATE_LYRICS".equals(job.operation()) ? payload.lyricsInput() : payload.storyInput();
      LyricsGenerationResult lyrics =
          lyricsGenerationService.generate(
              new LyricsGenerationRequest(
                  job.userId(),
                  job.workId().toString(),
                  operation(job),
                  userInput,
                  null,
                  null,
                  payload.songTitle(),
                  payload.mood(),
                  payload.musicStyle(),
                  payload.vocalPreference()));
      transactionTemplate.executeWithoutResult(status -> completeInitialCreation(job, lyrics));
    } catch (LyricsCreativeDomainException exception) {
      fail(job, "LYRICS_PRECHECK_FAILED", exception.getMessage(), false);
    } catch (LyricsQualityException exception) {
      fail(job, "LYRICS_QUALITY_FAILED", exception.getMessage(), false);
    } catch (LyricsGenerationTransientException exception) {
      fail(job, "LYRICS_GENERATION_FAILED", exception.getMessage(), true);
    } catch (RuntimeException exception) {
      log.warn(
          "Lyrics creation job failed. jobId={}, workId={}", job.id(), job.workId(), exception);
      fail(job, "LYRICS_GENERATION_FAILED", "AI 写词暂时失败，作品内容已保留。", true);
    }
  }

  private LyricsDraftRow preflight(LyricsEditJobRow job) {
    WorkRow work = findWork(job);
    if (work == null) {
      workRepository.markLyricsEditJobCancelled(job.id(), "WORK_NOT_FOUND", "Work not found");
      return null;
    }
    if (!WorkStateMachine.canEditLyrics(work.status())) {
      workRepository.markLyricsEditJobCancelled(
          job.id(), "WORK_NOT_EDITABLE", "Work is no longer editable");
      return null;
    }
    LyricsDraftRow latest = latestDraft(job);
    if (latest == null || stale(job, latest)) {
      workRepository.markLyricsEditJobCancelled(
          job.id(), "LYRICS_DRAFT_STALE", "Lyrics draft changed before edit completed");
      return null;
    }
    return latest;
  }

  private void completeInitialCreation(LyricsEditJobRow job, LyricsGenerationResult lyrics) {
    WorkRow work = findWork(job);
    if (work == null || work.status() != WorkStatus.LYRICS_GENERATING) {
      workRepository.markLyricsEditJobCancelled(
          job.id(), "WORK_NOT_LYRICS_GENERATING", "Work is no longer generating lyrics");
      return;
    }

    String title = firstNonBlank(lyrics.songTitle(), "Yanyun Lyrics");
    String summary = firstNonBlank(lyrics.songSummary(), "Yanyun lyrics draft.");
    if (!workRepository.markLyricsReadyFromGeneration(work.id(), work.userId(), title, summary)) {
      workRepository.markLyricsEditJobCancelled(
          job.id(), "WORK_VERSION_STALE", "Work changed before lyrics completed");
      return;
    }
    workRepository.insertLyricsDraft(
        new LyricsDraftRow(
            UUID.randomUUID(),
            work.id(),
            workRepository.nextLyricsVersion(work.id()),
            title,
            summary,
            lyrics.lyricsText(),
            lyrics.musicPrompt(),
            writeJson(lyrics.riskNotes()),
            writeJson(lyrics.yanyunReferences()),
            lyrics.coverPromptSeed(),
            safeScore(lyrics.qualityScore()),
            lyrics.knowledgeBaseVersion(),
            writeJson(lyrics.promptTemplateVersions()),
            null));
    workRepository.completeGenerationJob(
        job.id(), "SUCCEEDED", GenerationStage.WAITING_CONFIRM, null, null);
    workRepository.markLyricsEditJobSucceeded(job.id());
  }

  private void completeSuccess(LyricsEditJobRow job, LyricsGenerationResult lyrics) {
    WorkRow work = findWork(job);
    LyricsDraftRow latest = latestDraft(job);
    if (work == null || latest == null || !WorkStateMachine.canEditLyrics(work.status())) {
      workRepository.markLyricsEditJobCancelled(
          job.id(), "WORK_NOT_EDITABLE", "Work is no longer editable");
      return;
    }
    if (stale(job, latest)) {
      workRepository.markLyricsEditJobCancelled(
          job.id(), "LYRICS_DRAFT_STALE", "Lyrics draft changed before edit completed");
      return;
    }

    String title = firstNonBlank(lyrics.songTitle(), work.songTitle());
    String summary = firstNonBlank(lyrics.songSummary(), work.songSummary());
    if (!workRepository.markLyricsReadyIfVersion(
        work.id(), work.userId(), work.version(), title, summary, true)) {
      workRepository.markLyricsEditJobCancelled(
          job.id(), "WORK_VERSION_STALE", "Work changed before edit completed");
      return;
    }
    workRepository.insertLyricsDraft(
        new LyricsDraftRow(
            UUID.randomUUID(),
            work.id(),
            workRepository.nextLyricsVersion(work.id()),
            title,
            summary,
            lyrics.lyricsText(),
            lyrics.musicPrompt(),
            writeJson(lyrics.riskNotes()),
            writeJson(lyrics.yanyunReferences()),
            lyrics.coverPromptSeed(),
            safeScore(lyrics.qualityScore()),
            lyrics.knowledgeBaseVersion(),
            writeJson(lyrics.promptTemplateVersions()),
            null));
    workRepository.insertGenerationJob(
        work.id(),
        "LYRICS_" + job.operation(),
        "SUCCEEDED",
        GenerationStage.WAITING_CONFIRM,
        OffsetDateTime.now(),
        OffsetDateTime.now());
    workRepository.markLyricsEditJobSucceeded(job.id());
  }

  private WorkRow findWork(LyricsEditJobRow job) {
    return workRepository.findWorkForUser(job.workId(), job.userId()).orElse(null);
  }

  private LyricsDraftRow latestDraft(LyricsEditJobRow job) {
    return workRepository.findLatestLyricsDraft(job.workId()).orElse(null);
  }

  private boolean stale(LyricsEditJobRow job, LyricsDraftRow latest) {
    return !job.sourceLyricsDraftId().equals(latest.id())
        || !Integer.valueOf(latest.versionNo()).equals(job.sourceVersionNo());
  }

  private LyricsOperation operation(LyricsEditJobRow job) {
    return switch (job.operation()) {
      case "CREATE_INSPIRATION" -> LyricsOperation.INSPIRATION;
      case "CREATE_LYRICS" -> LyricsOperation.LYRICS;
      case "POLISH" -> LyricsOperation.POLISH;
      case "CONTINUE" -> LyricsOperation.CONTINUE;
      default -> throw new IllegalArgumentException("Unsupported lyrics operation");
    };
  }

  private void fail(
      LyricsEditJobRow job, String failureCode, String failureMessage, boolean retryable) {
    String status =
        workRepository.markLyricsEditJobFailed(job.id(), failureCode, failureMessage, retryable);
    if (initialCreation(job) && "FAILED".equals(status)) {
      FailureCode workFailureCode = workFailureCode(failureCode);
      workRepository.markLyricsGenerationFailed(
          job.workId(), job.userId(), workFailureCode, failureMessage, retryable);
      workRepository.completeGenerationJob(
          job.id(), "FAILED", GenerationStage.FAILED, workFailureCode, failureMessage);
    }
  }

  private boolean initialCreation(LyricsEditJobRow job) {
    return "CREATE_INSPIRATION".equals(job.operation()) || "CREATE_LYRICS".equals(job.operation());
  }

  private LyricsTaskPayload readPayload(LyricsEditJobRow job) {
    try {
      return objectMapper.readValue(job.requestPayloadJson(), LyricsTaskPayload.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to read lyrics task payload", exception);
    }
  }

  private FailureCode workFailureCode(String failureCode) {
    try {
      return FailureCode.valueOf(failureCode);
    } catch (RuntimeException exception) {
      return FailureCode.LYRICS_GENERATION_FAILED;
    }
  }

  private BigDecimal safeScore(BigDecimal score) {
    return score == null ? BigDecimal.ZERO : score;
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value == null ? List.of() : value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to write lyrics edit JSON payload", exception);
    }
  }

  private String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record LyricsTaskPayload(
      @JsonProperty("story_input") String storyInput,
      @JsonProperty("lyrics_input") String lyricsInput,
      String mood,
      @JsonProperty("music_style") String musicStyle,
      @JsonProperty("vocal_preference") String vocalPreference,
      @JsonProperty("song_title") String songTitle) {}
}
