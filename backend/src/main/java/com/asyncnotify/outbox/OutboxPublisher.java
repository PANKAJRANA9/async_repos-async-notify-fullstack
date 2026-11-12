package com.asyncnotify.outbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Component
public class OutboxPublisher {
  private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
  private final OutboxRepository repo;
  private final RabbitTemplate tpl;
  private final ObjectMapper om = new ObjectMapper();
  // Explicit constructor - NO @RequiredArgsConstructor, NO Lombok
  public OutboxPublisher(OutboxRepository repo, RabbitTemplate tpl){
    this.repo = repo; this.tpl = tpl;
  }
  @Transactional
  public void saveAndPublish(String aggregateId, String eventType, Map<String,Object> payload){
    OutboxEvent e = new OutboxEvent();
    e.setAggregateId(aggregateId);
    e.setEventType(eventType);
    try{ e.setPayload(om.writeValueAsString(payload)); }catch(Exception ex){ e.setPayload(payload.toString()); }
    e.setStatus("PENDING");
    repo.save(e);
    log.info("Outbox saved aggregateId={} eventType={}", aggregateId, eventType);
  }
  @Scheduled(fixedDelay=2000)
  @Transactional
  public void publishPending(){
    List<OutboxEvent> pending = repo.findTop100ByStatusOrderByCreatedAtAsc("PENDING");
    for(OutboxEvent e: pending){
      try{
        Map<String,Object> payload = om.readValue(e.getPayload(), Map.class);
        String channel = (String) payload.getOrDefault("channel","EMAIL");
        tpl.convertAndSend("notify.exchange","notify."+channel.toLowerCase(), payload);
        e.setStatus("PUBLISHED");
        e.setPublishedAt(LocalDateTime.now());
        repo.save(e);
      }catch(Exception ex){
        log.warn("Outbox publish failed for {}", e.getId(), ex);
        e.setRetryCount(e.getRetryCount()+1);
        if(e.getRetryCount()>5) e.setStatus("FAILED");
        repo.save(e);
      }
    }
  }
}
