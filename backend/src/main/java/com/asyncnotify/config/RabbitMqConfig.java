package com.asyncnotify.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
@Configuration
public class RabbitMqConfig {
  @Bean TopicExchange exchange(){ return new TopicExchange("notify.exchange"); }
  @Bean TopicExchange dlqExchange(){ return new TopicExchange("dlq.exchange"); }
  @Bean Queue emailQ(){ return QueueBuilder.durable("email.queue").withArgument("x-dead-letter-exchange","dlq.exchange").build(); }
  @Bean Queue pushQ(){ return QueueBuilder.durable("push.queue").withArgument("x-dead-letter-exchange","dlq.exchange").build(); }
  @Bean Queue smsQ(){ return QueueBuilder.durable("sms.queue").withArgument("x-dead-letter-exchange","dlq.exchange").build(); }
  @Bean Queue dlqQ(){ return QueueBuilder.durable("notify.dlq").build(); }
  @Bean Binding b1(){ return BindingBuilder.bind(emailQ()).to(exchange()).with("notify.email"); }
  @Bean Binding b2(){ return BindingBuilder.bind(pushQ()).to(exchange()).with("notify.push"); }
  @Bean Binding b3(){ return BindingBuilder.bind(smsQ()).to(exchange()).with("notify.sms"); }
  @Bean Binding bDlq(){ return BindingBuilder.bind(dlqQ()).to(dlqExchange()).with("#"); }
  @Bean Jackson2JsonMessageConverter converter(){ return new Jackson2JsonMessageConverter(); }
  @Bean RetryOperationsInterceptor retryInterceptor(RabbitTemplate tpl){
    return RetryInterceptorBuilder.stateless().maxAttempts(5).backOffOptions(1000,2.0,16000)
      .recoverer(new org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer(tpl,"dlq.exchange")).build();
  }
  @Bean SimpleRabbitListenerContainerFactory factory(ConnectionFactory cf, RetryOperationsInterceptor ri){
    SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
    f.setConnectionFactory(cf); f.setMessageConverter(converter()); f.setAdviceChain(ri); f.setDefaultRequeueRejected(false); return f;
  }
}
