package com.yanyun.music.worker;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TemporalWorkerProperties.class)
public class TemporalWorkerConfig {}
