package com.gastracker.allocation_service.enums;

public enum AllocationStatus {
    PENDING,    // dealer submitted, waiting for admin
    APPROVED,   // admin approved, awaiting delivery
    REJECTED,   // admin rejected
    DELIVERED   // dealer confirmed receipt
}
