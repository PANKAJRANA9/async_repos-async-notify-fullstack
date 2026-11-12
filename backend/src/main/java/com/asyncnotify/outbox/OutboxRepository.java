package com.asyncnotify.outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long>{
  List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(String status);
  long countByStatus(String status);
}
