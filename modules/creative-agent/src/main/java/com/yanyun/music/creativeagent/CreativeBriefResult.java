package com.yanyun.music.creativeagent;

import java.util.List;

public record CreativeBriefResult(
    CreativeDomainDecision domainDecision,
    String userIntentSummary,
    String theme,
    List<String> moodTags,
    String narrativeViewpoint,
    String musicDirection,
    List<String> yanyunReferences,
    List<String> constraints,
    List<String> riskNotes,
    String userFacingMessage,
    String yanyunRewriteSuggestion,
    List<String> freeformOpportunities,
    String creativeCore,
    String chosenAngle,
    List<String> alternativeAngles,
    String antiClicheStrategy,
    String voiceTexture,
    List<String> imagePool,
    String songEnergy,
    String songThesis,
    String pov,
    String centralTension,
    String emotionalTurn,
    String chorusFunction,
    String memoryDevice,
    String songCore,
    String singerVoice,
    String listenerTarget,
    String emotionalEngine,
    String chorusJob,
    String avoidDirection) {

  public CreativeBriefResult(
      CreativeDomainDecision domainDecision,
      String userIntentSummary,
      String theme,
      List<String> moodTags,
      String narrativeViewpoint,
      String musicDirection,
      List<String> yanyunReferences,
      List<String> constraints,
      List<String> riskNotes,
      String userFacingMessage,
      String yanyunRewriteSuggestion,
      List<String> freeformOpportunities,
      String creativeCore,
      String chosenAngle,
      List<String> alternativeAngles,
      String antiClicheStrategy,
      String voiceTexture,
      List<String> imagePool,
      String songEnergy,
      String songThesis,
      String pov,
      String centralTension,
      String emotionalTurn,
      String chorusFunction,
      String memoryDevice) {
    this(
        domainDecision,
        userIntentSummary,
        theme,
        moodTags,
        narrativeViewpoint,
        musicDirection,
        yanyunReferences,
        constraints,
        riskNotes,
        userFacingMessage,
        yanyunRewriteSuggestion,
        freeformOpportunities,
        creativeCore,
        chosenAngle,
        alternativeAngles,
        antiClicheStrategy,
        voiceTexture,
        imagePool,
        songEnergy,
        songThesis,
        pov,
        centralTension,
        emotionalTurn,
        chorusFunction,
        memoryDevice,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public CreativeBriefResult(
      CreativeDomainDecision domainDecision,
      String userIntentSummary,
      String theme,
      List<String> moodTags,
      String narrativeViewpoint,
      String musicDirection,
      List<String> yanyunReferences,
      List<String> constraints,
      List<String> riskNotes,
      String userFacingMessage,
      String yanyunRewriteSuggestion,
      List<String> freeformOpportunities,
      String creativeCore,
      String chosenAngle,
      List<String> alternativeAngles,
      String antiClicheStrategy,
      String voiceTexture,
      List<String> imagePool,
      String songEnergy) {
    this(
        domainDecision,
        userIntentSummary,
        theme,
        moodTags,
        narrativeViewpoint,
        musicDirection,
        yanyunReferences,
        constraints,
        riskNotes,
        userFacingMessage,
        yanyunRewriteSuggestion,
        freeformOpportunities,
        creativeCore,
        chosenAngle,
        alternativeAngles,
        antiClicheStrategy,
        voiceTexture,
        imagePool,
        songEnergy,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public CreativeBriefResult(
      CreativeDomainDecision domainDecision,
      String userIntentSummary,
      String theme,
      List<String> moodTags,
      String narrativeViewpoint,
      String musicDirection,
      List<String> yanyunReferences,
      List<String> constraints,
      List<String> riskNotes,
      String userFacingMessage,
      String yanyunRewriteSuggestion,
      List<String> freeformOpportunities) {
    this(
        domainDecision,
        userIntentSummary,
        theme,
        moodTags,
        narrativeViewpoint,
        musicDirection,
        yanyunReferences,
        constraints,
        riskNotes,
        userFacingMessage,
        yanyunRewriteSuggestion,
        freeformOpportunities,
        null,
        null,
        List.of(),
        null,
        null,
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public CreativeBriefResult(
      String userIntentSummary,
      String theme,
      List<String> moodTags,
      String narrativeViewpoint,
      String musicDirection,
      List<String> yanyunReferences,
      List<String> constraints,
      List<String> riskNotes) {
    this(
        CreativeDomainDecision.PASS,
        userIntentSummary,
        theme,
        moodTags,
        narrativeViewpoint,
        musicDirection,
        yanyunReferences,
        constraints,
        riskNotes,
        null,
        null,
        List.of(),
        null,
        null,
        List.of(),
        null,
        null,
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public CreativeBriefResult {
    domainDecision = domainDecision == null ? CreativeDomainDecision.PASS : domainDecision;
    userIntentSummary = firstNonBlank(userIntentSummary, "Yanyun creative brief.");
    theme = firstNonBlank(theme, "Yanyun memory");
    moodTags =
        moodTags == null || moodTags.isEmpty()
            ? List.of("cinematic", "warm")
            : List.copyOf(moodTags);
    narrativeViewpoint = firstNonBlank(narrativeViewpoint, "player-facing");
    musicDirection = firstNonBlank(musicDirection, "ancient chinese folk pop");
    yanyunReferences =
        yanyunReferences == null || yanyunReferences.isEmpty()
            ? List.of()
            : List.copyOf(yanyunReferences);
    constraints =
        constraints == null || constraints.isEmpty()
            ? List.of("keep lyrics singable", "avoid unsupported real-person claims")
            : List.copyOf(constraints);
    riskNotes = riskNotes == null ? List.of() : List.copyOf(riskNotes);
    userFacingMessage = blankToNull(userFacingMessage);
    yanyunRewriteSuggestion = blankToNull(yanyunRewriteSuggestion);
    freeformOpportunities =
        freeformOpportunities == null ? List.of() : List.copyOf(freeformOpportunities);
    creativeCore = firstNonBlank(creativeCore, theme);
    chosenAngle = firstNonBlank(chosenAngle, narrativeViewpoint);
    alternativeAngles =
        alternativeAngles == null || alternativeAngles.isEmpty()
            ? List.of()
            : List.copyOf(alternativeAngles);
    antiClicheStrategy =
        firstNonBlank(antiClicheStrategy, "avoid generic wuxia and first-response cliches");
    voiceTexture = firstNonBlank(voiceTexture, narrativeViewpoint);
    imagePool = imagePool == null || imagePool.isEmpty() ? List.of() : List.copyOf(imagePool);
    songEnergy = firstNonBlank(songEnergy, String.join(", ", moodTags));
    songThesis = firstNonBlank(songThesis, creativeCore);
    pov = firstNonBlank(pov, narrativeViewpoint);
    centralTension = firstNonBlank(centralTension, theme);
    emotionalTurn = firstNonBlank(emotionalTurn, "from first feeling to a changed meaning");
    chorusFunction = firstNonBlank(chorusFunction, "carry the song's main emotional argument");
    memoryDevice = firstNonBlank(memoryDevice, "repeatable phrase, image loop, or rhythm hook");
    songCore = firstNonBlank(songCore, songThesis);
    singerVoice = firstNonBlank(singerVoice, pov);
    listenerTarget = firstNonBlank(listenerTarget, "the listener implied by the user input");
    emotionalEngine = firstNonBlank(emotionalEngine, centralTension);
    chorusJob = firstNonBlank(chorusJob, chorusFunction);
    avoidDirection = firstNonBlank(avoidDirection, antiClicheStrategy);
  }

  private static String firstNonBlank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
