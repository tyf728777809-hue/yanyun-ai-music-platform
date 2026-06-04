package com.yanyun.music.publish;

import java.time.OffsetDateTime;

public record PublishHandoff(
    String packageObjectKey, String packageUrl, OffsetDateTime expiresAt) {}
