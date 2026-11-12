package com.asyncnotify.controller;
import com.asyncnotify.model.DlqMessage;
import com.asyncnotify.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
  private final NotificationService service;
  private final DlqService dlqService;
  private final IdempotencyService idem;
  public NotificationController(NotificationService service, DlqService dlqService, IdempotencyService idem){
    this.service = service; this.dlqService = dlqService; this.idem = idem;
  }
  @GetMapping("/stats") public Map<String,Object> stats(){ return service.stats(); }
  @GetMapping("/dlq") public List<DlqMessage> dlq(){ return dlqService.allDlq(); }
  @PostMapping("/dlq/{id}/replay") public Map<String,Object> replay(@PathVariable Long id, @RequestParam(required=false) String by){ return dlqService.replay(id, by); }
  @GetMapping("/dedup-count") public Map<String,Object> dedup(){ return Map.of("dedupCount", idem.dedupCount(), "source", "Redis KEYS idem:* - REAL"); }
}
