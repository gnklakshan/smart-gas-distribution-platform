package com.gastracker.allocation_service.dao.repository;

import com.gastracker.allocation_service.dao.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByAllocationId(String allocationId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
}
