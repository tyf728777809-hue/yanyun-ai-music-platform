package com.yanyun.music.worker;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yanyun.temporal")
public record TemporalWorkerProperties(String target, String namespace, String taskQueue) {}
