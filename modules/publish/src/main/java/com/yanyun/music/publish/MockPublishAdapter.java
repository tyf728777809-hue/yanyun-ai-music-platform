package com.yanyun.music.publish;

import java.time.OffsetDateTime;

public final class MockPublishAdapter implements PublishAdapter {

  @Override
  public PublishHandoff preparePackage(String workId) {
    OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(24);
    String packageUrl = "http://localhost:9000/yanyun-works-local/packages/" + workId + ".json";
    String packageObjectKey = "packages/" + workId + ".json";
    return new PublishHandoff(packageObjectKey, packageUrl, expiresAt);
  }

  @Override
  public PublishHandoff refreshPackageUrl(String workId) {
    return preparePackage(workId);
  }
}
