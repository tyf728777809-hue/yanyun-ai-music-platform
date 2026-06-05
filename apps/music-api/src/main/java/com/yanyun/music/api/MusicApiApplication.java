package com.yanyun.music.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.yanyun.music")
@EnableScheduling
public class MusicApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(MusicApiApplication.class, args);
  }
}
