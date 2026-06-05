package com.yanyun.music.api.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanyun.music.workflow.SongProductionWorkflowInput;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class WorkflowOutboxService {

  public static final String AGGREGATE_TYPE_WORK = "WORK";
  public static final String EVENT_TYPE_SONG_PRODUCTION_REQUESTED = "SONG_PRODUCTION_REQUESTED";

  private final WorkflowOutboxRepository outboxRepository;
  private final WorkflowDispatchProperties properties;
  private final ObjectMapper objectMapper;

  public WorkflowOutboxService(
      WorkflowOutboxRepository outboxRepository,
      WorkflowDispatchProperties properties,
      ObjectMapper objectMapper) {
    this.outboxRepository = outboxRepository;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public UUID enqueueSongProduction(UUID workId, SongProductionWorkflowInput input) {
    return outboxRepository.insert(
        AGGREGATE_TYPE_WORK,
        workId,
        EVENT_TYPE_SONG_PRODUCTION_REQUESTED,
        writeJson(input),
        properties.getOutbox().getMaxAttempts());
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to write workflow outbox payload", exception);
    }
  }
}
