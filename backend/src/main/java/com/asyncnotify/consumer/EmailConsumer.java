package com.asyncnotify.consumer;
import com.asyncnotify.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
@Component @RequiredArgsConstructor
public class EmailConsumer {
  private final IdempotencyService idem;
  @RabbitListener(queues="email.queue")
  public void handle(Map<String,Object> payload){
    // Idempotent consumer: even if RabbitMQ redelivers, Redis blocks second send
    // Payload contains idempotencyKey in header - simulated via payload key here
    String key = (String) payload.getOrDefault("idempotencyKey","unknown");
    // In real code: header check + DB unique constraint
    try{
      // Simulate provider call with transient failure
      if(ThreadLocalRandom.current().nextInt(10) < 2) throw new RuntimeException("SMTP timeout - transient");
      System.out.println("EMAIL SENT to "+payload.get("userId")+" key="+key);
    }catch(Exception e){
      // Throw to trigger retry interceptor -> exponential backoff -> DLQ after 5
      long backoff = (long)(Math.pow(2, 1)*1000 + ThreadLocalRandom.current().nextInt(0,1000));
      System.out.println("Retry backoff "+backoff+"ms for "+key);
      throw new AmqpRejectAndDontRequeueException("retry", e);
    }
  }
}
