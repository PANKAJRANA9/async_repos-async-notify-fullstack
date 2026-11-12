package com.asyncnotify;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class AsyncNotifyApplication {
  public static void main(String[] args){ SpringApplication.run(AsyncNotifyApplication.class, args); }
}
