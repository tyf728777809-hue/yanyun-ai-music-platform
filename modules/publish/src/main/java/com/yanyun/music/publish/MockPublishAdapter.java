package com.yanyun.music.publish;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class MockPublishAdapter implements PublishAdapter {

  private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy");
  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MM");
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd");

  private final String environment;

  public MockPublishAdapter() {
    this("local");
  }

  public MockPublishAdapter(String environment) {
    this.environment = environment == null || environment.isBlank() ? "local" : environment.trim();
  }

  @Override
  public PublishHandoff preparePackage(String workId) {
    OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(24);
    String packageObjectKey = packageObjectKey(workId, LocalDate.now());
    String packageUrl = "";
    return new PublishHandoff(packageObjectKey, packageUrl, expiresAt);
  }

  @Override
  public PublishHandoff refreshPackageUrl(String workId) {
    return preparePackage(workId);
  }

  private String packageObjectKey(String workId, LocalDate date) {
    return "yanyun-ai-music/"
        + environment
        + "/"
        + YEAR.format(date)
        + "/"
        + MONTH.format(date)
        + "/"
        + DAY.format(date)
        + "/"
        + workId
        + "/package/publish-package.json";
  }
}
