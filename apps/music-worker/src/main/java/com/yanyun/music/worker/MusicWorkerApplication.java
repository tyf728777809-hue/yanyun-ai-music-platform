package com.yanyun.music.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.yanyun.music")
public class MusicWorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(MusicWorkerApplication.class, args);
  }
}
