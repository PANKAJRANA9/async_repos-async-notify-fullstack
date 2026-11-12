package com.asyncnotify.outbox;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="outbox_event")
public class OutboxEvent {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  private String aggregateId; private String eventType; @Column(length=2000) private String payload; private String status="PENDING";
  private int retryCount=0; private LocalDateTime createdAt=LocalDateTime.now(); private LocalDateTime publishedAt;
  public Long getId(){return id;} public void setId(Long v){id=v;}
  public String getAggregateId(){return aggregateId;} public void setAggregateId(String v){aggregateId=v;}
  public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;}
  public String getPayload(){return payload;} public void setPayload(String v){payload=v;}
  public String getStatus(){return status;} public void setStatus(String v){status=v;}
  public int getRetryCount(){return retryCount;} public void setRetryCount(int v){retryCount=v;}
  public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
  public LocalDateTime getPublishedAt(){return publishedAt;} public void setPublishedAt(LocalDateTime v){publishedAt=v;}
}
