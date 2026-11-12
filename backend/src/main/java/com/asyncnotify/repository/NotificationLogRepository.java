package com.asyncnotify.repository;
import com.asyncnotify.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
  Optional<NotificationLog> findByIdempotencyKey(String key);
}
