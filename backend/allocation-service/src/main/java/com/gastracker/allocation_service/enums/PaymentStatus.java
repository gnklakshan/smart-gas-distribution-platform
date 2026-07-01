package com.gastracker.allocation_service.enums;

public enum PaymentStatus {
    PENDING,    // PaymentIntent created, awaiting confirmation from Stripe
    PAID,       // Stripe confirmed payment_intent.succeeded
    FAILED      // Stripe confirmed payment_intent.payment_failed
}
