package com.yanyun.music.storage;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ObjectStorageClientFactoryTest {

  @Test
  void createsLocalStorageByDefault() {
    ObjectStorageClient client = ObjectStorageClientFactory.create(new ObjectStorageProperties());

    assertInstanceOf(LocalObjectStorageClient.class, client);
  }

  @Test
  void rejectsUnknownProvider() {
    ObjectStorageProperties properties = new ObjectStorageProperties();
    properties.setProvider("unknown");

    assertThrows(
        IllegalArgumentException.class, () -> ObjectStorageClientFactory.create(properties));
  }
}
