package com.asyncnotify.controller;
import com.asyncnotify.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/events") @RequiredArgsConstructor
public class EventController {
  private final IdempotencyService idem;
  private final NotificationProducer producer;
  @PostMapping
  public ResponseEntity<Map<String,Object>> fire(@RequestBody Map<String,Object> body){
    String key = (String) body.getOrDefault("idempotencyKey", UUID.randomUUID().toString());
    String channel = (String) body.getOrDefault("channel", "EMAIL");
    if(!idem.acquire(key)){
      return ResponseEntity.ok(Map.of("status","DEDUPLICATED","message","Already processed - spam prevented","idempotencyKey",key));
    }
    producer.publish(channel, key, body);
    return ResponseEntity.accepted().body(Map.of("status","QUEUED","idempotencyKey",key));
  }
  @GetMapping("/health") public Map<String,String> health(){ return Map.of("status","UP"); }
}
