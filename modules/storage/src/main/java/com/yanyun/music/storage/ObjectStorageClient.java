package com.yanyun.music.storage;

public interface ObjectStorageClient {

  StoredObject putObject(ObjectStoragePutRequest request);
}
