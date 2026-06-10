package com.yanyun.music.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalObjectStorageClientTest {

  @TempDir Path tempDir;

  @Test
  void writesObjectUnderRootAndReturnsPublicUrl() throws Exception {
    LocalObjectStorageClient storage =
        new LocalObjectStorageClient(tempDir, "http://localhost:9000/yanyun-works-local/");

    StoredObject storedObject =
        storage.putObject(
            new ObjectStoragePutRequest(
                "packages/work-1.json",
                "application/json",
                "{\"work_id\":\"work-1\"}".getBytes(StandardCharsets.UTF_8)));

    assertEquals("packages/work-1.json", storedObject.objectKey());
    assertEquals(
        "http://localhost:9000/yanyun-works-local/packages/work-1.json", storedObject.url());
    assertEquals("application/json", storedObject.contentType());
    assertTrue(storedObject.sizeBytes() > 0);
    assertEquals(
        "{\"work_id\":\"work-1\"}", Files.readString(tempDir.resolve("packages/work-1.json")));
  }

  @Test
  void rejectsObjectKeyThatEscapesRoot() {
    LocalObjectStorageClient storage =
        new LocalObjectStorageClient(tempDir, "http://localhost:9000/yanyun-works-local");

    assertThrows(
        IllegalArgumentException.class,
        () ->
            storage.putObject(
                new ObjectStoragePutRequest(
                    "../outside.json", "application/json", "{}".getBytes(StandardCharsets.UTF_8))));
  }

  @Test
  void readsObjectContentBySafeKey() {
    LocalObjectStorageClient storage =
        new LocalObjectStorageClient(tempDir, "http://localhost:9000/yanyun-works-local");
    storage.putObject(
        new ObjectStoragePutRequest(
            "audio/work-1.mp3", "audio/mpeg", "fake-audio".getBytes(StandardCharsets.UTF_8)));

    byte[] content = storage.getObject("audio/work-1.mp3");

    assertEquals("fake-audio", new String(content, StandardCharsets.UTF_8));
  }

  @Test
  void rejectsReadObjectKeyThatEscapesRoot() {
    LocalObjectStorageClient storage =
        new LocalObjectStorageClient(tempDir, "http://localhost:9000/yanyun-works-local");

    assertThrows(IllegalArgumentException.class, () -> storage.getObject("../outside.json"));
  }

  @Test
  void returnsDownloadUrlWithExpiry() {
    LocalObjectStorageClient storage =
        new LocalObjectStorageClient(
            tempDir, "http://localhost:9000/yanyun-works-local", Duration.ofMinutes(15));

    ObjectStorageDownloadUrl url = storage.createDownloadUrl("packages/work-1.json");

    assertEquals("packages/work-1.json", url.objectKey());
    assertEquals("http://localhost:9000/yanyun-works-local/packages/work-1.json", url.url());
    assertTrue(url.expiresAt().isAfter(java.time.OffsetDateTime.now()));
  }
}
