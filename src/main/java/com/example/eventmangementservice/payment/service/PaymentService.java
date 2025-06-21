package com.example.eventmangementservice.payment.service;

import com.example.eventmangementservice.booking.model.Booking;
import com.example.eventmangementservice.booking.model.BookingStatus;
import com.example.eventmangementservice.booking.repository.BookingRepository;
import com.example.eventmangementservice.common.exception.BusinessException;
import com.example.eventmangementservice.common.exception.ResourceNotFoundException;
import com.example.eventmangementservice.common.outbox.OutboxMessage;
import com.example.eventmangementservice.common.outbox.OutboxRepository;
import com.example.eventmangementservice.common.outbox.OutboxStatus;
import com.example.eventmangementservice.payment.dto.PaymentCreateRequest;
import com.example.eventmangementservice.payment.dto.PaymentResponse;
import com.example.eventmangementservice.payment.gateway.PaymentGateway;
import com.example.eventmangementservice.payment.model.Payment;
import com.example.eventmangementservice.payment.model.PaymentMethod;
import com.example.eventmangementservice.payment.model.PaymentStatus;
import com.example.eventmangementservice.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final PaymentGateway paymentGateway;

    @Transactional
    public PaymentResponse initiatePayment(PaymentCreateRequest request, String userId) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", "id", request.getBookingId()));
        
        // Validate if the user is authorized to make payment
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("You are not authorized to make payment for this booking");
        }
        
        // Validate booking status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("Payment can only be initiated for pending bookings");
        }
        
        // Check if the booking has expired
        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Booking has expired. Please create a new booking");
        }
        
        // Check if payment already exists
        if (paymentRepository.findByBookingId(booking.getId()).isPresent()) {
            throw new BusinessException("Payment for this booking already exists");
        }
        
        // Create payment record
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getTotalAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentReference(generatePaymentReference());
        
        Payment savedPayment = paymentRepository.save(payment);
        
        // Call payment gateway to process payment
        String gatewayTransactionId = paymentGateway.processPayment(
                savedPayment.getPaymentReference(),
                savedPayment.getAmount(),
                request.getPaymentMethod(),
                request.getPaymentDetails()
        );
        
        savedPayment.setGatewayTransactionId(gatewayTransactionId);
        savedPayment = paymentRepository.save(savedPayment);
        
        createOutboxMessage("PAYMENT_INITIATED", savedPayment);
        
        return mapToPaymentResponse(savedPayment);
    }

    @Transactional
    public PaymentResponse completePayment(String paymentReference, String gatewayTransactionId, boolean success) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", "reference", paymentReference));
        
        payment.setGatewayTransactionId(gatewayTransactionId);
        
        if (success) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaymentDate(LocalDateTime.now());
            
            // Update booking status
            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            
            createOutboxMessage("PAYMENT_COMPLETED", payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            createOutboxMessage("PAYMENT_FAILED", payment);
        }
        
        Payment updatedPayment = paymentRepository.save(payment);
        return mapToPaymentResponse(updatedPayment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String reference) {
        Payment payment = paymentRepository.findByPaymentReference(reference)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", "reference", reference));
        
        return mapToPaymentResponse(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(UUID paymentId, String userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", "id", paymentId));
        
        // Validate if the user is authorized to request refund
        if (!payment.getBooking().getUserId().equals(userId)) {
            throw new BusinessException("You are not authorized to refund this payment");
        }
        
        // Validate payment status
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException("Only completed payments can be refunded");
        }
        
        // Call payment gateway to process refund
        boolean refundSuccess = paymentGateway.processRefund(
                payment.getGatewayTransactionId(),
                payment.getAmount()
        );
        
        if (refundSuccess) {
            payment.setStatus(PaymentStatus.REFUNDED);
            
            // Update booking status
            Booking booking = payment.getBooking();
            booking.setStatus(BookingStatus.REFUNDED);
            bookingRepository.save(booking);
            
            createOutboxMessage("PAYMENT_REFUNDED", payment);
        } else {
            throw new BusinessException("Failed to process refund");
        }
        
        Payment updatedPayment = paymentRepository.save(payment);
        return mapToPaymentResponse(updatedPayment);
    }

    private String generatePaymentReference() {
        return "PAY-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @SneakyThrows
    private void createOutboxMessage(String eventType, Payment payment) {
        OutboxMessage outboxMessage = OutboxMessage.builder()
                .aggregateType("PAYMENT")  // This determines it will go to "payments" topic
                .aggregateId(payment.getId().toString())
                .eventType(eventType)      // PAYMENT_INITIATED, PAYMENT_COMPLETED, PAYMENT_FAILED, PAYMENT_REFUNDED
                .payload(objectMapper.writeValueAsString(payment))
                .status(OutboxStatus.CREATED)
                .build();
        
        outboxRepository.save(outboxMessage);
        log.info("Created outbox message: {}", outboxMessage);
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBooking().getId())
                .bookingNumber(payment.getBooking().getBookingNumber())
                .paymentReference(payment.getPaymentReference())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .gatewayTransactionId(payment.getGatewayTransactionId())
                .paymentDate(payment.getPaymentDate())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
