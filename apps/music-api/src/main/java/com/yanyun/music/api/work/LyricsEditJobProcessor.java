package com.yanyun.music.api.work;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.lyrics.LyricsCreativeDomainException;
import com.yanyun.music.lyrics.LyricsGenerationRequest;
import com.yanyun.music.lyrics.LyricsGenerationResult;
import com.yanyun.music.lyrics.LyricsGenerationService;
import com.yanyun.music.lyrics.LyricsGenerationTransientException;
import com.yanyun.music.lyrics.LyricsOperation;
import com.yanyun.music.lyrics.LyricsQualityException;
import com.yanyun.music.workdomain.GenerationStage;
import com.yanyun.music.workdomain.WorkStateMachine;
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
        || job.sourceVersionNo() != latest.versionNo();
  }

  private LyricsOperation operation(LyricsEditJobRow job) {
    return switch (job.operation()) {
      case "POLISH" -> LyricsOperation.POLISH;
      case "CONTINUE" -> LyricsOperation.CONTINUE;
      default -> throw new IllegalArgumentException("Unsupported lyrics edit operation");
    };
  }

  private void fail(
      LyricsEditJobRow job, String failureCode, String failureMessage, boolean retryable) {
    workRepository.markLyricsEditJobFailed(job.id(), failureCode, failureMessage, retryable);
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
}
