package com.yanyun.music.storage;

public interface ObjectStorageClient {

  StoredObject putObject(ObjectStoragePutRequest request);

  ObjectStorageDownloadUrl createDownloadUrl(String objectKey);
}
