package com.asyncnotify.consumer;
import com.asyncnotify.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
@Component
public class SmsConsumer {
  private static final Logger log = LoggerFactory.getLogger(SmsConsumer.class);
  private final NotificationLogRepository logRepo;
  // FIX: Inject repo - old code had NO repo, so status never updated from QUEUED -> SENT
  public SmsConsumer(NotificationLogRepository logRepo){ this.logRepo = logRepo; }
  @RabbitListener(queues="sms.queue")
  public void handle(Map<String,Object> p){
    String key = (String) p.getOrDefault("idempotencyKey","unknown");
    try{
      if(ThreadLocalRandom.current().nextInt(10) < 2) throw new RuntimeException("SMS Gateway timeout");
      log.info("SMS SENT to {} key={}", p.get("userId"), key);
      // FIX: Update DB to SENT - THIS WAS MISSING
      logRepo.findByIdempotencyKey(key).ifPresent(l -> {
        l.setStatus("SENT");
        l.setAttempts(l.getAttempts()+1);
        logRepo.save(l);
      });
    }catch(Exception e){
      log.warn("SMS failed key={} will retry", key, e);
      throw new AmqpRejectAndDontRequeueException("retry", e);
    }
  }
}
