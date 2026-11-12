package com.asyncnotify.metrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicLong;
@Service
public class MetricsService {
  private final Counter sentCounter; private final Counter dedupCounter; private final Counter retryCounter; private final Counter dlqCounter;
  private final AtomicLong outboxPending=new AtomicLong(0);
  public MetricsService(MeterRegistry registry){
    sentCounter=Counter.builder("async_notify_sent_total").register(registry);
    dedupCounter=Counter.builder("async_notify_dedup_total").register(registry);
    retryCounter=Counter.builder("async_notify_retry_total").register(registry);
    dlqCounter=Counter.builder("async_notify_dlq_total").register(registry);
    Gauge.builder("async_notify_outbox_pending", outboxPending, AtomicLong::get).register(registry);
  }
  public void incSent(){ sentCounter.increment(); }
  public void incDedup(){ dedupCounter.increment(); }
  public void incRetry(){ retryCounter.increment(); }
  public void incDlq(){ dlqCounter.increment(); }
  public void setOutboxPending(long v){ outboxPending.set(v); }
}
