package com.gastracker.queue_service.enums;

public enum QueueStatus {
    WAITING,            // citizen is in the queue
    READY_FOR_PICKUP,   // dealer marks citizen ready
    COMPLETED,          // pickup done
    CANCELLED           // cancelled by citizen or dealer
}
