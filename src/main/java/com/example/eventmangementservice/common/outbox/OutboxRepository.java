package com.example.eventmangementservice.common.outbox;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxMessage, UUID> {
    
    List<OutboxMessage> findByProcessedFalseAndStatusNotOrderByCreatedAt(OutboxStatus status);
    
    @Query("SELECT o FROM OutboxMessage o WHERE o.processed = false AND o.status <> 'FAILED' ORDER BY o.createdAt")
    Page<OutboxMessage> findUnprocessedMessages(Pageable pageable);
    
    @Modifying
    @Query("UPDATE OutboxMessage o SET o.processed = true, o.processedAt = :now, o.status = 'PROCESSED' WHERE o.id = :id")
    void markAsProcessed(UUID id, LocalDateTime now);
    
    @Modifying
    @Query("UPDATE OutboxMessage o SET o.retryCount = o.retryCount + 1, o.status = :status WHERE o.id = :id")
    void updateRetryCount(UUID id, OutboxStatus status);
}
