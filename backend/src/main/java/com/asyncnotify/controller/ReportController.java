package com.asyncnotify.controller;
import com.asyncnotify.model.NotificationLog;
import com.asyncnotify.service.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping({"/api/v1/report", "/api/v1/reports"})
public class ReportController {
  private final NotificationService service;
  private final DlqService dlqService;
  public ReportController(NotificationService service, DlqService dlqService){ this.service = service; this.dlqService = dlqService; }

  // GAP FIX: Export must work when opened in browser tab too.
  // Browser sends Accept: text/html, but we force produces=text/csv + Content-Disposition: attachment
  // Nginx must NOT fallback to index.html for /api/ - see nginx.conf fix
  @GetMapping(value="/export", produces="text/csv")
  public ResponseEntity<String> exportCsv(){
    StringBuilder sb = new StringBuilder();
    sb.append("id,idempotencyKey,channel,userId,template,status,attempts,createdAt\n");
    for(NotificationLog l: service.all()){
      sb.append(l.getId()).append(",")
        .append(l.getIdempotencyKey()).append(",")
        .append(l.getChannel()).append(",")
        .append(l.getUserId()).append(",")
        .append(l.getTemplate()).append(",")
        .append(l.getStatus()).append(",")
        .append(l.getAttempts()).append(",")
        .append(l.getCreatedAt()).append("\n");
    }
    HttpHeaders h = new HttpHeaders();
    // FIX: attachment forces download, browser will NOT render as HTML page
    h.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=async-notify-report.csv");
    h.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
    h.setCacheControl("no-cache");
    return new ResponseEntity<>(sb.toString(), h, HttpStatus.OK);
  }
  @GetMapping("/stats") public Map<String,Object> stats(){ return service.stats(); }
  @GetMapping("/summary") public Map<String,Object> summary(){ Map<String,Object> s = service.stats(); s.put("generatedAt", new Date().toString()); return s; }
  @GetMapping("/dlq") public List<?> dlq(){ return dlqService.all(); }
  @PostMapping("/dlq/{id}/retry") public Map<String,String> retry(@PathVariable Long id){ dlqService.retry(id); return Map.of("status","RETRY_QUEUED","id",String.valueOf(id)); }
}
