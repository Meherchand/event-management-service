package com.example.eventmangementservice.common.outbox;

public enum OutboxStatus {
    CREATED,
    PROCESSING,
    PROCESSED,
    FAILED
}
