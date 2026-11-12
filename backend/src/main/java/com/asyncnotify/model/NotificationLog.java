package com.asyncnotify.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="notification_log", uniqueConstraints=@UniqueConstraint(columnNames="idempotency_key"))
public class NotificationLog {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(name="idempotency_key", nullable=false, unique=true) private String idempotencyKey;
  private String channel; private String userId; private String template;
  private String status="QUEUED"; private int attempts=0;
  private String lastError; private LocalDateTime nextRetryAt;
  private LocalDateTime createdAt=LocalDateTime.now();
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String k){this.idempotencyKey=k;}
  public String getChannel(){return channel;} public void setChannel(String c){this.channel=c;}
  public String getUserId(){return userId;} public void setUserId(String u){this.userId=u;}
  public String getTemplate(){return template;} public void setTemplate(String t){this.template=t;}
  public String getStatus(){return status;} public void setStatus(String s){this.status=s;}
  public int getAttempts(){return attempts;} public void setAttempts(int a){this.attempts=a;}
  public String getLastError(){return lastError;} public void setLastError(String e){this.lastError=e;}
  public LocalDateTime getNextRetryAt(){return nextRetryAt;} public void setNextRetryAt(LocalDateTime t){this.nextRetryAt=t;}
  public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
}
