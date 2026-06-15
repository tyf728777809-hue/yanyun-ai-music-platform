package com.yanyun.music.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.yanyun.music")
@EnableScheduling
public class MusicWorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(MusicWorkerApplication.class, args);
  }
}
