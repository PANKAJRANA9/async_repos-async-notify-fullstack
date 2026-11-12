package com.asyncnotify.service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;
@Service
public class NotificationProducer {
  private final RabbitTemplate tpl;
  public NotificationProducer(RabbitTemplate tpl){ this.tpl = tpl; }
  public void publish(String channel, String idempotencyKey, Map<String,Object> payload){
    String rk = "notify." + channel.toLowerCase();
    tpl.convertAndSend("notify.exchange", rk, payload, m -> {
      m.getMessageProperties().setHeader("idempotencyKey", idempotencyKey);
      m.getMessageProperties().setHeader("channel", channel);
      return m;
    });
  }
}
