package com.example.eventmangementservice.payment.dto;

import com.example.eventmangementservice.payment.model.PaymentMethod;
import com.example.eventmangementservice.payment.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    private UUID id;
    private UUID bookingId;
    private String bookingNumber;
    private String paymentReference;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentMethod paymentMethod;
    private String gatewayTransactionId;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
}
