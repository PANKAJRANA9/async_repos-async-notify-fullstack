package com.asyncnotify.service;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;
@Service @RequiredArgsConstructor
public class NotificationProducer {
  private final RabbitTemplate tpl;
  public void publish(String channel, String idempotencyKey, Map<String,Object> payload){
    String routingKey = "notify."+channel.toLowerCase();
    tpl.convertAndSend("notify.exchange", routingKey, payload, m -> {
      m.getMessageProperties().setHeader("idempotencyKey", idempotencyKey);
      m.getMessageProperties().setHeader("attempt", 0);
      return m;
    });
  }
}
