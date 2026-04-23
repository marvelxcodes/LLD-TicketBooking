package com.ticketbooking.dto;

import com.ticketbooking.enums.EventType;
import com.ticketbooking.enums.SeatCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for Event response.
 * Provides a flattened view of event data including seat pricing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private String name;
    private String description;
    private String venue;
    private String city;
    private EventType eventType;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private Integer totalSeats;
    private Integer availableSeats;
    private boolean active;
    private Long organizerId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
