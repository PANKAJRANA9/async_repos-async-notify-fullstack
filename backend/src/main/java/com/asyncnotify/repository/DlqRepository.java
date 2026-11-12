package com.asyncnotify.repository;
import com.asyncnotify.model.DlqMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface DlqRepository extends JpaRepository<DlqMessage, Long> {
}
