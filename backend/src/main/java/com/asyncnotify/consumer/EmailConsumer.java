package com.asyncnotify.consumer;
import com.asyncnotify.repository.DlqRepository;
import com.asyncnotify.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
@Component
public class EmailConsumer {
  // FIX: Explicit logger - NO @Slf4j, so no "cannot find symbol log"
  private static final Logger log = LoggerFactory.getLogger(EmailConsumer.class);
  private final NotificationLogRepository logRepo;
  private final DlqRepository dlqRepo;
  // Explicit constructor - NO @RequiredArgsConstructor
  public EmailConsumer(NotificationLogRepository logRepo, DlqRepository dlqRepo){
    this.logRepo = logRepo; this.dlqRepo = dlqRepo;
  }
  @RabbitListener(queues="email.queue")
  public void handle(Map<String,Object> payload){
    String key = (String) payload.getOrDefault("idempotencyKey","unknown");
    try{
      if(ThreadLocalRandom.current().nextInt(10) < 2) throw new RuntimeException("SMTP timeout - transient");
      log.info("EMAIL SENT to {} key={}", payload.get("userId"), key);
      logRepo.findByIdempotencyKey(key).ifPresent(l->{ l.setStatus("SENT"); l.setAttempts(l.getAttempts()+1); logRepo.save(l); });
    }catch(Exception e){
      throw new AmqpRejectAndDontRequeueException("retry", e);
    }
  }
}
