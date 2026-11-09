package com.asyncnotify.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="notification_log", uniqueConstraints=@UniqueConstraint(columnNames="idempotency_key"))
public class NotificationLog{ @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(name="idempotency_key", nullable=false) String idempotencyKey; String channel; String status; int attempts; LocalDateTime nextRetryAt; LocalDateTime createdAt; }