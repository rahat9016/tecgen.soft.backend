package com.tecgensoft.turf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TurfApplication {
  public static void main(String[] args) {
    SpringApplication.run(TurfApplication.class, args);
  }
}
