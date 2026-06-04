package com.yanyun.music.api.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class TestHashSupport {

  private final ObjectMapper objectMapper;

  TestHashSupport(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  String hash(Object value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of()
          .formatHex(
              digest.digest(
                  objectMapper.writeValueAsString(value).getBytes(StandardCharsets.UTF_8)));
    } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
