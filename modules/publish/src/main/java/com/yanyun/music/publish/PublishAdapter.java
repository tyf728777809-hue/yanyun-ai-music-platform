package com.yanyun.music.publish;

public interface PublishAdapter {

  PublishHandoff preparePackage(String workId);

  PublishHandoff refreshPackageUrl(String workId);
}
