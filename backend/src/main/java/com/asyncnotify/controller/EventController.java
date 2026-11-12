package com.asyncnotify.controller;
import com.asyncnotify.model.NotificationLog;
import com.asyncnotify.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
@RestController
@RequestMapping("/api/v1/events")
public class EventController {
  private final NotificationService service;
  private final IdempotencyService idem;
  private final NotificationProducer producer;
  private final com.asyncnotify.outbox.OutboxPublisher outbox;
  public EventController(NotificationService service, IdempotencyService idem, NotificationProducer producer, com.asyncnotify.outbox.OutboxPublisher outbox){
    this.service = service; this.idem = idem; this.producer = producer; this.outbox = outbox;
  }
  @PostMapping
  public ResponseEntity<Map<String,Object>> create(@RequestBody Map<String,Object> body){
    String key = (String) body.getOrDefault("idempotencyKey", "evt-"+System.currentTimeMillis());
    if(key == null || key.isBlank()) return ResponseEntity.badRequest().body(Map.of("status","ERROR","message","idempotencyKey is required"));
    // GAP: Duplicate must be explicit, not hidden. Frontend checks data.status == DEDUPLICATED
    if(!idem.acquire(key)){
      return ResponseEntity.ok(Map.of(
        "status","DEDUPLICATED",
        "idempotencyKey", key,
        "message","Already processed - spam prevented",
        "dedupCount", idem.dedupCount(),
        "blockedAt", LocalDateTime.now().toString()
      ));
    }
    NotificationLog log = new NotificationLog();
    log.setIdempotencyKey(key);
    log.setChannel((String) body.getOrDefault("channel","EMAIL"));
    log.setUserId((String) body.getOrDefault("userId","user-123"));
    log.setTemplate((String) body.getOrDefault("template","WELCOME"));
    log.setStatus("QUEUED"); log.setAttempts(0); log.setCreatedAt(LocalDateTime.now());
    service.save(log);
    // Outbox for durability
    outbox.saveAndPublish(key, "NOTIFICATION_CREATED", body);
    Map<String,Object> payload = new HashMap<>(body); payload.put("idempotencyKey", key);
    producer.publish(log.getChannel(), key, payload);
    return ResponseEntity.ok(Map.of("status","QUEUED","idempotencyKey",key,"channel",log.getChannel(),"message","Event queued"));
  }
  @GetMapping("/logs") public List<NotificationLog> logs(){ return service.all(); }
  @GetMapping("/health") public Map<String,String> health(){ return Map.of("status","UP"); }
}
