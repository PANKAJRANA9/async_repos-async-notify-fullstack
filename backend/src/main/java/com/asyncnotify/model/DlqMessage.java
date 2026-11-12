package com.asyncnotify.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="dlq_message")
public class DlqMessage {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  private String idempotencyKey;
  @Column(length=2000) private String payload;
  @Column(length=2000) private String error;
  private LocalDateTime createdAt=LocalDateTime.now();
  private boolean retried=false;
  private LocalDateTime replayedAt;
  private String replayedBy;
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String k){this.idempotencyKey=k;}
  public String getPayload(){return payload;} public void setPayload(String p){this.payload=p;}
  public String getError(){return error;} public void setError(String e){this.error=e;}
  public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime t){this.createdAt=t;}
  public boolean isRetried(){return retried;} public void setRetried(boolean r){this.retried=r;}
  public LocalDateTime getReplayedAt(){return replayedAt;} public void setReplayedAt(LocalDateTime t){this.replayedAt=t;}
  public String getReplayedBy(){return replayedBy;} public void setReplayedBy(String s){this.replayedBy=s;}
}
