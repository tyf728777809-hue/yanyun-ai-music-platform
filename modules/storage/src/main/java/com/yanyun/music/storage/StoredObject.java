package com.yanyun.music.storage;

public record StoredObject(String objectKey, String url, String contentType, long sizeBytes) {}
