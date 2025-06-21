package com.example.eventmangementservice.payment.repository;

import com.example.eventmangementservice.payment.model.Payment;
import com.example.eventmangementservice.payment.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    Optional<Payment> findByPaymentReference(String reference);
    
    Optional<Payment> findByBookingId(UUID bookingId);
    
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
    
    long countByStatus(PaymentStatus status);
}
