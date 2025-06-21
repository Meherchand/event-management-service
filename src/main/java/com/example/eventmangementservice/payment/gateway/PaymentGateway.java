package com.example.eventmangementservice.payment.gateway;

import com.example.eventmangementservice.payment.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGateway {
    
    String processPayment(String reference, BigDecimal amount, PaymentMethod method, Map<String, String> details);
    
    boolean processRefund(String transactionId, BigDecimal amount);
    
    String getPaymentStatus(String transactionId);
}
