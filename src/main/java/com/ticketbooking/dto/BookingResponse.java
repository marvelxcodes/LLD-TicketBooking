package com.ticketbooking.dto;

import com.ticketbooking.enums.BookingStatus;
import com.ticketbooking.enums.SeatCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for booking response.
 * Contains complete booking information including tickets and pricing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private String bookingReference;
    private Long userId;
    private String username;
    private Long eventId;
    private String eventName;
    private List<TicketDetail> tickets;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private LocalDateTime bookingTime;
    private LocalDateTime expiryTime;
    private PaymentResponse payment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketDetail {
        private Long id;
        private String ticketNumber;
        private String seatNumber;
        private String row;
        private SeatCategory category;
        private BigDecimal price;
    }
}
