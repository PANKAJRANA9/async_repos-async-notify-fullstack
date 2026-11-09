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
  public static final String EXCHANGE="notify.exchange";
  public static final String EMAIL_Q="email.queue";
  public static final String PUSH_Q="push.queue";
  public static final String SMS_Q="sms.queue";
  public static final String DLQ_EX="dlq.exchange";
  @Bean TopicExchange exchange(){return new TopicExchange(EXCHANGE);}
  @Bean TopicExchange dlqExchange(){return new TopicExchange(DLQ_EX);}
  @Bean Queue emailQ(){return QueueBuilder.durable(EMAIL_Q).withArgument("x-dead-letter-exchange",DLQ_EX).build();}
  @Bean Queue pushQ(){return QueueBuilder.durable(PUSH_Q).withArgument("x-dead-letter-exchange",DLQ_EX).build();}
  @Bean Queue smsQ(){return QueueBuilder.durable(SMS_Q).withArgument("x-dead-letter-exchange",DLQ_EX).build();}
  @Bean Binding b1(){return BindingBuilder.bind(emailQ()).to(exchange()).with("notify.email");}
  @Bean Binding b2(){return BindingBuilder.bind(pushQ()).to(exchange()).with("notify.push");}
  @Bean Binding b3(){return BindingBuilder.bind(smsQ()).to(exchange()).with("notify.sms");}
  @Bean Jackson2JsonMessageConverter converter(){return new Jackson2JsonMessageConverter();}
  @Bean RetryOperationsInterceptor retryInterceptor(RabbitTemplate tpl){
    return RetryInterceptorBuilder.stateless().maxAttempts(5)
      .backOffOptions(1000,2.0,16000) // 1s,2s,4s,8s,16s + jitter via listener
      .recoverer(new org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer(tpl, DLQ_EX)).build();
  }
  @Bean SimpleRabbitListenerContainerFactory factory(ConnectionFactory cf, RetryOperationsInterceptor ri){
    SimpleRabbitListenerContainerFactory f=new SimpleRabbitListenerContainerFactory();
    f.setConnectionFactory(cf); f.setMessageConverter(converter()); f.setAdviceChain(ri); f.setDefaultRequeueRejected(false); return f;
  }
}
