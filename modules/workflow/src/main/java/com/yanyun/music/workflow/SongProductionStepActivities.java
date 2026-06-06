package com.yanyun.music.workflow;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface SongProductionStepActivities {

  @ActivityMethod
  SongProductionStepResult lockQuota(SongProductionActivityContext context);

  @ActivityMethod
  SongProductionStepResult generateMusicPrompt(SongProductionActivityContext context);

  @ActivityMethod
  SongProductionStepResult preCheckMusicPrompt(SongProductionActivityContext context);

  @ActivityMethod
  SongProductionStepResult submitMusic(SongProductionActivityContext context);

  @ActivityMethod
  SongProductionStepResult pollMusic(SongProductionActivityContext context);

  @ActivityMethod
  SongProductionStepResult importAudio(SongProductionActivityContext context);

  @ActivityMethod
  SongProductionStepResult generateCoverPrompt(SongProductionActivityContext context);

  @ActivityMethod
  SongProductionStepResult generateCover(SongProductionActivityContext context);

  @ActivityMethod
  SongProductionStepResult renderVideo(SongProductionActivityContext context);

  @ActivityMethod
  SongProductionStepResult evaluatePackage(SongProductionActivityContext context);

  @ActivityMethod
  SongProductionStepResult preCheckPublishPackage(SongProductionActivityContext context);

  @ActivityMethod
  SongProductionStepResult assemblePublishPackage(SongProductionActivityContext context);

  @ActivityMethod
  SongProductionStepResult commitQuota(SongProductionActivityContext context);

  @ActivityMethod
  SongProductionStepResult releaseQuota(SongProductionActivityContext context);
}
