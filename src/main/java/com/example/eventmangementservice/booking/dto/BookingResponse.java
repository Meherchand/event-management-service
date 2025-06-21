package com.example.eventmangementservice.booking.dto;

import com.example.eventmangementservice.booking.model.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    
    private UUID id;
    private String bookingNumber;
    private UUID eventId;
    private String eventName;
    private String userId;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime expiresAt;
    private List<BookingItemDto> items;
    private LocalDateTime createdAt;
    private PaymentInfoDto payment;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingItemDto {
        private UUID id;
        private UUID ticketTypeId;
        private String ticketTypeName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInfoDto {
        private UUID id;
        private String paymentReference;
        private BigDecimal amount;
        private String status;
        private String paymentMethod;
        private LocalDateTime paymentDate;
    }
}
