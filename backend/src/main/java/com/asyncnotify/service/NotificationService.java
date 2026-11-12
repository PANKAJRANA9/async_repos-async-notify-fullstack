package com.asyncnotify.service;
import com.asyncnotify.model.NotificationLog;
import com.asyncnotify.repository.NotificationLogRepository;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class NotificationService {
  private final NotificationLogRepository repo;
  private final IdempotencyService idem;
  public NotificationService(NotificationLogRepository repo, IdempotencyService idem){
    this.repo = repo; this.idem = idem;
  }
  public List<NotificationLog> all(){ return repo.findAll(); }
  public NotificationLog save(NotificationLog l){ return repo.save(l); }
  public Optional<NotificationLog> findByKey(String key){ return repo.findByIdempotencyKey(key); }
  public long countByStatus(String s){ return repo.findAll().stream().filter(x->s.equals(x.getStatus())).count(); }
  public Map<String,Object> stats(){
    long sent = countByStatus("SENT");
    long queued = countByStatus("QUEUED");
    long failed = countByStatus("FAILED");
    long retry = countByStatus("FAILED_RETRYING");
    long dlq = countByStatus("DLQ");
    long total = repo.count();
    long dedup = dedupCount();
    double successRate = total==0 ? 0 : (double)sent/total*100;
    Map<String,Object> m = new HashMap<>();
    m.put("sent", sent); m.put("queued", queued); m.put("failed", failed);
    m.put("retry", retry); m.put("dlq", dlq); m.put("dedup", dedup);
    m.put("total", total); m.put("successRate", Math.round(successRate*100)/100.0);
    return m;
  }
  public long dedupCount(){ return idem.dedupCount(); }
}
