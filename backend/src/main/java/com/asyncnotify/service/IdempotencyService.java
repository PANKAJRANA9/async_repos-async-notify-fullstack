package com.asyncnotify.service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Set;
@Service
public class IdempotencyService {
  private final StringRedisTemplate redis;
  public IdempotencyService(StringRedisTemplate redis){ this.redis = redis; }
  public boolean acquire(String key){
    Boolean ok = redis.opsForValue().setIfAbsent("idem:"+key, "1", Duration.ofHours(24));
    return Boolean.TRUE.equals(ok);
  }
  public boolean exists(String key){ return Boolean.TRUE.equals(redis.hasKey("idem:"+key)); }
  public long dedupCount(){
    Set<String> keys = redis.keys("idem:*");
    return keys==null ? 0 : keys.size();
  }
  public void releaseForReplay(String key){ redis.delete("idem:"+key); }
}
