package com.asyncnotify.service;
// NO LOMBOK - Explicit constructor, so no "variable not initialized" error on any JDK
import com.asyncnotify.model.DlqMessage;
import com.asyncnotify.repository.DlqRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
@Service
public class DlqService {
  private final DlqRepository repo;
  private final NotificationProducer producer;
  private final IdempotencyService idem;

  // Explicit constructor - Spring injects all 3 - NO Lombok needed
  public DlqService(DlqRepository repo, NotificationProducer producer, IdempotencyService idem){
    this.repo = repo;
    this.producer = producer;
    this.idem = idem;
  }

  public List<DlqMessage> allDlq(){ return repo.findAll(); }
  public List<DlqMessage> all(){ return repo.findAll(); }

  public DlqMessage saveToDlq(String key, String payload, String error){
    DlqMessage m = new DlqMessage();
    m.setIdempotencyKey(key);
    m.setPayload(payload);
    m.setError(error);
    return repo.save(m);
  }

  public Map<String,Object> replay(Long id, String replayedBy){
    DlqMessage m = repo.findById(id).orElseThrow(() -> new RuntimeException("DLQ not found " + id));
    idem.releaseForReplay(m.getIdempotencyKey());
    Map<String,Object> payload = new HashMap<>();
    payload.put("idempotencyKey", m.getIdempotencyKey());
    payload.put("payload", m.getPayload());
    producer.publish("EMAIL", m.getIdempotencyKey(), payload);
    m.setRetried(true);
    m.setReplayedAt(LocalDateTime.now());
    m.setReplayedBy(replayedBy == null ? "admin" : replayedBy);
    repo.save(m);
    Map<String,Object> out = new HashMap<>();
    out.put("status", "REPLAYED");
    out.put("id", id);
    out.put("idempotencyKey", m.getIdempotencyKey());
    out.put("replayedAt", m.getReplayedAt().toString());
    out.put("replayedBy", m.getReplayedBy());
    out.put("proof", "Message re-published to EMAIL queue");
    return out;
  }
  public void retry(Long id){ replay(id, "admin"); }
}
