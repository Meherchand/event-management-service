package com.example.eventmangementservice.payment.controller;

import com.example.eventmangementservice.common.dto.ApiResponse;
import com.example.eventmangementservice.payment.dto.PaymentCreateRequest;
import com.example.eventmangementservice.payment.dto.PaymentResponse;
import com.example.eventmangementservice.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody PaymentCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        PaymentResponse payment = paymentService.initiatePayment(request, userDetails.getUsername());
        return new ResponseEntity<>(ApiResponse.success("Payment initiated successfully", payment), HttpStatus.CREATED);
    }

    @GetMapping("/{reference}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByReference(
            @PathVariable String reference) {
        PaymentResponse payment = paymentService.getPaymentByReference(reference);
        return ResponseEntity.ok(ApiResponse.success(payment));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        PaymentResponse payment = paymentService.refundPayment(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", payment));
    }

    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<PaymentResponse>> paymentWebhook(
            @RequestBody Map<String, Object> webhookPayload) {
        // Extract necessary information from webhook payload
        String paymentReference = (String) webhookPayload.get("paymentReference");
        String gatewayTransactionId = (String) webhookPayload.get("transactionId");
        boolean success = (boolean) webhookPayload.get("success");
        
        PaymentResponse payment = paymentService.completePayment(paymentReference, gatewayTransactionId, success);
        return ResponseEntity.ok(ApiResponse.success("Payment status updated", payment));
    }
}
