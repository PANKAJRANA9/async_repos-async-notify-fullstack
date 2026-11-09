package com.asyncnotify.service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
@Service @RequiredArgsConstructor
public class IdempotencyService {
  private final StringRedisTemplate redis;
  // SET NX EX 24h - returns true if first time, false if duplicate (prevents spam)
  public boolean acquire(String key){
    Boolean ok = redis.opsForValue().setIfAbsent("idem:"+key, "1", Duration.ofHours(24));
    return Boolean.TRUE.equals(ok);
  }
  public boolean exists(String key){ return Boolean.TRUE.equals(redis.hasKey("idem:"+key)); }
}
